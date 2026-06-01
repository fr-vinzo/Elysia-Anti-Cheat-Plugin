package fr.elysia.anticheat.listeners;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.world.*;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class BlockListener implements Listener {

    private final ElysiaAntiCheat plugin;
    private final XRayCheck xrayCheck;
    private final NukerCheck nukerCheck;
    private final InstaBreakCheck instaBreakCheck;
    private final FastPlaceCheck fastPlaceCheck;
    private final ScaffoldCheck scaffoldCheck;

    public BlockListener(ElysiaAntiCheat plugin) {
        this.plugin          = plugin;
        this.xrayCheck       = plugin.getCheckManager().getTyped(XRayCheck.class);
        this.nukerCheck      = plugin.getCheckManager().getTyped(NukerCheck.class);
        this.instaBreakCheck = plugin.getCheckManager().getTyped(InstaBreakCheck.class);
        this.fastPlaceCheck  = plugin.getCheckManager().getTyped(FastPlaceCheck.class);
        this.scaffoldCheck   = plugin.getCheckManager().getTyped(ScaffoldCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        if (instaBreakCheck != null) {
            instaBreakCheck.startBreaking(player, event.getBlock(), data);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;


        if (instaBreakCheck != null) {
            CheckResult r = instaBreakCheck.checkBreak(player, event.getBlock(), data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, instaBreakCheck.getName(), r.getDetails());
        }


        if (nukerCheck != null) {
            CheckResult r = nukerCheck.check(player, event.getBlock().getLocation(), data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, nukerCheck.getName(), r.getDetails());
        }


        boolean isOre = xrayCheck != null && xrayCheck.isOre(event.getBlock().getType());
        double oreWeight = xrayCheck != null ? xrayCheck.getOreWeight(event.getBlock().getType()) : 0;
        data.recordBlockMined(isOre, oreWeight);

        plugin.getAntiXRayManager().revealAdjacentOres(player, event.getBlock().getLocation());

        if (xrayCheck != null) {
            CheckResult r = xrayCheck.analyze(data);
            if (r.isFlagged()) {
                if (!data.isXraySuspect()) {
                    data.setXraySuspect(true);
                    plugin.getAntiXRayManager().sendFakeOres(player);
                }
                plugin.getViolationManager().flag(player, xrayCheck.getName(), r.getDetails());
            } else {
                data.setXraySuspect(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;


        if (fastPlaceCheck != null) {
            CheckResult r = fastPlaceCheck.check(player, data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, fastPlaceCheck.getName(), r.getDetails());
        }


        if (scaffoldCheck != null) {
            CheckResult r = scaffoldCheck.check(player, event.getBlock().getLocation(), data);
            if (r.isFlagged()) plugin.getViolationManager().flag(player, scaffoldCheck.getName(), r.getDetails());
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("elysiaac.bypass")) return;
        if (!plugin.getAntiXRayManager().isEnabled()) return;
        if (event.getTo() == null) return;

        Chunk from = event.getFrom().getChunk();
        Chunk to   = event.getTo().getChunk();
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) return;

        plugin.getAntiXRayManager().obfuscateChunkForPlayer(player, to);
    }
}
