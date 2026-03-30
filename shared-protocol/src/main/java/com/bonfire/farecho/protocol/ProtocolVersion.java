package com.bonfire.farecho.protocol;

public final class ProtocolVersion {
    public static final int CURRENT = 1;
    public static final int MIN_SUPPORTED = 1;

    private ProtocolVersion() {
    }

    public static boolean isSupported(int version) {
        return version >= MIN_SUPPORTED && version <= CURRENT;
    }
}
