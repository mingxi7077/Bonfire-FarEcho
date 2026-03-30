package com.bonfire.farecho.server.broadcast;

import com.bonfire.farecho.server.config.FarechoConfiguration;

public final class DistanceTierBroadcastPolicy implements BroadcastPolicy {
    @Override
    public long refreshIntervalMs(double planarDistance, FarechoConfiguration configuration) {
        if (planarDistance <= configuration.nearBandEnd()) {
            return configuration.nearRefreshMs();
        }
        if (planarDistance <= configuration.midBandEnd()) {
            return configuration.midRefreshMs();
        }
        return configuration.farRefreshMs();
    }

    @Override
    public boolean directionOnly(int targetIndex, int totalTargets, double planarDistance, FarechoConfiguration configuration) {
        if (planarDistance >= configuration.degradeDistance()) {
            return true;
        }
        return totalTargets > configuration.degradeWhenTargetCount() && targetIndex >= configuration.degradeWhenTargetCount();
    }
}
