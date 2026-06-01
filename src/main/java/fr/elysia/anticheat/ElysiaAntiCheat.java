package fr.elysia.anticheat;

import fr.elysia.anticheat.api.CheckAPI;
import fr.elysia.anticheat.commands.AnticheatCommand;
import fr.elysia.anticheat.commands.AnticheatTabCompleter;
import fr.elysia.anticheat.listeners.*;
import fr.elysia.anticheat.managers.*;
import fr.elysia.anticheat.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElysiaAntiCheat extends JavaPlugin {

    private static ElysiaAntiCheat instance;

    private PlayerDataManager playerDataManager;
    private CheckManager checkManager;
    private ViolationManager violationManager;
    private AlertManager alertManager;
    private AntiXRayManager antiXRayManager;
    private CheckAPI checkAPI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Managers
        playerDataManager = new PlayerDataManager(this);
        checkManager      = new CheckManager(this);
        violationManager  = new ViolationManager(this);
        alertManager      = new AlertManager(this);
        antiXRayManager   = new AntiXRayManager(this);
        checkAPI          = new CheckAPI(this);

        // Enregistrement des checks par défaut
        checkManager.registerDefaults();

        // Listeners
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new BlockListener(this), this);

        // Commandes
        var eacCmd = getCommand("eac");
        if (eacCmd != null) {
            eacCmd.setExecutor(new AnticheatCommand(this));
            eacCmd.setTabCompleter(new AnticheatTabCompleter(this));
        }

        // Tâche de décroissance des violations
        int decayInterval = getConfig().getInt("punishments.violation-decay-interval", 60);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> playerDataManager.decayAll(),
                decayInterval * 20L, decayInterval * 20L);

        // Obfuscation initiale pour les joueurs déjà en ligne (ex: /reload)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                playerDataManager.create(player);
                if (antiXRayManager.isEnabled()) {
                    antiXRayManager.obfuscateChunkForPlayer(player, player.getLocation().getChunk());
                }
            }
        }, 20L);

        getLogger().info(MessageUtil.colorize(
                "&6[Elysia AC] &aPlugin chargé avec succès ! " + checkManager.getAll().size() + " checks actifs."));
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        playerDataManager.getAll().forEach(d ->
                antiXRayManager.cleanup(d.getUuid()));
        getLogger().info("[Elysia AC] Plugin désactivé.");
    }

    // ---- Getters ----

    public static ElysiaAntiCheat getInstance() { return instance; }

    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public CheckManager getCheckManager()           { return checkManager; }
    public ViolationManager getViolationManager()   { return violationManager; }
    public AlertManager getAlertManager()           { return alertManager; }
    public AntiXRayManager getAntiXRayManager()     { return antiXRayManager; }
    public CheckAPI getCheckAPI()                   { return checkAPI; }
}
