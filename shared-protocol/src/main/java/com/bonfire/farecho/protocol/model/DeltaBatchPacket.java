package com.bonfire.farecho.protocol.model;

import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.ProtocolPacket;

public record DeltaBatchPacket(EchoDeltaBatch batch) implements ProtocolPacket {
    @Override
    public PacketType type() {
        return PacketType.DELTA_BATCH;
    }
}
