package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.combat.FastBowCheck;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class InteractListener implements Listener {

    private final ElysiaAntiCheat plugin;
    private final FastBowCheck fastBowCheck;

    public InteractListener(ElysiaAntiCheat plugin) {
        this.plugin       = plugin;
        this.fastBowCheck = plugin.getCheckManager().getTyped(FastBowCheck.class);
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onWindChargeHit(ProjectileHitEvent event) {
        String typeName = event.getEntity().getType().name();
        if (!typeName.equals("WIND_CHARGE") && !typeName.equals("BREEZE_WIND_CHARGE")) return;

        long now = System.currentTimeMillis();

        event.getEntity().getNearbyEntities(6, 6, 6).forEach(entity -> {
            if (!(entity instanceof Player nearby)) return;
            if (nearby.hasPermission("elysiaac.bypass")) return;
            PlayerData nearbyData = plugin.getPlayerDataManager().get(nearby.getUniqueId());
            if (nearbyData != null) {
                nearbyData.setLastWindChargeLaunchTime(now);
            }
        });


        Projectile proj = event.getEntity();
        if (proj.getShooter() instanceof Player shooter) {
            PlayerData shooterData = plugin.getPlayerDataManager().get(shooter.getUniqueId());
            if (shooterData != null) {
                shooterData.setLastWindChargeLaunchTime(now);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) return;
        if (player.hasPermission("elysiaac.bypass")) return;
        if (!(projectile instanceof Arrow)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        if (fastBowCheck != null) {
            CheckResult r = fastBowCheck.check(player, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, fastBowCheck.getName(), r.getDetails());
        }
    }
}
