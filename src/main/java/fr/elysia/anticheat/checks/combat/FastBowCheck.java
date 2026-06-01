package fr.elysia.anticheat.checks.combat;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Détecte un tir à l'arc trop rapide.
 * Un arc prend au moins 1 seconde (20 ticks) pour être pleinement bandé.
 * Avec Quick Charge III sur une arbalète, minimum ~400ms.
 */
public class FastBowCheck extends Check {

    public FastBowCheck(ElysiaAntiCheat plugin) {
        super(plugin, "FastBow", CheckCategory.COMBAT);
    }

    public CheckResult check(Player player, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        long now = System.currentTimeMillis();
        long lastShot = data.getLastBowShot();

        if (lastShot > 0) {
            long elapsed = now - lastShot;
            long minInterval = plugin.getConfig().getLong("checks.fast-bow.min-interval-ms", 400);

            if (elapsed < minInterval) {
                data.setLastBowShot(now);
                return CheckResult.fail(String.format("intervalle_tir=%dms (min=%dms)", elapsed, minInterval));
            }
        }

        data.setLastBowShot(now);
        return CheckResult.pass();
    }
}
