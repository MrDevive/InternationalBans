package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteCommand extends BasePunishmentCommand {

    public MuteCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.mute")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /mute <игрок> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/mute Player 3.1.1 §7- мут по ID причины");
            sender.sendMessage("§e/mute Player Оскорбления §7- мут с текстом");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.player-not-found"));
            return true;
        }

        if (target.hasPermission("internationalbans.bypass")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.cannot-mute"));
            return true;
        }

        if (plugin.getBanManager().isMuted(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.already-muted"));
            return true;
        }

        String reasonArg = args[1];

        return handlePunishment(sender, target, null, reasonArg, "mute", false);
    }
}