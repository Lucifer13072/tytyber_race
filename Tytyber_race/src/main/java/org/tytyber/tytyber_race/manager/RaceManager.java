package org.tytyber.tytyber_race.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.entity.Mob;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RaceManager implements Listener {
    private static final UUID modifierUUID = UUID.fromString("f3512a3f-4fc3-4e4d-8b33-bcc20fe0e9a9");

    private final FileConfiguration raceConfig;
    private final RaceDataStore dataStore;

    public RaceManager(FileConfiguration raceConfig, RaceDataStore dataStore) {
        this.raceConfig = raceConfig;
        this.dataStore = dataStore;
    }

    /** Основной метод применения эффектов */
    public void applyEffects(Player player) {
        clearRaceEffects(player);

        String race = dataStore.getPlayerRace(player.getUniqueId().toString());
        if (race == null) return;

        // === 1. Фиксируем базовый путь ===
        String base = raceConfig.contains("races." + race)
                ? "races." + race
                : race;

        World world = player.getWorld();
        long time = world.getTime();
        boolean isNight = time >= 13000 && time <= 23000;

        String section = base + ".effects." + (isNight ? "night_effects" : "day_effects");

        // проверка солнца
        if (!isNight && raceConfig.getBoolean(section + ".sun", false) && !isUnderOpenSky(player)) {
            return;
        }
        applyHealthModifier(player, section);

        applyPotionEffects(player, section);

    }

    /** Снимаем все ранее наложенные эффекты расы */
    public void clearRaceEffects(Player player) {
        // Убираем зелья
        for (PotionEffectType type : new PotionEffectType[]{
                PotionEffectType.SPEED, PotionEffectType.SLOWNESS,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.HASTE, PotionEffectType.MINING_FATIGUE,
                PotionEffectType.DOLPHINS_GRACE,
                PotionEffectType.REGENERATION, PotionEffectType.WITHER
        }) {
            player.removePotionEffect(type);
        }
        // Убираем модификатор здоровья
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.getModifiers().stream()
                    .filter(mod -> mod.getUniqueId().equals(modifierUUID))
                    .forEach(attr::removeModifier);
        }
    }

    /** Устанавливаем дополнительное здоровье по конфигу */
    private void applyHealthModifier(Player player, String section) {
        if (!raceConfig.contains(section + ".health")) return;
        double amount = raceConfig.getDouble(section + ".health", 0);
        if (amount == 0) return;

        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        // Снимаем старый модификатор
        attr.getModifiers().stream()
                .filter(mod -> mod.getUniqueId().equals(modifierUUID))
                .forEach(attr::removeModifier);

        // И добавляем новый
        AttributeModifier mod = new AttributeModifier(
                modifierUUID, "RaceHealthBoost", amount, AttributeModifier.Operation.ADD_NUMBER);
        attr.addModifier(mod);
    }

    /** Применяем остальные эффекты как зелья */
    private void applyPotionEffects(Player player, String section) {
        // speed
        applyOnePotion(player, section, "speed",
                val -> val > 0 ? PotionEffectType.SPEED : PotionEffectType.SLOWNESS);
        // jump
        applyOnePotion(player, section, "jump",
                val -> val > 0 ? PotionEffectType.JUMP_BOOST : null);
        // digging_speed
        applyOnePotion(player, section, "digging_speed",
                val -> val > 0 ? PotionEffectType.HASTE : PotionEffectType.MINING_FATIGUE);
        // swim_speed
        applyOnePotion(player, section, "swim_speed",
                val -> val > 0 ? PotionEffectType.DOLPHINS_GRACE : null);
        // regen
        applyOnePotion(player, section, "regen",
                val -> val > 0 ? PotionEffectType.REGENERATION : PotionEffectType.WITHER);
    }

    /** Универсальный метод для одного зелья */
    private void applyOnePotion(Player player, String section, String key, java.util.function.Function<Integer, PotionEffectType> mapper) {
        if (!raceConfig.contains(section + "." + key)) return;
        int val = raceConfig.getInt(section + "." + key, 0);
        if (val == 0) return;

        PotionEffectType type = mapper.apply(val);
        if (type == null) return;

        int amp = Math.max(Math.abs(val) - 1, 0);
        int dur = raceConfig.getInt(section + "." + key + "_duration", 240);
        player.addPotionEffect(new PotionEffect(type, dur, amp), true);
    }

    /** Проверяет, есть ли над головой небо */
    private boolean isUnderOpenSky(Player player) {
        Location loc = player.getLocation();
        return loc.getY() >= player.getWorld().getHighestBlockAt(loc).getY();
    }

    /** Prefix в табе */
    public void createRacePrefix(Player player, String race) {
        String base = raceConfig.contains("races." + race)
                ? "races." + race
                : race;

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = sb.getTeam(race);
        if (team == null) {
            team = sb.registerNewTeam(race);
        }

        // === 4. Префикс через base ===
        String prefix = raceConfig.getString(base + ".prefix", race + " ");
        team.setPrefix(prefix);
        team.addEntry(player.getName());
    }

    /** Защита от монстров для злых рас */
    public void handleEntityTarget(EntityTargetEvent e) {
        if (!(e.getEntity() instanceof Monster) || !(e.getTarget() instanceof Player)) return;
        Player pl = (Player) e.getTarget();
        String race = dataStore.getPlayerRace(pl.getUniqueId().toString());
        if (race == null) return;

        // === 5. Evil по base ===
        String base = raceConfig.contains("races." + race)
                ? "races." + race
                : race;

        if (raceConfig.getBoolean(base + ".evil", false)) {
            e.setCancelled(true);
        }
    }

    public List<String> getPlayersByRace(String race) {
        return dataStore.getPlayersByRace(race);
    }

    public boolean setPlayerRace(String uuid, String race) {
        return dataStore.setPlayerRace(uuid, race);
    }

    public String getPlayerRace(String uuid) {
        return dataStore.getPlayerRace(uuid);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        // Определяем имя расы и базовый путь (с учетом вложенности races.)
        String race = dataStore.getPlayerRace(player.getUniqueId().toString());
        if (race == null) return;
        String base = raceConfig.contains("races." + race) ? "races." + race : race;

        // Проверяем флаг evil
        if (raceConfig.getBoolean(base + ".evil", false)) {
            // Отменяем цель и принудительно сбрасываем
            event.setCancelled(true);
            mob.setTarget(null);
        }
    }
}
