package fr.elysia.anticheat.commands;

import fr.elysia.anticheat.ElysiaAntiCheat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AnticheatTabCompleter implements TabCompleter {

    private final ElysiaAntiCheat plugin;

    private static final List<String> BASE_SUBCOMMANDS = List.of(
            "alerts", "info", "verbose", "violations", "checks", "reset", "kick", "reload"
    );
    private static final List<String> ADMIN_ONLY = List.of("reset", "kick", "reload");

    public AnticheatTabCompleter(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            BASE_SUBCOMMANDS.stream()
                    .filter(s -> !ADMIN_ONLY.contains(s) || sender.hasPermission("elysiaac.admin"))
                    .filter(s -> s.startsWith(partial))
                    .forEach(completions::add);
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("info", "reset", "kick", "verbose").contains(sub)) {
                String partial = args[1].toLowerCase();
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .forEach(completions::add);
                return completions;
            }
        }

        return completions;
    }
}
