package org.tytyber.tytyber_race.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.attribute.AttributeInstance;

import java.util.*;
import java.util.stream.Collectors;

public class RaceManager {
    private final FileConfiguration raceConfig;
    private final Map<String, String> playerRaceMap = new HashMap<>();
    static final UUID modifierUUID = UUID.fromString("f3512a3f-4fc3-4e4d-8b33-bcc20fe0e9a9"); // твой фиксированный UUID

    public RaceManager(FileConfiguration raceConfig) {
        this.raceConfig = raceConfig;
        // Загружаем уже установленные расы игроков из конфига
        if (raceConfig.contains("players")) {
            for (String key : raceConfig.getConfigurationSection("players").getKeys(false)) {
                String race = raceConfig.getString("players." + key);
                playerRaceMap.put(key, race);
            }
        }
    }

    public boolean setPlayerRace(String uuid, String raceName) {
        // Проверяем, есть ли такая раса в конфиге (раса задается как корневой ключ, напр.: "elf")
        if (!raceConfig.contains(raceName)) {
            return false;
        }
        // Если всё ок, сохраняем выбор в карту
        playerRaceMap.put(uuid, raceName);
        // Обновляем конфигурацию, чтобы сохранить выбор в секции players
        raceConfig.set("players." + uuid, raceName);
        return true;
    }

    public String getPlayerRace(String uuid) {
        return playerRaceMap.get(uuid);
    }

    public List<String> getPlayersByRace(String raceName) {
        return playerRaceMap.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(raceName))
                .map(entry -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                    return (offlinePlayer.getName() != null ? offlinePlayer.getName() : entry.getKey());
                })
                .collect(Collectors.toList());
    }

    public void createRacePrefix(Player player, String race) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(race);

        if (team == null) {
            team = scoreboard.registerNewTeam(race);
            team.setPrefix(race);
        }
        team.addEntry(player.getName());
    }

    public void applyEffects(Player player) {
        String uuid = player.getUniqueId().toString();
        String raceName = getPlayerRace(uuid);
        if (raceName == null) return; // раса не выбрана

        World world = player.getWorld();
        long time = world.getTime();
        // Условно: ночь между 13000 и 23000
        boolean isNight = (time >= 13000 && time <= 23000);

        // Определяем путь к секции с эффектами
        String effectsSection = raceName + ".effects." + (isNight ? "night_effects" : "day_effects");
        // Для дневных эффектов проверяем, требуется ли наличие солнца
        if (!isNight && raceConfig.contains(effectsSection + ".sun")) {
            boolean sunRequired = raceConfig.getBoolean(effectsSection + ".sun", false);
            if (sunRequired && !isUnderOpenSky(player)) {
                // Если солнце требуется, но игрок не под открытым небом – пропускаем применение эффектов.
                return;
            }
        }

        // Применяем эффекты: speed, jump, digging_speed, swim_speed, health, regen
        applyCustomEffect(player, effectsSection, "speed");
        applyCustomEffect(player, effectsSection, "jump");
        applyCustomEffect(player, effectsSection, "digging_speed");
        applyCustomEffect(player, effectsSection, "swim_speed");
        applyCustomEffect(player, effectsSection, "health");
        applyCustomEffect(player, effectsSection, "regen");
    }


    private void applyCustomEffect(Player player, String configSection, String effectName) {
        if (!raceConfig.contains(configSection + "." + effectName)) return;
        int value = raceConfig.getInt(configSection + "." + effectName, 0);
        if (value == 0) return;

        if (effectName.equals("health")) {
            AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) {
                boolean alreadyApplied = attribute.getModifiers().stream()
                        .anyMatch(mod -> mod.getUniqueId().equals(modifierUUID));

                if (!alreadyApplied) {
                    AttributeModifier modifier = new AttributeModifier(modifierUUID, "ElfBoost", 4.0, AttributeModifier.Operation.ADD_NUMBER);
                    attribute.addModifier(modifier);
                }
            }
            return;
        }

        // Для остальных эффектов используем PotionEffect
        PotionEffectType effectType = null;
        switch (effectName) {
            case "speed":
                effectType = (value > 0) ? PotionEffectType.SPEED : PotionEffectType.SLOWNESS;
                break;
            case "jump":
                if (value > 0){
                    effectType = PotionEffectType.JUMP_BOOST;
                }
                break;
            case "digging_speed":
                effectType = (value > 0) ? PotionEffectType.HASTE : PotionEffectType.MINING_FATIGUE;
                break;
            case "swim_speed":
                if (value > 0) {
                    effectType = PotionEffectType.DOLPHINS_GRACE;
                }
                break;
            case "regen":
                effectType = (value > 0) ? PotionEffectType.REGENERATION : PotionEffectType.WITHER;
                break;
            default:
                break;
        }
        if (effectType == null) return;

        int amplifier = Math.max(Math.abs(value) - 1, 0);
        int duration = 240; // примерно 12 секунд
        PotionEffect potionEffect = new PotionEffect(effectType, duration, amplifier, true, false, false);
        player.addPotionEffect(potionEffect);
    }

    private boolean isUnderOpenSky(Player player) {
        Location loc = player.getLocation();
        int playerY = loc.getBlockY();
        int highestY = player.getWorld().getHighestBlockAt(loc).getY();
        return playerY >= highestY;
    }

    public void handleEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        if (!(event.getTarget() instanceof Player)) return;

        Player player = (Player) event.getTarget();
        String race = getPlayerRace(player.getUniqueId().toString());
        if (race == null) return;

        if (raceConfig.getBoolean(race + ".evil", false)) {
            event.setCancelled(true);
        }
    }
}
