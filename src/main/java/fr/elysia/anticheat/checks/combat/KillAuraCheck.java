package fr.elysia.anticheat.checks.combat;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import fr.elysia.anticheat.utils.MathUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Détecte le KillAura.
 *
 * Améliorations v2 :
 * - Le check d'angle utilise maintenant aussi le score de confiance
 * - Vérification multi-cibles conservée
 * - CPS : vérification séparée dans AutoClickerCheck (variance)
 */
public class KillAuraCheck extends Check {

    public KillAuraCheck(ElysiaAntiCheat plugin) {
        super(plugin, "KillAura", CheckCategory.COMBAT);
    }

    public CheckResult check(Player player, Entity target, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        data.recordHit(target.getUniqueId());

        // --- CPS brut ---
        int maxCPS = plugin.getConfig().getInt("checks.kill-aura.max-cps", 18);
        int cps = data.getCPS();
        if (cps > maxCPS) {
            return CheckResult.fail("CPS=" + cps + " max=" + maxCPS);
        }

        // --- Angle de regard ---
        // Les KA modernes ont une "smooth rotation" — on garde le check angle
        // mais à une valeur suffisamment stricte pour ne pas fausser positiver
        double maxAngle = plugin.getConfig().getDouble("checks.kill-aura.max-angle", 90.0);
        // Tolérance : joueurs de confiance → seuil d'angle légèrement réduit (plus strict)
        double effectiveMaxAngle = maxAngle * (2.0 - data.getToleranceMultiplier());
        double angle = MathUtil.getAngleToTarget(player, target);

        if (angle > effectiveMaxAngle) {
            return CheckResult.fail(String.format(
                    "angle=%.1f° max=%.1f° cible=%s", angle, effectiveMaxAngle, target.getType().name()));
        }

        // --- Multi-cibles ---
        int maxTargets = plugin.getConfig().getInt("checks.kill-aura.max-targets-per-tick", 2);
        int targets = data.getRecentTargetCount();
        if (targets > maxTargets) {
            return CheckResult.fail("cibles_500ms=" + targets + " max=" + maxTargets);
        }

        return CheckResult.pass();
    }
}
