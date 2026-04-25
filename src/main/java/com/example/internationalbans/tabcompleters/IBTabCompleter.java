package com.example.internationalbans.tabcompleters;

import com.example.internationalbans.InternationalBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.*;

public class IBTabCompleter extends BaseTabCompleter {
    public IBTabCompleter(InternationalBans plugin) { super(plugin); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (sender.hasPermission("internationalbans.admin")) {
                commands.add("reload");
                commands.add("stats");
            }
            commands.add("help");
            commands.add("info");
            return filterByPrefix(commands, args[0]);
        }
        return Collections.emptyList();
    }
}