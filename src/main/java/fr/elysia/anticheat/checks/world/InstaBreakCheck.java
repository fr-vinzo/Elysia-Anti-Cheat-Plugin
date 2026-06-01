package fr.elysia.anticheat.checks.world;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Détecte l'InstaBreak : casser un bloc beaucoup plus vite que possible.
 *
 * On compare la durée réelle de cassage avec la durée minimale théorique
 * selon la dureté du bloc, même avec les meilleurs outils et enchantements.
 *
 * Seuil conservateur : on utilise seulement les blocs durs (hardness ≥ config)
 * pour éviter les faux positifs sur les blocs à cassage rapide.
 */
public class InstaBreakCheck extends Check {

    public InstaBreakCheck(ElysiaAntiCheat plugin) {
        super(plugin, "InstaBreak", CheckCategory.WORLD);
    }

    /** Enregistre le début du cassage d'un bloc (BlockDamageEvent). */
    public void startBreaking(Player player, Block block, PlayerData data) {
        if (!isEnabled()) return;
        data.setBlockBreakStart(System.currentTimeMillis());
        data.setCurrentlyBreaking(block.getType());
    }

    /** Vérifie la durée du cassage quand le bloc est cassé (BlockBreakEvent). */
    public CheckResult checkBreak(Player player, Block block, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();
        if (player.getGameMode() == GameMode.CREATIVE) return CheckResult.pass();

        long startTime = data.getBlockBreakStart();
        if (startTime == 0) return CheckResult.pass();

        Material mat = block.getType();
        double minHardness = plugin.getConfig().getDouble("checks.instabreak.min-hardness", 1.5);

        // Ne checker que les blocs durs
        if (mat.getHardness() < minHardness) {
            data.setBlockBreakStart(0);
            return CheckResult.pass();
        }

        // Exemptions : Haste + meilleur outil peut drastiquement réduire le temps
        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            data.setBlockBreakStart(0);
            return CheckResult.pass(); // Trop complexe à calculer précisément
        }

        long elapsed = System.currentTimeMillis() - startTime;
        long minExpected = calculateMinBreakTime(mat);

        if (elapsed < minExpected && minExpected > 0) {
            data.setBlockBreakStart(0);
            return CheckResult.fail(String.format(
                    "cassage_rapide=%dms (min=%dms) bloc=%s",
                    elapsed, minExpected, mat.name()));
        }

        data.setBlockBreakStart(0);
        return CheckResult.pass();
    }

    /**
     * Temps minimal de cassage en ms avec l'outil optimal + Efficiency V.
     * Formule approximative : hardness × 50ms (très conservatrice).
     */
    private long calculateMinBreakTime(Material mat) {
        double hardness = mat.getHardness();
        if (hardness <= 0) return 0;
        // Avec Efficiency V + diamant, la durée est ~hardness * 25ms
        // On utilise une valeur encore plus basse pour éviter les faux positifs
        return (long) (hardness * 20);
    }
}
