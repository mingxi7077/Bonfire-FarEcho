package com.bonfire.farecho.server.interest;

import com.bonfire.farecho.server.config.FarechoConfiguration;
import java.util.UUID;

public final class NoOpPrioritySignalProvider implements PrioritySignalProvider {
    @Override
    public boolean hasRecentCombat(UUID observerId, UUID targetId, long nowMs, FarechoConfiguration configuration) {
        return false;
    }

    @Override
    public boolean hasRecentInteraction(UUID observerId, UUID targetId, long nowMs, FarechoConfiguration configuration) {
        return false;
    }

    @Override
    public String traceFor(UUID playerId, long nowMs, FarechoConfiguration configuration) {
        return "combat=0, interaction=0";
    }

    @Override
    public void clear(UUID playerId) {
    }

    @Override
    public long activeCombatLinks(long nowMs, FarechoConfiguration configuration) {
        return 0;
    }

    @Override
    public long activeInteractionLinks(long nowMs, FarechoConfiguration configuration) {
        return 0;
    }
}
