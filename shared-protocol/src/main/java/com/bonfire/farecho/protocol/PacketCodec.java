package com.bonfire.farecho.protocol;

public interface PacketCodec<T extends ProtocolPacket> {
    PacketType type();

    Class<T> payloadType();

    void encode(PacketWriter writer, T packet);

    T decode(PacketReader reader);
}
