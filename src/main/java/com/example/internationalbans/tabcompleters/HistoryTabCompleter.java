package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.*;

public class HistoryTabCompleter extends BaseTabCompleter {
    public HistoryTabCompleter(InternationalBans plugin) { super(plugin); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("internationalbans.history")) return Collections.emptyList();
        if (args.length == 1) {
            Set<String> players = new HashSet<>();
            for (var entry : plugin.getBanManager().getAllPunishments().entrySet()) {
                for (var punishment : entry.getValue()) {
                    players.add(punishment.getPlayerName());
                }
            }
            return filterByPrefix(new ArrayList<>(players), args[0]);
        }
        return Collections.emptyList();
    }
}