package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempBanCommand extends BasePunishmentCommand {

    public TempBanCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.tempban")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cИспользование: /tempban <игрок> <время> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/tempban Player 7d 1.1.1 §7- бан по ID причины");
            sender.sendMessage("§e/tempban Player 7d Гриферство §7- бан с текстом");
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

        if (plugin.getBanManager().isBanned(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.already-banned"));
            return true;
        }

        String timeArg = args[1];
        String reasonArg = args[2];

        return handlePunishment(sender, target, timeArg, reasonArg, "tempban", true);
    }
}