package fr.elysia.anticheat.checks.world;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Material;

import java.util.Set;

public class XRayCheck extends Check {

    private final Set<Material> oreTypes = Set.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS
    );

    // Minerais "communs" — comptent mais avec un poids moindre dans la détection
    private final Set<Material> commonOreTypes = Set.of(
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE
    );

    public XRayCheck(ElysiaAntiCheat plugin) {
        super(plugin, "XRay", CheckCategory.WORLD);
    }

    public boolean isOre(Material material) {
        return oreTypes.contains(material) || commonOreTypes.contains(material);
    }

    public boolean isRareOre(Material material) {
        return oreTypes.contains(material);
    }

    /**
     * Analyse les statistiques de minage d'un joueur et retourne un CheckResult.
     * N'est appelé que si le seuil minimum de blocs minés est atteint.
     */
    public CheckResult analyze(PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        int minBlocks = plugin.getConfig().getInt("checks.xray.min-blocks-mined", 50);
        if (data.getTotalBlocksMined() < minBlocks) return CheckResult.pass();

        // Réinitialiser si la fenêtre de temps est dépassée
        long windowSeconds = plugin.getConfig().getLong("checks.xray.time-window", 300);
        long elapsed = (System.currentTimeMillis() - data.getMiningWindowStart()) / 1000L;
        if (elapsed > windowSeconds) {
            data.resetMiningWindow();
            return CheckResult.pass();
        }

        double ratio = data.getOreRatio();
        double flagRatio = plugin.getConfig().getDouble("checks.xray.flag-ratio", 0.28);
        double suspectRatio = plugin.getConfig().getDouble("checks.xray.suspicious-ratio", 0.15);

        if (ratio >= flagRatio) {
            return CheckResult.fail(String.format(
                    "ratio=%.1f%% (minerais=%d/blocs=%d) [CONFIRMÉ]",
                    ratio * 100, data.getOresMined(), data.getTotalBlocksMined()));
        }

        if (ratio >= suspectRatio) {
            return CheckResult.fail(String.format(
                    "ratio=%.1f%% (minerais=%d/blocs=%d) [SUSPECT]",
                    ratio * 100, data.getOresMined(), data.getTotalBlocksMined()));
        }

        return CheckResult.pass();
    }
}
