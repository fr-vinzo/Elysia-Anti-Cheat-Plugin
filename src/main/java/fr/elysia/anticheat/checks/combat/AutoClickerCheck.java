package fr.elysia.anticheat.checks.combat;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;

/**
 * Détecte les AutoClickers par analyse de la variance des intervalles entre frappes.
 *
 * Un humain clique de façon irrégulière (coefficient de variation > ~15–20%).
 * Un autoclicker produit des intervalles quasi-constants (CV très faible).
 *
 * On combine ce check avec une vérification CPS élevée pour éviter les
 * faux positifs sur des joueurs qui cliquent lentement mais régulièrement.
 */
public class AutoClickerCheck extends Check {

    public AutoClickerCheck(ElysiaAntiCheat plugin) {
        super(plugin, "AutoClicker", CheckCategory.COMBAT);
    }

    public CheckResult check(PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        int minSamples = 10;
        if (data.getHitIntervalSampleCount() < minSamples) return CheckResult.pass();

        int minCPS = plugin.getConfig().getInt("checks.autoclicker.min-cps-to-check", 4);
        if (data.getCPS() < minCPS) return CheckResult.pass();

        double stdDev = data.getHitIntervalStdDev();
        double mean = data.getHitIntervalMean();
        if (mean <= 0) return CheckResult.pass();

        // Coefficient de variation (CV) en pourcentage
        double cv = (stdDev / mean) * 100.0;

        double minCV = plugin.getConfig().getDouble("checks.autoclicker.min-coefficient-variation", 12.0);
        // Tolérance : joueurs de confiance ont un seuil légèrement réduit
        double effectiveMinCV = minCV / data.getToleranceMultiplier();

        if (cv < effectiveMinCV) {
            return CheckResult.fail(String.format(
                    "CV_clics=%.1f%% (min=%.1f%%) CPS=%d std=%.1fms mean=%.1fms",
                    cv, effectiveMinCV, data.getCPS(), stdDev, mean));
        }

        return CheckResult.pass();
    }
}
