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
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class InteractListener implements Listener {

    private final ElysiaAntiCheat plugin;
    private final FastBowCheck fastBowCheck;

    public InteractListener(ElysiaAntiCheat plugin) {
        this.plugin       = plugin;
        this.fastBowCheck = plugin.getCheckManager().getTyped(FastBowCheck.class);
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
