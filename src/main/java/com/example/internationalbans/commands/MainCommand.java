package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainCommand implements CommandExecutor {

    private final InternationalBans plugin;

    public MainCommand(InternationalBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
            case "?":
                sendHelp(sender);
                break;

            case "info":
            case "version":
                sendInfo(sender);
                break;

            case "stats":
                sendStats(sender);
                break;

            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.unknown"));
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-header"));

        if (sender.hasPermission("internationalbans.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-reload"));
        }
        if (sender.hasPermission("internationalbans.ban")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-ban"));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-tempban"));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-unban"));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-ipban"));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-tempipban"));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-unbanip"));
        }
        if (sender.hasPermission("internationalbans.mute")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-mute"));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-tempmute"));
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-unmute"));
        }
        if (sender.hasPermission("internationalbans.kick")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-kick"));
        }
        if (sender.hasPermission("internationalbans.warn")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-warn"));
        }
        if (sender.hasPermission("internationalbans.history")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-history"));
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.help-footer"));
    }

    private void sendInfo(CommandSender sender) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("author", plugin.getDescription().getAuthors().toString());

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.info", placeholders));
    }

    private void sendStats(CommandSender sender) {
        if (!sender.hasPermission("internationalbans.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return;
        }

        plugin.getBanManager().getStatistics().thenAccept(stats -> {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("bans", String.valueOf(stats.getOrDefault("ban", 0) + stats.getOrDefault("tempban", 0)));
            placeholders.put("ipbans", String.valueOf(stats.getOrDefault("ipban", 0) + stats.getOrDefault("tempipban", 0)));
            placeholders.put("mutes", String.valueOf(stats.getOrDefault("mute", 0) + stats.getOrDefault("tempmute", 0)));
            placeholders.put("warns", String.valueOf(stats.getOrDefault("warn", 0)));
            placeholders.put("total", String.valueOf(stats.getOrDefault("total", 0)));
            placeholders.put("active", String.valueOf(stats.getOrDefault("active", 0)));
            placeholders.put("players", String.valueOf(stats.getOrDefault("unique_players", 0)));

            sender.sendMessage(plugin.getMessageManager().getMessage("commands.ib.stats", placeholders));
        }).exceptionally(e -> {
            sender.sendMessage("§cОшибка получения статистики");
            return null;
        });
    }
}