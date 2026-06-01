package fr.elysia.anticheat.checks.movement;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.api.CheckResult;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.checks.CheckCategory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

public class NoFallCheck extends Check {

    public NoFallCheck(ElysiaAntiCheat plugin) {
        super(plugin, "NoFall", CheckCategory.MOVEMENT);
    }


    public CheckResult checkDamage(Player player, EntityDamageEvent.DamageCause cause, double fallDistance) {
        if (!isEnabled()) return CheckResult.pass();

        double minFall = plugin.getConfig().getDouble("checks.no-fall.min-fall-distance", 4.0);
        if (fallDistance < minFall) return CheckResult.pass();


        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return CheckResult.pass();


        var boots = player.getInventory().getBoots();
        if (boots != null) {
            var ff = boots.getEnchantments();
            if (ff.containsKey(org.bukkit.enchantments.Enchantment.FEATHER_FALLING)) {
                return CheckResult.pass();
            }
        }


        Material below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
        if (isSoftLanding(below)) return CheckResult.pass();

        return CheckResult.pass();
    }


    public CheckResult checkNoDamage(Player player, double fallDistance) {
        if (!isEnabled()) return CheckResult.pass();

        double minFall = plugin.getConfig().getDouble("checks.no-fall.min-fall-distance", 4.0);
        if (fallDistance < minFall) return CheckResult.pass();


        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return CheckResult.pass();
        if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) return CheckResult.pass();

        if (player.getInventory().getItemInMainHand().getType() == Material.MACE) return CheckResult.pass();

        Material below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
        if (isSoftLanding(below)) return CheckResult.pass();

        return CheckResult.fail("chute=" + String.format("%.1f", fallDistance) + " blocs sans dégâts");
    }

    private boolean isSoftLanding(Material m) {
        return m == Material.WATER || m == Material.LAVA
                || m == Material.COBWEB || m == Material.HAY_BLOCK
                || m == Material.SLIME_BLOCK || m == Material.HONEY_BLOCK
                || m == Material.SWEET_BERRY_BUSH || m.name().contains("POWDER_SNOW")
                || m.name().endsWith("_BED");
    }
}
