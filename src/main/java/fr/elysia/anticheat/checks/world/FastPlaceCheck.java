package fr.elysia.anticheat.checks.world;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Détecte un placement de blocs trop rapide (FastPlace).
 * Vanilla max légitime : ~10–12 blocs/seconde en sprint.
 */
public class FastPlaceCheck extends Check {

    public FastPlaceCheck(ElysiaAntiCheat plugin) {
        super(plugin, "FastPlace", CheckCategory.WORLD);
    }

    public CheckResult check(Player player, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        data.recordPlace();
        int pps = data.getPlacesPerSecond();
        int maxPPS = plugin.getConfig().getInt("checks.fast-place.max-per-second", 12);

        if (pps > maxPPS) {
            return CheckResult.fail("placements/sec=" + pps + " max=" + maxPPS);
        }
        return CheckResult.pass();
    }
}
