package fr.elysia.anticheat.utils;

import fr.elysia.anticheat.ElysiaAntiCheat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private MessageUtil() {}

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String prefix() {
        String raw = ElysiaAntiCheat.getInstance().getConfig().getString("general.prefix", "&8[&6Elysia AC&8]&r");
        return colorize(raw);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(prefix() + " " + colorize(message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }


    public static void broadcastAlert(String message) {
        String formatted = prefix() + " " + colorize(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("elysiaac.alerts") && isAlertEnabled(p)) {
                p.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    public static void broadcastAdmin(String message) {
        String formatted = colorize(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("elysiaac.admin")) {
                p.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private static boolean isAlertEnabled(Player p) {
        return ElysiaAntiCheat.getInstance().getAlertManager().hasAlertsEnabled(p.getUniqueId());
    }


    public static String formatAlert(String playerName, String checkName, int vl, String details) {
        return "&8[&c⚠&8] &e" + playerName + " &7a déclenché &c" + checkName
                + " &7(VL: &c" + vl + "&7) &8» &f" + details;
    }

    public static String formatKick(String playerName, String checkName) {
        return "&8[&4✖&8] &e" + playerName + " &7a été &ckick &7par &c" + checkName;
    }

    public static String line(char c, int length) {
        return colorize("&8" + String.valueOf(c).repeat(length));
    }
}
