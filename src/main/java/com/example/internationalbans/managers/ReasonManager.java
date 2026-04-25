package com.example.internationalbans.managers;

import com.example.internationalbans.InternationalBans;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReasonManager {

    private final InternationalBans plugin;
    private final Map<String, Reason> reasons;
    private File reasonsFile;
    private YamlConfiguration reasonsConfig;

    public ReasonManager(InternationalBans plugin) {
        this.plugin = plugin;
        this.reasons = new LinkedHashMap<>();
        load();
    }

    public void load() {
        reasonsFile = new File(plugin.getDataFolder(), "reasons.yml");
        if (!reasonsFile.exists()) {
            plugin.saveResource("reasons.yml", false);
        }
        reasonsConfig = YamlConfiguration.loadConfiguration(reasonsFile);
        loadReasons();

        plugin.getLogger().info("Загружено причин: " + reasons.size());
    }

    private void loadReasons() {
        reasons.clear();

        if (reasonsConfig.contains("reasons")) {
            for (String key : reasonsConfig.getConfigurationSection("reasons").getKeys(false)) {
                loadReasonRecursive(key, "");
            }
        }
    }

    private void loadReasonRecursive(String currentKey, String parentPath) {
        String fullPath = parentPath.isEmpty() ? currentKey : parentPath + "." + currentKey;

        // Проверяем, есть ли у этого пути name
        if (reasonsConfig.contains("reasons." + fullPath + ".name")) {
            String name = reasonsConfig.getString("reasons." + fullPath + ".name");
            String description = reasonsConfig.getString("reasons." + fullPath + ".description", "");

            Reason reason = new Reason(fullPath, name, description);
            reasons.put(fullPath, reason);
        }

        // Рекурсивно загружаем вложенные причины
        if (reasonsConfig.contains("reasons." + fullPath)) {
            for (String subKey : reasonsConfig.getConfigurationSection("reasons." + fullPath).getKeys(false)) {
                if (!subKey.equals("name") && !subKey.equals("description")) {
                    loadReasonRecursive(subKey, fullPath);
                }
            }
        }
    }

    public Reason getReason(String id) {
        return reasons.get(id);
    }

    public boolean isReasonExists(String id) {
        return reasons.containsKey(id);
    }

    public Map<String, Reason> getAllReasons() {
        return reasons;
    }

    public String getAllReasonIds() {
        if (reasons.isEmpty()) return "нет доступных причин";
        return String.join("§7, §e", reasons.keySet());
    }

    public long parseTimeString(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return -1;

        timeStr = timeStr.trim().toLowerCase();

        if (timeStr.equals("-1") || timeStr.equals("perm") || timeStr.equals("permanent")) {
            return -1;
        }

        long totalMillis = 0;
        Pattern pattern = Pattern.compile("(\\d+)([smhdwM])");
        Matcher matcher = pattern.matcher(timeStr);

        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s": totalMillis += TimeUnit.SECONDS.toMillis(amount); break;
                case "m": totalMillis += TimeUnit.MINUTES.toMillis(amount); break;
                case "h": totalMillis += TimeUnit.HOURS.toMillis(amount); break;
                case "d": totalMillis += TimeUnit.DAYS.toMillis(amount); break;
                case "w": totalMillis += TimeUnit.DAYS.toMillis(amount * 7); break;
                case "M": totalMillis += TimeUnit.DAYS.toMillis(amount * 30); break;
            }
        }

        return totalMillis > 0 ? totalMillis : -1;
    }

    public String formatDuration(long millis) {
        if (millis == -1) return "навсегда";

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м ");

        String result = sb.toString().trim();
        return result.isEmpty() ? "0м" : result;
    }

    public static class Reason {
        private final String id;
        private final String name;
        private final String description;

        public Reason(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
}