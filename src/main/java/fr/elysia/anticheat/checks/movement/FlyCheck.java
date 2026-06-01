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

public class FlyCheck extends Check {

    public FlyCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Fly", CheckCategory.MOVEMENT);
    }

    public CheckResult check(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        // Exemptions légitimes
        if (player.getAllowFlight() || player.isFlying()) return CheckResult.pass();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return CheckResult.pass();
        if (player.isInsideVehicle()) return CheckResult.pass();
        if (player.isGliding()) return CheckResult.pass();
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) return CheckResult.pass();
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return CheckResult.pass();
        if (System.currentTimeMillis() - data.getTeleportTime() < 2000) return CheckResult.pass();

        boolean onGround = player.isOnGround();
        boolean movingDown = to.getY() < from.getY();

        if (onGround || movingDown) {
            data.resetAirtime();
            return CheckResult.pass();
        }

        // Le joueur monte ou reste stable en l'air
        double dy = to.getY() - from.getY();
        if (dy >= -0.08) { // pas en train de tomber à la vitesse attendue
            data.incrementAirtime();
        } else {
            data.resetAirtime();
        }

        int threshold = plugin.getConfig().getInt("checks.fly.airtime-threshold", 25);
        if (data.getAirtimeTicks() >= threshold) {
            return CheckResult.fail("airtime=" + data.getAirtimeTicks() + " ticks, dy=" + String.format("%.3f", dy));
        }

        return CheckResult.pass();
    }
}
