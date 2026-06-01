package fr.elysia.anticheat.data;

import org.bukkit.Location;

import java.util.*;

public class PlayerData {

    private final UUID uuid;
    private final String name;

    // ---- Mouvement ----
    private Location lastLocation;
    private Location previousLocation;
    private long lastMoveTime;
    private int airtimeTicks;
    private double lastFallDistance;
    private boolean teleported;
    private long teleportTime;
    private int speedViolations;
    private int flyViolations;
    private int noFallViolations;

    // ---- Combat ----
    private final Deque<Long> hitTimestamps = new ArrayDeque<>();
    private final Map<UUID, Long> recentTargets = new LinkedHashMap<>();
    private int killAuraViolations;
    private int reachViolations;

    // ---- Mining / XRay ----
    private int totalBlocksMined;
    private int oresMined;
    private long miningWindowStart;
    private int xrayViolations;
    private boolean xraySuspect;

    // ---- Violations globales ----
    private final Map<String, Integer> violationLevels = new HashMap<>();
    private final List<String> recentAlerts = new ArrayList<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.miningWindowStart = System.currentTimeMillis();
    }

    // ---- Getters / Setters Mouvement ----

    public Location getLastLocation() { return lastLocation; }
    public void setLastLocation(Location loc) { this.lastLocation = loc; }

    public Location getPreviousLocation() { return previousLocation; }
    public void setPreviousLocation(Location loc) { this.previousLocation = loc; }

    public long getLastMoveTime() { return lastMoveTime; }
    public void setLastMoveTime(long t) { this.lastMoveTime = t; }

    public int getAirtimeTicks() { return airtimeTicks; }
    public void incrementAirtime() { this.airtimeTicks++; }
    public void resetAirtime() { this.airtimeTicks = 0; }

    public double getLastFallDistance() { return lastFallDistance; }
    public void setLastFallDistance(double d) { this.lastFallDistance = d; }

    public boolean isTeleported() { return teleported; }
    public void setTeleported(boolean t) { this.teleported = t; }

    public long getTeleportTime() { return teleportTime; }
    public void setTeleportTime(long t) { this.teleportTime = t; }

    // ---- Getters / Setters Combat ----

    public Deque<Long> getHitTimestamps() { return hitTimestamps; }

    public void recordHit(UUID targetUUID) {
        long now = System.currentTimeMillis();
        hitTimestamps.addLast(now);
        while (!hitTimestamps.isEmpty() && now - hitTimestamps.peekFirst() > 1000) {
            hitTimestamps.pollFirst();
        }
        recentTargets.put(targetUUID, now);
        recentTargets.entrySet().removeIf(e -> now - e.getValue() > 500);
    }

    public int getCPS() { return hitTimestamps.size(); }

    public int getRecentTargetCount() { return recentTargets.size(); }

    // ---- Getters / Setters Mining ----

    public int getTotalBlocksMined() { return totalBlocksMined; }
    public int getOresMined() { return oresMined; }
    public long getMiningWindowStart() { return miningWindowStart; }

    public void recordBlockMined(boolean isOre) {
        totalBlocksMined++;
        if (isOre) oresMined++;
    }

    public void resetMiningWindow() {
        totalBlocksMined = 0;
        oresMined = 0;
        miningWindowStart = System.currentTimeMillis();
    }

    public double getOreRatio() {
        if (totalBlocksMined == 0) return 0;
        return (double) oresMined / totalBlocksMined;
    }

    public boolean isXraySuspect() { return xraySuspect; }
    public void setXraySuspect(boolean suspect) { this.xraySuspect = suspect; }

    // ---- Violations générales ----

    public int getViolationLevel(String checkName) {
        return violationLevels.getOrDefault(checkName, 0);
    }

    public int incrementViolation(String checkName) {
        int vl = violationLevels.getOrDefault(checkName, 0) + 1;
        violationLevels.put(checkName, vl);
        return vl;
    }

    public void resetViolation(String checkName) {
        violationLevels.remove(checkName);
    }

    public void resetAllViolations() {
        violationLevels.clear();
        recentAlerts.clear();
    }

    public Map<String, Integer> getAllViolations() {
        return Collections.unmodifiableMap(violationLevels);
    }

    public int getTotalViolations() {
        return violationLevels.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void addRecentAlert(String alert) {
        recentAlerts.add(0, alert);
        if (recentAlerts.size() > 20) recentAlerts.remove(recentAlerts.size() - 1);
    }

    public List<String> getRecentAlerts() {
        return Collections.unmodifiableList(recentAlerts);
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    /** Décroissance des violations (appelée périodiquement). */
    public void decayViolations(int amount) {
        violationLevels.replaceAll((k, v) -> Math.max(0, v - amount));
        violationLevels.entrySet().removeIf(e -> e.getValue() <= 0);
    }
}
