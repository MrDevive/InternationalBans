package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickCommand extends BasePunishmentCommand {

    public KickCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.kick")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /kick <игрок> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/kick Player 6.1 §7- кик по ID причины");
            sender.sendMessage("§e/kick Player Флуд §7- кик с текстом");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.player-not-found"));
            return true;
        }

        String reasonArg = args[1];

        return handlePunishment(sender, target, null, reasonArg, "kick", false);
    }
}