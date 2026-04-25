package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.*;

public class UnbanTabCompleter extends BaseTabCompleter {
    public UnbanTabCompleter(InternationalBans plugin) { super(plugin); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("internationalbans.unban")) return Collections.emptyList();
        if (args.length == 1) {
            Set<String> bannedPlayers = new HashSet<>();
            for (var entry : plugin.getBanManager().getAllPunishments().entrySet()) {
                for (var punishment : entry.getValue()) {
                    if (punishment.isBanType() && punishment.isActive() && !punishment.isExpired()) {
                        bannedPlayers.add(punishment.getPlayerName());
                    }
                }
            }
            return filterByPrefix(new ArrayList<>(bannedPlayers), args[0]);
        }
        return Collections.emptyList();
    }
}