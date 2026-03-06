package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for tutorial popup HUD zones.
 * <p>
 * S2C: Send popup text to display (empty = hide).
 */
public final class TutorialPopupNetworking {

    public record PopupS2C(String text) implements CustomPacketPayload {
        public static final Type<PopupS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_popup"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PopupS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.text, 4096),
                buf -> new PopupS2C(buf.readUtf(4096))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    /** Client-side: current popup text (empty = hidden). */
    private static String clientPopupText = "";

    private TutorialPopupNetworking() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Receive popup text
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PopupS2C.TYPE, PopupS2C.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    clientPopupText = payload.text();
                }));

        AromaAffect.LOGGER.debug("Tutorial popup networking initialized");
    }

    /**
     * Sends popup text to a player. Empty string hides the popup.
     */
    public static void sendPopup(ServerPlayer player, String text) {
        NetworkManager.sendToPlayer(player, new PopupS2C(text));
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
