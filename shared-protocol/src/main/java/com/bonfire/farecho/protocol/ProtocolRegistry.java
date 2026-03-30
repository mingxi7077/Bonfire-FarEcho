package com.bonfire.farecho.protocol;

import com.bonfire.farecho.protocol.codec.ClientCapsPacketCodec;
import com.bonfire.farecho.protocol.codec.DebugStatsPacketCodec;
import com.bonfire.farecho.protocol.codec.DeltaBatchPacketCodec;
import com.bonfire.farecho.protocol.codec.HelloPacketCodec;
import com.bonfire.farecho.protocol.codec.RemoveBatchPacketCodec;
import com.bonfire.farecho.protocol.codec.ServerCapsPacketCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ProtocolRegistry {
    private final Map<PacketType, PacketCodec<? extends ProtocolPacket>> codecsByType = new EnumMap<>(PacketType.class);
    private final Map<Class<?>, PacketCodec<? extends ProtocolPacket>> codecsByClass = new HashMap<>();

    public static ProtocolRegistry createDefault() {
        ProtocolRegistry registry = new ProtocolRegistry();
        registry.register(new HelloPacketCodec());
        registry.register(new ServerCapsPacketCodec());
        registry.register(new ClientCapsPacketCodec());
        registry.register(new DeltaBatchPacketCodec());
        registry.register(new RemoveBatchPacketCodec());
        registry.register(new DebugStatsPacketCodec());
        return registry;
    }

    public <T extends ProtocolPacket> void register(PacketCodec<T> codec) {
        codecsByType.put(codec.type(), codec);
        codecsByClass.put(codec.payloadType(), codec);
    }

    public byte[] encodeFrame(long sequence, ProtocolPacket packet) {
        PacketCodec<ProtocolPacket> codec = resolveCodec(packet);
        ByteBuf payloadBuf = Unpooled.buffer(256);
        ByteBuf frame = Unpooled.buffer(320);
        try {
            PacketWriter payloadWriter = new PacketWriter(payloadBuf);
            codec.encode(payloadWriter, packet);

            frame.writeShort(ProtocolVersion.CURRENT);
            frame.writeByte(packet.type().id());
            frame.writeInt((int) sequence);
            VarInts.writeVarInt(frame, payloadBuf.readableBytes());
            frame.writeBytes(payloadBuf);
            return ByteBufUtil.getBytes(frame);
        } finally {
            payloadBuf.release();
            frame.release();
        }
    }

    public ProtocolFrame decodeFrame(byte[] frameBytes) {
        ByteBuf frame = Unpooled.wrappedBuffer(frameBytes);
        try {
            PacketReader reader = new PacketReader(frame);
            int version = reader.readUnsignedShort();
            if (!ProtocolVersion.isSupported(version)) {
                throw new ProtocolException("Unsupported protocol version: " + version);
            }

            PacketType packetType = PacketType.fromId(Byte.toUnsignedInt(reader.readByte()));
            long sequence = reader.readUnsignedInt();
            int payloadLength = reader.readVarInt();
            if (payloadLength < 0 || payloadLength > frame.readableBytes()) {
                throw new ProtocolException("Invalid payload length: " + payloadLength);
            }

            ByteBuf payloadSlice = frame.readSlice(payloadLength);
            PacketReader payloadReader = new PacketReader(payloadSlice);
            PacketCodec<? extends ProtocolPacket> codec = resolveCodec(packetType);
            ProtocolPacket packet = codec.decode(payloadReader);
            if (payloadSlice.isReadable()) {
                throw new ProtocolException("Packet left unread bytes: " + payloadSlice.readableBytes());
            }
            return new ProtocolFrame(version, sequence, packet);
        } catch (IndexOutOfBoundsException ex) {
            throw new ProtocolException("Malformed frame", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private PacketCodec<ProtocolPacket> resolveCodec(ProtocolPacket packet) {
        PacketCodec<? extends ProtocolPacket> codec = codecsByClass.get(packet.getClass());
        if (codec == null) {
            throw new ProtocolException("No codec for packet class: " + packet.getClass().getName());
        }
        return (PacketCodec<ProtocolPacket>) codec;
    }

    private PacketCodec<? extends ProtocolPacket> resolveCodec(PacketType packetType) {
        PacketCodec<? extends ProtocolPacket> codec = codecsByType.get(packetType);
        if (codec == null) {
            throw new ProtocolException("No codec for packet type: " + packetType);
        }
        return codec;
    }
}
