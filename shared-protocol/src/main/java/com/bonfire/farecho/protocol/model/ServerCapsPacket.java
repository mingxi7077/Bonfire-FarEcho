package com.bonfire.farecho.protocol.model;

import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.ProtocolPacket;

public record ServerCapsPacket(Capabilities capabilities) implements ProtocolPacket {
    @Override
    public PacketType type() {
        return PacketType.SERVER_CAPS;
    }
}
