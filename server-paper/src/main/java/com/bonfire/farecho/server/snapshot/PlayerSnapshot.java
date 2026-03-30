package com.bonfire.farecho.server.snapshot;

import java.util.UUID;

public record PlayerSnapshot(
    UUID uuid,
    String name,
    String worldKey,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    long lastMoveTick,
    int flags,
    String teamName
) {
}
