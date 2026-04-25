package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.*;

public class BanTabCompleter extends BaseTabCompleter {
    public BanTabCompleter(InternationalBans plugin) { super(plugin); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("internationalbans.ban")) return Collections.emptyList();

        if (args.length == 1) {
            return filterByPrefix(getOnlinePlayers(), args[0]);
        }

        if (args.length == 2) {
            // Показываем все полные ID причин
            return filterByPrefix(getAllReasonIds(), args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> getAllReasonIds() {
        return new ArrayList<>(plugin.getReasonManager().getAllReasons().keySet());
    }
}