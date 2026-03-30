package com.bonfire.farecho.protocol.model;

import java.util.Set;

public record Capabilities(
    String implementation,
    int protocolVersion,
    Set<String> features,
    int maxTargets,
    boolean hudEnabled,
    boolean worldMarkerEnabled
) {
}
