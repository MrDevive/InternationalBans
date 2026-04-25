package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IpBanCommand extends BasePunishmentCommand {

    public IpBanCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.ipban")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /ipban <игрок> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/ipban Player 5.1 §7- IP бан по ID причины");
            sender.sendMessage("§e/ipban Player DDoS угрозы §7- IP бан с текстом");
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

        String reasonArg = args[1];

        return handlePunishment(sender, target, null, reasonArg, "ipban", false);
    }
}