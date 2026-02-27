package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.animation.TutorialAnimationType;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for tutorial animation synchronization.
 * <p>
 * S2C packets:
 * <ul>
 *   <li>PLAY - Tells client to start rendering animation particles</li>
 *   <li>RESET - Tells client to clear any active animation state</li>
 * </ul>
 */
public final class TutorialAnimationNetworking {

    private static final ResourceLocation ANIMATION_PLAY_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_animation_play");

    private static final ResourceLocation ANIMATION_RESET_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_animation_reset");

    private static boolean initialized = false;

    private TutorialAnimationNetworking() {
    }

    /**
     * Initializes networking receivers on the client.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Client receives animation play
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                ANIMATION_PLAY_PACKET_ID,
                (buf, context) -> {
                    int typeOrdinal = buf.readVarInt();
                    int x1 = buf.readInt();
                    int y1 = buf.readInt();
                    int z1 = buf.readInt();
                    int x2 = buf.readInt();
                    int y2 = buf.readInt();
                    int z2 = buf.readInt();

                    TutorialAnimationType[] types = TutorialAnimationType.values();
                    if (typeOrdinal < 0 || typeOrdinal >= types.length) {
                        AromaAffect.LOGGER.warn("Received invalid animation type ordinal: {}", typeOrdinal);
                        return;
                    }
                    TutorialAnimationType type = types[typeOrdinal];
                    BlockPos corner1 = new BlockPos(x1, y1, z1);
                    BlockPos corner2 = new BlockPos(x2, y2, z2);

                    context.queue(() -> {
                        // Call client-side handler via reflection
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.animation.client.TutorialAnimationRenderer"
                            );
                            rendererClass.getMethod("onAnimationPlay",
                                            TutorialAnimationType.class, BlockPos.class, BlockPos.class)
                                    .invoke(null, type, corner1, corner2);
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to invoke animation renderer on client", e);
                        }
                    });
                }
        );

        // Client receives animation reset
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                ANIMATION_RESET_PACKET_ID,
                (buf, context) -> {
                    context.queue(() -> {
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.animation.client.TutorialAnimationRenderer"
                            );
                            rendererClass.getMethod("clearAnimations").invoke(null);
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to clear animation renderer on client", e);
                        }
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial animation networking initialized");
    }

    /**
     * Sends an animation play packet to a player.
     *
     * @param player  the player to send to
     * @param type    the animation type
     * @param corner1 first corner of the cuboid
     * @param corner2 second corner of the cuboid
     */
    public static void sendAnimationPlay(ServerPlayer player, TutorialAnimationType type,
                                         BlockPos corner1, BlockPos corner2) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        buf.writeVarInt(type.ordinal());
        buf.writeInt(corner1.getX());
        buf.writeInt(corner1.getY());
        buf.writeInt(corner1.getZ());
        buf.writeInt(corner2.getX());
        buf.writeInt(corner2.getY());
        buf.writeInt(corner2.getZ());
        NetworkManager.sendToPlayer(player, ANIMATION_PLAY_PACKET_ID, buf);
    }

    /**
     * Sends an animation reset packet to a player (clears client state).
     *
     * @param player the player to send to
     */
    public static void sendAnimationReset(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        NetworkManager.sendToPlayer(player, ANIMATION_RESET_PACKET_ID, buf);
    }
}
