package com.bonfire.farecho.server;

import com.bonfire.farecho.protocol.model.EchoFlags;
import com.bonfire.farecho.protocol.model.Relation;
import com.bonfire.farecho.server.config.FarechoConfiguration;
import com.bonfire.farecho.server.interest.DefaultInterestEngine;
import com.bonfire.farecho.server.interest.ObserverContext;
import com.bonfire.farecho.server.interest.PrioritySignalProvider;
import com.bonfire.farecho.server.interest.TargetCandidate;
import com.bonfire.farecho.server.snapshot.PlayerSnapshot;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultInterestEngineTest {

    @Test
    void filtersAndSortsByPriorityThenDistance() {
        DefaultInterestEngine engine = new DefaultInterestEngine();
        FarechoConfiguration config = new FarechoConfiguration(
            64,
            3000,
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
            500,
            15000,
            30000
        );
        ObserverContext observer = new ObserverContext(
            UUID.randomUUID(),
            "minecraft:overworld",
            0,
            64,
            0,
            "team_a",
            false,
            false,
            false,
            true
        );

        PlayerSnapshot friend = new PlayerSnapshot(UUID.randomUUID(), "Friend", "minecraft:overworld", 300, 64, 0, 0, 0, 1, 0, "team_a");
        PlayerSnapshot hostile = new PlayerSnapshot(UUID.randomUUID(), "Hostile", "minecraft:overworld", 420, 64, 0, 0, 0, 1, 0, "team_b");
        PlayerSnapshot neutral = new PlayerSnapshot(UUID.randomUUID(), "Neutral", "minecraft:overworld", 900, 64, 0, 0, 0, 1, 0, "");
        PlayerSnapshot hidden = new PlayerSnapshot(UUID.randomUUID(), "Hidden", "minecraft:overworld", 500, 64, 0, 0, 0, 1, EchoFlags.ADMIN_HIDDEN, "");

        List<TargetCandidate> result = engine.selectTargets(observer, List.of(friend, hostile, neutral, hidden), config);

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(friend.uuid(), result.get(0).snapshot().uuid());
        Assertions.assertEquals(Relation.FRIEND, result.get(0).relation());
        Assertions.assertEquals(hostile.uuid(), result.get(1).snapshot().uuid());
        Assertions.assertEquals(Relation.HOSTILE, result.get(1).relation());
        Assertions.assertEquals(neutral.uuid(), result.get(2).snapshot().uuid());
    }

    @Test
    void dropsTargetsInsideNativeTrackingRange() {
        DefaultInterestEngine engine = new DefaultInterestEngine();
        FarechoConfiguration config = new FarechoConfiguration(
            64,
            3000,
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
            500,
            15000,
            30000
        );
        ObserverContext observer = new ObserverContext(
            UUID.randomUUID(),
            "minecraft:overworld",
            0,
            64,
            0,
            "",
            true,
            true,
            false,
            true
        );

        PlayerSnapshot close = new PlayerSnapshot(UUID.randomUUID(), "Close", "minecraft:overworld", 120, 64, 0, 0, 0, 1, 0, "");
        List<TargetCandidate> result = engine.selectTargets(observer, List.of(close), config);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void recentCombatHasHighestPriority() {
        UUID observerId = UUID.randomUUID();
        UUID combatTargetId = UUID.randomUUID();
        PrioritySignalProvider signalProvider = new PrioritySignalProvider() {
            @Override
            public boolean hasRecentCombat(UUID observer, UUID target, long nowMs, FarechoConfiguration configuration) {
                return observerId.equals(observer) && combatTargetId.equals(target);
            }

            @Override
            public boolean hasRecentInteraction(UUID observerId, UUID targetId, long nowMs, FarechoConfiguration configuration) {
                return false;
            }

            @Override
            public String traceFor(UUID playerId, long nowMs, FarechoConfiguration configuration) {
                return "combat=1, interaction=0";
            }

            @Override
            public void clear(UUID playerId) {
            }

            @Override
            public long activeCombatLinks(long nowMs, FarechoConfiguration configuration) {
                return 1;
            }

            @Override
            public long activeInteractionLinks(long nowMs, FarechoConfiguration configuration) {
                return 0;
            }
        };
        DefaultInterestEngine engine = new DefaultInterestEngine(signalProvider);
        FarechoConfiguration config = new FarechoConfiguration(
            64,
            3000,
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
        ObserverContext observer = new ObserverContext(
            observerId,
            "minecraft:overworld",
            0,
            64,
            0,
            "team_a",
            true,
            true,
            false,
            true
        );

        PlayerSnapshot teammate = new PlayerSnapshot(UUID.randomUUID(), "Teammate", "minecraft:overworld", 300, 64, 0, 0, 0, 1, 0, "team_a");
        PlayerSnapshot combatTarget = new PlayerSnapshot(combatTargetId, "Combat", "minecraft:overworld", 700, 64, 0, 0, 0, 1, 0, "team_b");

        List<TargetCandidate> result = engine.selectTargets(observer, List.of(teammate, combatTarget), config);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(combatTargetId, result.get(0).snapshot().uuid());
        Assertions.assertEquals("combat", result.get(0).priorityHint());
    }
}
