package com.bonfire.farecho.protocol.codec;

import com.bonfire.farecho.protocol.PacketCodec;
import com.bonfire.farecho.protocol.PacketReader;
import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.PacketWriter;
import com.bonfire.farecho.protocol.model.RemoveBatchPacket;

public final class RemoveBatchPacketCodec implements PacketCodec<RemoveBatchPacket> {
    @Override
    public PacketType type() {
        return PacketType.REMOVE_BATCH;
    }

    @Override
    public Class<RemoveBatchPacket> payloadType() {
        return RemoveBatchPacket.class;
    }

    @Override
    public void encode(PacketWriter writer, RemoveBatchPacket packet) {
        CodecSupport.writeRemoveBatch(writer, packet.batch());
    }

    @Override
    public RemoveBatchPacket decode(PacketReader reader) {
        return new RemoveBatchPacket(CodecSupport.readRemoveBatch(reader));
    }
}
