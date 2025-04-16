package org.tytyber.tytyber_race.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.tytyber.tytyber_race.Tytyber_race;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RaceConfigSave {
    private final Tytyber_race plugin;
    private final File playerDataFile;
    private FileConfiguration playerData;

    private final Map<UUID, String> playerRaces = new HashMap<>();


    public RaceConfigSave(Tytyber_race plugin) {
        this.plugin = plugin;
        this.playerDataFile = new File(plugin.getDataFolder(), "players.yml");
        this.playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        loadData();
    }

    public boolean setPlayerRace(String uuidString, String race) {
        UUID uuid = UUID.fromString(uuidString);
        if (!plugin.getConfig().contains("races." + race)) {
            return false;
        }

        // Проверка на изменение расы
        if (playerRaces.containsKey(uuid) && playerRaces.get(uuid).equals(race)) {
            return false;  // Если раса не изменилась, ничего не делаем
        }

        playerRaces.put(uuid, race);
        playerData.set("players." + uuid.toString(), race);
        saveData();
        return true;
    }

    public String getPlayerRace(String uuidString) {
        UUID uuid = UUID.fromString(uuidString);
        return playerRaces.get(uuid);
    }

    public List<String> getPlayersByRace(String race) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerRaces.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(race)) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                list.add(name != null ? name : entry.getKey().toString());
            }
        }
        return list;
    }

    public void loadData() {
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().warning("[TytyberRace] Ошибка при создании файла players.yml!");
                e.printStackTrace();
            }
            return; // если файла нет, выходим
        }

        if (playerData.contains("players")) {
            for (String key : playerData.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String race = playerData.getString("players." + key);
                    playerRaces.put(uuid, race);
                } catch (IllegalArgumentException ignored) {
                    Bukkit.getLogger().warning("[TytyberRace] Невозможно загрузить UUID: " + key);
                }
            }
        }
    }

    public void saveData() {
        try {
            playerData.save(playerDataFile);
            Bukkit.getLogger().info("[TytyberRace] Данные успешно сохранены в players.yml!");
        } catch (IOException e) {
            Bukkit.getLogger().warning("[TytyberRace] Ошибка при сохранении players.yml!");
            e.printStackTrace();
        }
    }
}
