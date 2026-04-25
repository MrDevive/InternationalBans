package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ReloadCommand implements CommandExecutor {

    private final InternationalBans plugin;
    private long lastReloadTime = 0;

    public ReloadCommand(InternationalBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            performReload(sender);
            return true;
        }

        Player player = (Player) sender;
        int cooldown = plugin.getPluginConfig().getInt("command-settings.reload-cooldown", 5);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastReloadTime < cooldown * 1000L && !player.hasPermission("internationalbans.reload.bypass")) {
            long remaining = (cooldown * 1000L - (currentTime - lastReloadTime)) / 1000;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("seconds", String.valueOf(remaining));
            player.sendMessage(plugin.getMessageManager().getMessage("commands.reload.cooldown", placeholders));
            return true;
        }

        boolean requireConfirm = plugin.getPluginConfig().getBoolean("command-settings.require-confirm-reload", false);

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            performReload(sender);
            lastReloadTime = currentTime;
        } else if (requireConfirm) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload.confirm"));
        } else {
            performReload(sender);
            lastReloadTime = currentTime;
        }

        return true;
    }

    private void performReload(CommandSender sender) {
        long startTime = System.currentTimeMillis();

        try {
            // Используем новый метод reloadAllConfigs()
            plugin.reloadAllConfigs();

            // Перезагрузка базы данных
            plugin.getBanManager().load();

            long duration = System.currentTimeMillis() - startTime;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(duration));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload.success", placeholders));

            plugin.getLogger().info("Плагин перезагружен " +
                    (sender instanceof Player ? "игроком " + sender.getName() : "из консоли") +
                    " за " + duration + "ms");

        } catch (Exception e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload.fail"));
            plugin.getLogger().severe("Ошибка перезагрузки: " + e.getMessage());
            e.printStackTrace();
        }
    }
}