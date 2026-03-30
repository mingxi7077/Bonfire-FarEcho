package com.bonfire.farecho.server.interest;

import com.bonfire.farecho.server.config.FarechoConfiguration;
import java.util.UUID;

public interface PrioritySignalProvider {
    boolean hasRecentCombat(UUID observerId, UUID targetId, long nowMs, FarechoConfiguration configuration);

    boolean hasRecentInteraction(UUID observerId, UUID targetId, long nowMs, FarechoConfiguration configuration);

    String traceFor(UUID playerId, long nowMs, FarechoConfiguration configuration);

    void clear(UUID playerId);

    long activeCombatLinks(long nowMs, FarechoConfiguration configuration);

    long activeInteractionLinks(long nowMs, FarechoConfiguration configuration);
}
