package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.ElysiaAntiCheat;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Gestionnaire SQLite pour la persistance des violations.
 * Stocke l'historique complet même pour les joueurs hors ligne.
 */
public class DatabaseManager {

    private final ElysiaAntiCheat plugin;
    private Connection connection;

    public DatabaseManager(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
    }

    public void init() {
        if (!plugin.getConfig().getBoolean("database.enabled", false)) return;

        try {
            String fileName = plugin.getConfig().getString("database.file", "violations.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            dbFile.getParentFile().mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            plugin.getLogger().info("[DB] Base de données SQLite initialisée : " + dbFile.getName());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Erreur d'initialisation SQLite", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS violations (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid      TEXT NOT NULL,
                        name      TEXT NOT NULL,
                        check_name TEXT NOT NULL,
                        vl        INTEGER NOT NULL,
                        details   TEXT,
                        timestamp INTEGER NOT NULL
                    )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON violations(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON violations(timestamp)");
        }
    }

    public boolean isEnabled() {
        return connection != null && plugin.getConfig().getBoolean("database.enabled", false);
    }

    /** Enregistre une violation de façon asynchrone. */
    public void logViolation(String uuid, String name, String checkName, int vl, String details) {
        if (!isEnabled()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO violations (uuid, name, check_name, vl, details, timestamp) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, uuid);
                ps.setString(2, name);
                ps.setString(3, checkName);
                ps.setInt(4, vl);
                ps.setString(5, details);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();

                pruneOldRecords(uuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[DB] Erreur lors de l'enregistrement", e);
            }
        });
    }

    /** Récupère les N dernières violations d'un joueur (async + callback). */
    public void getViolations(String uuid, int limit, java.util.function.Consumer<List<String>> callback) {
        if (!isEnabled()) { callback.accept(Collections.emptyList()); return; }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> results = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT check_name, vl, details, timestamp FROM violations WHERE uuid=? ORDER BY timestamp DESC LIMIT ?")) {
                ps.setString(1, uuid);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    long ts = rs.getLong("timestamp");
                    String date = new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date(ts));
                    results.add(String.format("[%s] %s VL:%d — %s",
                            date, rs.getString("check_name"), rs.getInt("vl"), rs.getString("details")));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[DB] Erreur de lecture", e);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(results));
        });
    }

    private void pruneOldRecords(String uuid) throws SQLException {
        int max = plugin.getConfig().getInt("database.max-records-per-player", 100);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM violations WHERE uuid=? AND id NOT IN " +
                "(SELECT id FROM violations WHERE uuid=? ORDER BY timestamp DESC LIMIT ?)")) {
            ps.setString(1, uuid);
            ps.setString(2, uuid);
            ps.setInt(3, max);
            ps.executeUpdate();
        }
    }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }
}
