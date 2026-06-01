package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Détecte une montée d'échelle / liane trop rapide.
 * Vitesse max vanilla sur une échelle : ~0.118 blocs/tick.
 */
public class FastLadderCheck extends Check {

    // Vitesse max vanilla sur échelle/lianes
    private static final double MAX_LADDER_SPEED = 0.118;

    public FastLadderCheck(ElysiaAntiCheat plugin) {
        super(plugin, "FastLadder", CheckCategory.MOVEMENT);
    }

    public CheckResult check(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();
        if (player.isInsideVehicle()) return CheckResult.pass();

        Material atFeet = to.getBlock().getType();
        if (!isClimbable(atFeet)) {
            data.setLastLadderY(0);
            return CheckResult.pass();
        }

        double dy = to.getY() - from.getY();
        if (dy <= 0) return CheckResult.pass(); // descend, pas de check

        double maxSpeed = plugin.getConfig().getDouble("checks.fast-ladder.max-speed", MAX_LADDER_SPEED);
        double tolerance = data.getToleranceMultiplier() * maxSpeed;

        if (dy > tolerance) {
            return CheckResult.fail(String.format("vitesse_échelle=%.4f max=%.4f", dy, tolerance));
        }

        data.setLastLadderY(to.getY());
        data.setLastLadderTime(System.currentTimeMillis());
        return CheckResult.pass();
    }

    private boolean isClimbable(Material m) {
        return m == Material.LADDER || m == Material.VINE
                || m == Material.TWISTING_VINES || m == Material.WEEPING_VINES
                || m == Material.SCAFFOLDING;
    }
}
