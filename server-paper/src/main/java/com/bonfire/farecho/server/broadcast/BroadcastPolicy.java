package com.bonfire.farecho.server.broadcast;

import com.bonfire.farecho.server.config.FarechoConfiguration;

public interface BroadcastPolicy {
    long refreshIntervalMs(double planarDistance, FarechoConfiguration configuration);

    boolean directionOnly(int targetIndex, int totalTargets, double planarDistance, FarechoConfiguration configuration);
}
