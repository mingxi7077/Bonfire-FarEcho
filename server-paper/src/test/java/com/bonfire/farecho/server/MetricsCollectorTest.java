package com.bonfire.farecho.server;

import com.bonfire.farecho.server.metrics.MetricsCollector;
import com.bonfire.farecho.server.metrics.MetricsSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetricsCollectorTest {

    @Test
    void emitsPerSecondMetricsFromCompletedWindow() {
        MetricsCollector collector = new MetricsCollector();
        collector.recordCycle(10, 2, 2, 1, 500L, 7, 1000L);
        collector.recordCycle(20, 4, 1, 0, 500L, 13, 1500L);

        MetricsSnapshot snapshot = collector.snapshot(2000L);

        Assertions.assertEquals(2L, snapshot.cycles());
        Assertions.assertEquals(30L, snapshot.targets());
        Assertions.assertEquals(6L, snapshot.clipped());
        Assertions.assertEquals(3L, snapshot.packets());
        Assertions.assertEquals(1L, snapshot.degraded());
        Assertions.assertEquals(1000L, snapshot.bytes());
        Assertions.assertEquals(0.2D, snapshot.clippedRatio(), 0.0001D);
        Assertions.assertEquals(500.0D, snapshot.bytesPerCycle(), 0.0001D);

        Assertions.assertEquals(30L, snapshot.targetsPerSecond());
        Assertions.assertEquals(0.2D, snapshot.clippedRatioPerSecond(), 0.0001D);
        Assertions.assertEquals(3L, snapshot.packetsPerSecond());
        Assertions.assertEquals(1L, snapshot.degradedPerSecond());
        Assertions.assertEquals(1000L, snapshot.bytesPerSecond());
        Assertions.assertEquals(20L, snapshot.clientRenderCountPerSecond());
    }

    @Test
    void scalesPerSecondFromActiveWindowBeforeFirstRotation() {
        MetricsCollector collector = new MetricsCollector();
        collector.recordCycle(8, 2, 1, 1, 400L, 5, 5000L);

        MetricsSnapshot snapshot = collector.snapshot(5500L);

        Assertions.assertEquals(16L, snapshot.targetsPerSecond());
        Assertions.assertEquals(0.25D, snapshot.clippedRatioPerSecond(), 0.0001D);
        Assertions.assertEquals(2L, snapshot.packetsPerSecond());
        Assertions.assertEquals(2L, snapshot.degradedPerSecond());
        Assertions.assertEquals(800L, snapshot.bytesPerSecond());
        Assertions.assertEquals(10L, snapshot.clientRenderCountPerSecond());
    }
}
