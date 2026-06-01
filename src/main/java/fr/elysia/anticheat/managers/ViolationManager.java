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

    /**
     * Enregistre une violation pour un joueur sur un check donné.
     * Déclenche les alertes et punitions si les seuils sont atteints.
     */
    public void flag(Player player, String checkName, String details) {
        if (player.hasPermission("elysiaac.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        int vl = data.incrementViolation(checkName);
        String alertMsg = MessageUtil.formatAlert(player.getName(), checkName, vl, details);
        data.addRecentAlert("§7[VL:" + vl + "] §c" + checkName + " §8» §f" + details);

        // Lancer l'event Bukkit (API — d'autres plugins peuvent l'annuler)
        ViolationEvent event = new ViolationEvent(player, checkName, vl, details);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        // Seuil d'alerte staff
        var checkObj = plugin.getCheckManager().get(checkName);
        int alertVL = checkObj != null ? getAlertVL(checkName) : 5;
        int kickVL = checkObj != null ? getKickVL(checkName) : 15;

        if (vl >= alertVL) {
            plugin.getAlertManager().sendAlert(player, checkName, vl, details);
        }

        if (plugin.getConfig().getBoolean("general.log-violations", true)) {
            plugin.getLogger().info("[VIOLATION] " + player.getName() + " | " + checkName
                    + " | VL:" + vl + " | " + details);
        }

        // Kick si seuil atteint
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
        });
    }

    private int getAlertVL(String checkName) {
        String key = "checks." + checkName.toLowerCase().replace(" ", "-") + ".alert-vl";
        return plugin.getConfig().getInt(key, 5);
    }

    private int getKickVL(String checkName) {
        String key = "checks." + checkName.toLowerCase().replace(" ", "-") + ".kick-vl";
        return plugin.getConfig().getInt(key, 15);
    }
}
