package com.bonfire.farecho.protocol;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PacketType {
    HELLO(1, "farecho:hello"),
    SERVER_CAPS(2, "farecho:server_caps"),
    CLIENT_CAPS(3, "farecho:client_caps"),
    DELTA_BATCH(4, "farecho:delta_batch"),
    REMOVE_BATCH(5, "farecho:remove_batch"),
    DEBUG_STATS(6, "farecho:debug_stats");

    private static final Map<Integer, PacketType> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(PacketType::id, Function.identity()));

    private static final Map<String, PacketType> BY_CHANNEL = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(PacketType::channelId, Function.identity()));

    private final int id;
    private final String channelId;

    PacketType(int id, String channelId) {
        this.id = id;
        this.channelId = channelId;
    }

    public int id() {
        return id;
    }

    public String channelId() {
        return channelId;
    }

    public static PacketType fromId(int id) {
        PacketType packetType = BY_ID.get(id);
        if (packetType == null) {
            throw new ProtocolException("Unknown packet type id: " + id);
        }
        return packetType;
    }

    public static PacketType fromChannel(String channel) {
        PacketType packetType = BY_CHANNEL.get(channel);
        if (packetType == null) {
            throw new ProtocolException("Unknown channel: " + channel);
        }
        return packetType;
    }
}
