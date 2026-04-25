package com.example.internationalbans;

import com.example.internationalbans.commands.*;
import com.example.internationalbans.listeners.ChatListener;
import com.example.internationalbans.listeners.JoinListener;
import com.example.internationalbans.managers.BanManager;
import com.example.internationalbans.managers.MessageManager;
import com.example.internationalbans.managers.ReasonManager;
import com.example.internationalbans.tabcompleters.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class InternationalBans extends JavaPlugin {

    private static InternationalBans instance;
    private BanManager banManager;
    private MessageManager messageManager;
    private ReasonManager reasonManager;
    private FileConfiguration pluginConfig;  // Исправлено: FileConfiguration вместо YamlConfiguration
    private YamlConfiguration databaseConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Сохранение конфигураций
        saveDefaultConfig();
        saveResource("message.yml", false);
        saveResource("reasons.yml", false);
        saveResource("database.yml", false);

        // Загрузка конфигов
        loadConfigs();
        loadDatabaseConfig();

        // Инициализация менеджеров
        this.messageManager = new MessageManager(this);
        this.reasonManager = new ReasonManager(this);
        this.banManager = new BanManager(this);

        // Регистрация команд и табуляции
        registerCommands();
        registerTabCompleters();

        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        // Запуск автосохранения
        startAutoSave();

        getLogger().info("=========================================");
        getLogger().info("InternationalBans успешно загружен!");
        getLogger().info("Версия: " + getDescription().getVersion());
        getLogger().info("База данных: " + (databaseConfig.getBoolean("mysql.enabled") ? "MySQL" : "YAML"));
        getLogger().info("=========================================");
    }

    private void registerCommands() {
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("tempipban").setExecutor(new TempIpBanCommand(this));
        getCommand("unbanip").setExecutor(new UnbanIpCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("reload").setExecutor(new ReloadCommand(this));
        getCommand("ib").setExecutor(new MainCommand(this));
    }

    private void registerTabCompleters() {
        getCommand("ban").setTabCompleter(new BanTabCompleter(this));
        getCommand("tempban").setTabCompleter(new TempBanTabCompleter(this));
        getCommand("unban").setTabCompleter(new UnbanTabCompleter(this));
        getCommand("ipban").setTabCompleter(new IpBanTabCompleter(this));
        getCommand("tempipban").setTabCompleter(new TempBanTabCompleter(this));
        getCommand("unbanip").setTabCompleter(new UnbanTabCompleter(this));
        getCommand("mute").setTabCompleter(new MuteTabCompleter(this));
        getCommand("tempmute").setTabCompleter(new TempMuteTabCompleter(this));
        getCommand("unmute").setTabCompleter(new UnmuteTabCompleter(this));
        getCommand("kick").setTabCompleter(new KickTabCompleter(this));
        getCommand("warn").setTabCompleter(new WarnTabCompleter(this));
        getCommand("history").setTabCompleter(new HistoryTabCompleter(this));
        getCommand("ib").setTabCompleter(new IBTabCompleter(this));
    }

    private void startAutoSave() {
        int interval = getPluginConfig().getInt("database.auto-save-interval", 6000);
        if (interval > 0) {
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                if (banManager != null) {
                    banManager.saveAll();
                }
            }, interval, interval);
        }
    }

    /**
     * Загрузка основной конфигурации
     */
    private void loadConfigs() {
        super.reloadConfig();
        pluginConfig = getConfig();  // Теперь тип совпадает - FileConfiguration
    }

    /**
     * Перезагрузка всех конфигураций (для команды /reload)
     */
    public void reloadAllConfigs() {
        // Перезагрузка основной конфигурации
        super.reloadConfig();
        pluginConfig = getConfig();

        // Перезагрузка database.yml
        loadDatabaseConfig();

        // Перезагрузка сообщений
        if (messageManager != null) {
            messageManager.reload();
        }

        // Перезагрузка причин
        if (reasonManager != null) {
            reasonManager.load();
        }

        getLogger().info("Конфигурация перезагружена!");
    }

    private void loadDatabaseConfig() {
        databaseConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "database.yml"));
    }

    @Override
    public void onDisable() {
        if (banManager != null) {
            banManager.saveAll();
            banManager.close();
        }
        getLogger().info("InternationalBans выгружен!");
    }

    public static InternationalBans getInstance() {
        return instance;
    }

    public BanManager getBanManager() {
        return banManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ReasonManager getReasonManager() {
        return reasonManager;
    }

    public FileConfiguration getPluginConfig() {  // Возвращаем FileConfiguration
        return pluginConfig;
    }

    public YamlConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

    public int getMaxWarnings() {
        return pluginConfig.getInt("settings.max-warnings", 3);
    }

    public boolean isAutoBanEnabled() {
        return pluginConfig.getBoolean("settings.auto-ban-after-warnings", true);
    }

    public long getAutoBanDuration() {
        return pluginConfig.getLong("settings.auto-ban-duration", -1L);
    }

    public String getAutoBanReason() {
        return pluginConfig.getString("settings.auto-ban-reason", "Превышение лимита предупреждений");
    }
}