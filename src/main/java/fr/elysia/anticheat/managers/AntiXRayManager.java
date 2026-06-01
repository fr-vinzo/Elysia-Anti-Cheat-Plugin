package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.ElysiaAntiCheat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiXRayManager {

    private final ElysiaAntiCheat plugin;
    private final Set<Material> oreTypes = new LinkedHashSet<>();
    private Material replacementBlock;
    private int maxY;


    private final Map<UUID, Map<Location, Material>> hiddenOres = new ConcurrentHashMap<>();

    private final Map<UUID, Set<Long>> processedChunks = new ConcurrentHashMap<>();

    public AntiXRayManager(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        oreTypes.clear();
        String replacementName = plugin.getConfig().getString("anti-xray.replacement-block", "STONE");
        replacementBlock = Material.matchMaterial(replacementName);
        if (replacementBlock == null) replacementBlock = Material.STONE;
        maxY = plugin.getConfig().getInt("anti-xray.max-y", 16);

        for (String name : plugin.getConfig().getStringList("anti-xray.ore-types")) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) oreTypes.add(mat);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("anti-xray.enabled", true);
    }




    public void obfuscateChunkForPlayer(Player player, Chunk chunk) {
        if (!isEnabled() || player.hasPermission("elysiaac.bypass")) return;

        int radius = plugin.getConfig().getInt("anti-xray.obfuscation-radius", 1);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk target = chunk.getWorld().getChunkAt(chunk.getX() + dx, chunk.getZ() + dz);
                if (target.isLoaded()) obfuscateSingleChunk(player, target);
            }
        }
    }

    private void obfuscateSingleChunk(Player player, Chunk chunk) {
        UUID uuid = player.getUniqueId();
        long chunkKey = chunkKey(chunk);

        Set<Long> done = processedChunks.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        if (!done.add(chunkKey)) return;

        Map<Location, Material> hidden = hiddenOres.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());


        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map.Entry<Location, Material>> toHide = new ArrayList<>();
            World world = chunk.getWorld();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y <= maxY; y++) {
                        Material mat = snapshot.getBlockType(x, y, z);
                        if (!oreTypes.contains(mat)) continue;
                        if (isVisibleInSnapshot(snapshot, x, y, z, world.getMinHeight())) continue;

                        int wx = chunk.getX() * 16 + x;
                        int wz = chunk.getZ() * 16 + z;
                        Location loc = new Location(world, wx, y, wz);
                        toHide.add(Map.entry(loc, mat));
                    }
                }
            }

            if (toHide.isEmpty()) return;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                for (var entry : toHide) {
                    hidden.put(entry.getKey(), entry.getValue());
                    player.sendBlockChange(entry.getKey(), replacementBlock.createBlockData());
                }
            });
        });
    }


    public void revealAdjacentOres(Player player, Location broken) {
        if (!isEnabled()) return;

        Map<Location, Material> hidden = hiddenOres.get(player.getUniqueId());
        if (hidden == null || hidden.isEmpty()) return;

        int revealRadius = plugin.getConfig().getInt("anti-xray.reveal-radius", 3);

        for (int dx = -revealRadius; dx <= revealRadius; dx++) {
            for (int dy = -revealRadius; dy <= revealRadius; dy++) {
                for (int dz = -revealRadius; dz <= revealRadius; dz++) {
                    Location loc = broken.clone().add(dx, dy, dz);
                    Material real = hidden.remove(loc);
                    if (real != null) {
                        player.sendBlockChange(loc, real.createBlockData());
                    }
                }
            }
        }
    }


    public void sendFakeOres(Player player) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("anti-xray.fake-ores-on-suspect", true)) return;

        int count = plugin.getConfig().getInt("anti-xray.fake-ores-count", 30);
        Location base = player.getLocation();
        World world = player.getWorld();

        Random rng = new Random(player.getUniqueId().getMostSignificantBits());

        Material[] rareOres = {
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.EMERALD_ORE, Material.ANCIENT_DEBRIS
        };

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            for (int i = 0; i < count; i++) {
                int dx = rng.nextInt(40) - 20;
                int dy = rng.nextInt(24) - 12;
                int dz = rng.nextInt(40) - 20;
                int y = base.getBlockY() + dy;
                if (y < world.getMinHeight() || y > maxY) continue;

                Location fake = new Location(world, base.getBlockX() + dx, y, base.getBlockZ() + dz);
                Block real = fake.getBlock();
                if (real.getType() == Material.STONE || real.getType() == Material.DEEPSLATE
                        || real.getType() == Material.TUFF) {
                    player.sendBlockChange(fake, rareOres[rng.nextInt(rareOres.length)].createBlockData());
                }
            }
        });
    }

    public void cleanup(UUID uuid) {
        hiddenOres.remove(uuid);
        processedChunks.remove(uuid);
    }

    private boolean isVisibleInSnapshot(ChunkSnapshot snap, int x, int y, int z, int minY) {
        int[][] faces = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (int[] f : faces) {
            int nx = x + f[0], ny = y + f[1], nz = z + f[2];

            if (nx < 0 || nx > 15 || nz < 0 || nz > 15) return true;
            if (ny < minY || ny > 319) return true;
            Material neighbor = snap.getBlockType(nx, ny, nz);
            if (neighbor == Material.AIR || neighbor == Material.CAVE_AIR || neighbor == Material.VOID_AIR
                    || neighbor == Material.WATER || neighbor == Material.LAVA) return true;
        }
        return false;
    }

    private long chunkKey(Chunk chunk) {
        return ((long) chunk.getX() << 32) | (chunk.getZ() & 0xFFFFFFFFL);
    }
}
