package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class TutorialFinishNetworking {

    private static final ResourceLocation FINISH_OPEN_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_finish_open");

    private static boolean initialized = false;

    private TutorialFinishNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Server tells client to open FinishScreen
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FINISH_OPEN_PACKET,
                (buf, context) -> context.queue(() -> openFinishOnClient()));

        AromaAffect.LOGGER.debug("Tutorial finish networking initialized");
    }

    /**
     * Server sends this to a player to open the finish screen.
     */
    public static void sendOpenFinish(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        NetworkManager.sendToPlayer(player, FINISH_OPEN_PACKET, buf);
    }

    private static void openFinishOnClient() {
        AromaAffect.LOGGER.info("openFinishOnClient() called - attempting to open screen via reflection");
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.finishscreen.client.TutorialFinishClient");
            AromaAffect.LOGGER.info("Found TutorialFinishClient class");
            clientClass.getMethod("open").invoke(null);
            AromaAffect.LOGGER.info("Reflection call completed successfully");
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.error("Failed to open finish screen via network packet: {}", e.getMessage(), e);
        }
    }
}
