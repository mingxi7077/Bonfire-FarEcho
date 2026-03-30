package com.bonfire.farecho.protocol.codec;

import com.bonfire.farecho.protocol.PacketCodec;
import com.bonfire.farecho.protocol.PacketReader;
import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.PacketWriter;
import com.bonfire.farecho.protocol.model.DebugStats;
import com.bonfire.farecho.protocol.model.DebugStatsPacket;

public final class DebugStatsPacketCodec implements PacketCodec<DebugStatsPacket> {
    @Override
    public PacketType type() {
        return PacketType.DEBUG_STATS;
    }

    @Override
    public Class<DebugStatsPacket> payloadType() {
        return DebugStatsPacket.class;
    }

    @Override
    public void encode(PacketWriter writer, DebugStatsPacket packet) {
        DebugStats stats = packet.stats();
        writer.writeLong(stats.serverTick());
        writer.writeVarInt(stats.targetCount());
        writer.writeDouble(stats.clippedRatio());
        writer.writeVarInt(stats.packetsSent());
        writer.writeVarInt(stats.degradedCount());
        writer.writeLong(stats.bytesSent());
        writer.writeVarInt(stats.clientRenderCount());
    }

    @Override
    public DebugStatsPacket decode(PacketReader reader) {
        return new DebugStatsPacket(new DebugStats(
            reader.readLong(),
            reader.readVarInt(),
            reader.readDouble(),
            reader.readVarInt(),
            reader.readVarInt(),
            reader.readLong(),
            reader.readVarInt()
        ));
    }
}
