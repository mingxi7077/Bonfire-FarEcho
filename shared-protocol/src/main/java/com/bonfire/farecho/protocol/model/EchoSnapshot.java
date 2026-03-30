package com.bonfire.farecho.protocol.model;

import java.util.UUID;

public record EchoSnapshot(
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
    Relation relation,
    String skinHint,
    boolean directionOnly,
    double planarDistance,
    double bearingDegrees
) {
}
