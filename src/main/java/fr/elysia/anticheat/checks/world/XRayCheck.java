package fr.elysia.anticheat.checks.world;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Material;

import java.util.Map;

public class XRayCheck extends Check {


    private static final Map<Material, Double> ORE_WEIGHTS = Map.ofEntries(
            Map.entry(Material.DIAMOND_ORE, 3.0),
            Map.entry(Material.DEEPSLATE_DIAMOND_ORE, 3.0),
            Map.entry(Material.ANCIENT_DEBRIS, 4.0),
            Map.entry(Material.EMERALD_ORE, 3.0),
            Map.entry(Material.DEEPSLATE_EMERALD_ORE, 3.0),
            Map.entry(Material.GOLD_ORE, 2.0),
            Map.entry(Material.DEEPSLATE_GOLD_ORE, 2.0),
            Map.entry(Material.LAPIS_ORE, 1.5),
            Map.entry(Material.DEEPSLATE_LAPIS_ORE, 1.5),
            Map.entry(Material.IRON_ORE, 0.5),
            Map.entry(Material.DEEPSLATE_IRON_ORE, 0.5),
            Map.entry(Material.COPPER_ORE, 0.3),
            Map.entry(Material.DEEPSLATE_COPPER_ORE, 0.3),
            Map.entry(Material.REDSTONE_ORE, 0.4),
            Map.entry(Material.DEEPSLATE_REDSTONE_ORE, 0.4),
            Map.entry(Material.COAL_ORE, 0.1),
            Map.entry(Material.DEEPSLATE_COAL_ORE, 0.1)
    );

    public XRayCheck(ElysiaAntiCheat plugin) {
        super(plugin, "XRay", CheckCategory.WORLD);
    }

    public boolean isOre(Material material) {
        return ORE_WEIGHTS.containsKey(material);
    }

    public double getOreWeight(Material material) {
        return ORE_WEIGHTS.getOrDefault(material, 0.0);
    }


    public CheckResult analyze(PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        int minBlocks = plugin.getConfig().getInt("checks.xray.min-blocks-mined", 50);
        if (data.getTotalBlocksMined() < minBlocks) return CheckResult.pass();

        long windowSeconds = plugin.getConfig().getLong("checks.xray.time-window", 300);
        long elapsed = (System.currentTimeMillis() - data.getMiningWindowStart()) / 1000L;
        if (elapsed > windowSeconds) {
            data.resetMiningWindow();
            return CheckResult.pass();
        }

        double weightedRatio = data.getWeightedOreRatio();
        double rawRatio = data.getOreRatio();

        double flagRatio = plugin.getConfig().getDouble("checks.xray.flag-ratio", 0.28);
        double suspectRatio = plugin.getConfig().getDouble("checks.xray.suspicious-ratio", 0.15);


        flagRatio *= data.getToleranceMultiplier();
        suspectRatio *= data.getToleranceMultiplier();


        if (weightedRatio >= flagRatio || rawRatio >= flagRatio * 0.8) {
            return CheckResult.fail(String.format(
                    "ratio_pondéré=%.2f ratio_brut=%.1f%% (minerais=%d/blocs=%d)",
                    weightedRatio, rawRatio * 100, data.getOresMined(), data.getTotalBlocksMined()));
        }

        if (weightedRatio >= suspectRatio || rawRatio >= suspectRatio * 0.8) {
            return CheckResult.fail(String.format(
                    "[SUSPECT] ratio=%.1f%% score=%.2f (minerais=%d/blocs=%d)",
                    rawRatio * 100, weightedRatio, data.getOresMined(), data.getTotalBlocksMined()));
        }

        return CheckResult.pass();
    }
}
