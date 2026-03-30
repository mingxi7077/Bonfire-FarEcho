package com.bonfire.farecho.client.render;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;

public final class FarEchoGhostPlayerEntity extends OtherClientPlayerEntity {
    private final UUID sourcePlayerUuid;
    private final Map<UUID, SkinTextures> skinCache;

    public FarEchoGhostPlayerEntity(
        ClientWorld world,
        GameProfile profile,
        UUID sourcePlayerUuid,
        Map<UUID, SkinTextures> skinCache
    ) {
        super(world, profile);
        this.sourcePlayerUuid = sourcePlayerUuid;
        this.skinCache = skinCache;
    }

    @Override
    public SkinTextures getSkinTextures() {
        SkinTextures defaultSkin = DefaultSkinHelper.getSkinTextures(sourcePlayerUuid);
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null) {
            PlayerListEntry entry = networkHandler.getPlayerListEntry(sourcePlayerUuid);
            if (entry != null) {
                SkinTextures entrySkin = entry.getSkinTextures();
                if (!entrySkin.equals(defaultSkin)) {
                    skinCache.put(sourcePlayerUuid, entrySkin);
                    return entrySkin;
                }
            }
        }

        SkinTextures cached = skinCache.get(sourcePlayerUuid);
        if (cached != null) {
            return cached;
        }
        return defaultSkin;
    }

    @Override
    public boolean shouldRender(double distanceSquared) {
        // Force body visibility for long-range FarEcho targets.
        return true;
    }
}
