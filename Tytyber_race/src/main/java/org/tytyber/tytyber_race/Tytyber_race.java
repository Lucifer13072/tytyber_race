package org.tytyber.tytyber_race;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.tytyber.tytyber_race.manager.RaceConfigSave;
import org.tytyber.tytyber_race.manager.RaceDataStore;
import org.tytyber.tytyber_race.manager.RaceManager;
import org.tytyber.tytyber_race.command.RaceCommand;

import java.io.File;

public class Tytyber_race extends JavaPlugin {
    private RaceManager raceManager;
    private RaceConfigSave raceConfigSave;
    private RaceDataStore raceDataStore;

    @Override
    public void onEnable() {
        // сохраняем дефолтные ресурсы
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Сохраняем дефолтный config.yml только если его ещё нет
        File raceFile = new File(getDataFolder(), "config.yml");
        if (!raceFile.exists()) {
            saveResource("config.yml", false);
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        // 1) DataStore + Manager + ConfigSave
        this.raceDataStore = new RaceDataStore(getDataFolder(), getConfig());
        this.raceManager   = new RaceManager(getConfig(), raceDataStore);
        getServer().getPluginManager().registerEvents(raceManager, this);
        this.raceConfigSave= new RaceConfigSave(raceDataStore);


        // 2) Создаём единый экземпляр команды
        RaceCommand executor = new RaceCommand(this, raceManager, raceConfigSave);

        // 3) Регистрируем все команды на этот же экземпляр
        getCommand("race").setExecutor(executor);
        getCommand("raceset").setExecutor(executor);
        getCommand("raceprefix").setExecutor(executor);
        getCommand("racereload").setExecutor(executor);

        getLogger().info("[TytyberRace] STARTED!");

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getOnlinePlayers().forEach(p -> raceManager.applyEffects(p));
        }, 0L, 100L);
    }
    public RaceManager getRaceManager() {
        return raceManager;
    }

    public RaceConfigSave getRaceConfigSave() {
        return raceConfigSave;
    }

    public RaceDataStore getRaceDataStore() {
        return raceDataStore;
    }
}
