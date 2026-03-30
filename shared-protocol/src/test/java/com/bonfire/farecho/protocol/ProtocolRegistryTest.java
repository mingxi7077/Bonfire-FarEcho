package com.bonfire.farecho.protocol;

import com.bonfire.farecho.protocol.model.Capabilities;
import com.bonfire.farecho.protocol.model.ClientCapsPacket;
import com.bonfire.farecho.protocol.model.DeltaBatchPacket;
import com.bonfire.farecho.protocol.model.EchoDeltaBatch;
import com.bonfire.farecho.protocol.model.EchoSnapshot;
import com.bonfire.farecho.protocol.model.Relation;
import com.bonfire.farecho.protocol.model.RemoveBatchPacket;
import com.bonfire.farecho.protocol.model.EchoRemoveBatch;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProtocolRegistryTest {

    @Test
    void deltaBatchRoundTrip() {
        ProtocolRegistry registry = ProtocolRegistry.createDefault();
        EchoSnapshot snapshot = new EchoSnapshot(
            UUID.randomUUID(),
            "Alpha",
            "minecraft:overworld",
            120.5,
            68.0,
            -40.25,
            45.0f,
            2.0f,
            12345L,
            3,
            Relation.FRIEND,
            "skin:alpha",
            false,
            642.0,
            33.3
        );
        DeltaBatchPacket packet = new DeltaBatchPacket(new EchoDeltaBatch(200L, List.of(snapshot)));

        byte[] encoded = registry.encodeFrame(18L, packet);
        ProtocolFrame decoded = registry.decodeFrame(encoded);

        Assertions.assertEquals(ProtocolVersion.CURRENT, decoded.protocolVersion());
        Assertions.assertEquals(18L, decoded.sequence());
        Assertions.assertInstanceOf(DeltaBatchPacket.class, decoded.packet());
        DeltaBatchPacket decodedPacket = (DeltaBatchPacket) decoded.packet();
        Assertions.assertEquals(packet, decodedPacket);
    }

    @Test
    void capabilitiesRoundTrip() {
        ProtocolRegistry registry = ProtocolRegistry.createDefault();
        ClientCapsPacket packet = new ClientCapsPacket(new Capabilities(
            "fabric-client",
            ProtocolVersion.CURRENT,
            Set.of("hud", "world-marker"),
            96,
            true,
            true
        ));

        byte[] encoded = registry.encodeFrame(3L, packet);
        ProtocolFrame decoded = registry.decodeFrame(encoded);
        Assertions.assertEquals(packet, decoded.packet());
    }

    @Test
    void removeBatchRoundTrip() {
        ProtocolRegistry registry = ProtocolRegistry.createDefault();
        RemoveBatchPacket packet = new RemoveBatchPacket(
            new EchoRemoveBatch(List.of(UUID.randomUUID(), UUID.randomUUID()))
        );

        ProtocolFrame decoded = registry.decodeFrame(registry.encodeFrame(99L, packet));
        Assertions.assertEquals(packet, decoded.packet());
    }

    @Test
    void rejectsUnsupportedProtocolVersion() {
        ProtocolRegistry registry = ProtocolRegistry.createDefault();
        DeltaBatchPacket packet = new DeltaBatchPacket(new EchoDeltaBatch(1L, List.of()));
        byte[] encoded = registry.encodeFrame(1L, packet);
        encoded[0] = 0;
        encoded[1] = 99;

        Assertions.assertThrows(ProtocolException.class, () -> registry.decodeFrame(encoded));
    }
}
