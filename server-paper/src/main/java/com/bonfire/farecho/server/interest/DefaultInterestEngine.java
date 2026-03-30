package com.bonfire.farecho.server.interest;

import com.bonfire.farecho.protocol.model.EchoFlags;
import com.bonfire.farecho.protocol.model.Relation;
import com.bonfire.farecho.server.config.FarechoConfiguration;
import com.bonfire.farecho.server.snapshot.PlayerSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DefaultInterestEngine implements InterestEngine {
    private final PrioritySignalProvider prioritySignalProvider;

    public DefaultInterestEngine() {
        this(new NoOpPrioritySignalProvider());
    }

    public DefaultInterestEngine(PrioritySignalProvider prioritySignalProvider) {
        this.prioritySignalProvider = prioritySignalProvider;
    }

    @Override
    public List<TargetCandidate> selectTargets(
        ObserverContext observer,
        java.util.Collection<PlayerSnapshot> snapshots,
        FarechoConfiguration configuration
    ) {
        List<TargetCandidate> candidates = new ArrayList<>();
        long nowMs = System.currentTimeMillis();

        for (PlayerSnapshot snapshot : snapshots) {
            if (snapshot.uuid().equals(observer.uuid())) {
                continue;
            }
            if (!snapshot.worldKey().equals(observer.worldKey())) {
                continue;
            }
            if (EchoFlags.has(snapshot.flags(), EchoFlags.ADMIN_HIDDEN) && !observer.canSeeHidden()) {
                continue;
            }
            if (EchoFlags.has(snapshot.flags(), EchoFlags.INVISIBLE) && !observer.canSeeInvisible()) {
                continue;
            }
            if (configuration.hideSpectatorTargets() && EchoFlags.has(snapshot.flags(), EchoFlags.SPECTATOR)) {
                continue;
            }

            double dx = snapshot.x() - observer.x();
            double dz = snapshot.z() - observer.z();
            double planarDistance = Math.hypot(dx, dz);
            if (planarDistance <= configuration.nativeTrackingRange()) {
                continue;
            }
            if (planarDistance > configuration.maxRadius()) {
                continue;
            }

            Relation relation = relationOf(observer, snapshot, planarDistance, configuration);
            boolean recentCombat = prioritySignalProvider.hasRecentCombat(observer.uuid(), snapshot.uuid(), nowMs, configuration);
            boolean recentInteraction = prioritySignalProvider.hasRecentInteraction(observer.uuid(), snapshot.uuid(), nowMs, configuration);
            int priority = resolvePriority(relation, recentCombat, recentInteraction);
            String priorityHint = resolvePriorityHint(relation, recentCombat, recentInteraction);

            double bearing = Math.toDegrees(Math.atan2(dx, dz));
            candidates.add(new TargetCandidate(
                snapshot,
                planarDistance,
                bearing,
                relation,
                priority,
                priorityHint,
                recentCombat,
                recentInteraction
            ));
        }

        candidates.sort(Comparator
            .comparingInt(TargetCandidate::priority)
            .thenComparingDouble(TargetCandidate::planarDistance));

        return candidates;
    }

    private int resolvePriority(Relation relation, boolean recentCombat, boolean recentInteraction) {
        if (recentCombat) {
            return 0;
        }
        if (relation == Relation.FRIEND) {
            return 1;
        }
        if (recentInteraction) {
            return 2;
        }
        if (relation == Relation.HOSTILE) {
            return 3;
        }
        return 4;
    }

    private String resolvePriorityHint(Relation relation, boolean recentCombat, boolean recentInteraction) {
        if (recentCombat) {
            return "combat";
        }
        if (relation == Relation.FRIEND) {
            return "team";
        }
        if (recentInteraction) {
            return "interaction";
        }
        if (relation == Relation.HOSTILE) {
            return "hostile";
        }
        return "neutral";
    }

    private Relation relationOf(
        ObserverContext observer,
        PlayerSnapshot target,
        double planarDistance,
        FarechoConfiguration configuration
    ) {
        if (!observer.teamName().isBlank() && observer.teamName().equals(target.teamName())) {
            return Relation.FRIEND;
        }
        if (observer.pvpEnabled() && planarDistance <= configuration.hostileDistance()) {
            return Relation.HOSTILE;
        }
        return Relation.NEUTRAL;
    }
}
