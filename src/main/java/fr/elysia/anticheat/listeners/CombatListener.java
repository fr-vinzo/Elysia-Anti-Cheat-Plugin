package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.combat.*;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Material;
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
    private final VelocityCheck velocityCheck;
    private final AutoClickerCheck autoClickerCheck;
    private final AimBotCheck aimBotCheck;

    public CombatListener(ElysiaAntiCheat plugin) {
        this.plugin           = plugin;
        this.killAuraCheck    = plugin.getCheckManager().getTyped(KillAuraCheck.class);
        this.reachCheck       = plugin.getCheckManager().getTyped(ReachCheck.class);
        this.velocityCheck    = plugin.getCheckManager().getTyped(VelocityCheck.class);
        this.autoClickerCheck = plugin.getCheckManager().getTyped(AutoClickerCheck.class);
        this.aimBotCheck      = plugin.getCheckManager().getTyped(AimBotCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.hasPermission("elysiaac.bypass")) return;

        Entity target = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().get(attacker.getUniqueId());
        if (data == null) return;

        // Reach
        if (reachCheck != null) {
            CheckResult r = reachCheck.check(attacker, target);
            if (r.isFlagged()) plugin.getViolationManager().flag(attacker, reachCheck.getName(), r.getDetails());
        }

        // KillAura
        if (killAuraCheck != null) {
            CheckResult r = killAuraCheck.check(attacker, target, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(attacker, killAuraCheck.getName(), r.getDetails());
        }

        // AutoClicker variance
        if (autoClickerCheck != null) {
            CheckResult r = autoClickerCheck.check(data);
            if (r.isFlagged()) plugin.getViolationManager().flag(attacker, autoClickerCheck.getName(), r.getDetails());
        }

        // AimBot
        if (aimBotCheck != null) {
            CheckResult r = aimBotCheck.check(attacker, target, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(attacker, aimBotCheck.getName(), r.getDetails());
        }

        // Mace smash attack (1.21) : l'attaquant est projeté en l'air après l'impact
        if (attacker.getInventory().getItemInMainHand().getType() == Material.MACE) {
            data.setLastMaceSmashTime(System.currentTimeMillis());
        }

        // Velocity : enregistrer le knockback attendu pour la victime
        if (velocityCheck != null && target instanceof Player victim) {
            PlayerData victimData = plugin.getPlayerDataManager().get(victim.getUniqueId());
            if (victimData != null) {
                velocityCheck.recordKnockback(victim, attacker, victimData);
            }
        }
    }
}
