package org.tytyber.tytyber_race.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tytyber.tytyber_race.Tytyber_race;
import org.tytyber.tytyber_race.manager.RaceManager;
import org.tytyber.tytyber_race.manager.RaceConfigSave;

import java.util.List;

public class RaceCommand implements CommandExecutor {
    private final Tytyber_race plugin;
    private final RaceManager raceManager;
    private final RaceConfigSave raceConfigSave;


    public RaceCommand(Tytyber_race plugin, RaceManager raceManager,RaceConfigSave raceConfigSave) {
        this.plugin = plugin;
        this.raceManager = raceManager;
        this.raceConfigSave = raceConfigSave;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (raceConfigSave == null) {
            sender.sendMessage("Конфигурация расы не загружена. Обратись к Тутуберу.");
            return false;
        }
        // Без аргументов или неверное использование – отправляем справку
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // Обработка подкоманд:
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Эта команда доступна только игрокам.");
                return true;
            }
            
            // Проверяем, если игрок уже выбрал расу
            String currentRace = raceManager.getPlayerRace(player.getUniqueId().toString());
            if (currentRace != null) {
                player.sendMessage("Ты уже выбрал расу.");
                return true;
            }
            String raceName = args[0];

            // Сохраняем выбранную расу через RaceConfigSave
            boolean result = raceConfigSave.setPlayerRace(player.getUniqueId().toString(), raceName);
            if (result) {
                player.sendMessage("Поздравляю! Твоя раса: " + raceName);
            } else {
                player.sendMessage("Ошибка: возможно, такая раса не существует.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            // /race list [раса]
            if (!sender.hasPermission("race.admin")) {
                sender.sendMessage("У тебя нет прав на выполнение этой команды.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("Используй: /race list [раса]");
                return true;
            }
            String raceName = args[1];
            List<String> players = raceManager.getPlayersByRace(raceName);
            if (players.isEmpty()) {
                sender.sendMessage("Нет игроков с расой " + raceName);
            } else {
                sender.sendMessage("Игроки с расой " + raceName + ": " + String.join(", ", players));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("prefix") && args.length >= 2 && args[1].equalsIgnoreCase("create")) {
            // /race prefix create
            if (!sender.hasPermission("race.admin")) {
                sender.sendMessage("У тебя нет прав на выполнение этой команды.");
                return true;
            }
            // Создаем префикс для игрока в таб-листе, основываясь на его расе
            if (!(sender instanceof Player)) {
                sender.sendMessage("Эта команда доступна только игрокам.");
                return true;
            }
            String race = raceManager.getPlayerRace(player.getUniqueId().toString());
            if (race == null) {
                player.sendMessage("У тебя не установлена раса!");
                return true;
            }
            // Здесь нужно реализовать логику установки табпрефикса – например, через Scoreboard
            raceManager.createRacePrefix(player, race);
            player.sendMessage("Префикс для расы " + race + " создан!");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            // Проверка прав администратора
            if (!sender.hasPermission("race.admin")) {
                sender.sendMessage("У тебя нет прав на выполнение этой команды.");
                return true;
            }

            // Проверка на количество аргументов
            if (args.length != 2) {
                sender.sendMessage("Использование: /race set [ник] [раса]");
                return true;
            }

            // Получаем ник и расу из аргументов
            String targetName = args[1];
            String race = args[2];

            // Проверяем, существует ли игрок с таким ником
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer == null) {
                sender.sendMessage("Игрок с таким ником не найден.");
                return true;
            }


            // Устанавливаем расу игроку
            boolean result = raceConfigSave.setPlayerRace(targetPlayer.getUniqueId().toString(), race);
            if (result) {
                // Создаем префикс для игрока в табе
                raceManager.createRacePrefix(targetPlayer, race);
                targetPlayer.sendMessage("Твоя раса была установлена на: " + race);
                sender.sendMessage("Раса игрока " + targetName + " установлена на: " + race);
            } else {
                sender.sendMessage("Ошибка при установке расы для игрока " + targetName);
            }

            return true;
        }
        // Если команда просто /race [раса] – одноразовое назначение
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Эта команда доступна только игрокам.");
                return true;
            }
            // Проверяем, если игрок уже выбрал расу
            if (raceManager.getPlayerRace(player.getUniqueId().toString()) != null) {
                player.sendMessage("Ты уже выбрал расу.");
                return true;
            }
            String raceName = args[0];
            boolean result = raceManager.setPlayerRace(player.getUniqueId().toString(), raceName);
            if (result) {
                player.sendMessage("Поздравляю! Твоя раса: " + raceName);
            } else {
                player.sendMessage("Ошибка: возможно, такая раса не существует.");
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("Команды:");
        sender.sendMessage("/race [раса] - выбрать расу (однократно)");
        sender.sendMessage("/race set [ник] [раса] - установить расу игрока (админ)");
        sender.sendMessage("/race list [раса] - список игроков с расой (админ)");
        sender.sendMessage("/race prefix create - создать префикс в табе (админ)");
    }
}
