package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerConnectionListener implements Listener {

    private final ElysiaAntiCheat plugin;

    public PlayerConnectionListener(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var data = plugin.getPlayerDataManager().create(event.getPlayer());
        data.setLastLocation(event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().remove(event.getPlayer().getUniqueId());
        plugin.getAntiXRayManager().cleanup(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        var data = plugin.getPlayerDataManager().get(event.getPlayer().getUniqueId());
        if (data == null) return;
        data.setTeleported(true);
        data.setTeleportTime(System.currentTimeMillis());
        if (event.getTo() != null) {
            data.setLastLocation(event.getTo());
        }
    }
}
