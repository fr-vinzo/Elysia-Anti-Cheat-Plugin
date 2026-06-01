package fr.elysia.anticheat.api;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Point d'entrée de l'API publique d'Elysia Anti-Cheat.
 * D'autres plugins peuvent s'y connecter pour enregistrer des checks
 * ou consulter les données de violation.
 *
 * Usage :
 * <pre>
 *   CheckAPI api = CheckAPI.get();
 *   api.registerCheck(new MyCustomCheck(plugin));
 *   int vl = api.getViolationLevel(player, "MyCheck");
 * </pre>
 */
public class CheckAPI {

    private static CheckAPI instance;
    private final ElysiaAntiCheat plugin;

    public CheckAPI(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static CheckAPI get() {
        if (instance == null) throw new IllegalStateException("ElysiaAntiCheat n'est pas encore chargé.");
        return instance;
    }

    /** Enregistre un check personnalisé dans le système. */
    public void registerCheck(Check check) {
        plugin.getCheckManager().register(check);
    }

    /** Supprime un check par son nom. */
    public void unregisterCheck(String checkName) {
        plugin.getCheckManager().unregister(checkName);
    }

    /** Retourne tous les checks enregistrés. */
    public Collection<Check> getChecks() {
        return plugin.getCheckManager().getAll();
    }

    /** Retourne le niveau de violation d'un joueur pour un check donné. */
    public int getViolationLevel(Player player, String checkName) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data == null ? 0 : data.getViolationLevel(checkName);
    }

    /** Retourne les données brutes du joueur (null si hors ligne). */
    public PlayerData getPlayerData(Player player) {
        return plugin.getPlayerDataManager().get(player.getUniqueId());
    }

    /** Remet à zéro toutes les violations d'un joueur. */
    public void resetViolations(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) data.resetAllViolations();
    }
}
