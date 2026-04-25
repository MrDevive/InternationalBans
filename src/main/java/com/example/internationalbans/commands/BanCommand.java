package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanCommand extends BasePunishmentCommand {

    public BanCommand(InternationalBans plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("internationalbans.ban")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("errors.no-permission"));
            return true;
        }

        // Формат: /ban <игрок> <id причины или текст>
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /ban <игрок> <id причины или текст>");
            sender.sendMessage("§7Примеры:");
            sender.sendMessage("§e/ban Devive 1.1.1 §7- бан по ID причины");
            sender.sendMessage("§e/ban Devive Гриферство §7- бан с текстом");
            sender.sendMessage("§7Доступные ID: §e" + plugin.getReasonManager().getAllReasonIds());
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

        String reasonArg = args[1];

        return handlePunishment(sender, target, null, reasonArg, "ban", false);
    }
}