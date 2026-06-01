package fr.elysia.anticheat.checks.combat;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class VelocityCheck extends Check {


    private static final double MIN_KB_MOVEMENT = 0.08;

    public VelocityCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Velocity", CheckCategory.COMBAT);
    }


    public void recordKnockback(Player victim, Player attacker, PlayerData data) {
        if (!isEnabled()) return;
        if (victim.hasPotionEffect(PotionEffectType.RESISTANCE)) return;


        Vector kb = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector())
                .setY(0).normalize();

        data.setExpectedKbX(kb.getX());
        data.setExpectedKbZ(kb.getZ());
        data.setLastKnockbackTime(System.currentTimeMillis());
    }


    public CheckResult checkMovement(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        long since = System.currentTimeMillis() - data.getLastKnockbackTime();

        if (since < 50 || since > 300) return CheckResult.pass();

        double kbX = data.getExpectedKbX();
        double kbZ = data.getExpectedKbZ();
        if (kbX == 0 && kbZ == 0) return CheckResult.pass();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double movement = Math.sqrt(dx * dx + dz * dz);


        double dot = (dx * kbX + dz * kbZ);

        double minPercent = plugin.getConfig().getDouble("checks.velocity.min-knockback-percent", 40.0) / 100.0;


        if (movement < MIN_KB_MOVEMENT || dot < minPercent * movement) {

            data.setLastKnockbackTime(0);
            return CheckResult.fail(String.format("recul_ignoré: dépl=%.3f dot=%.3f", movement, dot));
        }

        data.setLastKnockbackTime(0);
        return CheckResult.pass();
    }
}
