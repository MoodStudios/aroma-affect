package com.ovrtechnology.tutorial.searchdiamond.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.SearchDiamondNetworking;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Client-side entry point for SearchDiamond screens.
 * Called via reflection from SearchDiamondNetworking.
 */
public final class SearchDiamondClient {

    private SearchDiamondClient() {}

    /**
     * Opens the start screen with instructions.
     */
    public static void openStartScreen() {
        AromaAffect.LOGGER.info("SearchDiamondClient.openStartScreen() called");
        try {
            Minecraft.getInstance().setScreen(new SearchDiamondStartScreen());
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error opening SearchDiamond start screen: ", e);
        }
    }

    /**
     * Opens the success screen when diamond is found.
     */
    public static void openSuccessScreen() {
        AromaAffect.LOGGER.info("SearchDiamondClient.openSuccessScreen() called");
        try {
            Minecraft.getInstance().setScreen(new SearchDiamondSuccessScreen());
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error opening SearchDiamond success screen: ", e);
        }
    }

    /**
     * Sends a packet to the server indicating the player clicked "Let's Go!".
     */
    public static void sendPlayerReady() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AromaAffect.LOGGER.info("[SearchDiamond] Sending player ready packet to server");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), mc.player.registryAccess());
        NetworkManager.sendToServer(SearchDiamondNetworking.getPlayerReadyPacketId(), buf);
    }
}
