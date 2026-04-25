package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempMuteCommand extends BasePunishmentCommand {

    public TempMuteCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.tempmute")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cИспользование: /tempmute <игрок> <время> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/tempmute Player 1h 3.1.1 §7- мут по ID причины");
            sender.sendMessage("§e/tempmute Player 1h Оскорбления §7- мут с текстом");
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

        String timeArg = args[1];
        String reasonArg = args[2];

        return handlePunishment(sender, target, timeArg, reasonArg, "tempmute", true);
    }
}