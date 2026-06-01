package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.ViolationEvent;
import fr.elysia.anticheat.data.PlayerData;
import fr.elysia.anticheat.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ViolationManager {

    private final ElysiaAntiCheat plugin;

    public ViolationManager(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
    }

    public void flag(Player player, String checkName, String details) {
        if (player.hasPermission("elysiaac.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        int vl = data.incrementViolation(checkName);
        data.addRecentAlert("§7[VL:" + vl + "] §c" + checkName + " §8» §f" + details);

        // API event — annulable par d'autres plugins
        ViolationEvent event = new ViolationEvent(player, checkName, vl, details);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        int alertVL = getAlertVL(checkName);
        int kickVL  = getKickVL(checkName);

        if (vl >= alertVL) {
            plugin.getAlertManager().sendAlert(player, checkName, vl, details);
            // Discord — uniquement au-dessus du seuil configuré
            plugin.getDiscordWebhookManager().sendViolationAlert(
                    player.getName(), checkName, vl, details);
        }

        if (plugin.getConfig().getBoolean("general.log-violations", true)) {
            plugin.getLogger().info("[VIOLATION] " + player.getName()
                    + " | " + checkName + " | VL:" + vl + " | " + details);
        }

        // Persistance SQLite
        plugin.getDatabaseManager().logViolation(
                player.getUniqueId().toString(), player.getName(), checkName, vl, details);

        if (vl >= kickVL) {
            kickPlayer(player, checkName);
        }
    }

    private void kickPlayer(Player player, String checkName) {
        String rawMsg = plugin.getConfig().getString("punishments.kick-message",
                "&c&lElysia Anti-Cheat\n&7Comportement suspect détecté.\n&7Contacte le staff si c'est une erreur.");
        String kickMsg = MessageUtil.colorize(rawMsg);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            player.kickPlayer(kickMsg);
            MessageUtil.broadcastAlert(MessageUtil.formatKick(player.getName(), checkName));
            plugin.getDiscordWebhookManager().sendKickAlert(player.getName(), checkName);
        });
    }

    private int getAlertVL(String checkName) {
        return plugin.getConfig().getInt(
                "checks." + key(checkName) + ".alert-vl", 5);
    }

    private int getKickVL(String checkName) {
        return plugin.getConfig().getInt(
                "checks." + key(checkName) + ".kick-vl", 15);
    }

    private String key(String checkName) {
        return checkName.toLowerCase().replace(" ", "-");
    }
}
