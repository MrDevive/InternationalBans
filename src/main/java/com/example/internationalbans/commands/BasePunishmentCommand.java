package com.example.internationalbans.commands;

import com.example.internationalbans.InternationalBans;
import com.example.internationalbans.managers.ReasonManager;
import com.example.internationalbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public abstract class BasePunishmentCommand implements CommandExecutor {

    protected final InternationalBans plugin;

    public BasePunishmentCommand(InternationalBans plugin) {
        this.plugin = plugin;
    }

    protected boolean handlePunishment(CommandSender sender, Player target,
                                       String timeArg, String reasonArg,
                                       String defaultType, boolean requiresTime) {

        long finalDuration = -1;
        String finalType = defaultType;
        String finalReason;

        // Парсим время если команда требует время
        if (requiresTime) {
            if (timeArg == null) {
                sender.sendMessage("§cВы должны указать время!");
                sender.sendMessage("§7Форматы: §e1s, 1m, 1h, 1d, 1w, 1M, -1, perm");
                return false;
            }

            if (timeArg.equals("-1") || timeArg.equalsIgnoreCase("perm") || timeArg.equalsIgnoreCase("permanent")) {
                finalDuration = -1;
            } else {
                finalDuration = plugin.getReasonManager().parseTimeString(timeArg);
                if (finalDuration <= 0) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("errors.invalid-time"));
                    return false;
                }
            }

            // Для временных команд, если duration = -1, оставляем тип как есть (ban/ipban/mute)
            // Если duration > 0, меняем тип на временный
            if (finalDuration > 0) {
                if (finalType.equals("ban")) finalType = "tempban";
                if (finalType.equals("ipban")) finalType = "tempipban";
                if (finalType.equals("mute")) finalType = "tempmute";
            }
        } else {
            // Для перманентных команд (ban, ipban, mute, kick, warn) duration всегда -1
            finalDuration = -1;
        }

        // Проверяем, является ли аргумент причины ID
        ReasonManager.Reason reason = null;

        if (reasonArg != null && reasonArg.matches("^\\d+(\\.\\d+)*$")) {
            reason = plugin.getReasonManager().getReason(reasonArg);
            if (reason != null) {
                finalReason = reason.getName();
                sender.sendMessage("§7Использована причина: §e" + finalReason + " §7(ID: " + reasonArg + ")");
            } else {
                sender.sendMessage("§cПричина с ID '" + reasonArg + "' не найдена!");
                sender.sendMessage("§7Доступные ID: §e" + plugin.getReasonManager().getAllReasonIds());
                return false;
            }
        } else {
            finalReason = reasonArg != null ? reasonArg : "Нарушение правил";
        }

        String ip = "0.0.0.0";
        if (target.getAddress() != null && target.getAddress().getAddress() != null) {
            ip = target.getAddress().getAddress().getHostAddress();
        }

        Punishment punishment = new Punishment(
                finalType,
                target.getName(),
                target.getUniqueId(),
                sender.getName(),
                finalReason,
                System.currentTimeMillis(),
                finalDuration,
                ip
        );

        plugin.getBanManager().addPunishment(punishment);

        // Отправка сообщений
        sendPunishmentMessages(sender, target, finalType, finalReason, finalDuration);

        return true;
    }

    private void sendPunishmentMessages(CommandSender sender, Player target,
                                        String type, String reason, long duration) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("reason", reason != null ? reason : "Не указана");
        placeholders.put("staff", sender.getName());
        if (duration != -1) {
            placeholders.put("duration", plugin.getReasonManager().formatDuration(duration));
        }

        // Кик для банов
        if (type.contains("ban")) {
            String kickKey = type.equals("tempban") ? "kick.tempban" :
                    (type.equals("ipban") || type.equals("tempipban")) ? "kick.ipban" : "kick.ban";
            String kickMessage = plugin.getMessageManager().getMessage(kickKey, placeholders);
            if (kickMessage != null && !kickMessage.isEmpty()) {
                target.kickPlayer(kickMessage);
            } else {
                target.kickPlayer("§cВы были забанены!\n§7Причина: " + reason);
            }
        }
        // Мут
        else if (type.contains("mute")) {
            String notifyKey = type.equals("tempmute") ? "tempmute.notify" : "mute.notify";
            String notifyMessage = plugin.getMessageManager().getMessage(notifyKey, placeholders);
            target.sendMessage(notifyMessage);
        }
        // Варн
        else if (type.equals("warn")) {
            int warningsCount = plugin.getBanManager().getWarningsCount(target.getUniqueId());
            placeholders.put("warnings", String.valueOf(warningsCount));
            target.sendMessage(plugin.getMessageManager().getMessage("warn.notify", placeholders));
        }
        // Кик
        else if (type.equals("kick")) {
            String kickMessage = plugin.getMessageManager().getMessage("kick.kick", placeholders);
            if (kickMessage != null && !kickMessage.isEmpty()) {
                target.kickPlayer(kickMessage);
            } else {
                target.kickPlayer("§cВас выгнали с сервера!\n§7Причина: " + reason);
            }
        }

        // Бродкаст
        broadcastPunishment(sender, target, type, reason, duration);

        // Логирование
        plugin.getBanManager().logToStaff(type.toUpperCase(), target.getName(), sender.getName(), reason);

        // Сообщение отправителю
        sender.sendMessage("§aИгрок §e" + target.getName() + " §aнаказан. Причина: §e" + reason);

        // Автобан для варнов
        if (type.equals("warn")) {
            checkAutoBan(sender, target);
        }
    }

    private void broadcastPunishment(CommandSender sender, Player target,
                                     String type, String reason, long duration) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("reason", reason != null ? reason : "Не указана");
        placeholders.put("staff", sender.getName());
        if (duration != -1) {
            placeholders.put("duration", plugin.getReasonManager().formatDuration(duration));
        }

        String broadcastKey;
        switch (type) {
            case "tempban": broadcastKey = "broadcast.tempban"; break;
            case "ipban": broadcastKey = "broadcast.ipban"; break;
            case "tempipban": broadcastKey = "broadcast.tempipban"; break;
            case "mute": broadcastKey = "broadcast.mute"; break;
            case "tempmute": broadcastKey = "broadcast.tempmute"; break;
            case "kick": broadcastKey = "broadcast.kick"; break;
            case "warn": broadcastKey = "broadcast.warn"; break;
            default: broadcastKey = "broadcast.ban";
        }

        String message = plugin.getMessageManager().getMessage(broadcastKey, placeholders);
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcast(message, "internationalbans.notify");
        }
    }

    private void checkAutoBan(CommandSender sender, Player target) {
        int warningsCount = plugin.getBanManager().getWarningsCount(target.getUniqueId());
        int maxWarnings = plugin.getMaxWarnings();

        if (plugin.isAutoBanEnabled() && maxWarnings > 0 && warningsCount >= maxWarnings) {
            long duration = plugin.getAutoBanDuration();
            String reason = plugin.getAutoBanReason();
            String type = duration == -1 ? "ban" : "tempban";

            String ip = "0.0.0.0";
            if (target.getAddress() != null && target.getAddress().getAddress() != null) {
                ip = target.getAddress().getAddress().getHostAddress();
            }

            Punishment punishment = new Punishment(
                    type, target.getName(), target.getUniqueId(), "System",
                    reason, System.currentTimeMillis(), duration, ip
            );
            plugin.getBanManager().addPunishment(punishment);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            placeholders.put("reason", reason);
            placeholders.put("staff", "System");
            if (duration != -1) {
                placeholders.put("duration", plugin.getReasonManager().formatDuration(duration));
            }

            String kickKey = duration == -1 ? "kick.ban" : "kick.tempban";
            String kickMessage = plugin.getMessageManager().getMessage(kickKey, placeholders);
            if (kickMessage != null && !kickMessage.isEmpty()) {
                target.kickPlayer(kickMessage);
            } else {
                target.kickPlayer("§cВы были забанены!\n§7Причина: " + reason);
            }

            String broadcastMsg = plugin.getMessageManager().getMessage("broadcast." + type, placeholders);
            if (broadcastMsg != null && !broadcastMsg.isEmpty()) {
                Bukkit.broadcast(broadcastMsg, "internationalbans.notify");
            }

            plugin.getBanManager().logToStaff("AUTO-BAN", target.getName(), "System", reason);
        }
    }
}