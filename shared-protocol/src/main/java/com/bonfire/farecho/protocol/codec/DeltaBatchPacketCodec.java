package com.bonfire.farecho.protocol.codec;

import com.bonfire.farecho.protocol.PacketCodec;
import com.bonfire.farecho.protocol.PacketReader;
import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.PacketWriter;
import com.bonfire.farecho.protocol.model.DeltaBatchPacket;

public final class DeltaBatchPacketCodec implements PacketCodec<DeltaBatchPacket> {
    @Override
    public PacketType type() {
        return PacketType.DELTA_BATCH;
    }

    @Override
    public Class<DeltaBatchPacket> payloadType() {
        return DeltaBatchPacket.class;
    }

    @Override
    public void encode(PacketWriter writer, DeltaBatchPacket packet) {
        CodecSupport.writeDeltaBatch(writer, packet.batch());
    }

    @Override
    public DeltaBatchPacket decode(PacketReader reader) {
        return new DeltaBatchPacket(CodecSupport.readDeltaBatch(reader));
    }
}
