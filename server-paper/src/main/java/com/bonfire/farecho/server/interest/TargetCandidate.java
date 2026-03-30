package com.bonfire.farecho.server.interest;

import com.bonfire.farecho.protocol.model.Relation;
import com.bonfire.farecho.server.snapshot.PlayerSnapshot;

public record TargetCandidate(
    PlayerSnapshot snapshot,
    double planarDistance,
    double bearingDegrees,
    Relation relation,
    int priority,
    String priorityHint,
    boolean recentCombat,
    boolean recentInteraction
) {
}
