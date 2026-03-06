package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for boss spawn cinematics.
 * <p>
 * Sends camera control packets to client for dramatic boss reveals.
 */
public final class TutorialBossCinematicNetworking {

    public record BossCinematicPayload(String bossType,
                                       double cameraX, double cameraY, double cameraZ,
                                       float yaw, float pitch,
                                       double bossX, double bossY, double bossZ) implements CustomPacketPayload {
        public static final Type<BossCinematicPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_boss_cinematic"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BossCinematicPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.bossType, 64);
                    buf.writeDouble(payload.cameraX);
                    buf.writeDouble(payload.cameraY);
                    buf.writeDouble(payload.cameraZ);
                    buf.writeFloat(payload.yaw);
                    buf.writeFloat(payload.pitch);
                    buf.writeDouble(payload.bossX);
                    buf.writeDouble(payload.bossY);
                    buf.writeDouble(payload.bossZ);
                },
                buf -> new BossCinematicPayload(
                        buf.readUtf(64),
                        buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readFloat(), buf.readFloat(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble()
                )
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private TutorialBossCinematicNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // S2C: Boss cinematic
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                BossCinematicPayload.TYPE,
                BossCinematicPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        handleCinematicOnClient(payload.bossType(),
                                payload.cameraX(), payload.cameraY(), payload.cameraZ(),
                                payload.yaw(), payload.pitch(),
                                payload.bossX(), payload.bossY(), payload.bossZ());
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial boss cinematic networking initialized");
    }

    /**
     * Sends a boss cinematic to a player.
     */
    public static void sendBossCinematic(ServerPlayer player, String bossType,
                                          double cameraX, double cameraY, double cameraZ,
                                          float yaw, float pitch,
                                          double bossX, double bossY, double bossZ) {
        NetworkManager.sendToPlayer(player, new BossCinematicPayload(
                bossType, cameraX, cameraY, cameraZ, yaw, pitch, bossX, bossY, bossZ
        ));
    }

    private static void handleCinematicOnClient(String bossType,
                                                  double cameraX, double cameraY, double cameraZ,
                                                  float yaw, float pitch,
                                                  double bossX, double bossY, double bossZ) {
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.boss.client.TutorialBossCinematicClient"
            );
            clientClass.getMethod("playCinematic",
                            String.class, double.class, double.class, double.class,
                            float.class, float.class, double.class, double.class, double.class)
                    .invoke(null, bossType, cameraX, cameraY, cameraZ, yaw, pitch, bossX, bossY, bossZ);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to play boss cinematic on client", e);
        }
    }
}
