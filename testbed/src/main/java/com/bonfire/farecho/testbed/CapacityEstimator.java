package com.bonfire.farecho.testbed;

public final class CapacityEstimator {
    private CapacityEstimator() {
    }

    public static CapacityProjection project(
        int baselinePlayers,
        double baselineCpuMsPerTick,
        double baselineAvgKbPerObserverPerSec,
        int targetPlayers,
        double safetyFactor
    ) {
        if (baselinePlayers <= 0 || targetPlayers <= 0) {
            throw new IllegalArgumentException("player count must be positive");
        }
        double scale = (double) targetPlayers / baselinePlayers;
        double projectedCpu = baselineCpuMsPerTick * scale * safetyFactor;
        double projectedAvgBandwidth = baselineAvgKbPerObserverPerSec * scale * safetyFactor;
        return new CapacityProjection(targetPlayers, projectedCpu, projectedAvgBandwidth);
    }

    public record CapacityProjection(
        int players,
        double projectedCpuMsPerTick,
        double projectedAvgKbPerObserverPerSec
    ) {
    }
}
