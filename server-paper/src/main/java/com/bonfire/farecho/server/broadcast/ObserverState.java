package com.bonfire.farecho.server.broadcast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ObserverState {
    private final Map<UUID, Long> lastSentAtMs = new HashMap<>();
    private final Set<UUID> visibleTargets = new HashSet<>();

    public Map<UUID, Long> lastSentAtMs() {
        return lastSentAtMs;
    }

    public Set<UUID> visibleTargets() {
        return visibleTargets;
    }
}
