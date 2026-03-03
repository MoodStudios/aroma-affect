package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for tutorial dream overlay effect.
 * <p>
 * Sends dream overlay progress to clients so they can render a white
 * screen fade effect during the tutorial ending sequence.
 */
public final class TutorialDreamOverlayNetworking {

    private static final ResourceLocation DREAM_OVERLAY_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_dream_overlay");

    private static boolean initialized = false;

    private TutorialDreamOverlayNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Server tells client to show/update/hide dream overlay
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DREAM_OVERLAY_PACKET,
                (buf, context) -> {
                    float progress = buf.readFloat();
                    context.queue(() -> handleOverlayOnClient(progress));
                });

        AromaAffect.LOGGER.debug("Tutorial dream overlay networking initialized");
    }

    /**
     * Sends dream overlay progress to a player.
     *
     * @param player   the player
     * @param progress 0.0 = no overlay, 1.0 = full white screen
     */
    public static void sendOverlayProgress(ServerPlayer player, float progress) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeFloat(progress);
        NetworkManager.sendToPlayer(player, DREAM_OVERLAY_PACKET, buf);
    }

    /**
     * Sends clear overlay to a player (progress = 0).
     */
    public static void sendClearOverlay(ServerPlayer player) {
        sendOverlayProgress(player, 0.0f);
    }

    private static void handleOverlayOnClient(float progress) {
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.dream.client.TutorialDreamOverlayClient");
            clientClass.getMethod("setProgress", float.class).invoke(null, progress);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to update dream overlay via network packet", e);
        }
    }
}
