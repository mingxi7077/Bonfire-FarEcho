package com.bonfire.farecho.server.config;

import org.bukkit.configuration.file.FileConfiguration;

public record FarechoConfiguration(
    int maxTargetsPerObserver,
    double maxRadius,
    double nativeTrackingRange,
    double nearBandEnd,
    double midBandEnd,
    long nearRefreshMs,
    long midRefreshMs,
    long farRefreshMs,
    double degradeDistance,
    int degradeWhenTargetCount,
    boolean hideSpectatorTargets,
    boolean debugEnabled,
    double hostileDistance,
    long recentCombatWindowMs,
    long recentInteractionWindowMs
) {
    private static final double BLOCKS_PER_CHUNK = 16.0;

    public static FarechoConfiguration from(FileConfiguration config) {
        return new FarechoConfiguration(
            config.getInt("maxTargetsPerObserver", 64),
            readDistance(config, "maxRadius", "maxRadiusChunks", 8192.0),
            readDistance(config, "nativeTrackingRange", "nativeTrackingRangeChunks", 128.0),
            readDistance(config, "near.bandEnd", "near.bandEndChunks", 1024.0),
            readDistance(config, "mid.bandEnd", "mid.bandEndChunks", 2048.0),
            config.getLong("near.refreshMs", 250L),
            config.getLong("mid.refreshMs", 500L),
            config.getLong("far.refreshMs", 1000L),
            readDistance(config, "degradeThresholds.distance", "degradeThresholds.distanceChunks", 8192.0),
            config.getInt("degradeThresholds.targetCount", 48),
            config.getBoolean("rules.hideSpectatorTargets", true),
            config.getBoolean("debugEnabled", false),
            config.getDouble("rules.hostileDistance", 128.0),
            config.getLong("rules.recentCombatWindowMs", 15000L),
            config.getLong("rules.recentInteractionWindowMs", 30000L)
        );
    }

    private static double readDistance(
        FileConfiguration config,
        String blockPath,
        String chunkPath,
        double defaultBlocks
    ) {
        if (config.isSet(chunkPath)) {
            return Math.max(0.0, config.getDouble(chunkPath, 0.0)) * BLOCKS_PER_CHUNK;
        }
        return config.getDouble(blockPath, defaultBlocks);
    }
}
