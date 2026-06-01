package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import fr.elysia.anticheat.utils.MathUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class SpeedCheck extends Check {

    private static final double BASE_MAX_SPEED = 0.36;

    public SpeedCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Speed", CheckCategory.MOVEMENT);
    }

    public CheckResult check(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();
        if (player.isInsideVehicle()) return CheckResult.pass();
        if (player.isFlying()) return CheckResult.pass();
        if (player.isGliding()) return CheckResult.pass();
        if (player.isSwimming()) return CheckResult.pass();
        if (System.currentTimeMillis() - data.getTeleportTime() < 2500) return CheckResult.pass();

        if (System.currentTimeMillis() - data.getLastWindChargeLaunchTime() < 3000) return CheckResult.pass();
        if (System.currentTimeMillis() - data.getLastMaceSmashTime() < 3000) return CheckResult.pass();

        double speed = MathUtil.horizontalSpeed(from, to);
        data.addSpeedSample(speed);


        if (data.getSpeedSampleCount() < 8) return CheckResult.pass();

        double avgSpeed = data.getAverageSpeed();
        double maxSpeed = getMaxAllowedSpeed(player, data);

        if (avgSpeed > maxSpeed) {
            double excess = avgSpeed - maxSpeed;
            return CheckResult.fail(String.format(
                    "vitesse_moy=%.3f max=%.3f excès=%.3f (conf=%.2f)",
                    avgSpeed, maxSpeed, excess, data.getConfidenceScore()));
        }
        return CheckResult.pass();
    }

    private double getMaxAllowedSpeed(Player player, PlayerData data) {
        double tolerance = plugin.getConfig().getDouble("checks.speed.tolerance", 1.25);

        tolerance *= data.getToleranceMultiplier();
        double max = BASE_MAX_SPEED * tolerance;


        var speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (speedEffect != null) {
            max += (speedEffect.getAmplifier() + 1) * 0.040;
        }


        Block blockBelow = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        Material below = blockBelow.getType();
        if (below == Material.ICE || below == Material.PACKED_ICE) {
            max *= 2.5;
        } else if (below == Material.BLUE_ICE) {
            max *= 4.5;
        }



        return max;
    }
}
