package fr.elysia.anticheat.checks.world;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import fr.elysia.anticheat.utils.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Détecte le Scaffold / Tower hack :
 * - Scaffold : poser des blocs sous soi tout en se déplaçant horizontalement
 *   (pont automatique en avançant).
 * - Tower : poser des blocs sous soi tout en sautant verticalement.
 *
 * Un joueur légitime place parfois des blocs sous lui, mais pas de façon
 * répétée et coordonnée à chaque tick de déplacement.
 */
public class ScaffoldCheck extends Check {

    public ScaffoldCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Scaffold", CheckCategory.WORLD);
    }

    public CheckResult check(Player player, Location placedBlock, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        Location playerFeet = player.getLocation();

        // Le bloc posé doit être au niveau des pieds ou en dessous
        int dy = playerFeet.getBlockY() - placedBlock.getBlockY();
        if (dy < 0 || dy > 2) {
            data.resetScaffold();
            return CheckResult.pass();
        }

        // Vérifier si le joueur se déplace horizontalement (scaffold) ou verticalement (tower)
        double horizontalSpeed = data.getPreviousLocation() != null
                ? MathUtil.horizontalSpeed(data.getLastLocation(), playerFeet)
                : 0;

        boolean moving = horizontalSpeed > 0.05 || !player.isOnGround();
        if (!moving) {
            data.resetScaffold();
            return CheckResult.pass();
        }

        // Incrémenter le compteur de blocs scaffold consécutifs
        long now = System.currentTimeMillis();
        // Reset si plus de 2 secondes sans scaffold
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
