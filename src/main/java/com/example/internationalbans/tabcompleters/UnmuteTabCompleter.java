package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.*;

public class UnmuteTabCompleter extends BaseTabCompleter {
    public UnmuteTabCompleter(InternationalBans plugin) { super(plugin); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("internationalbans.unmute")) return Collections.emptyList();
        if (args.length == 1) {
            Set<String> mutedPlayers = new HashSet<>();
            for (var entry : plugin.getBanManager().getAllPunishments().entrySet()) {
                for (var punishment : entry.getValue()) {
                    if ((punishment.getType().equals("mute") || punishment.getType().equals("tempmute"))
                            && punishment.isActive() && !punishment.isExpired()) {
                        mutedPlayers.add(punishment.getPlayerName());
                    }
                }
            }
            return filterByPrefix(new ArrayList<>(mutedPlayers), args[0]);
        }
        return Collections.emptyList();
    }
}