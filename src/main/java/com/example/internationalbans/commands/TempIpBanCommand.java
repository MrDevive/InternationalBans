package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempIpBanCommand extends BasePunishmentCommand {

    public TempIpBanCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.tempipban")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cИспользование: /tempipban <игрок> <время> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/tempipban Player 30d 4.1.1 §7- IP бан по ID причины");
            sender.sendMessage("§e/tempipban Player 7d DDoS §7- IP бан с текстом");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.player-not-found"));
            return true;
        }

        if (target.hasPermission("internationalbans.bypass")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.cannot-ban"));
            return true;
        }

        String ip = target.getAddress() != null && target.getAddress().getAddress() != null
                ? target.getAddress().getAddress().getHostAddress() : "0.0.0.0";

        if (plugin.getBanManager().isIpBanned(ip)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.already-banned"));
            return true;
        }

        String timeArg = args[1];
        String reasonArg = args[2];

        return handlePunishment(sender, target, timeArg, reasonArg, "tempipban", true);
    }
}