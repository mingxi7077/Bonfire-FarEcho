package com.bonfire.farecho.protocol;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public final class PacketWriter {
    private static final int MAX_STRING_LENGTH = 32767;

    private final ByteBuf out;

    public PacketWriter(ByteBuf out) {
        this.out = out;
    }

    public ByteBuf out() {
        return out;
    }

    public void writeBoolean(boolean value) {
        out.writeBoolean(value);
    }

    public void writeByte(int value) {
        out.writeByte(value);
    }

    public void writeShort(int value) {
        out.writeShort(value);
    }

    public void writeInt(int value) {
        out.writeInt(value);
    }

    public void writeLong(long value) {
        out.writeLong(value);
    }

    public void writeFloat(float value) {
        out.writeFloat(value);
    }

    public void writeDouble(double value) {
        out.writeDouble(value);
    }

    public void writeVarInt(int value) {
        VarInts.writeVarInt(out, value);
    }

    public void writeString(String value) {
        String safe = value == null ? "" : value;
        byte[] bytes = safe.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_LENGTH * 4) {
            throw new ProtocolException("String too long: " + bytes.length);
        }
        writeVarInt(bytes.length);
        out.writeBytes(bytes);
    }

    public void writeUuid(UUID value) {
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
    }

    public <E extends Enum<E>> void writeEnum(E value) {
        writeVarInt(value.ordinal());
    }

    public <T> void writeCollection(Collection<T> values, Consumer<T> encoder) {
        writeVarInt(values.size());
        for (T value : values) {
            encoder.accept(value);
        }
    }
}
