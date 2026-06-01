package fr.elysia.anticheat.checks.combat;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import fr.elysia.anticheat.utils.MathUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KillAuraCheck extends Check {

    public KillAuraCheck(ElysiaAntiCheat plugin) {
        super(plugin, "KillAura", CheckCategory.COMBAT);
    }

    public CheckResult check(Player player, Entity target, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        data.recordHit(target.getUniqueId());


        int maxCPS = plugin.getConfig().getInt("checks.kill-aura.max-cps", 18);
        int cps = data.getCPS();
        if (cps > maxCPS) {
            return CheckResult.fail("CPS=" + cps + " max=" + maxCPS);
        }




        double maxAngle = plugin.getConfig().getDouble("checks.kill-aura.max-angle", 90.0);
        double effectiveMaxAngle = maxAngle * data.getToleranceMultiplier();
        double angle = MathUtil.getAngleToTarget(player, target);

        if (angle > effectiveMaxAngle) {
            return CheckResult.fail(String.format(
                    "angle=%.1f° max=%.1f° cible=%s", angle, effectiveMaxAngle, target.getType().name()));
        }


        int maxTargets = plugin.getConfig().getInt("checks.kill-aura.max-targets-per-tick", 2);
        int targets = data.getRecentTargetCount();
        if (targets > maxTargets) {
            return CheckResult.fail("cibles_500ms=" + targets + " max=" + maxTargets);
        }

        return CheckResult.pass();
    }
}
