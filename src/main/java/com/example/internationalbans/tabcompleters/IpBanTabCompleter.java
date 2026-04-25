package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.*;

public class IpBanTabCompleter extends BaseTabCompleter {
    public IpBanTabCompleter(InternationalBans plugin) { super(plugin); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("internationalbans.ipban")) return Collections.emptyList();

        if (args.length == 1) {
            return filterByPrefix(getOnlinePlayers(), args[0]);
        }

        if (args.length == 2) {
            // Показываем все ID причин (фильтрация по типу больше не нужна)
            return filterByPrefix(getReasonIds(), args[1]);
        }

        return Collections.emptyList();
    }
}