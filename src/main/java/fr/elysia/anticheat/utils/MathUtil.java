package fr.elysia.anticheat.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class MathUtil {

    private MathUtil() {}

    public static double distance3D(Location a, Location b) {
        if (!a.getWorld().equals(b.getWorld())) return Double.MAX_VALUE;
        return a.distance(b);
    }

    public static double distanceXZ(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }


    public static double getAngleToTarget(Player player, Entity target) {
        Vector eyeDir = player.getLocation().getDirection().normalize();
        double targetCenterY = target.getLocation().getY() + target.getHeight() / 2.0;
        Vector targetCenter = new Vector(
                target.getLocation().getX(),
                targetCenterY,
                target.getLocation().getZ());
        Vector toTarget = targetCenter.subtract(player.getEyeLocation().toVector()).normalize();
        double dot = Math.max(-1.0, Math.min(1.0, eyeDir.dot(toTarget)));
        return Math.toDegrees(Math.acos(dot));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }


    public static double horizontalSpeed(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static boolean isBetween(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
