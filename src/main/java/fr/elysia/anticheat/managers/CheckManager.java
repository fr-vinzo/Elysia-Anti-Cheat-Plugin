package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.checks.combat.KillAuraCheck;
import fr.elysia.anticheat.checks.combat.ReachCheck;
import fr.elysia.anticheat.checks.movement.FlyCheck;
import fr.elysia.anticheat.checks.movement.NoFallCheck;
import fr.elysia.anticheat.checks.movement.SpeedCheck;
import fr.elysia.anticheat.checks.world.XRayCheck;

import java.util.*;
import java.util.stream.Collectors;

public class CheckManager {

    private final ElysiaAntiCheat plugin;
    private final Map<String, Check> checks = new LinkedHashMap<>();

    public CheckManager(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
        register(new SpeedCheck(plugin));
        register(new FlyCheck(plugin));
        register(new NoFallCheck(plugin));
        register(new KillAuraCheck(plugin));
        register(new ReachCheck(plugin));
        register(new XRayCheck(plugin));
    }

    public void register(Check check) {
        checks.put(check.getName().toLowerCase(), check);
        if (plugin.getConfig().getBoolean("general.debug", false)) {
            plugin.getLogger().info("Check enregistré : " + check.getName());
        }
    }

    public void unregister(String name) {
        checks.remove(name.toLowerCase());
    }

    public Check get(String name) {
        return checks.get(name.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    public <T extends Check> T getTyped(Class<T> type) {
        return checks.values().stream()
                .filter(c -> type.isAssignableFrom(c.getClass()))
                .map(c -> (T) c)
                .findFirst()
                .orElse(null);
    }

    public Collection<Check> getAll() {
        return Collections.unmodifiableCollection(checks.values());
    }

    public List<Check> getByCategory(CheckCategory category) {
        return checks.values().stream()
                .filter(c -> c.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<String> getCheckNames() {
        return new ArrayList<>(checks.keySet());
    }
}
