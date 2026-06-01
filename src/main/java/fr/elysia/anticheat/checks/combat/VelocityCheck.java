package fr.elysia.anticheat.checks.combat;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Détecte le Velocity hack : ignorer le recul (knockback) après avoir
 * reçu un coup. Principe : on enregistre la direction attendue du knockback,
 * puis 2 ticks plus tard on vérifie si le joueur a bougé dans cette direction.
 */
public class VelocityCheck extends Check {

    // Tolérance en blocs : le joueur doit avoir bougé d'au moins ce minimum
    private static final double MIN_KB_MOVEMENT = 0.08;

    public VelocityCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Velocity", CheckCategory.COMBAT);
    }

    /** Appelé quand le joueur reçoit un coup (EntityDamageByEntityEvent). */
    public void recordKnockback(Player victim, Player attacker, PlayerData data) {
        if (!isEnabled()) return;
        if (victim.hasPotionEffect(PotionEffectType.RESISTANCE)) return;

        // Direction du knockback : de l'attaquant vers la victime
        Vector kb = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector())
                .setY(0).normalize();

        data.setExpectedKbX(kb.getX());
        data.setExpectedKbZ(kb.getZ());
        data.setLastKnockbackTime(System.currentTimeMillis());
    }

    /** Appelé au tick suivant (PlayerMoveEvent) pour vérifier si le recul a bien été appliqué. */
    public CheckResult checkMovement(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        long since = System.currentTimeMillis() - data.getLastKnockbackTime();
        // Vérifier entre 50ms et 300ms après le coup
        if (since < 50 || since > 300) return CheckResult.pass();

        double kbX = data.getExpectedKbX();
        double kbZ = data.getExpectedKbZ();
        if (kbX == 0 && kbZ == 0) return CheckResult.pass();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double movement = Math.sqrt(dx * dx + dz * dz);

        // Calculer le dot product : si le joueur bouge dans la direction du knockback
        double dot = (dx * kbX + dz * kbZ);

        double minPercent = plugin.getConfig().getDouble("checks.velocity.min-knockback-percent", 40.0) / 100.0;

        // Si le mouvement est très faible ou dans la mauvaise direction
        if (movement < MIN_KB_MOVEMENT || dot < minPercent * movement) {
            // Réinitialiser pour ne pas flaguer plusieurs fois pour le même coup
            data.setLastKnockbackTime(0);
            return CheckResult.fail(String.format("recul_ignoré: dépl=%.3f dot=%.3f", movement, dot));
        }

        data.setLastKnockbackTime(0);
        return CheckResult.pass();
    }
}
