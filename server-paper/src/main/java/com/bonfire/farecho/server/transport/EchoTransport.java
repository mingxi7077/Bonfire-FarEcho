package com.bonfire.farecho.server.transport;

import com.bonfire.farecho.protocol.PacketType;
import com.bonfire.farecho.protocol.ProtocolException;
import com.bonfire.farecho.protocol.ProtocolFrame;
import com.bonfire.farecho.protocol.ProtocolPacket;
import com.bonfire.farecho.protocol.ProtocolRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EchoTransport {
    private final JavaPlugin plugin;
    private final ProtocolRegistry protocolRegistry;
    private final InboundPacketHandler inboundHandler;
    private final AtomicLong sequence = new AtomicLong();

    public EchoTransport(JavaPlugin plugin, ProtocolRegistry protocolRegistry, InboundPacketHandler inboundHandler) {
        this.plugin = plugin;
        this.protocolRegistry = protocolRegistry;
        this.inboundHandler = inboundHandler;
    }

    public void registerChannels() {
        for (PacketType type : PacketType.values()) {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, type.channelId());
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, type.channelId(), this::handleIncoming);
        }
    }

    public void unregisterChannels() {
        for (PacketType type : PacketType.values()) {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, type.channelId());
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, type.channelId());
        }
    }

    public int send(Player player, ProtocolPacket packet) {
        byte[] frame = protocolRegistry.encodeFrame(sequence.incrementAndGet(), packet);
        player.sendPluginMessage(plugin, packet.type().channelId(), frame);
        return frame.length;
    }

    private void handleIncoming(String channel, Player player, byte[] message) {
        try {
            ProtocolFrame frame = protocolRegistry.decodeFrame(message);
            PacketType channelType = PacketType.fromChannel(channel);
            if (channelType != frame.packet().type()) {
                plugin.getLogger().warning("FarEcho channel mismatch: " + channel + " / " + frame.packet().type());
                return;
            }
            inboundHandler.handle(player, channel, frame);
        } catch (ProtocolException ex) {
            plugin.getLogger().warning("FarEcho protocol decode failed for " + player.getName() + ": " + ex.getMessage());
        }
    }
}
