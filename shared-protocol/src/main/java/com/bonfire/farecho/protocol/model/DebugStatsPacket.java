package com.bonfire.farecho.protocol.model;

import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.ProtocolPacket;

public record DebugStatsPacket(DebugStats stats) implements ProtocolPacket {
    @Override
    public PacketType type() {
        return PacketType.DEBUG_STATS;
    }
}
