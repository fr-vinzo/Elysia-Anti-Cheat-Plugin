package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import fr.elysia.anticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class JesusCheck extends Check {

    public JesusCheck(ElysiaAntiCheat plugin) {
        super(plugin, "Jesus", CheckCategory.MOVEMENT);
    }

    public CheckResult check(Player player, Location from, Location to, PlayerData data) {
        if (!isEnabled()) return CheckResult.pass();
        if (player.isInsideVehicle()) return CheckResult.pass();
        if (player.isSwimming()) { data.resetWaterWalk(); return CheckResult.pass(); }
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) { data.resetWaterWalk(); return CheckResult.pass(); }
        if (System.currentTimeMillis() - data.getTeleportTime() < 2000) { data.resetWaterWalk(); return CheckResult.pass(); }


        Block blockBelow = to.clone().subtract(0, 0.05, 0).getBlock();
        if (blockBelow.getType() != Material.WATER) {
            data.resetWaterWalk();
            return CheckResult.pass();
        }


        Block blockAtFeet = to.getBlock();
        boolean inWater = blockAtFeet.getType() == Material.WATER;
        if (inWater) { data.resetWaterWalk(); return CheckResult.pass(); }


        if (hasFrostWalker(player)) { data.resetWaterWalk(); return CheckResult.pass(); }


        data.incrementWaterWalk();
        int minTicks = plugin.getConfig().getInt("checks.jesus.min-ticks", 5);

        if (data.getWaterWalkTicks() >= minTicks) {
            return CheckResult.fail("marche_sur_eau=" + data.getWaterWalkTicks() + " ticks");
        }
        return CheckResult.pass();
    }

    private boolean hasFrostWalker(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null) return false;
        return boots.getEnchantments().containsKey(org.bukkit.enchantments.Enchantment.FROST_WALKER);
    }
}
