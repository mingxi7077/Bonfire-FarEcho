package com.bonfire.farecho.protocol.model;

public record DebugStats(
    long serverTick,
    int targetCount,
    double clippedRatio,
    int packetsSent,
    int degradedCount,
    long bytesSent,
    int clientRenderCount
) {
}
