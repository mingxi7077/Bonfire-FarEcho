package com.bonfire.farecho.client;

import com.bonfire.farecho.client.network.ClientNetworkManager;
import com.bonfire.farecho.client.render.GhostBodyManager;
import com.bonfire.farecho.client.render.HudEchoRenderer;
import com.bonfire.farecho.client.render.WorldMarkerRenderer;
import com.bonfire.farecho.client.state.EchoStateStore;
import com.bonfire.farecho.protocol.ProtocolRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FarEchoClientMod implements ClientModInitializer {
    public static final String MOD_ID = "farecho";
    public static final Logger LOGGER = LoggerFactory.getLogger("FarEchoClient");

    @Override
    public void onInitializeClient() {
        ProtocolRegistry protocolRegistry = ProtocolRegistry.createDefault();
        EchoStateStore stateStore = new EchoStateStore();
        ClientNetworkManager networkManager = new ClientNetworkManager(protocolRegistry, stateStore);
        networkManager.registerReceivers();

        HudEchoRenderer hudRenderer = new HudEchoRenderer(stateStore);
        hudRenderer.register();

        GhostBodyManager ghostBodyManager = new GhostBodyManager(stateStore);
        WorldMarkerRenderer worldMarkerRenderer = new WorldMarkerRenderer(stateStore);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> networkManager.sendHelloAndCapabilities(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ghostBodyManager.clear(client);
            stateStore.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            stateStore.expireStale(System.currentTimeMillis() - 5000L);
            ghostBodyManager.tick(client);
            worldMarkerRenderer.tick(client);
        });
    }
}

