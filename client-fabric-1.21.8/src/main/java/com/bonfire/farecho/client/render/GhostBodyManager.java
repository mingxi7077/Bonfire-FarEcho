package com.bonfire.farecho.client.render;

import com.bonfire.farecho.client.state.EchoStateStore;
import com.bonfire.farecho.protocol.model.EchoFlags;
import com.bonfire.farecho.protocol.model.EchoSnapshot;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class GhostBodyManager {
    private final EchoStateStore stateStore;
    private final Map<UUID, GhostEntry> ghostsByTarget = new HashMap<>();
    private final Map<UUID, SkinTextures> skinCacheByPlayer = new HashMap<>();
    private int nextEntityId = -2_000_000;

    public GhostBodyManager(EchoStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            clear(client);
            return;
        }

        ClientWorld world = client.world;
        List<EchoSnapshot> snapshots = stateStore.visibleSnapshots(client);
        Set<UUID> desiredTargets = new HashSet<>();
        refreshSkinCache(world);

        for (EchoSnapshot snapshot : snapshots) {
            if (snapshot.directionOnly()) {
                continue;
            }
            desiredTargets.add(snapshot.uuid());
            GhostEntry entry = ghostsByTarget.get(snapshot.uuid());
            if (entry == null || entry.world() != world || world.getEntityById(entry.entityId()) == null) {
                entry = createGhost(world, snapshot);
            }
            updateGhost(entry.entity(), snapshot);
        }

        removeUndesired(world, desiredTargets);
    }

    public void clear(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world != null) {
            for (GhostEntry entry : ghostsByTarget.values()) {
                world.removeEntity(entry.entityId(), Entity.RemovalReason.DISCARDED);
            }
        }
        ghostsByTarget.clear();
        skinCacheByPlayer.clear();
    }

    private GhostEntry createGhost(ClientWorld world, EchoSnapshot snapshot) {
        GameProfile profile = new GameProfile(snapshot.uuid(), snapshot.name());
        FarEchoGhostPlayerEntity ghost = new FarEchoGhostPlayerEntity(world, profile, snapshot.uuid(), skinCacheByPlayer);

        int entityId = allocateEntityId(world);
        ghost.setId(entityId);
        // Use dedicated entity UUID to avoid colliding with real-player UUID lookup.
        ghost.setUuid(ghostUuidFor(snapshot.uuid()));
        ghost.setNoGravity(true);
        ghost.setOnGround(true);
        ghost.refreshPositionAndAngles(snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
        ghost.setHeadYaw(snapshot.yaw());
        ghost.setBodyYaw(snapshot.yaw());
        world.addEntity(ghost);

        GhostEntry entry = new GhostEntry(entityId, ghost, world);
        ghostsByTarget.put(snapshot.uuid(), entry);
        return entry;
    }

    private void refreshSkinCache(ClientWorld world) {
        for (AbstractClientPlayerEntity player : world.getPlayers()) {
            if (player != null && !player.isRemoved()) {
                skinCacheByPlayer.put(player.getUuid(), player.getSkinTextures());
            }
        }
    }

    private void updateGhost(OtherClientPlayerEntity ghost, EchoSnapshot snapshot) {
        double dx = snapshot.x() - ghost.getX();
        double dy = snapshot.y() - ghost.getY();
        double dz = snapshot.z() - ghost.getZ();
        ghost.setVelocityClient(dx, dy, dz);
        ghost.updateTrackedPositionAndAngles(new Vec3d(snapshot.x(), snapshot.y(), snapshot.z()), snapshot.yaw(), snapshot.pitch());
        ghost.setHeadYaw(snapshot.yaw());
        ghost.setBodyYaw(snapshot.yaw());

        boolean sneaking = EchoFlags.has(snapshot.flags(), EchoFlags.SNEAKING);
        boolean flying = EchoFlags.has(snapshot.flags(), EchoFlags.FLYING);
        ghost.setSneaking(sneaking);
        ghost.setOnGround(!flying);
    }

    private void removeUndesired(ClientWorld world, Set<UUID> desiredTargets) {
        List<UUID> toRemove = ghostsByTarget.keySet().stream()
            .filter(targetId -> !desiredTargets.contains(targetId))
            .toList();
        for (UUID targetId : toRemove) {
            GhostEntry removed = ghostsByTarget.remove(targetId);
            if (removed != null) {
                world.removeEntity(removed.entityId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private int allocateEntityId(ClientWorld world) {
        while (world.getEntityById(nextEntityId) != null) {
            nextEntityId--;
        }
        return nextEntityId--;
    }

    private UUID ghostUuidFor(UUID targetUuid) {
        return UUID.nameUUIDFromBytes(("farecho-ghost-" + targetUuid).getBytes(StandardCharsets.UTF_8));
    }

    private record GhostEntry(int entityId, OtherClientPlayerEntity entity, ClientWorld world) {
    }
}
