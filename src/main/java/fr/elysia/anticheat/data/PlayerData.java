package fr.elysia.anticheat.data;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

public class PlayerData {

    private final UUID uuid;
    private final String name;


    private Location lastLocation;
    private Location previousLocation;
    private long lastMoveTime;
    private int airtimeTicks;
    private double lastFallDistance;
    private boolean teleported;
    private long teleportTime;


    private final Deque<Double> speedSamples = new ArrayDeque<>();
    private static final int SPEED_WINDOW = 10;


    private final Deque<Long> moveTimes = new ArrayDeque<>();


    private double lastLadderY;
    private long lastLadderTime;


    private int waterWalkTicks;


    private boolean wasOnGround;


    private final Deque<Long> hitTimestamps = new ArrayDeque<>();
    private final Map<UUID, Long> recentTargets = new LinkedHashMap<>();


    private long lastHitTime;
    private final Deque<Long> hitIntervals = new ArrayDeque<>();


    private long lastKnockbackTime;
    private double expectedKbX;
    private double expectedKbZ;


    private final Deque<Double> aimAngles = new ArrayDeque<>();


    private long lastBowShot;


    private long lastMaceSmashTime;
    private long lastWindChargeLaunchTime;


    private int totalBlocksMined;
    private int oresMined;
    private double weightedOreScore;
    private long miningWindowStart;
    private boolean xraySuspect;


    private long blockBreakStart;
    private Material currentlyBreaking;


    private final Deque<Location> recentBreakLocations = new ArrayDeque<>();
    private long lastBreakTime;


    private final Deque<Long> placeTimes = new ArrayDeque<>();
    private int scaffoldCount;
    private long lastScaffoldTime;



    private double confidenceScore = 0.5;
    private final long joinTime;


    private final Map<String, Integer> violationLevels = new HashMap<>();
    private final List<String> recentAlerts = new ArrayList<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.miningWindowStart = System.currentTimeMillis();
        this.joinTime = System.currentTimeMillis();
    }





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


    public void addSpeedSample(double speed) {
        speedSamples.addLast(speed);
        if (speedSamples.size() > SPEED_WINDOW) speedSamples.pollFirst();
    }

    public double getAverageSpeed() {
        if (speedSamples.isEmpty()) return 0;
        return speedSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public int getSpeedSampleCount() { return speedSamples.size(); }


    public void recordMovePacket() {
        long now = System.currentTimeMillis();
        moveTimes.addLast(now);
        while (!moveTimes.isEmpty() && now - moveTimes.peekFirst() > 1000) moveTimes.pollFirst();
    }

    public int getMovePacketsPerSecond() { return moveTimes.size(); }


    public double getLastLadderY() { return lastLadderY; }
    public void setLastLadderY(double y) { this.lastLadderY = y; }
    public long getLastLadderTime() { return lastLadderTime; }
    public void setLastLadderTime(long t) { this.lastLadderTime = t; }


    public int getWaterWalkTicks() { return waterWalkTicks; }
    public void incrementWaterWalk() { this.waterWalkTicks++; }
    public void resetWaterWalk() { this.waterWalkTicks = 0; }


    public boolean wasOnGround() { return wasOnGround; }
    public void setWasOnGround(boolean b) { this.wasOnGround = b; }





    public void recordHit(UUID targetUUID) {
        long now = System.currentTimeMillis();
        hitTimestamps.addLast(now);
        while (!hitTimestamps.isEmpty() && now - hitTimestamps.peekFirst() > 1000) hitTimestamps.pollFirst();

        recentTargets.put(targetUUID, now);
        recentTargets.entrySet().removeIf(e -> now - e.getValue() > 500);

        if (lastHitTime > 0) {
            long interval = now - lastHitTime;
            hitIntervals.addLast(interval);
            if (hitIntervals.size() > 20) hitIntervals.pollFirst();
        }
        lastHitTime = now;
    }

    public int getCPS() { return hitTimestamps.size(); }
    public int getRecentTargetCount() { return recentTargets.size(); }


    public double getHitIntervalStdDev() {
        if (hitIntervals.size() < 5) return Double.MAX_VALUE;
        double mean = hitIntervals.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = hitIntervals.stream()
                .mapToDouble(i -> Math.pow(i - mean, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }

    public double getHitIntervalMean() {
        if (hitIntervals.isEmpty()) return 0;
        return hitIntervals.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    public int getHitIntervalSampleCount() { return hitIntervals.size(); }


    public long getLastKnockbackTime() { return lastKnockbackTime; }
    public void setLastKnockbackTime(long t) { this.lastKnockbackTime = t; }
    public double getExpectedKbX() { return expectedKbX; }
    public void setExpectedKbX(double v) { this.expectedKbX = v; }
    public double getExpectedKbZ() { return expectedKbZ; }
    public void setExpectedKbZ(double v) { this.expectedKbZ = v; }


    public void recordAimAngle(double angle) {
        aimAngles.addLast(angle);
        if (aimAngles.size() > 15) aimAngles.pollFirst();
    }

    public double getAverageAimAngle() {
        if (aimAngles.isEmpty()) return 45.0;
        return aimAngles.stream().mapToDouble(Double::doubleValue).average().orElse(45.0);
    }

    public int getAimAngleSampleCount() { return aimAngles.size(); }


    public long getLastBowShot() { return lastBowShot; }
    public void setLastBowShot(long t) { this.lastBowShot = t; }


    public long getLastMaceSmashTime() { return lastMaceSmashTime; }
    public void setLastMaceSmashTime(long t) { this.lastMaceSmashTime = t; }
    public long getLastWindChargeLaunchTime() { return lastWindChargeLaunchTime; }
    public void setLastWindChargeLaunchTime(long t) { this.lastWindChargeLaunchTime = t; }





    public int getTotalBlocksMined() { return totalBlocksMined; }
    public int getOresMined() { return oresMined; }
    public double getWeightedOreScore() { return weightedOreScore; }
    public long getMiningWindowStart() { return miningWindowStart; }

    public void recordBlockMined(boolean isOre, double oreWeight) {
        totalBlocksMined++;
        if (isOre) {
            oresMined++;
            weightedOreScore += oreWeight;
        }
    }

    public void resetMiningWindow() {
        totalBlocksMined = 0;
        oresMined = 0;
        weightedOreScore = 0;
        miningWindowStart = System.currentTimeMillis();
    }

    public double getOreRatio() {
        if (totalBlocksMined == 0) return 0;
        return (double) oresMined / totalBlocksMined;
    }

    public double getWeightedOreRatio() {
        if (totalBlocksMined == 0) return 0;
        return weightedOreScore / totalBlocksMined;
    }

    public boolean isXraySuspect() { return xraySuspect; }
    public void setXraySuspect(boolean suspect) { this.xraySuspect = suspect; }


    public long getBlockBreakStart() { return blockBreakStart; }
    public void setBlockBreakStart(long t) { this.blockBreakStart = t; }
    public Material getCurrentlyBreaking() { return currentlyBreaking; }
    public void setCurrentlyBreaking(Material m) { this.currentlyBreaking = m; }


    public void recordBreakLocation(Location loc) {
        recentBreakLocations.addLast(loc.clone());
        if (recentBreakLocations.size() > 12) recentBreakLocations.pollFirst();
        lastBreakTime = System.currentTimeMillis();
    }

    public Deque<Location> getRecentBreakLocations() { return recentBreakLocations; }
    public long getLastBreakTime() { return lastBreakTime; }


    public void recordPlace() {
        long now = System.currentTimeMillis();
        placeTimes.addLast(now);
        while (!placeTimes.isEmpty() && now - placeTimes.peekFirst() > 1000) placeTimes.pollFirst();
    }

    public int getPlacesPerSecond() { return placeTimes.size(); }

    public int getScaffoldCount() { return scaffoldCount; }
    public void incrementScaffold() {
        this.scaffoldCount++;
        this.lastScaffoldTime = System.currentTimeMillis();
    }
    public void resetScaffold() { this.scaffoldCount = 0; }
    public long getLastScaffoldTime() { return lastScaffoldTime; }





    public double getConfidenceScore() { return confidenceScore; }

    public void increaseConfidence() {
        confidenceScore = Math.min(1.0, confidenceScore + 0.005);
    }

    public void decreaseConfidence(double amount) {
        confidenceScore = Math.max(0.0, confidenceScore - amount);
    }


    public double getToleranceMultiplier() {
        return 1.0 + (confidenceScore * 0.4);
    }

    public long getJoinTime() { return joinTime; }





    public int getViolationLevel(String checkName) {
        return violationLevels.getOrDefault(checkName, 0);
    }

    public int incrementViolation(String checkName) {
        int vl = violationLevels.getOrDefault(checkName, 0) + 1;
        violationLevels.put(checkName, vl);
        decreaseConfidence(0.03);
        return vl;
    }

    public void resetViolation(String checkName) {
        violationLevels.remove(checkName);
    }

    public void resetAllViolations() {
        violationLevels.clear();
        recentAlerts.clear();
        xraySuspect = false;
        resetMiningWindow();
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

    public void decayViolations(int amount) {
        violationLevels.replaceAll((k, v) -> Math.max(0, v - amount));
        violationLevels.entrySet().removeIf(e -> e.getValue() <= 0);
        increaseConfidence();
    }
}
