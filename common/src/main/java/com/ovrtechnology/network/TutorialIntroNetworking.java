package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.spawn.TutorialJoinHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialIntroNetworking {

    public record IntroOpenS2C() implements CustomPacketPayload {
        public static final Type<IntroOpenS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_intro_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, IntroOpenS2C> STREAM_CODEC =
                StreamCodec.unit(new IntroOpenS2C());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record IntroStartC2S() implements CustomPacketPayload {
        public static final Type<IntroStartC2S> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_intro_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, IntroStartC2S> STREAM_CODEC =
                StreamCodec.unit(new IntroStartC2S());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private TutorialIntroNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Server tells client to open IntroScreen
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, IntroOpenS2C.TYPE, IntroOpenS2C.STREAM_CODEC,
                (payload, context) -> context.queue(() -> openIntroOnClient()));

        // C2S: Client tells server the player clicked START
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, IntroStartC2S.TYPE, IntroStartC2S.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    Player player = context.getPlayer();
                    if (player instanceof ServerPlayer serverPlayer) {
                        TutorialJoinHandler.handleIntroStart(serverPlayer);
                    }
                }));

        AromaAffect.LOGGER.debug("Tutorial intro networking initialized");
    }

    /**
     * Server sends this to a player to open the intro screen.
     */
    public static void sendOpenIntro(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new IntroOpenS2C());
    }

    /**
     * Client sends this to the server when the player clicks START.
     */
    public static void sendStartToServer(RegistryAccess registryAccess) {
        NetworkManager.sendToServer(new IntroStartC2S());
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
