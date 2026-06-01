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

/**
 * Détecte le Nuker : casser des blocs dans un rayon étendu simultanément.
 *
 * Vanilla : un joueur ne peut casser qu'un bloc à la fois, dans un rayon ~4.5 blocs.
 * Un nuker casse plusieurs blocs par tick dans différentes directions.
 *
 * Approche : si les derniers blocs cassés sont très éloignés les uns des autres
 * en peu de temps, c'est suspect.
 */
public class NukerCheck extends Check {

    public NukerCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Nuker", CheckCategory.WORLD);
    }

    public CheckResult check(Player player, Location broken, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        data.recordBreakLocation(broken);

        List<Location> recent = new ArrayList<>(data.getRecentBreakLocations());
        int minBreaks = plugin.getConfig().getInt("checks.nuker.min-breaks", 5);
        if (recent.size() < minBreaks) return CheckResult.pass();

        // Calculer la dispersion maximale des positions récentes
        double maxSpread = 0;
        for (int i = 0; i < recent.size(); i++) {
            for (int j = i + 1; j < recent.size(); j++) {
                if (!recent.get(i).getWorld().equals(recent.get(j).getWorld())) continue;
                double dist = recent.get(i).distance(recent.get(j));
                maxSpread = Math.max(maxSpread, dist);
            }
        }

        double maxAllowed = plugin.getConfig().getDouble("checks.nuker.max-spread", 4.0);
        if (maxSpread > maxAllowed) {
            return CheckResult.fail(String.format(
                    "dispersion=%.1f blocs (max=%.1f), count=%d",
                    maxSpread, maxAllowed, recent.size()));
        }

        return CheckResult.pass();
    }
}
