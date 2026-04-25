package com.example.internationalbans.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Punishment {

    private String type;
    private String playerName;
    private UUID playerUUID;
    private String staffName;
    private String reason;
    private long time;
    private long duration;
    private boolean active;
    private String ip;

    public Punishment(String type, String playerName, UUID playerUUID, String staffName,
                      String reason, long time, long duration, String ip) {
        this.type = type;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.staffName = staffName;
        this.reason = reason;
        this.time = time;
        this.duration = duration;
        this.active = true;
        this.ip = ip;
    }

    public String getType() { return type; }
    public String getPlayerName() { return playerName; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getStaffName() { return staffName; }
    public String getReason() { return reason; }
    public long getTime() { return time; }
    public long getDuration() { return duration; }
    public boolean isActive() { return active; }
    public String getIp() { return ip; }

    public void setActive(boolean active) { this.active = active; }

    public boolean isPermanent() {
        return duration == -1;
    }

    public boolean isExpired() {
        if (isPermanent()) return false;
        return System.currentTimeMillis() > time + duration;
    }

    public String getRemainingTime() {
        if (isPermanent()) return "навсегда";
        long remaining = (time + duration) - System.currentTimeMillis();
        if (remaining <= 0) return "истекло";

        long days = remaining / 86400000;
        long hours = (remaining % 86400000) / 3600000;
        long minutes = (remaining % 3600000) / 60000;
        long seconds = (remaining % 60000) / 1000;

        if (days > 0) {
            return String.format("%dд %dч %dм", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dч %dм", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dм %dс", minutes, seconds);
        } else {
            return String.format("%dс", seconds);
        }
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(new Date(time));
    }

    public String getTypeDisplay() {
        switch (type) {
            case "ban": return "Бан";
            case "tempban": return "Временный бан";
            case "ipban": return "IP-бан";
            case "tempipban": return "Временный IP-бан";
            case "mute": return "Мут";
            case "tempmute": return "Временный мут";
            case "warn": return "Предупреждение";
            case "kick": return "Кик";
            default: return type;
        }
    }

    public boolean isBanType() {
        return type.equals("ban") || type.equals("tempban") ||
                type.equals("ipban") || type.equals("tempipban");
    }
}