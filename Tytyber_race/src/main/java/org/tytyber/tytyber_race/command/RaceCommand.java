package org.tytyber.tytyber_race.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tytyber.tytyber_race.Tytyber_race;
import org.tytyber.tytyber_race.manager.RaceManager;
import org.tytyber.tytyber_race.manager.RaceConfigSave;
import org.tytyber.tytyber_race.manager.RaceDataStore;

public class RaceCommand implements CommandExecutor {
    private final Tytyber_race plugin;
    private final RaceManager raceManager;
    private final RaceConfigSave raceConfigSave;

    public RaceCommand(Tytyber_race plugin, RaceManager raceManager, RaceConfigSave raceConfigSave) {
        this.plugin = plugin;
        this.raceManager = raceManager;
        this.raceConfigSave = raceConfigSave;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "race":
                return handleRace(sender, args);
            case "raceset":
                return handleRaceSet(sender, args);
            case "raceprefix":
                return handleRacePrefix(sender, args);
            case "racereload":
                return handleReload(sender);
            default:
                return false;
        }
    }

    private boolean handleRace(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игрок может выбрать расу.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Использование: /race [раса]");
            return true;
        }
        String race = args[0];
        String current = raceManager.getPlayerRace(player.getUniqueId().toString());
        if (current != null) {
            player.sendMessage("Ты уже выбрал расу: " + current);
            return true;
        }
        boolean success = raceConfigSave.setPlayerRace(player.getUniqueId().toString(), race);
        if (success) {
            player.sendMessage("Ваша раса установлена: " + race);
            raceManager.applyEffects(player);
        } else {
            player.sendMessage("Ошибка: неизвестная раса или уже установлена.");
        }
        return true;
    }

    private boolean handleRaceSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("race.admin")) {
            sender.sendMessage("У тебя нет прав.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage("Использование: /raceset [раса] [ник]");
            return true;
        }
        String race = args[0];
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("Игрок не найден: " + targetName);
            return true;
        }
        boolean success = raceConfigSave.setPlayerRace(target.getUniqueId().toString(), race);
        if (success) {
            target.sendMessage("Твоя раса установлена: " + race);
            sender.sendMessage("Установлена раса " + race + " для " + targetName);
            raceManager.applyEffects(target);
        } else {
            sender.sendMessage("Ошибка при установке расы.");
        }
        return true;
    }

    private boolean handleRacePrefix(CommandSender sender, String[] args) {
        if (!sender.hasPermission("race.admin")) {
            sender.sendMessage("У тебя нет прав.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage("Использование: /raceprefix [префикс] [ник]");
            return true;
        }
        String prefix = args[0];
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("Игрок не найден: " + targetName);
            return true;
        }
        // Создаем/обновляем префикс
        raceManager.createRacePrefix(target, prefix);
        sender.sendMessage("Префикс " + prefix + " установлен для " + targetName);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("race.admin")) {
            sender.sendMessage("У тебя нет прав.");
            return true;
        }
        plugin.reloadConfig();
        // Если добавили метод reloadPlayers():
        sender.sendMessage("Конфигы (config.yml и players.yml) перезагружены.");
        return true;
    }
}