package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for tutorial portal overlay effect.
 * <p>
 * Sends portal progress to clients so they can render the Nether-style
 * portal overlay during teleportation buildup.
 */
public final class TutorialPortalOverlayNetworking {

    private static final ResourceLocation PORTAL_OVERLAY_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_portal_overlay");

    private static boolean initialized = false;

    private TutorialPortalOverlayNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Server tells client to show/update/hide portal overlay
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PORTAL_OVERLAY_PACKET,
                (buf, context) -> {
                    float progress = buf.readFloat();
                    context.queue(() -> handleOverlayOnClient(progress));
                });

        AromaAffect.LOGGER.debug("Tutorial portal overlay networking initialized");
    }

    /**
     * Sends portal overlay progress to a player.
     *
     * @param player   the player
     * @param progress 0.0 = no overlay, 1.0 = full overlay (about to teleport)
     */
    public static void sendOverlayProgress(ServerPlayer player, float progress) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeFloat(progress);
        NetworkManager.sendToPlayer(player, PORTAL_OVERLAY_PACKET, buf);
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
                    "com.ovrtechnology.tutorial.portal.client.TutorialPortalOverlayClient");
            clientClass.getMethod("setProgress", float.class).invoke(null, progress);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to update portal overlay via network packet", e);
        }
    }
}
