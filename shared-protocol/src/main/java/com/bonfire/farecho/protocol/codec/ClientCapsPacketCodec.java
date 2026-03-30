package com.bonfire.farecho.protocol.codec;

import com.bonfire.farecho.protocol.PacketCodec;
import com.bonfire.farecho.protocol.PacketReader;
import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.PacketWriter;
import com.bonfire.farecho.protocol.model.ClientCapsPacket;

public final class ClientCapsPacketCodec implements PacketCodec<ClientCapsPacket> {
    @Override
    public PacketType type() {
        return PacketType.CLIENT_CAPS;
    }

    @Override
    public Class<ClientCapsPacket> payloadType() {
        return ClientCapsPacket.class;
    }

    @Override
    public void encode(PacketWriter writer, ClientCapsPacket packet) {
        CodecSupport.writeCapabilities(writer, packet.capabilities());
    }

    @Override
    public ClientCapsPacket decode(PacketReader reader) {
        return new ClientCapsPacket(CodecSupport.readCapabilities(reader));
    }
}
