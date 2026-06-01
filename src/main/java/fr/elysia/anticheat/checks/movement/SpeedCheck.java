package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.utils.MathUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class SpeedCheck extends Check {

    // Vitesse max en sprint vanilla (blocs/tick × 20 ticks = blocs/sec)
    private static final double BASE_MAX_SPEED = 0.34; // blocs/tick ~= 6.8 blocs/sec

    public SpeedCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Speed", CheckCategory.MOVEMENT);
    }

    public CheckResult check(Player player, Location from, Location to) {
        if (!isEnabled()) return CheckResult.pass();
        if (player.isInsideVehicle()) return CheckResult.pass();
        if (player.isFlying()) return CheckResult.pass();
        if (player.isSwimming()) return CheckResult.pass();

        double speed = MathUtil.horizontalSpeed(from, to);
        double maxSpeed = getMaxAllowedSpeed(player);

        if (speed > maxSpeed) {
            double excess = speed - maxSpeed;
            return CheckResult.fail(String.format("vitesse=%.2f max=%.2f excès=%.2f", speed, maxSpeed, excess));
        }
        return CheckResult.pass();
    }

    private double getMaxAllowedSpeed(Player player) {
        double tolerance = plugin.getConfig().getDouble("checks.speed.tolerance", 1.25);
        double max = BASE_MAX_SPEED * tolerance;

        // Potion de vitesse : chaque niveau ajoute ~0.04 blocs/tick
        var speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (speedEffect != null) {
            max += (speedEffect.getAmplifier() + 1) * 0.040;
        }

        // Blocs glissants (glace, glace bleue)
        Block blockBelow = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        Material below = blockBelow.getType();
        if (below == Material.ICE || below == Material.PACKED_ICE) {
            max *= 2.5;
        } else if (below == Material.BLUE_ICE) {
            max *= 4.0;
        }

        // Tolérance téléportation
        if (System.currentTimeMillis() - getPlayerTeleportTime(player) < 2000) {
            return Double.MAX_VALUE;
        }

        return max;
    }

    private long getPlayerTeleportTime(Player player) {
        var data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data == null ? 0 : data.getTeleportTime();
    }
}
