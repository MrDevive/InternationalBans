package com.example.internationalbans.managers;

import com.example.internationalbans.InternationalBans;
import com.example.internationalbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BanManager {

    private final InternationalBans plugin;
    private DatabaseManager databaseManager;
    private final Map<UUID, List<Punishment>> punishments;
    private final Map<String, List<Punishment>> ipPunishments;
    private File banFile;
    private YamlConfiguration banConfig;
    private boolean useMySQL;

    public BanManager(InternationalBans plugin) {
        this.plugin = plugin;
        this.punishments = new ConcurrentHashMap<>();
        this.ipPunishments = new ConcurrentHashMap<>();
        this.useMySQL = plugin.getDatabaseConfig().getBoolean("mysql.enabled", false);

        if (useMySQL) {
            this.databaseManager = new DatabaseManager(plugin);
            loadFromDatabase();
        } else {
            loadFromYaml();
        }
    }

    private void loadFromDatabase() {
        databaseManager.loadAllPunishments().thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getLogger().info("Данные загружены из MySQL"));
        });
    }

    private void loadFromYaml() {
        banFile = new File(plugin.getDataFolder(), "ban_base.yml");
        if (!banFile.exists()) {
            plugin.saveResource("ban_base.yml", false);
        }
        banConfig = YamlConfiguration.loadConfiguration(banFile);
        loadYamlPunishments();
    }

    private void loadYamlPunishments() {
        punishments.clear();
        ipPunishments.clear();

        if (!banConfig.contains("punishments")) return;

        for (String key : banConfig.getConfigurationSection("punishments").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверный UUID: " + key);
                continue;
            }

            List<Map<?, ?>> punishmentList = banConfig.getMapList("punishments." + key);
            List<Punishment> playerPunishments = new ArrayList<>();

            for (Map<?, ?> map : punishmentList) {
                // Безопасное получение значений
                String type = getStringFromMap(map, "type", "unknown");
                String playerName = getStringFromMap(map, "playerName", "unknown");
                String staffName = getStringFromMap(map, "staffName", "Console");
                String reason = getStringFromMap(map, "reason", "Не указана");
                String ip = getStringFromMap(map, "ip", "");
                long time = getLongFromMap(map, "time", System.currentTimeMillis());
                long duration = getLongFromMap(map, "duration", -1L);
                boolean active = getBooleanFromMap(map, "active", true);

                Punishment punishment = new Punishment(type, playerName, uuid, staffName,
                        reason, time, duration, ip);
                punishment.setActive(active);
                playerPunishments.add(punishment);

                if ((type.equals("ipban") || type.equals("tempipban")) && active && !ip.isEmpty()) {
                    ipPunishments.computeIfAbsent(ip, k -> new ArrayList<>()).add(punishment);
                }
            }
            punishments.put(uuid, playerPunishments);
        }

        plugin.getLogger().info("Загружено наказаний из YAML: " + countAllPunishments());
    }

    // Вспомогательные методы для безопасного извлечения значений из Map

    private String getStringFromMap(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value.toString();
    }

    private long getLongFromMap(Map<?, ?> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanFromMap(Map<?, ?> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String str = value.toString().toLowerCase();
        return str.equals("true") || str.equals("yes") || str.equals("1");
    }

    private int countAllPunishments() {
        return punishments.values().stream().mapToInt(List::size).sum();
    }

    public void addPunishment(Punishment punishment) {
        if (useMySQL) {
            databaseManager.addPunishment(punishment);
        }

        punishments.computeIfAbsent(punishment.getPlayerUUID(), k -> new ArrayList<>()).add(punishment);

        if ((punishment.getType().equals("ipban") || punishment.getType().equals("tempipban"))
                && !punishment.getIp().isEmpty() && punishment.isActive()) {
            ipPunishments.computeIfAbsent(punishment.getIp(), k -> new ArrayList<>()).add(punishment);
        }

        if (!useMySQL) {
            saveToYaml();
        }
    }

    public boolean isBanned(UUID uuid) {
        if (useMySQL) {
            return databaseManager.isBanned(uuid);
        }
        return !getActivePunishments(uuid, "ban").isEmpty() ||
                !getActivePunishments(uuid, "tempban").isEmpty() ||
                !getActivePunishments(uuid, "ipban").isEmpty() ||
                !getActivePunishments(uuid, "tempipban").isEmpty();
    }

    public boolean isIpBanned(String ip) {
        if (useMySQL) {
            return databaseManager.isIpBanned(ip);
        }
        if (ip == null || ip.isEmpty()) return false;
        List<Punishment> ipBans = ipPunishments.getOrDefault(ip, new ArrayList<>());
        for (Punishment punishment : ipBans) {
            if ((punishment.getType().equals("ipban") || punishment.getType().equals("tempipban"))
                    && punishment.isActive() && !punishment.isExpired()) {
                return true;
            } else if (punishment.isExpired()) {
                punishment.setActive(false);
                if (!useMySQL) saveToYaml();
            }
        }
        return false;
    }

    public Punishment getActiveBan(UUID uuid) {
        if (useMySQL) {
            return databaseManager.getActiveBan(uuid);
        }
        List<Punishment> bans = getActivePunishments(uuid, "ban");
        if (!bans.isEmpty()) return bans.get(0);
        bans = getActivePunishments(uuid, "tempban");
        if (!bans.isEmpty()) return bans.get(0);
        bans = getActivePunishments(uuid, "ipban");
        if (!bans.isEmpty()) return bans.get(0);
        bans = getActivePunishments(uuid, "tempipban");
        return bans.isEmpty() ? null : bans.get(0);
    }

    public List<Punishment> getPlayerPunishments(UUID uuid) {
        if (useMySQL) {
            return databaseManager.getPlayerPunishments(uuid);
        }
        return punishments.getOrDefault(uuid, new ArrayList<>());
    }

    public List<Punishment> getActivePunishments(UUID uuid, String type) {
        List<Punishment> active = new ArrayList<>();
        List<Punishment> playerPuns = punishments.getOrDefault(uuid, new ArrayList<>());

        for (Punishment punishment : playerPuns) {
            if (punishment.getType().equals(type) && punishment.isActive()) {
                if (punishment.isExpired()) {
                    punishment.setActive(false);
                    if (!useMySQL) saveToYaml();
                } else {
                    active.add(punishment);
                }
            }
        }
        return active;
    }

    public boolean isMuted(UUID uuid) {
        return !getActivePunishments(uuid, "mute").isEmpty() ||
                !getActivePunishments(uuid, "tempmute").isEmpty();
    }

    public Punishment getActiveMute(UUID uuid) {
        List<Punishment> mutes = getActivePunishments(uuid, "mute");
        if (!mutes.isEmpty()) return mutes.get(0);
        mutes = getActivePunishments(uuid, "tempmute");
        return mutes.isEmpty() ? null : mutes.get(0);
    }

    public Punishment getActiveIpBan(String ip) {
        if (useMySQL) {
            if (databaseManager.isIpBanned(ip)) {
                // В MySQL нужно получить конкретное наказание
                // Для простоты возвращаем null
            }
            return null;
        }
        if (ip == null || ip.isEmpty()) return null;
        List<Punishment> ipBans = ipPunishments.getOrDefault(ip, new ArrayList<>());
        for (Punishment punishment : ipBans) {
            if ((punishment.getType().equals("ipban") || punishment.getType().equals("tempipban"))
                    && punishment.isActive() && !punishment.isExpired()) {
                return punishment;
            }
        }
        return null;
    }

    public void removeActivePunishments(UUID uuid, String type) {
        if (useMySQL) {
            databaseManager.updatePunishmentStatus(uuid, type, false, "Console");
        }

        List<Punishment> playerPuns = punishments.getOrDefault(uuid, new ArrayList<>());
        for (Punishment punishment : playerPuns) {
            if (punishment.getType().equals(type) && punishment.isActive()) {
                punishment.setActive(false);
            }
        }

        if (!useMySQL) saveToYaml();
    }

    public void removeActivePunishmentsByIp(String ip, String type) {
        if (useMySQL) {
            // Поиск всех игроков с этим IP и снятие бана
        }

        for (List<Punishment> punList : punishments.values()) {
            for (Punishment punishment : punList) {
                if (punishment.getType().equals(type) && punishment.isActive() && ip.equals(punishment.getIp())) {
                    punishment.setActive(false);
                }
            }
        }

        if (ipPunishments.containsKey(ip)) {
            for (Punishment punishment : ipPunishments.get(ip)) {
                if (punishment.getType().equals(type) && punishment.isActive()) {
                    punishment.setActive(false);
                }
            }
        }

        if (!useMySQL) saveToYaml();
    }

    public int getWarningsCount(UUID uuid) {
        return getActivePunishments(uuid, "warn").size();
    }

    private void saveToYaml() {
        for (Map.Entry<UUID, List<Punishment>> entry : punishments.entrySet()) {
            List<Map<String, Object>> punishmentList = new ArrayList<>();
            for (Punishment punishment : entry.getValue()) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", punishment.getType());
                map.put("playerName", punishment.getPlayerName());
                map.put("playerUUID", punishment.getPlayerUUID().toString());
                map.put("staffName", punishment.getStaffName());
                map.put("reason", punishment.getReason());
                map.put("time", punishment.getTime());
                map.put("duration", punishment.getDuration());
                map.put("active", punishment.isActive());
                map.put("ip", punishment.getIp());
                punishmentList.add(map);
            }
            banConfig.set("punishments." + entry.getKey().toString(), punishmentList);
        }

        try {
            banConfig.save(banFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка сохранения ban_base.yml: " + e.getMessage());
        }
    }

    public void saveAll() {
        if (useMySQL) {
            databaseManager.cleanExpiredPunishments();
        } else {
            saveToYaml();
        }
    }

    public void load() {
        if (useMySQL) {
            loadFromDatabase();
        } else {
            loadFromYaml();
        }
    }

    public CompletableFuture<Map<String, Integer>> getStatistics() {
        if (useMySQL && databaseManager != null) {
            return databaseManager.getStatistics();
        }

        // YAML статистика
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("ban", 0);
        stats.put("tempban", 0);
        stats.put("ipban", 0);
        stats.put("tempipban", 0);
        stats.put("mute", 0);
        stats.put("tempmute", 0);
        stats.put("warn", 0);
        stats.put("active", 0);

        for (List<Punishment> punList : punishments.values()) {
            for (Punishment p : punList) {
                stats.put("total", stats.get("total") + 1);
                stats.put(p.getType(), stats.getOrDefault(p.getType(), 0) + 1);
                if (p.isActive() && !p.isExpired()) {
                    stats.put("active", stats.get("active") + 1);
                }
            }
        }

        stats.put("unique_players", punishments.size());

        return CompletableFuture.completedFuture(stats);
    }

    public void close() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public Map<UUID, List<Punishment>> getAllPunishments() {
        return punishments;
    }

    public void logToStaff(String action, String target, String staff, String reason) {
        String logMsg = String.format("[%s] %s: %s наказал %s (%s)",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()),
                action, staff, target, reason);

        File logFile = new File(plugin.getDataFolder(), "staff.log");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (java.io.FileWriter fw = new java.io.FileWriter(logFile, true);
                 java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
                bw.write(logMsg);
                bw.newLine();
            } catch (java.io.IOException e) {
                plugin.getLogger().warning("Не удалось записать в staff.log: " + e.getMessage());
            }
        });
    }
}