package com.bonfire.farecho.server.broadcast;

import com.bonfire.farecho.protocol.model.DebugStats;
import com.bonfire.farecho.protocol.model.DebugStatsPacket;
import com.bonfire.farecho.protocol.model.DeltaBatchPacket;
import com.bonfire.farecho.protocol.model.EchoDeltaBatch;
import com.bonfire.farecho.protocol.model.EchoSnapshot;
import com.bonfire.farecho.protocol.model.RemoveBatchPacket;
import com.bonfire.farecho.protocol.model.EchoRemoveBatch;
import com.bonfire.farecho.server.config.FarechoConfiguration;
import com.bonfire.farecho.server.interest.InterestEngine;
import com.bonfire.farecho.server.interest.ObserverContext;
import com.bonfire.farecho.server.interest.TargetCandidate;
import com.bonfire.farecho.server.metrics.MetricsCollector;
import com.bonfire.farecho.server.metrics.MetricsSnapshot;
import com.bonfire.farecho.server.snapshot.SnapshotProvider;
import com.bonfire.farecho.server.transport.EchoTransport;
import com.bonfire.farecho.server.transport.HandshakeService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class EchoBroadcastService implements Runnable {
    private final SnapshotProvider snapshotProvider;
    private final InterestEngine interestEngine;
    private final BroadcastPolicy broadcastPolicy;
    private final HandshakeService handshakeService;
    private final EchoTransport transport;
    private final MetricsCollector metrics;
    private final Supplier<FarechoConfiguration> configurationSupplier;
    private final Map<UUID, ObserverState> states = new HashMap<>();
    private final Map<UUID, String> traceSummaries = new HashMap<>();

    private long serverTick = 0L;
    private long lastDebugSentAtMs = 0L;

    public EchoBroadcastService(
        SnapshotProvider snapshotProvider,
        InterestEngine interestEngine,
        BroadcastPolicy broadcastPolicy,
        HandshakeService handshakeService,
        EchoTransport transport,
        MetricsCollector metrics,
        Supplier<FarechoConfiguration> configurationSupplier
    ) {
        this.snapshotProvider = snapshotProvider;
        this.interestEngine = interestEngine;
        this.broadcastPolicy = broadcastPolicy;
        this.handshakeService = handshakeService;
        this.transport = transport;
        this.metrics = metrics;
        this.configurationSupplier = configurationSupplier;
    }

    @Override
    public void run() {
        serverTick++;
        long nowMs = System.currentTimeMillis();
        FarechoConfiguration configuration = configurationSupplier.get();
        List<Player> debugRecipients = new ArrayList<>();

        for (Player observer : Bukkit.getOnlinePlayers()) {
            if (!handshakeService.isFarEchoClient(observer)) {
                continue;
            }
            debugRecipients.add(observer);

            ObserverState state = states.computeIfAbsent(observer.getUniqueId(), unused -> new ObserverState());
            ObserverContext observerContext = ObserverContext.fromPlayer(observer);
            List<TargetCandidate> allCandidates = interestEngine.selectTargets(
                observerContext,
                snapshotProvider.snapshots(),
                configuration
            );

            int clippedCount = Math.max(0, allCandidates.size() - configuration.maxTargetsPerObserver());
            List<TargetCandidate> selected = allCandidates.subList(0, Math.min(allCandidates.size(), configuration.maxTargetsPerObserver()));
            Set<UUID> visibleThisCycle = new HashSet<>();
            List<EchoSnapshot> deltas = new ArrayList<>();
            Map<String, Integer> priorityCounts = new LinkedHashMap<>();

            int degradedCount = 0;
            int packetsSent = 0;
            long bytesSent = 0;

            for (int i = 0; i < selected.size(); i++) {
                TargetCandidate candidate = selected.get(i);
                UUID targetId = candidate.snapshot().uuid();
                visibleThisCycle.add(targetId);
                priorityCounts.merge(candidate.priorityHint(), 1, Integer::sum);

                long intervalMs = broadcastPolicy.refreshIntervalMs(candidate.planarDistance(), configuration);
                long lastSentAt = state.lastSentAtMs().getOrDefault(targetId, 0L);
                if (nowMs - lastSentAt < intervalMs) {
                    continue;
                }

                boolean directionOnly = broadcastPolicy.directionOnly(
                    i,
                    selected.size(),
                    candidate.planarDistance(),
                    configuration
                );
                if (directionOnly) {
                    degradedCount++;
                }

                deltas.add(toEchoSnapshot(candidate, directionOnly));
                state.lastSentAtMs().put(targetId, nowMs);
            }

            if (!deltas.isEmpty()) {
                bytesSent += transport.send(observer, new DeltaBatchPacket(new EchoDeltaBatch(serverTick, deltas)));
                packetsSent++;
            }

            Set<UUID> removed = new HashSet<>(state.visibleTargets());
            removed.removeAll(visibleThisCycle);
            if (!removed.isEmpty()) {
                bytesSent += transport.send(observer, new RemoveBatchPacket(new EchoRemoveBatch(List.copyOf(removed))));
                packetsSent++;
            }

            state.visibleTargets().clear();
            state.visibleTargets().addAll(visibleThisCycle);

            metrics.recordCycle(
                selected.size(),
                clippedCount,
                packetsSent,
                degradedCount,
                bytesSent,
                visibleThisCycle.size(),
                nowMs
            );
            traceSummaries.put(
                observer.getUniqueId(),
                "selected=" + selected.size() + ", clipped=" + clippedCount + ", priorities=" + priorityCounts
            );
        }

        if (configuration.debugEnabled() && nowMs - lastDebugSentAtMs >= 1000L) {
            MetricsSnapshot metricsSnapshot = metrics.snapshot(nowMs);
            DebugStats debugStats = new DebugStats(
                serverTick,
                saturatedToInt(metricsSnapshot.targetsPerSecond()),
                metricsSnapshot.clippedRatioPerSecond(),
                saturatedToInt(metricsSnapshot.packetsPerSecond()),
                saturatedToInt(metricsSnapshot.degradedPerSecond()),
                metricsSnapshot.bytesPerSecond(),
                saturatedToInt(metricsSnapshot.clientRenderCountPerSecond())
            );
            for (Player recipient : debugRecipients) {
                transport.send(recipient, new DebugStatsPacket(debugStats));
            }
            lastDebugSentAtMs = nowMs;
        }
    }

    public String trace(UUID observerId) {
        ObserverState state = states.get(observerId);
        if (state == null) {
            return "observer-not-tracked";
        }
        String summary = traceSummaries.getOrDefault(observerId, "selected=0, clipped=0, priorities={}");
        return "visible=" + state.visibleTargets().size()
            + ", lastSentMap=" + state.lastSentAtMs().size()
            + ", " + summary;
    }

    public void forget(UUID observerId) {
        states.remove(observerId);
        traceSummaries.remove(observerId);
    }

    private EchoSnapshot toEchoSnapshot(TargetCandidate candidate, boolean directionOnly) {
        return new EchoSnapshot(
            candidate.snapshot().uuid(),
            candidate.snapshot().name(),
            candidate.snapshot().worldKey(),
            candidate.snapshot().x(),
            candidate.snapshot().y(),
            candidate.snapshot().z(),
            candidate.snapshot().yaw(),
            candidate.snapshot().pitch(),
            candidate.snapshot().lastMoveTick(),
            candidate.snapshot().flags(),
            candidate.relation(),
            "",
            directionOnly,
            candidate.planarDistance(),
            candidate.bearingDegrees()
        );
    }

    private int saturatedToInt(long value) {
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }
}
