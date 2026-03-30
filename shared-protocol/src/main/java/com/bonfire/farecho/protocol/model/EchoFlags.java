package com.bonfire.farecho.protocol.model;

public final class EchoFlags {
    public static final int INVISIBLE = 1;
    public static final int SNEAKING = 1 << 1;
    public static final int FLYING = 1 << 2;
    public static final int SPECTATOR = 1 << 3;
    public static final int ADMIN_HIDDEN = 1 << 4;

    private EchoFlags() {
    }

    public static boolean has(int flags, int mask) {
        return (flags & mask) == mask;
    }
}
