package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.animation.TutorialAnimationType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

    public record AnimationPlayPayload(int typeOrdinal, int x1, int y1, int z1,
                                       int x2, int y2, int z2) implements CustomPacketPayload {
        public static final Type<AnimationPlayPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_animation_play"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AnimationPlayPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.typeOrdinal);
                    buf.writeInt(payload.x1);
                    buf.writeInt(payload.y1);
                    buf.writeInt(payload.z1);
                    buf.writeInt(payload.x2);
                    buf.writeInt(payload.y2);
                    buf.writeInt(payload.z2);
                },
                buf -> new AnimationPlayPayload(
                        buf.readVarInt(),
                        buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt()
                )
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AnimationResetPayload() implements CustomPacketPayload {
        public static final Type<AnimationResetPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_animation_reset"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AnimationResetPayload> STREAM_CODEC =
                StreamCodec.unit(new AnimationResetPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

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
                AnimationPlayPayload.TYPE,
                AnimationPlayPayload.STREAM_CODEC,
                (payload, context) -> {
                    int typeOrdinal = payload.typeOrdinal();
                    TutorialAnimationType[] types = TutorialAnimationType.values();
                    if (typeOrdinal < 0 || typeOrdinal >= types.length) {
                        AromaAffect.LOGGER.warn("Received invalid animation type ordinal: {}", typeOrdinal);
                        return;
                    }
                    TutorialAnimationType type = types[typeOrdinal];
                    BlockPos corner1 = new BlockPos(payload.x1(), payload.y1(), payload.z1());
                    BlockPos corner2 = new BlockPos(payload.x2(), payload.y2(), payload.z2());

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
                AnimationResetPayload.TYPE,
                AnimationResetPayload.STREAM_CODEC,
                (payload, context) -> {
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
        NetworkManager.sendToPlayer(player, new AnimationPlayPayload(
                type.ordinal(),
                corner1.getX(), corner1.getY(), corner1.getZ(),
                corner2.getX(), corner2.getY(), corner2.getZ()
        ));
    }

    /**
     * Sends an animation reset packet to a player (clears client state).
     *
     * @param player the player to send to
     */
    public static void sendAnimationReset(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new AnimationResetPayload());
    }
}
