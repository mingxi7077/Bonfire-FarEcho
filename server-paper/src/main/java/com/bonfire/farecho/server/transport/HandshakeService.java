package com.bonfire.farecho.server.transport;

import com.bonfire.farecho.protocol.ProtocolFrame;
import com.bonfire.farecho.protocol.ProtocolVersion;
import com.bonfire.farecho.protocol.model.Capabilities;
import com.bonfire.farecho.protocol.model.ClientCapsPacket;
import com.bonfire.farecho.protocol.model.HelloPacket;
import com.bonfire.farecho.protocol.model.ServerCapsPacket;
import com.bonfire.farecho.server.config.FarechoConfiguration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HandshakeService {
    private final JavaPlugin plugin;
    private final EchoTransport transport;
    private final Supplier<FarechoConfiguration> configSupplier;
    private final Set<UUID> farEchoClients = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<UUID, Capabilities> clientCaps = new ConcurrentHashMap<>();

    public HandshakeService(JavaPlugin plugin, EchoTransport transport, Supplier<FarechoConfiguration> configSupplier) {
        this.plugin = plugin;
        this.transport = transport;
        this.configSupplier = configSupplier;
    }

    public void handleInbound(Player player, String channel, ProtocolFrame frame) {
        switch (frame.packet()) {
            case HelloPacket helloPacket -> handleHello(player, helloPacket);
            case ClientCapsPacket capsPacket -> clientCaps.put(player.getUniqueId(), capsPacket.capabilities());
            default -> {
            }
        }
    }

    public boolean isFarEchoClient(Player player) {
        return farEchoClients.contains(player.getUniqueId());
    }

    public int knownClientCount() {
        return farEchoClients.size();
    }

    public Capabilities capabilities(UUID playerId) {
        return clientCaps.get(playerId);
    }

    public void forget(UUID playerId) {
        farEchoClients.remove(playerId);
        clientCaps.remove(playerId);
    }

    private void handleHello(Player player, HelloPacket helloPacket) {
        if (helloPacket.senderId() == null || !helloPacket.senderId().equals(player.getUniqueId())) {
            plugin.getLogger().warning("FarEcho hello sender mismatch: " + player.getName());
            return;
        }
        farEchoClients.add(player.getUniqueId());

        FarechoConfiguration configuration = configSupplier.get();
        Capabilities serverCapabilities = new Capabilities(
            "paper-server",
            ProtocolVersion.CURRENT,
            Set.of("hud", "world-marker", "delta", "remove", "debug"),
            configuration.maxTargetsPerObserver(),
            true,
            true
        );
        transport.send(player, new ServerCapsPacket(serverCapabilities));
    }
}
