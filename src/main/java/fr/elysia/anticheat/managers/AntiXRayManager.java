package fr.elysia.anticheat.managers;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère l'obfuscation des minerais côté client.
 *
 * Principe :
 *  - Quand un joueur charge un chunk ou se déplace, les blocs de minerais
 *    cachés (non adjacents à l'air) lui sont remplacés visuellement par de la pierre.
 *  - Quand il mine un bloc adjacent à un minerai, le minerai lui est révélé.
 *  - Si le joueur est suspect de X-Ray, des faux minerais lui sont envoyés
 *    pour perturber sa vision.
 */
public class AntiXRayManager {

    private final ElysiaAntiCheat plugin;
    private final Set<Material> oreTypes = new LinkedHashSet<>();
    private Material replacementBlock;
    private int maxY;

    /** Blocs obfusqués par joueur : Location -> matériau réel */
    private final Map<UUID, Map<Location, Material>> hiddenOres = new ConcurrentHashMap<>();

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

        List<String> configOres = plugin.getConfig().getStringList("anti-xray.ore-types");
        for (String name : configOres) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) oreTypes.add(mat);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("anti-xray.enabled", true);
    }

    /** Obfusque les minerais d'un chunk pour un joueur donné (appelé sync). */
    public void obfuscateChunkForPlayer(Player player, Chunk chunk) {
        if (!isEnabled()) return;
        if (player.hasPermission("elysiaac.bypass")) return;

        UUID uuid = player.getUniqueId();
        Map<Location, Material> hidden = hiddenOres.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        // Scan asynchrone, envoi sync
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map.Entry<Location, Material>> toHide = new ArrayList<>();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = chunk.getWorld().getMinHeight(); y <= maxY; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (!oreTypes.contains(block.getType())) continue;
                        if (isVisibleToAir(block)) continue; // Déjà visible naturellement

                        Location loc = block.getLocation();
                        toHide.add(Map.entry(loc, block.getType()));
                    }
                }
            }

            if (toHide.isEmpty()) return;

            // Envoi sur le thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                for (var entry : toHide) {
                    Location loc = entry.getKey();
                    hidden.put(loc, entry.getValue());
                    player.sendBlockChange(loc, replacementBlock.createBlockData());
                }
            });
        });
    }

    /** Révèle les vrais minerais adjacents quand un joueur mine un bloc. */
    public void revealAdjacentOres(Player player, Location broken) {
        if (!isEnabled()) return;

        UUID uuid = player.getUniqueId();
        Map<Location, Material> hidden = hiddenOres.get(uuid);
        if (hidden == null || hidden.isEmpty()) return;

        int[][] offsets = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (int[] off : offsets) {
            Location adjacent = broken.clone().add(off[0], off[1], off[2]);
            Material real = hidden.remove(adjacent);
            if (real != null) {
                player.sendBlockChange(adjacent, real.createBlockData());
            }
        }
    }

    /** Envoie de faux minerais à un X-rayer suspecté pour perturber sa vision. */
    public void sendFakeOres(Player player) {
        if (!isEnabled()) return;
        if (!plugin.getConfig().getBoolean("anti-xray.fake-ores-on-suspect", true)) return;

        int count = plugin.getConfig().getInt("anti-xray.fake-ores-count", 30);
        Location base = player.getLocation();
        World world = player.getWorld();
        Random rng = new Random();

        // Minerais rares à envoyer comme faux
        Material[] rareOres = {
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.EMERALD_ORE
        };

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            for (int i = 0; i < count; i++) {
                int dx = rng.nextInt(32) - 16;
                int dy = rng.nextInt(20) - 10;
                int dz = rng.nextInt(32) - 16;
                int y = base.getBlockY() + dy;
                if (y < world.getMinHeight() || y > maxY) continue;

                Location fake = new Location(world,
                        base.getBlockX() + dx,
                        y,
                        base.getBlockZ() + dz);

                Block real = fake.getBlock();
                // N'envoie des faux que sur de la pierre/deepslate pour ne pas masquer les vrais blocs
                if (real.getType() == Material.STONE || real.getType() == Material.DEEPSLATE
                        || real.getType() == Material.TUFF) {
                    Material fakeOre = rareOres[rng.nextInt(rareOres.length)];
                    player.sendBlockChange(fake, fakeOre.createBlockData());
                }
            }
        });
    }

    /** Nettoie les données d'un joueur à sa déconnexion. */
    public void cleanup(UUID uuid) {
        hiddenOres.remove(uuid);
    }

    private boolean isVisibleToAir(Block block) {
        int[][] faces = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (int[] f : faces) {
            Block neighbor = block.getRelative(f[0], f[1], f[2]);
            if (neighbor.isEmpty() || neighbor.isLiquid()) return true;
        }
        return false;
    }
}
