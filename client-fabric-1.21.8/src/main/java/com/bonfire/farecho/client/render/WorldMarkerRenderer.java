package com.bonfire.farecho.client.render;

import com.bonfire.farecho.client.state.EchoStateStore;
import com.bonfire.farecho.protocol.model.EchoSnapshot;
import com.bonfire.farecho.protocol.model.Relation;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

public final class WorldMarkerRenderer {
    private final EchoStateStore stateStore;
    private int ticks;

    public WorldMarkerRenderer(EchoStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        ticks++;
        if (ticks % 5 != 0) {
            return;
        }

        List<EchoSnapshot> snapshots = stateStore.visibleSnapshots(client);
        int limit = Math.min(12, snapshots.size());
        for (int i = 0; i < limit; i++) {
            EchoSnapshot snapshot = snapshots.get(i);
            // Body mode handles full snapshots. Keep particles only as degraded fallback.
            if (!snapshot.directionOnly()) {
                continue;
            }

            client.world.addParticleClient(
                relationParticle(snapshot.relation()),
                snapshot.x(),
                snapshot.y() + 2.2D,
                snapshot.z(),
                0.0D,
                0.02D,
                0.0D
            );

            if (i < 6) {
                client.world.addParticleClient(
                    ParticleTypes.END_ROD,
                    snapshot.x(),
                    snapshot.y() + 2.6D,
                    snapshot.z(),
                    0.0D,
                    0.0D,
                    0.0D
                );
            }
        }
    }

    private ParticleEffect relationParticle(Relation relation) {
        return switch (relation) {
            case FRIEND -> ParticleTypes.HAPPY_VILLAGER;
            case HOSTILE -> ParticleTypes.ANGRY_VILLAGER;
            case NEUTRAL -> ParticleTypes.GLOW;
        };
    }
}
