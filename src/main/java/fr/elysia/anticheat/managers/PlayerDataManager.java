package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final ElysiaAntiCheat plugin;
    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    public PlayerDataManager(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
    }

    public PlayerData create(Player player) {
        PlayerData data = new PlayerData(player.getUniqueId(), player.getName());
        data.setLastLocation(player.getLocation());
        dataMap.put(player.getUniqueId(), data);
        return data;
    }

    public void remove(UUID uuid) {
        dataMap.remove(uuid);
    }

    public PlayerData get(UUID uuid) {
        return dataMap.get(uuid);
    }

    public PlayerData getOrCreate(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(),
                id -> new PlayerData(id, player.getName()));
    }

    public Collection<PlayerData> getAll() {
        return dataMap.values();
    }

    public boolean has(UUID uuid) {
        return dataMap.containsKey(uuid);
    }

    /** Décroissance périodique des violations pour réduire les faux positifs. */
    public void decayAll() {
        int amount = plugin.getConfig().getInt("punishments.violation-decay-rate", 2);
        for (PlayerData data : dataMap.values()) {
            data.decayViolations(amount);
        }
    }
}
