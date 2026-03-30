package com.bonfire.farecho.protocol;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class PacketReader {
    private static final int MAX_STRING_BYTES = 32767 * 4;

    private final ByteBuf in;

    public PacketReader(ByteBuf in) {
        this.in = in;
    }

    public ByteBuf in() {
        return in;
    }

    public boolean readBoolean() {
        return in.readBoolean();
    }

    public byte readByte() {
        return in.readByte();
    }

    public short readShort() {
        return in.readShort();
    }

    public int readUnsignedShort() {
        return in.readUnsignedShort();
    }

    public int readInt() {
        return in.readInt();
    }

    public long readUnsignedInt() {
        return Integer.toUnsignedLong(in.readInt());
    }

    public long readLong() {
        return in.readLong();
    }

    public float readFloat() {
        return in.readFloat();
    }

    public double readDouble() {
        return in.readDouble();
    }

    public int readVarInt() {
        return VarInts.readVarInt(in);
    }

    public String readString() {
        int length = readVarInt();
        if (length < 0 || length > MAX_STRING_BYTES || in.readableBytes() < length) {
            throw new ProtocolException("Invalid string length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public UUID readUuid() {
        long msb = in.readLong();
        long lsb = in.readLong();
        return new UUID(msb, lsb);
    }

    public <E extends Enum<E>> E readEnum(Class<E> enumType) {
        int ordinal = readVarInt();
        E[] constants = enumType.getEnumConstants();
        if (ordinal < 0 || ordinal >= constants.length) {
            throw new ProtocolException("Invalid enum ordinal " + ordinal + " for " + enumType.getSimpleName());
        }
        return constants[ordinal];
    }

    public <T> List<T> readList(Supplier<T> decoder) {
        int size = readVarInt();
        if (size < 0 || size > 100000) {
            throw new ProtocolException("Invalid list size: " + size);
        }
        List<T> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(decoder.get());
        }
        return values;
    }
}
