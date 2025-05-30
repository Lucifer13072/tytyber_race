package org.tytyber.tytyber_race.manager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RaceDataStore {
    private final File playerDataFile;
    private final FileConfiguration raceConfig;
    private final FileConfiguration playerData;
    private final Map<UUID, String> playerRaceMap = new HashMap<>();

    public RaceDataStore(File dataFolder, FileConfiguration raceConfig) {
        this.raceConfig = raceConfig;
        this.playerDataFile = new File(dataFolder, "players.yml");

        if (!playerDataFile.exists()) {
            try {
                dataFolder.mkdirs();
                playerDataFile.createNewFile();
                FileConfiguration tmp = YamlConfiguration.loadConfiguration(playerDataFile);
                tmp.set("players", new HashMap<String, String>());
                tmp.save(playerDataFile);
            } catch (IOException e) {
                Bukkit.getLogger().warning("[TytyberRace] Не удалось создать players.yml");
                e.printStackTrace();
            }
        }

        this.playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        loadFromFile();
    }

    private void loadFromFile() {
        if (!playerData.contains("players")) return;
        for (String key : playerData.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String race = playerData.getString("players." + key);
                if (race != null) {
                    playerRaceMap.put(uuid, race);
                }
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[TytyberRace] Ошибка UUID: " + key);
            }
        }
    }

    public void saveToFile() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[TytyberRace] Ошибка при сохранении players.yml!");
            e.printStackTrace();
        }
    }

    public boolean setPlayerRace(String uuidString, String race) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!raceConfig.contains("races." + race)) {
            return false;
        }

        String current = playerRaceMap.get(uuid);
        if (race.equals(current)) return false;

        playerRaceMap.put(uuid, race);
        playerData.set("players." + uuid.toString(), race);
        saveToFile();
        return true;
    }

    public String getPlayerRace(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            return playerRaceMap.get(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<String> getPlayersByRace(String race) {
        List<String> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerRaceMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(race)) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                players.add(player.getName() != null ? player.getName() : entry.getKey().toString());
            }
        }
        return players;
    }

}
