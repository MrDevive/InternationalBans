package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.*;

public class TempBanTabCompleter extends BaseTabCompleter {
    public TempBanTabCompleter(InternationalBans plugin) { super(plugin); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("internationalbans.tempban")) return Collections.emptyList();
        if (args.length == 1) return filterByPrefix(getOnlinePlayers(), args[0]);
        if (args.length == 2) return filterByPrefix(getTimeSuggestions(), args[1]);
        if (args.length == 3) return filterByPrefix(getReasonIds(), args[2]);
        return Collections.emptyList();
    }
}