package com.bonfire.farecho.server.metrics;

import java.util.concurrent.atomic.LongAdder;

public final class MetricsCollector {
    private final LongAdder cycles = new LongAdder();
    private final LongAdder targets = new LongAdder();
    private final LongAdder clipped = new LongAdder();
    private final LongAdder packets = new LongAdder();
    private final LongAdder degraded = new LongAdder();
    private final LongAdder bytes = new LongAdder();
    private final LongAdder clientRenderCount = new LongAdder();

    private long windowStartedAtMs;
    private long windowCycles;
    private long windowTargets;
    private long windowClipped;
    private long windowPackets;
    private long windowDegraded;
    private long windowBytes;
    private long windowClientRenderCount;

    private long lastWindowDurationMs = 1000L;
    private long lastWindowCycles;
    private long lastWindowTargets;
    private long lastWindowClipped;
    private long lastWindowPackets;
    private long lastWindowDegraded;
    private long lastWindowBytes;
    private long lastWindowClientRenderCount;

    public void recordCycle(int targetCount, int clippedCount, int packetsSent, int degradedCount, long bytesSent) {
        recordCycle(targetCount, clippedCount, packetsSent, degradedCount, bytesSent, 0, System.currentTimeMillis());
    }

    public synchronized void recordCycle(
        int targetCount,
        int clippedCount,
        int packetsSent,
        int degradedCount,
        long bytesSent,
        int clientRenderObjects,
        long nowMs
    ) {
        rotateWindowIfNeeded(nowMs);

        cycles.increment();
        targets.add(targetCount);
        clipped.add(clippedCount);
        packets.add(packetsSent);
        degraded.add(degradedCount);
        bytes.add(bytesSent);
        clientRenderCount.add(clientRenderObjects);

        windowCycles++;
        windowTargets += targetCount;
        windowClipped += clippedCount;
        windowPackets += packetsSent;
        windowDegraded += degradedCount;
        windowBytes += bytesSent;
        windowClientRenderCount += clientRenderObjects;
    }

    public MetricsSnapshot snapshot() {
        return snapshot(System.currentTimeMillis());
    }

    public synchronized MetricsSnapshot snapshot(long nowMs) {
        rotateWindowIfNeeded(nowMs);

        long cycleCount = cycles.sum();
        long targetCount = targets.sum();
        long clippedCount = clipped.sum();
        long packetCount = packets.sum();
        long degradedCount = degraded.sum();
        long byteCount = bytes.sum();
        double clippedRatio = targetCount == 0 ? 0.0 : (double) clippedCount / (double) targetCount;
        double bytesPerCycle = cycleCount == 0 ? 0.0 : (double) byteCount / (double) cycleCount;

        long windowDurationMs = lastWindowDurationMs;
        long secondTargets = lastWindowTargets;
        long secondClipped = lastWindowClipped;
        long secondPackets = lastWindowPackets;
        long secondDegraded = lastWindowDegraded;
        long secondBytes = lastWindowBytes;
        long secondClientRender = lastWindowClientRenderCount;

        if (lastWindowCycles == 0L && windowCycles > 0L) {
            windowDurationMs = activeWindowDurationMs(nowMs);
            secondTargets = windowTargets;
            secondClipped = windowClipped;
            secondPackets = windowPackets;
            secondDegraded = windowDegraded;
            secondBytes = windowBytes;
            secondClientRender = windowClientRenderCount;
        }

        long targetsPerSecond = scalePerSecond(secondTargets, windowDurationMs);
        long packetsPerSecond = scalePerSecond(secondPackets, windowDurationMs);
        long degradedPerSecond = scalePerSecond(secondDegraded, windowDurationMs);
        long bytesPerSecond = scalePerSecond(secondBytes, windowDurationMs);
        long clientRenderPerSecond = scalePerSecond(secondClientRender, windowDurationMs);
        double clippedRatioPerSecond = secondTargets == 0 ? 0.0 : (double) secondClipped / (double) secondTargets;

        return new MetricsSnapshot(
            cycleCount,
            targetCount,
            clippedCount,
            packetCount,
            degradedCount,
            byteCount,
            clippedRatio,
            bytesPerCycle,
            targetsPerSecond,
            clippedRatioPerSecond,
            packetsPerSecond,
            degradedPerSecond,
            bytesPerSecond,
            clientRenderPerSecond
        );
    }

    private void rotateWindowIfNeeded(long nowMs) {
        if (windowStartedAtMs == 0L) {
            windowStartedAtMs = nowMs;
            return;
        }

        long elapsed = nowMs - windowStartedAtMs;
        if (elapsed < 1000L) {
            return;
        }

        lastWindowDurationMs = Math.max(1L, elapsed);
        lastWindowCycles = windowCycles;
        lastWindowTargets = windowTargets;
        lastWindowClipped = windowClipped;
        lastWindowPackets = windowPackets;
        lastWindowDegraded = windowDegraded;
        lastWindowBytes = windowBytes;
        lastWindowClientRenderCount = windowClientRenderCount;

        windowStartedAtMs = nowMs;
        windowCycles = 0L;
        windowTargets = 0L;
        windowClipped = 0L;
        windowPackets = 0L;
        windowDegraded = 0L;
        windowBytes = 0L;
        windowClientRenderCount = 0L;
    }

    private long activeWindowDurationMs(long nowMs) {
        if (windowStartedAtMs == 0L) {
            return 1000L;
        }
        return Math.max(1L, nowMs - windowStartedAtMs);
    }

    private long scalePerSecond(long value, long durationMs) {
        long safeDurationMs = Math.max(1L, durationMs);
        return Math.round((double) value * 1000.0D / (double) safeDurationMs);
    }
}
