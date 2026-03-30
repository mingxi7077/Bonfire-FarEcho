package com.bonfire.farecho.client.state;

import com.bonfire.farecho.protocol.model.DebugStats;
import com.bonfire.farecho.protocol.model.EchoDeltaBatch;
import com.bonfire.farecho.protocol.model.EchoRemoveBatch;
import com.bonfire.farecho.protocol.model.EchoSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class EchoStateStore {
    private final Map<UUID, TrackedEcho> echoes = new ConcurrentHashMap<>();
    private volatile DebugStats lastDebugStats;

    public void applyDelta(EchoDeltaBatch batch) {
        long now = System.currentTimeMillis();
        for (EchoSnapshot snapshot : batch.snapshots()) {
            echoes.put(snapshot.uuid(), new TrackedEcho(snapshot, now));
        }
    }

    public void applyRemove(EchoRemoveBatch batch) {
        for (UUID targetId : batch.targetIds()) {
            echoes.remove(targetId);
        }
    }

    public void clear() {
        echoes.clear();
        lastDebugStats = null;
    }

    public void expireStale(long oldestAllowedTimestampMs) {
        echoes.entrySet().removeIf(entry -> entry.getValue().updatedAtMs() < oldestAllowedTimestampMs);
    }

    public List<EchoSnapshot> visibleSnapshots(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return List.of();
        }
        String worldKey = client.world.getRegistryKey().getValue().toString();
        Vec3d cameraPos = cameraPos(client);
        List<EchoSnapshot> snapshots = new ArrayList<>();
        for (TrackedEcho trackedEcho : echoes.values()) {
            EchoSnapshot snapshot = trackedEcho.snapshot();
            if (!worldKey.equals(snapshot.worldKey())) {
                continue;
            }
            PlayerEntity vanillaEntity = client.world.getPlayerByUuid(snapshot.uuid());
            if (vanillaEntity != null && !vanillaEntity.isRemoved() && vanillaEntity.shouldRender(cameraPos.x, cameraPos.y, cameraPos.z)) {
                continue;
            }
            snapshots.add(snapshot);
        }
        snapshots.sort(Comparator.comparingDouble(EchoSnapshot::planarDistance));
        return snapshots;
    }

    private Vec3d cameraPos(MinecraftClient client) {
        if (client.gameRenderer != null && client.gameRenderer.getCamera() != null) {
            return client.gameRenderer.getCamera().getPos();
        }
        return client.player != null ? client.player.getPos() : Vec3d.ZERO;
    }

    public DebugStats lastDebugStats() {
        return lastDebugStats;
    }

    public void setLastDebugStats(DebugStats debugStats) {
        this.lastDebugStats = debugStats;
    }

    private record TrackedEcho(EchoSnapshot snapshot, long updatedAtMs) {
    }
}
