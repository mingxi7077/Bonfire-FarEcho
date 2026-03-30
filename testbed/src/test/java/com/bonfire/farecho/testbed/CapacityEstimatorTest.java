package com.bonfire.farecho.testbed;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CapacityEstimatorTest {

    @Test
    void projectsWithSafetyFactor() {
        CapacityEstimator.CapacityProjection projection = CapacityEstimator.project(
            50,
            1.5,
            6.0,
            100,
            1.2
        );

        Assertions.assertEquals(100, projection.players());
        Assertions.assertEquals(3.6, projection.projectedCpuMsPerTick(), 0.0001);
        Assertions.assertEquals(14.4, projection.projectedAvgKbPerObserverPerSec(), 0.0001);
    }
}
