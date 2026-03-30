package com.bonfire.farecho.protocol.codec;

import com.bonfire.farecho.protocol.PacketCodec;
import com.bonfire.farecho.protocol.PacketReader;
import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.PacketWriter;
import com.bonfire.farecho.protocol.model.HelloPacket;

public final class HelloPacketCodec implements PacketCodec<HelloPacket> {
    @Override
    public PacketType type() {
        return PacketType.HELLO;
    }

    @Override
    public Class<HelloPacket> payloadType() {
        return HelloPacket.class;
    }

    @Override
    public void encode(PacketWriter writer, HelloPacket packet) {
        writer.writeUuid(packet.senderId());
        writer.writeString(packet.senderKind());
        writer.writeString(packet.modVersion());
    }

    @Override
    public HelloPacket decode(PacketReader reader) {
        return new HelloPacket(reader.readUuid(), reader.readString(), reader.readString());
    }
}
