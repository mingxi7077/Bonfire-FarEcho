package com.bonfire.farecho.server.metrics;

public record MetricsSnapshot(
    long cycles,
    long targets,
    long clipped,
    long packets,
    long degraded,
    long bytes,
    double clippedRatio,
    double bytesPerCycle,
    long targetsPerSecond,
    double clippedRatioPerSecond,
    long packetsPerSecond,
    long degradedPerSecond,
    long bytesPerSecond,
    long clientRenderCountPerSecond
) {
}
