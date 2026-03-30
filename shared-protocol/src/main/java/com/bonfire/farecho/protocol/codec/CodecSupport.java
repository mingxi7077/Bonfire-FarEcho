package com.bonfire.farecho.protocol.codec;

import com.bonfire.farecho.protocol.PacketReader;
import com.bonfire.farecho.protocol.PacketWriter;
import com.bonfire.farecho.protocol.model.Capabilities;
import com.bonfire.farecho.protocol.model.EchoDeltaBatch;
import com.bonfire.farecho.protocol.model.EchoRemoveBatch;
import com.bonfire.farecho.protocol.model.EchoSnapshot;
import com.bonfire.farecho.protocol.model.Relation;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class CodecSupport {
    private CodecSupport() {
    }

    static void writeCapabilities(PacketWriter writer, Capabilities capabilities) {
        writer.writeString(capabilities.implementation());
        writer.writeVarInt(capabilities.protocolVersion());
        writer.writeCollection(capabilities.features(), writer::writeString);
        writer.writeVarInt(capabilities.maxTargets());
        writer.writeBoolean(capabilities.hudEnabled());
        writer.writeBoolean(capabilities.worldMarkerEnabled());
    }

    static Capabilities readCapabilities(PacketReader reader) {
        String implementation = reader.readString();
        int protocolVersion = reader.readVarInt();
        Set<String> features = new HashSet<>(reader.readList(reader::readString));
        return new Capabilities(
            implementation,
            protocolVersion,
            features,
            reader.readVarInt(),
            reader.readBoolean(),
            reader.readBoolean()
        );
    }

    static void writeEchoSnapshot(PacketWriter writer, EchoSnapshot snapshot) {
        writer.writeUuid(snapshot.uuid());
        writer.writeString(snapshot.name());
        writer.writeString(snapshot.worldKey());
        writer.writeDouble(snapshot.x());
        writer.writeDouble(snapshot.y());
        writer.writeDouble(snapshot.z());
        writer.writeFloat(snapshot.yaw());
        writer.writeFloat(snapshot.pitch());
        writer.writeLong(snapshot.lastMoveTick());
        writer.writeInt(snapshot.flags());
        writer.writeEnum(snapshot.relation());
        writer.writeString(snapshot.skinHint());
        writer.writeBoolean(snapshot.directionOnly());
        writer.writeDouble(snapshot.planarDistance());
        writer.writeDouble(snapshot.bearingDegrees());
    }

    static EchoSnapshot readEchoSnapshot(PacketReader reader) {
        return new EchoSnapshot(
            reader.readUuid(),
            reader.readString(),
            reader.readString(),
            reader.readDouble(),
            reader.readDouble(),
            reader.readDouble(),
            reader.readFloat(),
            reader.readFloat(),
            reader.readLong(),
            reader.readInt(),
            reader.readEnum(Relation.class),
            reader.readString(),
            reader.readBoolean(),
            reader.readDouble(),
            reader.readDouble()
        );
    }

    static void writeDeltaBatch(PacketWriter writer, EchoDeltaBatch batch) {
        writer.writeLong(batch.serverTick());
        writer.writeCollection(batch.snapshots(), snapshot -> writeEchoSnapshot(writer, snapshot));
    }

    static EchoDeltaBatch readDeltaBatch(PacketReader reader) {
        long tick = reader.readLong();
        return new EchoDeltaBatch(tick, reader.readList(() -> readEchoSnapshot(reader)));
    }

    static void writeRemoveBatch(PacketWriter writer, EchoRemoveBatch batch) {
        writer.writeCollection(batch.targetIds(), writer::writeUuid);
    }

    static EchoRemoveBatch readRemoveBatch(PacketReader reader) {
        return new EchoRemoveBatch(reader.readList(reader::readUuid));
    }

    static void writeUuidSet(PacketWriter writer, Set<UUID> values) {
        writer.writeCollection(values, writer::writeUuid);
    }
}
