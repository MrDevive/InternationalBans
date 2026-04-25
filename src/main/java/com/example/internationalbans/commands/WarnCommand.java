package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarnCommand extends BasePunishmentCommand {

    public WarnCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.warn")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /warn <игрок> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/warn Player 7.1 §7- предупреждение по ID причины");
            sender.sendMessage("§e/warn Player Нарушение §7- предупреждение с текстом");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.player-not-found"));
            return true;
        }

        String reasonArg = args[1];

        return handlePunishment(sender, target, null, reasonArg, "warn", false);
    }
}