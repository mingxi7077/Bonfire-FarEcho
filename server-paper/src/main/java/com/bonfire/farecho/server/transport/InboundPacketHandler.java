package com.bonfire.farecho.server.transport;

import com.bonfire.farecho.protocol.ProtocolFrame;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface InboundPacketHandler {
    void handle(Player player, String channel, ProtocolFrame frame);
}
