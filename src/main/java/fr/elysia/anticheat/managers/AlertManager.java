package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {

    /** Admins ayant désactivé leurs alertes. */
    private final Set<UUID> alertsDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AlertManager() {}

    /** Envoie une alerte à tous les admins en ligne ayant les alertes activées. */
    public void sendAlert(Player target, String checkName, int vl, String details) {
        String msg = MessageUtil.formatAlert(target.getName(), checkName, vl, details);
        MessageUtil.broadcastAlert(msg);
    }

    /** Active/désactive les alertes pour un admin. Retourne le nouvel état. */
    public boolean toggleAlerts(UUID adminUUID) {
        if (alertsDisabled.contains(adminUUID)) {
            alertsDisabled.remove(adminUUID);
            return true; // maintenant actif
        } else {
            alertsDisabled.add(adminUUID);
            return false; // maintenant inactif
        }
    }

    public boolean hasAlertsEnabled(UUID uuid) {
        return !alertsDisabled.contains(uuid);
    }

    /** Envoie un message de diffusion global aux admins en ligne. */
    public void broadcastToAdmins(String message) {
        String formatted = MessageUtil.prefix() + " " + MessageUtil.colorize(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("elysiaac.admin")) {
                p.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }
}
