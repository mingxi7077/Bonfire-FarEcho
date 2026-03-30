package com.bonfire.farecho.protocol.codec;

import com.bonfire.farecho.protocol.PacketCodec;
import com.bonfire.farecho.protocol.PacketReader;
import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.PacketWriter;
import com.bonfire.farecho.protocol.model.ServerCapsPacket;

public final class ServerCapsPacketCodec implements PacketCodec<ServerCapsPacket> {
    @Override
    public PacketType type() {
        return PacketType.SERVER_CAPS;
    }

    @Override
    public Class<ServerCapsPacket> payloadType() {
        return ServerCapsPacket.class;
    }

    @Override
    public void encode(PacketWriter writer, ServerCapsPacket packet) {
        CodecSupport.writeCapabilities(writer, packet.capabilities());
    }

    @Override
    public ServerCapsPacket decode(PacketReader reader) {
        return new ServerCapsPacket(CodecSupport.readCapabilities(reader));
    }
}
