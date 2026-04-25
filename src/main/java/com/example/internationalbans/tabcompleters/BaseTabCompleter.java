package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import com.example.internationalbans.managers.ReasonManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseTabCompleter implements TabCompleter {

    protected final InternationalBans plugin;

    public BaseTabCompleter(InternationalBans plugin) {
        this.plugin = plugin;
    }

    protected List<String> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    protected List<String> getReasonIds() {
        return new ArrayList<>(plugin.getReasonManager().getAllReasons().keySet());
    }

    protected List<String> filterByPrefix(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    protected List<String> getTimeSuggestions() {
        return Arrays.asList("1s", "30s", "1m", "5m", "10m", "30m", "1h", "2h", "6h", "12h",
                "1d", "2d", "3d", "7d", "14d", "30d", "perm", "-1");
    }
}