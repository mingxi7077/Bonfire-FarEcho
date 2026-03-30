package com.bonfire.farecho.server.snapshot;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface SnapshotProvider {
    Collection<PlayerSnapshot> snapshots();

    Optional<PlayerSnapshot> snapshot(UUID uuid);

    void refresh(Player player);

    void remove(UUID playerId);

    int size();
}
