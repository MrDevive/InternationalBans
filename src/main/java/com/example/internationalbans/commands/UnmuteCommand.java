package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UnmuteCommand implements CommandExecutor {

    private final InternationalBans plugin;

    public UnmuteCommand(InternationalBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.unmute")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.unmute.usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!plugin.getBanManager().isMuted(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.not-muted"));
            return true;
        }

        UUID uuid = target.getUniqueId();

        plugin.getBanManager().removeActivePunishments(uuid, "mute");
        plugin.getBanManager().removeActivePunishments(uuid, "tempmute");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("staff", sender.getName());

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.unmute.success", placeholders));

        String broadcastMsg = plugin.getMessageManager().getMessage("broadcast.unmute", placeholders);
        Bukkit.broadcast(broadcastMsg, "internationalbans.notify");

        plugin.getBanManager().logToStaff("UNMUTE", target.getName(), sender.getName(), "Снятие мута");

        return true;
    }
}