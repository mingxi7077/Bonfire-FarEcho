package com.bonfire.farecho.protocol.model;

import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.ProtocolPacket;
import java.util.UUID;

public record HelloPacket(UUID senderId, String senderKind, String modVersion) implements ProtocolPacket {
    @Override
    public PacketType type() {
        return PacketType.HELLO;
    }
}
