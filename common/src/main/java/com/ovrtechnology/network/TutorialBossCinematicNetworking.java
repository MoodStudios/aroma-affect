package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for boss spawn cinematics.
 * <p>
 * Sends camera control packets to client for dramatic boss reveals.
 */
public final class TutorialBossCinematicNetworking {

    private static final ResourceLocation BOSS_CINEMATIC_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_boss_cinematic");

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
                BOSS_CINEMATIC_PACKET,
                (buf, context) -> {
                    String bossType = buf.readUtf(64);
                    double cameraX = buf.readDouble();
                    double cameraY = buf.readDouble();
                    double cameraZ = buf.readDouble();
                    float yaw = buf.readFloat();
                    float pitch = buf.readFloat();
                    double bossX = buf.readDouble();
                    double bossY = buf.readDouble();
                    double bossZ = buf.readDouble();

                    context.queue(() -> {
                        handleCinematicOnClient(bossType, cameraX, cameraY, cameraZ, yaw, pitch, bossX, bossY, bossZ);
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
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        buf.writeUtf(bossType, 64);
        buf.writeDouble(cameraX);
        buf.writeDouble(cameraY);
        buf.writeDouble(cameraZ);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeDouble(bossX);
        buf.writeDouble(bossY);
        buf.writeDouble(bossZ);
        NetworkManager.sendToPlayer(player, BOSS_CINEMATIC_PACKET, buf);
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
