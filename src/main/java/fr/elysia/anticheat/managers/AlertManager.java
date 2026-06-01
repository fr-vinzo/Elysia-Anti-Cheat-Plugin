package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {


    private final Set<UUID> alertsDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AlertManager() {}


    public void sendAlert(Player target, String checkName, int vl, String details) {
        String msg = MessageUtil.formatAlert(target.getName(), checkName, vl, details);
        MessageUtil.broadcastAlert(msg);
    }


    public boolean toggleAlerts(UUID adminUUID) {
        if (alertsDisabled.contains(adminUUID)) {
            alertsDisabled.remove(adminUUID);
            return true;
        } else {
            alertsDisabled.add(adminUUID);
            return false;
        }
    }

    public boolean hasAlertsEnabled(UUID uuid) {
        return !alertsDisabled.contains(uuid);
    }


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
