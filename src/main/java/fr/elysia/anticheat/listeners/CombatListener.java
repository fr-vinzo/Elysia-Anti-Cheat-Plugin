package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.combat.KillAuraCheck;
import fr.elysia.anticheat.checks.combat.ReachCheck;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final ElysiaAntiCheat plugin;
    private final KillAuraCheck killAuraCheck;
    private final ReachCheck reachCheck;

    public CombatListener(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
        this.killAuraCheck = plugin.getCheckManager().getTyped(KillAuraCheck.class);
        this.reachCheck = plugin.getCheckManager().getTyped(ReachCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (player.hasPermission("elysiaac.bypass")) return;

        Entity target = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        // Reach
        if (reachCheck != null) {
            CheckResult r = reachCheck.check(player, target);
            if (r.isFlagged()) {
                plugin.getViolationManager().flag(player, reachCheck.getName(), r.getDetails());
            }
        }

        // KillAura
        if (killAuraCheck != null) {
            CheckResult r = killAuraCheck.check(player, target, data);
            if (r.isFlagged()) {
                plugin.getViolationManager().flag(player, killAuraCheck.getName(), r.getDetails());
            }
        }
    }
}
