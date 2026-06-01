package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;

/**
 * Détecte le Timer hack : le client envoie plus de paquets de mouvement
 * par seconde que le serveur n'en accepte (~20/s), ce qui permet d'aller
 * plus vite sans déclencher les checks classiques de vitesse.
 */
public class TimerCheck extends Check {

    public TimerCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Timer", CheckCategory.MOVEMENT);
    }

    public CheckResult check(PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();

        data.recordMovePacket();
        int pps = data.getMovePacketsPerSecond();
        // 20 TPS × tolérance légère = 24 max normal
        int maxPPS = plugin.getConfig().getInt("checks.timer.max-packets-per-second", 24);

        // Appliquer tolérance confiance joueur
        int effectiveMax = (int) (maxPPS * data.getToleranceMultiplier());

        if (pps > effectiveMax) {
            return CheckResult.fail("paquets/sec=" + pps + " max=" + effectiveMax);
        }
        return CheckResult.pass();
    }
}
