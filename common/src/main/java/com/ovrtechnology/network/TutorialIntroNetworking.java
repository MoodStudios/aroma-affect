package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.spawn.TutorialJoinHandler;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialIntroNetworking {

    private static final ResourceLocation INTRO_OPEN_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_intro_open");
    private static final ResourceLocation INTRO_PLAY_DEMO_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_intro_play_demo");
    private static final ResourceLocation INTRO_WALKAROUND_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_intro_walkaround");

    private static boolean initialized = false;

    private TutorialIntroNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Server tells client to open IntroScreen
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, INTRO_OPEN_PACKET,
                (buf, context) -> context.queue(() -> openIntroOnClient()));

        // C2S: Client tells server the player clicked PLAY DEMO
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, INTRO_PLAY_DEMO_PACKET,
                (buf, context) -> context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player instanceof ServerPlayer serverPlayer) {
                        TutorialJoinHandler.handlePlayDemo(serverPlayer);
                    }
                }));

        // C2S: Client tells server the player clicked WALKAROUND
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, INTRO_WALKAROUND_PACKET,
                (buf, context) -> context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player instanceof ServerPlayer serverPlayer) {
                        TutorialJoinHandler.handleWalkaround(serverPlayer);
                    }
                }));

        AromaAffect.LOGGER.debug("Tutorial intro networking initialized");
    }

    /**
     * Server sends this to a player to open the intro screen.
     */
    public static void sendOpenIntro(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        NetworkManager.sendToPlayer(player, INTRO_OPEN_PACKET, buf);
    }

    /**
     * Client sends this to the server when the player clicks PLAY DEMO.
     */
    public static void sendPlayDemoToServer(RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        NetworkManager.sendToServer(INTRO_PLAY_DEMO_PACKET, buf);
    }

    /**
     * Client sends this to the server when the player clicks WALKAROUND.
     */
    public static void sendWalkaroundToServer(RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        NetworkManager.sendToServer(INTRO_WALKAROUND_PACKET, buf);
    }

    private static void openIntroOnClient() {
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.intro.client.TutorialIntroClient");
            clientClass.getMethod("open").invoke(null);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to open intro screen via network packet", e);
        }
    }
}
