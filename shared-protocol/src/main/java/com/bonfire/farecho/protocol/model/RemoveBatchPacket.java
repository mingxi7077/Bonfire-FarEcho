package com.bonfire.farecho.protocol.model;

import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.ProtocolPacket;

public record RemoveBatchPacket(EchoRemoveBatch batch) implements ProtocolPacket {
    @Override
    public PacketType type() {
        return PacketType.REMOVE_BATCH;
    }
}
