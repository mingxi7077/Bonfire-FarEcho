package com.bonfire.farecho.server;

import com.bonfire.farecho.server.broadcast.DistanceTierBroadcastPolicy;
import com.bonfire.farecho.server.config.FarechoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DistanceTierBroadcastPolicyTest {

    private final DistanceTierBroadcastPolicy policy = new DistanceTierBroadcastPolicy();
    private final FarechoConfiguration config = new FarechoConfiguration(
        64,
        3072,
        256,
        768,
        1536,
        250,
        500,
        1000,
        1536,
        48,
        true,
        false,
        128,
        15000,
        30000
    );

    @Test
    void usesDistanceTierIntervals() {
        Assertions.assertEquals(250, policy.refreshIntervalMs(400, config));
        Assertions.assertEquals(500, policy.refreshIntervalMs(900, config));
        Assertions.assertEquals(1000, policy.refreshIntervalMs(2000, config));
    }

    @Test
    void degradesByDistanceAndOverflow() {
        Assertions.assertTrue(policy.directionOnly(1, 10, 2000, config));
        Assertions.assertFalse(policy.directionOnly(1, 10, 1000, config));
        Assertions.assertTrue(policy.directionOnly(60, 80, 1000, config));
    }
}
