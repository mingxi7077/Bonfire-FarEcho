package com.bonfire.farecho.protocol;

import io.netty.buffer.ByteBuf;

public final class VarInts {
    private VarInts() {
    }

    public static void writeVarInt(ByteBuf out, int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    public static int readVarInt(ByteBuf in) {
        int value = 0;
        int position = 0;
        while (true) {
            if (!in.isReadable()) {
                throw new ProtocolException("Incomplete varint");
            }
            int currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;
            if ((currentByte & 0x80) == 0) {
                return value;
            }
            position += 7;
            if (position >= 32) {
                throw new ProtocolException("Varint is too large");
            }
        }
    }
}
