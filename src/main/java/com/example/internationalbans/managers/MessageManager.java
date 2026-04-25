package com.example.internationalbans.managers;

import com.example.internationalbans.InternationalBans;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private final InternationalBans plugin;
    private final Map<String, String> messages;
    private File messageFile;
    private YamlConfiguration messageConfig;

    public MessageManager(InternationalBans plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        load();
    }

    public void load() {
        messageFile = new File(plugin.getDataFolder(), "message.yml");
        if (!messageFile.exists()) {
            plugin.saveResource("message.yml", false);
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        loadMessages();
    }

    private void loadMessages() {
        messages.clear();
        for (String key : messageConfig.getKeys(true)) {
            if (messageConfig.isString(key)) {
                messages.put(key, messageConfig.getString(key));
            }
        }
    }

    public String getMessage(String key) {
        String message = messages.getOrDefault(key, "&cСообщение не найдено: " + key);
        return translateColors(message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messages.getOrDefault(key, "&cСообщение не найдено: " + key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return translateColors(message);
    }

    private String translateColors(String message) {
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hexColor).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public void reload() {
        load();
    }
}