package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.combat.VelocityCheck;
import fr.elysia.anticheat.checks.movement.*;
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
    private final TimerCheck timerCheck;
    private final StepCheck stepCheck;
    private final FastLadderCheck fastLadderCheck;
    private final JesusCheck jesusCheck;
    private final VelocityCheck velocityCheck;

    public PlayerMoveListener(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
        this.speedCheck      = plugin.getCheckManager().getTyped(SpeedCheck.class);
        this.flyCheck        = plugin.getCheckManager().getTyped(FlyCheck.class);
        this.noFallCheck     = plugin.getCheckManager().getTyped(NoFallCheck.class);
        this.timerCheck      = plugin.getCheckManager().getTyped(TimerCheck.class);
        this.stepCheck       = plugin.getCheckManager().getTyped(StepCheck.class);
        this.fastLadderCheck = plugin.getCheckManager().getTyped(FastLadderCheck.class);
        this.jesusCheck      = plugin.getCheckManager().getTyped(JesusCheck.class);
        this.velocityCheck   = plugin.getCheckManager().getTyped(VelocityCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;

        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        boolean posChanged = from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;


        if (timerCheck != null) {
            CheckResult r = timerCheck.check(data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, timerCheck.getName(), r.getDetails());
        }

        if (!posChanged) return;


        if (speedCheck != null) {
            CheckResult r = speedCheck.check(player, from, to, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, speedCheck.getName(), r.getDetails());
        }


        if (flyCheck != null) {
            CheckResult r = flyCheck.check(player, from, to, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, flyCheck.getName(), r.getDetails());
        }


        if (stepCheck != null) {
            CheckResult r = stepCheck.check(player, from, to, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, stepCheck.getName(), r.getDetails());
        }


        if (fastLadderCheck != null) {
            CheckResult r = fastLadderCheck.check(player, from, to, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, fastLadderCheck.getName(), r.getDetails());
        }


        if (jesusCheck != null) {
            CheckResult r = jesusCheck.check(player, from, to, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, jesusCheck.getName(), r.getDetails());
        }


        if (velocityCheck != null) {
            CheckResult r = velocityCheck.checkMovement(player, from, to, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, velocityCheck.getName(), r.getDetails());
        }


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

        if (noFallCheck != null && event.getDamage() <= 0 && fallDist > 0) {
            CheckResult r = noFallCheck.checkNoDamage(player, fallDist);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, noFallCheck.getName(), r.getDetails());
        }
    }
}
