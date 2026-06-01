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

    /** Angle en degrés entre le regard du joueur et la direction vers la cible. */
    public static double getAngleToTarget(Player player, Entity target) {
        Vector eyeDir = player.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector()
                .subtract(player.getEyeLocation().toVector()).normalize();
        double dot = eyeDir.dot(toTarget);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Calcule la vitesse horizontale entre deux positions. */
    public static double horizontalSpeed(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static boolean isBetween(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
