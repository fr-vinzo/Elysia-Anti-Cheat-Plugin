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
 * Détecte le Nuker : casser un grand nombre de blocs très dispersés en très peu de temps.
 *
 * Distinction pioche 5x5 légitime vs nuker :
 * - Pioche 5x5 : dispersion max ~5.7 blocs (diagonale d'un carré 4×4), fenêtre ~500ms
 * - Vrai nuker  : dispersion > 7 blocs OU breaks dans un rayon impossible en < 200ms
 *
 * Les seuils sont configurables dans config.yml.
 */
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

        // Fenêtre temporelle : ne considérer que les blocs cassés dans les X dernières ms
        long windowMs = plugin.getConfig().getLong("checks.nuker.time-window-ms", 300);
        long now = System.currentTimeMillis();
        long lastBreak = data.getLastBreakTime();
        if (now - lastBreak > windowMs) return CheckResult.pass();

        // Calculer la dispersion maximale entre les positions récentes
        double maxSpread = 0;
        for (int i = 0; i < recent.size(); i++) {
            for (int j = i + 1; j < recent.size(); j++) {
                if (!recent.get(i).getWorld().equals(recent.get(j).getWorld())) continue;
                double dist = recent.get(i).distance(recent.get(j));
                maxSpread = Math.max(maxSpread, dist);
            }
        }

        // Seuil : 6.5 blocs couvre une pioche 5x5 (diagonale = 5.66)
        // Au-delà c'est un nuker
        double maxAllowed = plugin.getConfig().getDouble("checks.nuker.max-spread", 6.5);

        if (maxSpread > maxAllowed) {
            return CheckResult.fail(String.format(
                    "dispersion=%.1f blocs (max=%.1f) en %dms, count=%d",
                    maxSpread, maxAllowed, windowMs, recent.size()));
        }

        return CheckResult.pass();
    }
}
