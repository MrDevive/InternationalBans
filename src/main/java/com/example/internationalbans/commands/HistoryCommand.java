package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import com.example.internationalbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class HistoryCommand implements CommandExecutor {

    private final InternationalBans plugin;

    public HistoryCommand(InternationalBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.history")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.history.usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        List<Punishment> punishments = plugin.getBanManager().getPlayerPunishments(target.getUniqueId());

        if (punishments.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.history.empty", placeholders));
            return true;
        }

        String sortOrder = plugin.getPluginConfig().getString("history.sort-order", "newest_first");
        int maxEntries = plugin.getPluginConfig().getInt("history.max-entries", 50);

        List<Punishment> sortedList = new ArrayList<>(punishments);
        if (sortOrder.equals("newest_first")) {
            sortedList.sort((a, b) -> Long.compare(b.getTime(), a.getTime()));
        } else {
            sortedList.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));
        }

        if (sortedList.size() > maxEntries) {
            sortedList = sortedList.subList(0, maxEntries);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        sender.sendMessage(plugin.getMessageManager().getMessage("commands.history.header", placeholders));

        for (Punishment punishment : sortedList) {
            String status = punishment.isActive() && !punishment.isExpired() ?
                    plugin.getMessageManager().getMessage("commands.history.active") :
                    plugin.getMessageManager().getMessage("commands.history.expired");

            Map<String, String> msgPlaceholders = new HashMap<>();
            msgPlaceholders.put("type", punishment.getTypeDisplay());
            msgPlaceholders.put("staff", punishment.getStaffName());
            msgPlaceholders.put("reason", punishment.getReason());
            msgPlaceholders.put("date", punishment.getFormattedDate());
            msgPlaceholders.put("status", status);

            sender.sendMessage(plugin.getMessageManager().getMessage("commands.history.format", msgPlaceholders));
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.history.footer"));

        return true;
    }
}