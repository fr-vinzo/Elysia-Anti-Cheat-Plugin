package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.ElysiaAntiCheat;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Level;

/**
 * Envoie des alertes critiques sur un webhook Discord.
 * Utilise l'API Discord Embed pour un rendu propre.
 * Aucune dépendance externe — uniquement java.net.
 */
public class DiscordWebhookManager {

    private final ElysiaAntiCheat plugin;
    private String webhookUrl;

    public DiscordWebhookManager(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false)
                && webhookUrl != null && !webhookUrl.isBlank();
    }

    /** Envoie une alerte de violation sur Discord de façon asynchrone. */
    public void sendViolationAlert(String playerName, String checkName, int vl, String details) {
        if (!isEnabled()) return;

        int minVL = plugin.getConfig().getInt("discord.min-vl", 10);
        boolean kicksOnly = plugin.getConfig().getBoolean("discord.kicks-only", false);
        if (kicksOnly) return;
        if (vl < minVL) return;

        String json = buildEmbed(
                "⚠ Alerte Anti-Cheat",
                "**" + playerName + "** a déclenché **" + checkName + "**",
                0xFFA500, // Orange
                playerName, checkName, vl, details);

        sendAsync(json);
    }

    /** Envoie un message de kick sur Discord. */
    public void sendKickAlert(String playerName, String checkName) {
        if (!isEnabled()) return;

        String json = buildEmbed(
                "🔨 Joueur expulsé",
                "**" + playerName + "** a été kické pour **" + checkName + "**",
                0xFF0000, // Rouge
                playerName, checkName, -1, "Seuil de VL atteint");

        sendAsync(json);
    }

    private String buildEmbed(String title, String description, int color,
                               String playerName, String checkName, int vl, String details) {
        String vlField = vl >= 0 ? ",{\"name\":\"VL\",\"value\":\"" + vl + "\",\"inline\":true}" : "";
        return String.format("""
                {
                  "username": "Elysia Anti-Cheat",
                  "avatar_url": "https://minotar.net/avatar/%s",
                  "embeds": [{
                    "title": "%s",
                    "description": "%s",
                    "color": %d,
                    "fields": [
                      {"name":"Joueur","value":"%s","inline":true},
                      {"name":"Check","value":"%s","inline":true}%s,
                      {"name":"Détails","value":"%s","inline":false}
                    ],
                    "footer": {"text": "Elysia Server"},
                    "timestamp": "%s"
                  }]
                }
                """,
                playerName, title, description, color,
                playerName, checkName, vlField, escapeJson(details),
                Instant.now().toString());
    }

    private void sendAsync(String json) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "ElysiaAntiCheat/1.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code != 204 && code != 200) {
                    plugin.getLogger().warning("[Discord] Code réponse inattendu : " + code);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Discord] Envoi échoué", e);
            }
        });
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
