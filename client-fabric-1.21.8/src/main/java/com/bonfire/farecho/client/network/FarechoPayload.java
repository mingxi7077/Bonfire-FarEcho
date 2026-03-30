package com.bonfire.farecho.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FarechoPayload(CustomPayload.Id<FarechoPayload> id, byte[] data) implements CustomPayload {
    public static CustomPayload.Id<FarechoPayload> id(String channel) {
        return new CustomPayload.Id<>(Identifier.of(channel));
    }

    public static PacketCodec<PacketByteBuf, FarechoPayload> codec(CustomPayload.Id<FarechoPayload> id) {
        return PacketCodec.of(
            (payload, buf) -> buf.writeBytes(payload.data()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new FarechoPayload(id, bytes);
            }
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return id;
    }
}
