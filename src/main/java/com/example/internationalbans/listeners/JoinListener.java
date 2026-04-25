package com.example.internationalbans.listeners;

import com.example.internationalbans.InternationalBans;
import com.example.internationalbans.models.Punishment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashMap;
import java.util.Map;

public class JoinListener implements Listener {

    private final InternationalBans plugin;

    public JoinListener(InternationalBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ip = event.getAddress().getHostAddress();

        if (plugin.getBanManager().isIpBanned(ip)) {
            Punishment ipBan = plugin.getBanManager().getActiveIpBan(ip);
            if (ipBan != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", ipBan.getReason());
                placeholders.put("staff", ipBan.getStaffName());

                String kickMessage;
                if (ipBan.getType().equals("tempipban")) {
                    placeholders.put("duration", ipBan.getRemainingTime());
                    kickMessage = plugin.getMessageManager().getMessage("kick.tempipban", placeholders);
                } else {
                    kickMessage = plugin.getMessageManager().getMessage("kick.ipban", placeholders);
                }

                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMessage);
                return;
            }
        }

        if (plugin.getBanManager().isBanned(player.getUniqueId())) {
            Punishment ban = plugin.getBanManager().getActiveBan(player.getUniqueId());
            if (ban != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", ban.getReason());
                placeholders.put("staff", ban.getStaffName());

                String kickMessage;
                if (ban.getType().equals("tempban")) {
                    placeholders.put("duration", ban.getRemainingTime());
                    kickMessage = plugin.getMessageManager().getMessage("kick.tempban", placeholders);
                } else {
                    kickMessage = plugin.getMessageManager().getMessage("kick.ban", placeholders);
                }

                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMessage);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getBanManager().isMuted(player.getUniqueId())) {
            Punishment mute = plugin.getBanManager().getActiveMute(player.getUniqueId());
            if (mute != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", mute.getReason());

                if (!mute.isPermanent()) {
                    placeholders.put("time", mute.getRemainingTime());
                    player.sendMessage(plugin.getMessageManager().getMessage("join.muted", placeholders));
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("join.muted-permanent", placeholders));
                }
            }
        }

        int warnings = plugin.getBanManager().getWarningsCount(player.getUniqueId());
        if (warnings > 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(warnings));
            player.sendMessage(plugin.getMessageManager().getMessage("join.warnings", placeholders));
        }
    }
}