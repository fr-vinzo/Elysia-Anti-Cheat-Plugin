package fr.elysia.anticheat.checks.combat;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.utils.MathUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ReachCheck extends Check {

    public ReachCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Reach", CheckCategory.COMBAT);
    }

    public CheckResult check(Player player, Entity target) {
        if (!isEnabled()) return CheckResult.pass();

        double maxReach = plugin.getConfig().getDouble("checks.reach.max-reach", 4.5);

        org.bukkit.Location targetCenter = target.getLocation().clone();
        targetCenter.setY(targetCenter.getY() + target.getHeight() / 2.0);
        double distance = MathUtil.distance3D(player.getEyeLocation(), targetCenter);

        int ping = player.getPing();
        double lagBuffer = Math.min(ping / 1000.0 * 4.0, 1.5);
        double effectiveMax = maxReach + lagBuffer;

        if (distance > effectiveMax) {
            return CheckResult.fail(String.format("distance=%.2f max=%.2f (ping=%dms)", distance, effectiveMax, ping));
        }

        return CheckResult.pass();
    }
}
