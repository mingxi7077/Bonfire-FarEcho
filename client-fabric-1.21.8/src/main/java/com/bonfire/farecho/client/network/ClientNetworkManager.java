package com.bonfire.farecho.client.network;

import com.bonfire.farecho.client.FarEchoClientMod;
import com.bonfire.farecho.client.state.EchoStateStore;
import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.ProtocolFrame;
import com.bonfire.farecho.protocol.ProtocolPacket;
import com.bonfire.farecho.protocol.ProtocolRegistry;
import com.bonfire.farecho.protocol.ProtocolVersion;
import com.bonfire.farecho.protocol.model.Capabilities;
import com.bonfire.farecho.protocol.model.ClientCapsPacket;
import com.bonfire.farecho.protocol.model.DebugStatsPacket;
import com.bonfire.farecho.protocol.model.DeltaBatchPacket;
import com.bonfire.farecho.protocol.model.HelloPacket;
import com.bonfire.farecho.protocol.model.RemoveBatchPacket;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.CustomPayload;

public final class ClientNetworkManager {
    private final ProtocolRegistry protocolRegistry;
    private final EchoStateStore stateStore;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<PacketType, CustomPayload.Id<FarechoPayload>> payloadIds = new EnumMap<>(PacketType.class);

    public ClientNetworkManager(ProtocolRegistry protocolRegistry, EchoStateStore stateStore) {
        this.protocolRegistry = protocolRegistry;
        this.stateStore = stateStore;
        for (PacketType packetType : PacketType.values()) {
            payloadIds.put(packetType, FarechoPayload.id(packetType.channelId()));
        }
    }

    public void registerReceivers() {
        for (PacketType packetType : PacketType.values()) {
            CustomPayload.Id<FarechoPayload> payloadId = payloadIds.get(packetType);
            PayloadTypeRegistry.playS2C().register(payloadId, FarechoPayload.codec(payloadId));
            PayloadTypeRegistry.playC2S().register(payloadId, FarechoPayload.codec(payloadId));
            ClientPlayNetworking.registerGlobalReceiver(payloadId, (payload, context) ->
                context.client().execute(() -> handleIncoming(payload.data()))
            );
        }
    }

    public void sendHelloAndCapabilities(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        send(new HelloPacket(client.player.getUuid(), "fabric-client", "0.2.0"));
        send(new ClientCapsPacket(new Capabilities(
            "fabric-client",
            ProtocolVersion.CURRENT,
            Set.of("hud", "world-marker", "debug"),
            96,
            true,
            true
        )));
    }

    private void send(ProtocolPacket packet) {
        byte[] frame = protocolRegistry.encodeFrame(sequence.incrementAndGet(), packet);
        CustomPayload.Id<FarechoPayload> id = payloadIds.get(packet.type());
        if (id == null) {
            throw new IllegalStateException("Missing payload id for " + packet.type());
        }
        ClientPlayNetworking.send(new FarechoPayload(id, frame));
    }

    private void handleIncoming(byte[] frameBytes) {
        ProtocolFrame frame = protocolRegistry.decodeFrame(frameBytes);
        switch (frame.packet()) {
            case DeltaBatchPacket deltaBatchPacket -> stateStore.applyDelta(deltaBatchPacket.batch());
            case RemoveBatchPacket removeBatchPacket -> stateStore.applyRemove(removeBatchPacket.batch());
            case DebugStatsPacket debugStatsPacket -> stateStore.setLastDebugStats(debugStatsPacket.stats());
            default -> FarEchoClientMod.LOGGER.debug("FarEcho client ignored packet {}", frame.packet().type());
        }
    }
}
