package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for tutorial popup HUD zones.
 * <p>
 * S2C: Send popup text to display (empty = hide).
 */
public final class TutorialPopupNetworking {

    private static final ResourceLocation POPUP_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_popup");

    private static boolean initialized = false;

    /** Client-side: current popup text (empty = hidden). */
    private static String clientPopupText = "";

    private TutorialPopupNetworking() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Receive popup text
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                POPUP_PACKET,
                (buf, context) -> {
                    String text = buf.readUtf(4096);
                    context.queue(() -> {
                        clientPopupText = text;
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial popup networking initialized");
    }

    /**
     * Sends popup text to a player. Empty string hides the popup.
     */
    public static void sendPopup(ServerPlayer player, String text) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        buf.writeUtf(text, 4096);
        NetworkManager.sendToPlayer(player, POPUP_PACKET, buf);
    }

    /**
     * Hides the popup for a player.
     */
    public static void sendClearPopup(ServerPlayer player) {
        sendPopup(player, "");
    }

    /**
     * Gets the current client-side popup text.
     */
    public static String getClientPopupText() {
        return clientPopupText;
    }

    /**
     * Checks if there is an active popup on the client.
     */
    public static boolean hasClientPopup() {
        return !clientPopupText.isEmpty();
    }
}
