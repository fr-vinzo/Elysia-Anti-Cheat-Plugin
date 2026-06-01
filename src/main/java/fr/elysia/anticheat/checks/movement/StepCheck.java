package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class StepCheck extends Check {

    public StepCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Step", CheckCategory.MOVEMENT);
    }

    @SuppressWarnings("deprecation")
    public CheckResult check(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();
        if (player.isInsideVehicle()) return CheckResult.pass();
        if (player.isFlying()) return CheckResult.pass();
        if (player.isGliding()) return CheckResult.pass();
        if (System.currentTimeMillis() - data.getTeleportTime() < 2000) return CheckResult.pass();

        double dy = to.getY() - from.getY();


        if (!data.wasOnGround()) {
            data.setWasOnGround(player.isOnGround());
            return CheckResult.pass();
        }

        double maxStep = plugin.getConfig().getDouble("checks.step.max-step-height", 0.65);



        if (data.wasOnGround() && dy > maxStep && dy < 1.5) {


            double tolerance = data.getToleranceMultiplier() * maxStep;
            if (dy > tolerance) {
                data.setWasOnGround(player.isOnGround());
                return CheckResult.fail(String.format("step=%.3f blocs (max=%.3f)", dy, tolerance));
            }
        }

        data.setWasOnGround(player.isOnGround());
        return CheckResult.pass();
    }
}
