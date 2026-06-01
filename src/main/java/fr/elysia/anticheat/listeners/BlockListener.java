package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.world.XRayCheck;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class BlockListener implements Listener {

    private final ElysiaAntiCheat plugin;
    private final XRayCheck xrayCheck;

    public BlockListener(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
        this.xrayCheck = plugin.getCheckManager().getTyped(XRayCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        boolean isOre = xrayCheck != null && xrayCheck.isOre(event.getBlock().getType());
        data.recordBlockMined(isOre);

        // Révéler les minerais adjacents obfusqués
        plugin.getAntiXRayManager().revealAdjacentOres(player, event.getBlock().getLocation());

        // Analyser les statistiques XRay
        if (xrayCheck != null) {
            CheckResult r = xrayCheck.analyze(data);
            if (r.isFlagged()) {
                // Marquer comme suspect si pas déjà fait
                if (!data.isXraySuspect()) {
                    data.setXraySuspect(true);
                    // Envoyer des faux minerais pour perturber le X-rayer
                    plugin.getAntiXRayManager().sendFakeOres(player);
                }
                plugin.getViolationManager().flag(player, xrayCheck.getName(), r.getDetails());
            } else {
                data.setXraySuspect(false);
            }
        }
    }

    /** Détecte le changement de chunk pour obfusquer les nouveaux chunks chargés. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;
        if (!plugin.getAntiXRayManager().isEnabled()) return;

        if (event.getFrom().getChunk().equals(event.getTo() == null ? event.getFrom().getChunk() : event.getTo().getChunk()))
            return;

        if (event.getTo() == null) return;

        Chunk newChunk = event.getTo().getChunk();
        plugin.getAntiXRayManager().obfuscateChunkForPlayer(player, newChunk);
    }
}
