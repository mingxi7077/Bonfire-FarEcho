package com.bonfire.farecho.protocol.model;

import java.util.List;
import java.util.UUID;

public record EchoRemoveBatch(List<UUID> targetIds) {
}
