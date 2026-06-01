package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.movement.FlyCheck;
import fr.elysia.anticheat.checks.movement.NoFallCheck;
import fr.elysia.anticheat.checks.movement.SpeedCheck;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final ElysiaAntiCheat plugin;
    private final SpeedCheck speedCheck;
    private final FlyCheck flyCheck;
    private final NoFallCheck noFallCheck;

    public PlayerMoveListener(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
        this.speedCheck = plugin.getCheckManager().getTyped(SpeedCheck.class);
        this.flyCheck = plugin.getCheckManager().getTyped(FlyCheck.class);
        this.noFallCheck = plugin.getCheckManager().getTyped(NoFallCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Ignorer si seulement la rotation a changé
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        // Speed
        if (speedCheck != null) {
            CheckResult r = speedCheck.check(player, from, to);
            if (r.isFlagged()) {
                plugin.getViolationManager().flag(player, speedCheck.getName(), r.getDetails());
            }
        }

        // Fly
        if (flyCheck != null) {
            CheckResult r = flyCheck.check(player, from, to, data);
            if (r.isFlagged()) {
                plugin.getViolationManager().flag(player, flyCheck.getName(), r.getDetails());
            }
        }

        // Mise à jour de la position
        data.setPreviousLocation(from.clone());
        data.setLastLocation(to.clone());
        data.setLastMoveTime(System.currentTimeMillis());
        data.setTeleported(false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.hasPermission("elysiaac.bypass")) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        double fallDist = player.getFallDistance();
        data.setLastFallDistance(fallDist);

        // NoFall vérifie si les dégâts reçus sont cohérents avec la chute
        if (noFallCheck != null && event.getDamage() <= 0 && fallDist > 0) {
            CheckResult r = noFallCheck.checkNoDamage(player, fallDist);
            if (r.isFlagged()) {
                plugin.getViolationManager().flag(player, noFallCheck.getName(), r.getDetails());
            }
        }
    }
}
