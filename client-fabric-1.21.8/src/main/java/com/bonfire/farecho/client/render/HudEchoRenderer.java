package com.bonfire.farecho.client.render;

import com.bonfire.farecho.client.state.EchoStateStore;
import com.bonfire.farecho.protocol.model.DebugStats;
import com.bonfire.farecho.protocol.model.EchoSnapshot;
import com.bonfire.farecho.protocol.model.Relation;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class HudEchoRenderer {
    private final EchoStateStore stateStore;

    public HudEchoRenderer(EchoStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) {
            return;
        }

        List<EchoSnapshot> snapshots = stateStore.visibleSnapshots(client);
        int y = 8;
        int maxRows = Math.min(8, snapshots.size());
        for (int i = 0; i < maxRows; i++) {
            EchoSnapshot snapshot = snapshots.get(i);
            String direction = directionArrow(snapshot.bearingDegrees() - client.player.getYaw());
            String line = direction + " " + snapshot.name() + " " + (int) snapshot.planarDistance() + "m";
            drawContext.drawText(client.textRenderer, line, 8, y, color(snapshot.relation()), true);
            y += 10;
        }

        DebugStats debugStats = stateStore.lastDebugStats();
        if (debugStats != null) {
            int debugY = client.getWindow().getScaledHeight() - 20;
            String debug = "FarEcho/s t=" + debugStats.targetCount()
                + " c=" + String.format("%.2f", debugStats.clippedRatio())
                + " p=" + debugStats.packetsSent()
                + " d=" + debugStats.degradedCount()
                + " b=" + debugStats.bytesSent()
                + " r=" + debugStats.clientRenderCount();
            drawContext.drawText(client.textRenderer, debug, 8, debugY, 0xFFE2E2E2, true);
        }
    }

    private int color(Relation relation) {
        return switch (relation) {
            case FRIEND -> 0xFF6BD26B;
            case HOSTILE -> 0xFFE05D5D;
            case NEUTRAL -> 0xFFE7D35B;
        };
    }

    private String directionArrow(double relativeDegrees) {
        double normalized = normalizeAngle(relativeDegrees);
        if (normalized >= -45 && normalized <= 45) {
            return "N";
        }
        if (normalized > 45 && normalized < 135) {
            return "E";
        }
        if (normalized < -45 && normalized > -135) {
            return "W";
        }
        return "S";
    }

    private double normalizeAngle(double angle) {
        double normalized = angle % 360.0;
        if (normalized > 180.0) {
            normalized -= 360.0;
        }
        if (normalized < -180.0) {
            normalized += 360.0;
        }
        return normalized;
    }
}
