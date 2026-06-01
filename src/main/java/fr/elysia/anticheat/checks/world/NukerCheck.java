package fr.elysia.anticheat.checks.world;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NukerCheck extends Check {

    public NukerCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Nuker", CheckCategory.WORLD);
    }

    public CheckResult check(Player player, Location broken, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        data.recordBreakLocation(broken);

        List<Location> recent = new ArrayList<>(data.getRecentBreakLocations());
        int minBreaks = plugin.getConfig().getInt("checks.nuker.min-breaks", 8);
        if (recent.size() < minBreaks) return CheckResult.pass();


        long windowMs = plugin.getConfig().getLong("checks.nuker.time-window-ms", 300);
        long now = System.currentTimeMillis();
        long lastBreak = data.getLastBreakTime();
        if (now - lastBreak > windowMs) return CheckResult.pass();


        double maxSpread = 0;
        for (int i = 0; i < recent.size(); i++) {
            for (int j = i + 1; j < recent.size(); j++) {
                if (!recent.get(i).getWorld().equals(recent.get(j).getWorld())) continue;
                double dist = recent.get(i).distance(recent.get(j));
                maxSpread = Math.max(maxSpread, dist);
            }
        }



        double maxAllowed = plugin.getConfig().getDouble("checks.nuker.max-spread", 6.5);

        if (maxSpread > maxAllowed) {
            return CheckResult.fail(String.format(
                    "dispersion=%.1f blocs (max=%.1f) en %dms, count=%d",
                    maxSpread, maxAllowed, windowMs, recent.size()));
        }

        return CheckResult.pass();
    }
}
