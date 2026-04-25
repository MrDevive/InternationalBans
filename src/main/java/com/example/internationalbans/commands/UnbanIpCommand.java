package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class UnbanIpCommand implements CommandExecutor {

    private final InternationalBans plugin;

    public UnbanIpCommand(InternationalBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.unbanip")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.unbanip.usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        String ip = null;
        if (target.isOnline() && target.getPlayer() != null) {
            ip = target.getPlayer().getAddress().getAddress().getHostAddress();
        } else {
            var punishments = plugin.getBanManager().getPlayerPunishments(target.getUniqueId());
            for (var punishment : punishments) {
                if ((punishment.getType().equals("ipban") || punishment.getType().equals("tempipban"))
                        && punishment.isActive() && !punishment.getIp().isEmpty()) {
                    ip = punishment.getIp();
                    break;
                }
            }
        }

        if (ip == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.not-banned"));
            return true;
        }

        plugin.getBanManager().removeActivePunishmentsByIp(ip, "ipban");
        plugin.getBanManager().removeActivePunishmentsByIp(ip, "tempipban");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("staff", sender.getName());

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.unbanip.success", placeholders));

        String broadcastMsg = plugin.getMessageManager().getMessage("broadcast.unbanip", placeholders);
        Bukkit.broadcast(broadcastMsg, "internationalbans.notify");

        plugin.getBanManager().logToStaff("UNBAN-IP", target.getName(), sender.getName(), "Снятие IP бана");

        return true;
    }
}