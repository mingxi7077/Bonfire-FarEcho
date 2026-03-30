package com.bonfire.farecho.server.interest;

import com.bonfire.farecho.server.config.FarechoConfiguration;
import com.bonfire.farecho.server.snapshot.PlayerSnapshot;
import java.util.Collection;
import java.util.List;

public interface InterestEngine {
    List<TargetCandidate> selectTargets(
        ObserverContext observer,
        Collection<PlayerSnapshot> snapshots,
        FarechoConfiguration configuration
    );
}
