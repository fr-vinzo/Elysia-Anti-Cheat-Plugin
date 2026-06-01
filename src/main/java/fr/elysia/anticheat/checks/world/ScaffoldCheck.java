package fr.elysia.anticheat.checks.world;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import fr.elysia.anticheat.utils.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class ScaffoldCheck extends Check {

    public ScaffoldCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Scaffold", CheckCategory.WORLD);
    }

    public CheckResult check(Player player, Location placedBlock, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        Location playerFeet = player.getLocation();


        int dy = playerFeet.getBlockY() - placedBlock.getBlockY();
        if (dy < 0 || dy > 2) {
            data.resetScaffold();
            return CheckResult.pass();
        }


        double horizontalSpeed = data.getPreviousLocation() != null
                ? MathUtil.horizontalSpeed(data.getPreviousLocation(), playerFeet)
                : 0;

        boolean moving = horizontalSpeed > 0.15 || !player.isOnGround();
        if (!moving) {
            data.resetScaffold();
            return CheckResult.pass();
        }


        long now = System.currentTimeMillis();

        if (now - data.getLastScaffoldTime() > 2000 && data.getLastScaffoldTime() > 0) {
            data.resetScaffold();
        }

        data.incrementScaffold();

        int threshold = plugin.getConfig().getInt("checks.scaffold.min-consecutive", 6);
        if (data.getScaffoldCount() >= threshold) {
            return CheckResult.fail("scaffold=" + data.getScaffoldCount()
                    + " blocs consécutifs, vitesse=" + String.format("%.2f", horizontalSpeed));
        }

        return CheckResult.pass();
    }
}
