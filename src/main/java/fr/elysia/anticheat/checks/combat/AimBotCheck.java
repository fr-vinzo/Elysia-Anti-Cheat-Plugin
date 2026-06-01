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
 * Détecte les AimBots par analyse de la précision de visée.
 *
 * Un joueur avec aimbot frappe toujours avec un angle quasi-nul vers la cible
 * (< 3°). Sur 10+ frappes consécutives, cette perfection est statistiquement
 * impossible pour un humain.
 *
 * Ce check est intentionnellement conservateur pour éviter les faux positifs.
 */
public class AimBotCheck extends Check {

    public AimBotCheck(ElysiaAntiCheat plugin) {
        super(plugin, "AimBot", CheckCategory.COMBAT);
    }

    public CheckResult check(Player player, Entity target, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        double angle = MathUtil.getAngleToTarget(player, target);
        data.recordAimAngle(angle);

        int minSamples = plugin.getConfig().getInt("checks.aimbot.min-samples", 10);
        if (data.getAimAngleSampleCount() < minSamples) return CheckResult.pass();

        double avgAngle = data.getAverageAimAngle();
        double maxAvgAngle = plugin.getConfig().getDouble("checks.aimbot.max-avg-angle", 3.0);

        // Tolérance : joueurs de confiance ont un seuil plus bas
        double effectiveMax = maxAvgAngle / data.getToleranceMultiplier();

        if (avgAngle < effectiveMax) {
            return CheckResult.fail(String.format(
                    "angle_moyen=%.2f° (seuil=%.1f°) sur %d frappes",
                    avgAngle, effectiveMax, data.getAimAngleSampleCount()));
        }

        return CheckResult.pass();
    }
}
