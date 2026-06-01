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

public class InstaBreakCheck extends Check {

    public InstaBreakCheck(ElysiaAntiCheat plugin) {
        super(plugin, "InstaBreak", CheckCategory.WORLD);
    }


    public void startBreaking(Player player, Block block, PlayerData data) {
        if (!isEnabled()) return;
        data.setBlockBreakStart(System.currentTimeMillis());
        data.setCurrentlyBreaking(block.getType());
    }


    public CheckResult checkBreak(Player player, Block block, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();
        if (player.getGameMode() == GameMode.CREATIVE) return CheckResult.pass();

        long startTime = data.getBlockBreakStart();
        if (startTime == 0) return CheckResult.pass();

        Material mat = block.getType();
        double minHardness = plugin.getConfig().getDouble("checks.instabreak.min-hardness", 1.5);


        if (mat.getHardness() < minHardness) {
            data.setBlockBreakStart(0);
            return CheckResult.pass();
        }


        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            data.setBlockBreakStart(0);
            return CheckResult.pass();
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


    private long calculateMinBreakTime(Material mat) {
        double hardness = mat.getHardness();
        if (hardness <= 0) return 0;


        return (long) (hardness * 20);
    }
}
