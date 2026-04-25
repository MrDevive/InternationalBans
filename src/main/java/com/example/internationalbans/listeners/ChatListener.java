package com.example.internationalbans.listeners;

import com.example.internationalbans.InternationalBans;
import com.example.internationalbans.models.Punishment;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class ChatListener implements Listener {

    private final InternationalBans plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public ChatListener(InternationalBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        if (plugin.getBanManager().isMuted(event.getPlayer().getUniqueId())) {
            Punishment mute = plugin.getBanManager().getActiveMute(event.getPlayer().getUniqueId());

            if (mute != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", mute.getReason());

                Component message;
                if (!mute.isPermanent()) {
                    placeholders.put("time", mute.getRemainingTime());
                    String msg = plugin.getMessageManager().getMessage("chat.muted", placeholders);
                    message = serializer.deserialize(msg);
                } else {
                    String msg = plugin.getMessageManager().getMessage("chat.muted-permanent", placeholders);
                    message = serializer.deserialize(msg);
                }

                event.getPlayer().sendMessage(message);
                event.setCancelled(true);
            }
        }
    }
}