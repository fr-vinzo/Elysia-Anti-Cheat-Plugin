package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("deprecation")
public class FlyCheck extends Check {

    public FlyCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Fly", CheckCategory.MOVEMENT);
    }

    public CheckResult check(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        if (player.getAllowFlight() || player.isFlying()) return CheckResult.pass();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return CheckResult.pass();
        if (player.isInsideVehicle()) return CheckResult.pass();
        if (player.isGliding()) return CheckResult.pass();
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) return CheckResult.pass();
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return CheckResult.pass();
        if (System.currentTimeMillis() - data.getTeleportTime() < 2500) return CheckResult.pass();

        if (System.currentTimeMillis() - data.getLastMaceSmashTime() < 3000) return CheckResult.pass();

        if (System.currentTimeMillis() - data.getLastWindChargeLaunchTime() < 3000) return CheckResult.pass();

        boolean onGround = player.isOnGround();
        double dy = to.getY() - from.getY();

        if (onGround) {
            data.resetAirtime();
            return CheckResult.pass();
        }





        if (dy < -0.15) {

            data.resetAirtime();
            return CheckResult.pass();
        }

        data.incrementAirtime();

        int threshold = plugin.getConfig().getInt("checks.fly.airtime-threshold", 25);

        int effectiveThreshold = (int) (threshold * data.getToleranceMultiplier());

        if (data.getAirtimeTicks() >= effectiveThreshold) {
            return CheckResult.fail("airtime=" + data.getAirtimeTicks() + " ticks, dy=" + String.format("%.4f", dy));
        }

        return CheckResult.pass();
    }
}
