package org.tytyber.tytyber_race;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.tytyber.tytyber_race.command.RaceCommand;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.tytyber.tytyber_race.manager.RaceManager;
import org.tytyber.tytyber_race.manager.RaceConfigSave;

import java.io.File;
import java.io.IOException;

public final class Tytyber_race extends JavaPlugin {

    private File raceConfigFile;
    private File saveConfigFile;
    private FileConfiguration raceConfig;
    private RaceManager raceManager;
    private RaceConfigSave raceConfigSave;

    @Override
    public void onEnable() {
        // Загружаем/инициализируем race.yml
        createRaceConfig();
        this.raceConfigSave = new RaceConfigSave(this);
        // Инициализируем RaceManager
        raceManager = new RaceManager(raceConfig);

        // Регистрируем команду /race
        getCommand("race").setExecutor(new RaceCommand(this, raceManager, raceConfigSave));

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> raceManager.applyEffects(player));
        }, 0L, 100L);
        getServer().getPluginManager().registerEvents(new Listener(){
            @EventHandler
            public void onEntityTarget(EntityTargetEvent event) {
                raceManager.handleEntityTarget(event);
            }
        }, this);
        getLogger().info("RacePlugin успешно загружен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RacePlugin отключён");
    }

    public void createRaceConfig() {
        raceConfigFile = new File(getDataFolder(), "race.yml");
        saveConfigFile = new File(getDataFolder(), "players.yml");
        if (!raceConfigFile.exists()) {
            // Создаем папку плагина, если её нет
            raceConfigFile.getParentFile().mkdirs();
            saveResource("race.yml", false);
            saveResource("players.yml", false);
        }
        raceConfig = YamlConfiguration.loadConfiguration(raceConfigFile);
    }

    public FileConfiguration getRaceConfig() {
        return raceConfig;
    }

    public void saveRaceConfig() {
        try {
            raceConfig.save(raceConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
