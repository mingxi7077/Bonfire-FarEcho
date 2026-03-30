package com.bonfire.farecho.protocol.model;

import java.util.List;

public record EchoDeltaBatch(long serverTick, List<EchoSnapshot> snapshots) {
}
