package com.bonfire.farecho.protocol;

public record ProtocolFrame(int protocolVersion, long sequence, ProtocolPacket packet) {
}
