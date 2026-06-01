package fr.elysia.anticheat.api;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Collection;

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


    public void registerCheck(Check check) {
        plugin.getCheckManager().register(check);
    }


    public void unregisterCheck(String checkName) {
        plugin.getCheckManager().unregister(checkName);
    }


    public Collection<Check> getChecks() {
        return plugin.getCheckManager().getAll();
    }


    public int getViolationLevel(Player player, String checkName) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data == null ? 0 : data.getViolationLevel(checkName);
    }


    public PlayerData getPlayerData(Player player) {
        return plugin.getPlayerDataManager().get(player.getUniqueId());
    }


    public void resetViolations(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) data.resetAllViolations();
    }
}
