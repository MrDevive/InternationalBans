package com.example.internationalbans.managers;

import com.example.internationalbans.InternationalBans;
import com.example.internationalbans.models.Punishment;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final InternationalBans plugin;
    private HikariDataSource dataSource;
    private boolean useMySQL;
    private final String tableName;

    private final Map<UUID, List<Punishment>> punishmentsCache;
    private final Map<String, List<Punishment>> ipPunishmentsCache;
    private final Map<UUID, Boolean> bannedStatusCache;

    public DatabaseManager(InternationalBans plugin) {
        this.plugin = plugin;
        this.punishmentsCache = new ConcurrentHashMap<>();
        this.ipPunishmentsCache = new ConcurrentHashMap<>();
        this.bannedStatusCache = new ConcurrentHashMap<>();
        this.tableName = plugin.getDatabaseConfig().getString("mysql.table-prefix", "international_bans");
        this.useMySQL = plugin.getDatabaseConfig().getBoolean("mysql.enabled", false);

        if (useMySQL) {
            initMySQL();
        }
    }

    private void initMySQL() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=%s",
                    plugin.getDatabaseConfig().getString("mysql.host", "localhost"),
                    plugin.getDatabaseConfig().getInt("mysql.port", 3306),
                    plugin.getDatabaseConfig().getString("mysql.database", "minecraft"),
                    plugin.getDatabaseConfig().getBoolean("mysql.use-ssl", false),
                    plugin.getDatabaseConfig().getBoolean("mysql.allow-public-key-retrieval", true)
            ));
            config.setUsername(plugin.getDatabaseConfig().getString("mysql.username", "root"));
            config.setPassword(plugin.getDatabaseConfig().getString("mysql.password", ""));
            config.setMaximumPoolSize(plugin.getDatabaseConfig().getInt("mysql.pool.max-pool-size", 10));
            config.setMinimumIdle(plugin.getDatabaseConfig().getInt("mysql.pool.min-idle", 2));
            config.setConnectionTimeout(plugin.getDatabaseConfig().getLong("mysql.pool.connection-timeout", 30000));
            config.setIdleTimeout(plugin.getDatabaseConfig().getLong("mysql.pool.idle-timeout", 600000));
            config.setMaxLifetime(plugin.getDatabaseConfig().getLong("mysql.pool.max-lifetime", 1800000));
            config.setPoolName("InternationalBansPool");

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            createTables();
            plugin.getLogger().info("MySQL подключён успешно!");
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка подключения к MySQL: " + e.getMessage());
            useMySQL = false;
        }
    }

    private void createTables() {
        String createTable = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY," +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`player_name` VARCHAR(16) NOT NULL," +
                "`staff_name` VARCHAR(16) NOT NULL," +
                "`punishment_type` VARCHAR(20) NOT NULL," +
                "`reason` TEXT NOT NULL," +
                "`ip_address` VARCHAR(45) DEFAULT NULL," +
                "`time_issued` BIGINT NOT NULL," +
                "`duration` BIGINT NOT NULL DEFAULT -1," +
                "`is_active` BOOLEAN DEFAULT TRUE," +
                "`banned_status` BOOLEAN DEFAULT FALSE," +
                "`unbanned_at` BIGINT DEFAULT NULL," +
                "`unbanned_by` VARCHAR(16) DEFAULT NULL," +
                "INDEX `idx_player_uuid` (`player_uuid`)," +
                "INDEX `idx_ip_address` (`ip_address`)," +
                "INDEX `idx_is_active` (`is_active`)," +
                "INDEX `idx_banned_status` (`banned_status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createTable);
            plugin.getLogger().info("Таблица '" + tableName + "' создана/проверена");
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка создания таблицы: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource не инициализирован");
        }
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> addPunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO `" + tableName + "` " +
                    "(player_uuid, player_name, staff_name, punishment_type, reason, " +
                    "ip_address, time_issued, duration, is_active, banned_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, punishment.getPlayerUUID().toString());
                stmt.setString(2, punishment.getPlayerName());
                stmt.setString(3, punishment.getStaffName());
                stmt.setString(4, punishment.getType());
                stmt.setString(5, punishment.getReason());
                stmt.setString(6, punishment.getIp());
                stmt.setLong(7, punishment.getTime());
                stmt.setLong(8, punishment.getDuration());
                stmt.setBoolean(9, punishment.isActive());

                boolean bannedStatus = punishment.isBanType() && punishment.isActive();
                stmt.setBoolean(10, bannedStatus);

                stmt.executeUpdate();
                updateCache(punishment);

            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка добавления наказания: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> loadAllPunishments() {
        return CompletableFuture.runAsync(() -> {
            punishmentsCache.clear();
            ipPunishmentsCache.clear();
            bannedStatusCache.clear();

            String sql = "SELECT * FROM `" + tableName + "` ORDER BY time_issued DESC";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Punishment punishment = extractPunishment(rs);
                    UUID uuid = punishment.getPlayerUUID();

                    punishmentsCache.computeIfAbsent(uuid, k -> new ArrayList<>()).add(punishment);

                    if (punishment.getIp() != null && !punishment.getIp().isEmpty() &&
                            (punishment.getType().equals("ipban") || punishment.getType().equals("tempipban"))) {
                        ipPunishmentsCache.computeIfAbsent(punishment.getIp(), k -> new ArrayList<>()).add(punishment);
                    }

                    if (punishment.isBanType() && punishment.isActive() && !punishment.isExpired()) {
                        bannedStatusCache.put(uuid, true);
                    }
                }

                plugin.getLogger().info("Загружено наказаний: " + countAllPunishments());

            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка загрузки наказаний: " + e.getMessage());
            }
        });
    }

    private Punishment extractPunishment(ResultSet rs) throws SQLException {
        String type = rs.getString("punishment_type");
        String playerName = rs.getString("player_name");
        UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
        String staffName = rs.getString("staff_name");
        String reason = rs.getString("reason");
        long time = rs.getLong("time_issued");
        long duration = rs.getLong("duration");
        String ip = rs.getString("ip_address");
        boolean isActive = rs.getBoolean("is_active");

        Punishment punishment = new Punishment(type, playerName, playerUUID, staffName,
                reason, time, duration, ip != null ? ip : "");
        punishment.setActive(isActive);
        return punishment;
    }

    public CompletableFuture<Void> updatePunishmentStatus(UUID uuid, String punishmentType,
                                                          boolean isActive, String unbannedBy) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE `" + tableName + "` SET is_active = ?, unbanned_at = ?, unbanned_by = ? " +
                    "WHERE player_uuid = ? AND punishment_type = ? AND is_active = true";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setBoolean(1, isActive);
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(3, unbannedBy);
                stmt.setString(4, uuid.toString());
                stmt.setString(5, punishmentType);
                stmt.executeUpdate();

                updateBannedStatus(uuid);
                invalidateCache(uuid);

            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка обновления статуса: " + e.getMessage());
            }
        });
    }

    private void updateBannedStatus(UUID uuid) {
        String checkSql = "SELECT COUNT(*) as count FROM `" + tableName + "` " +
                "WHERE player_uuid = ? AND punishment_type IN ('ban', 'tempban', 'ipban', 'tempipban') " +
                "AND is_active = true AND (duration = -1 OR (time_issued + duration) > ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            stmt.setString(1, uuid.toString());
            stmt.setLong(2, System.currentTimeMillis());
            ResultSet rs = stmt.executeQuery();

            boolean isBanned = rs.next() && rs.getInt("count") > 0;

            String updateSql = "UPDATE `" + tableName + "` SET banned_status = ? WHERE player_uuid = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setBoolean(1, isBanned);
                updateStmt.setString(2, uuid.toString());
                updateStmt.executeUpdate();
            }

            bannedStatusCache.put(uuid, isBanned);

        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка обновления banned_status: " + e.getMessage());
        }
    }

    public boolean isBanned(UUID uuid) {
        if (bannedStatusCache.containsKey(uuid)) {
            return bannedStatusCache.get(uuid);
        }

        String sql = "SELECT COUNT(*) as count FROM `" + tableName + "` " +
                "WHERE player_uuid = ? AND punishment_type IN ('ban', 'tempban', 'ipban', 'tempipban') " +
                "AND is_active = true AND (duration = -1 OR (time_issued + duration) > ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setLong(2, System.currentTimeMillis());
            ResultSet rs = stmt.executeQuery();

            boolean banned = rs.next() && rs.getInt("count") > 0;
            bannedStatusCache.put(uuid, banned);
            return banned;

        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка проверки бана: " + e.getMessage());
            return false;
        }
    }

    public boolean isIpBanned(String ip) {
        String sql = "SELECT COUNT(*) as count FROM `" + tableName + "` " +
                "WHERE ip_address = ? AND punishment_type IN ('ipban', 'tempipban') " +
                "AND is_active = true AND (duration = -1 OR (time_issued + duration) > ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ip);
            stmt.setLong(2, System.currentTimeMillis());
            ResultSet rs = stmt.executeQuery();

            return rs.next() && rs.getInt("count") > 0;

        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка проверки IP бана: " + e.getMessage());
            return false;
        }
    }

    public Punishment getActiveBan(UUID uuid) {
        List<Punishment> punishments = punishmentsCache.getOrDefault(uuid, new ArrayList<>());
        for (Punishment p : punishments) {
            if (p.isBanType() && p.isActive() && !p.isExpired()) {
                return p;
            }
        }
        return null;
    }

    public List<Punishment> getPlayerPunishments(UUID uuid) {
        return new ArrayList<>(punishmentsCache.getOrDefault(uuid, new ArrayList<>()));
    }

    private void updateCache(Punishment punishment) {
        UUID uuid = punishment.getPlayerUUID();
        punishmentsCache.computeIfAbsent(uuid, k -> new ArrayList<>()).add(punishment);

        if (punishment.getIp() != null && !punishment.getIp().isEmpty() &&
                (punishment.getType().equals("ipban") || punishment.getType().equals("tempipban"))) {
            ipPunishmentsCache.computeIfAbsent(punishment.getIp(), k -> new ArrayList<>()).add(punishment);
        }

        if (punishment.isBanType() && punishment.isActive()) {
            bannedStatusCache.put(uuid, true);
        }
    }

    private void invalidateCache(UUID uuid) {
        punishmentsCache.remove(uuid);
        bannedStatusCache.remove(uuid);

        String sql = "SELECT * FROM `" + tableName + "` WHERE player_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            List<Punishment> punishments = new ArrayList<>();
            while (rs.next()) {
                punishments.add(extractPunishment(rs));
            }
            punishmentsCache.put(uuid, punishments);

        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка инвалидации кэша: " + e.getMessage());
        }
    }

    public CompletableFuture<Map<String, Integer>> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> stats = new HashMap<>();
            stats.put("total", 0);
            stats.put("bans", 0);
            stats.put("tempbans", 0);
            stats.put("ipbans", 0);
            stats.put("tempipbans", 0);
            stats.put("mutes", 0);
            stats.put("tempmutes", 0);
            stats.put("warns", 0);
            stats.put("active", 0);
            stats.put("unique_players", 0);

            String sql = "SELECT punishment_type, is_active, COUNT(DISTINCT player_uuid) as unique_players, COUNT(*) as total " +
                    "FROM `" + tableName + "` GROUP BY punishment_type, is_active WITH ROLLUP";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    String type = rs.getString("punishment_type");
                    boolean isActive = rs.getBoolean("is_active");
                    int count = rs.getInt("total");

                    if (type == null) {
                        stats.put("total", count);
                        stats.put("unique_players", rs.getInt("unique_players"));
                    } else {
                        stats.put(type, stats.getOrDefault(type, 0) + count);
                        if (isActive) {
                            stats.put("active", stats.get("active") + count);
                        }
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка получения статистики: " + e.getMessage());
            }

            return stats;
        });
    }

    public CompletableFuture<Integer> cleanExpiredPunishments() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tableName + "` SET is_active = false, banned_status = false " +
                    "WHERE is_active = true AND duration != -1 AND (time_issued + duration) < ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, System.currentTimeMillis());
                int updated = stmt.executeUpdate();

                if (updated > 0) {
                    punishmentsCache.clear();
                    bannedStatusCache.clear();
                    loadAllPunishments();
                }

                return updated;

            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка очистки наказаний: " + e.getMessage());
                return 0;
            }
        });
    }

    private int countAllPunishments() {
        return punishmentsCache.values().stream().mapToInt(List::size).sum();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Соединение с MySQL закрыто");
        }
    }

    public boolean isUseMySQL() {
        return useMySQL;
    }
}