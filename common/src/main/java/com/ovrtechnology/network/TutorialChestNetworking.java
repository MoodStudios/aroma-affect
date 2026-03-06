package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.chest.TutorialChest;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Networking for tutorial chest synchronization.
 * <p>
 * Syncs chest positions to clients for particle rendering.
 */
public final class TutorialChestNetworking {

    public record ChestSyncPayload(BlockPos[] positions) implements CustomPacketPayload {
        public static final Type<ChestSyncPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_chest_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChestSyncPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.positions.length);
                    for (BlockPos pos : payload.positions) {
                        buf.writeInt(pos.getX());
                        buf.writeInt(pos.getY());
                        buf.writeInt(pos.getZ());
                    }
                },
                buf -> {
                    int count = buf.readVarInt();
                    BlockPos[] positions = new BlockPos[count];
                    for (int i = 0; i < count; i++) {
                        positions[i] = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
                    }
                    return new ChestSyncPayload(positions);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChestConsumedPayload(BlockPos position) implements CustomPacketPayload {
        public static final Type<ChestConsumedPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_chest_consumed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChestConsumedPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeInt(payload.position.getX());
                    buf.writeInt(payload.position.getY());
                    buf.writeInt(payload.position.getZ());
                },
                buf -> new ChestConsumedPayload(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChestClearPayload() implements CustomPacketPayload {
        public static final Type<ChestClearPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_chest_clear"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChestClearPayload> STREAM_CODEC =
                StreamCodec.unit(new ChestClearPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private TutorialChestNetworking() {
    }

    /**
     * Initializes networking receivers on the client.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Client receives chest sync (list of positions)
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                ChestSyncPayload.TYPE,
                ChestSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    BlockPos[] positions = payload.positions();

                    context.queue(() -> {
                        // Call client-side handler via reflection
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.chest.client.TutorialChestRenderer"
                            );
                            rendererClass.getMethod("setChestPositions", BlockPos[].class)
                                    .invoke(null, (Object) positions);
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to set chest positions on client", e);
                        }
                    });
                }
        );

        // Client receives chest consumed (single position to remove)
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                ChestConsumedPayload.TYPE,
                ChestConsumedPayload.STREAM_CODEC,
                (payload, context) -> {
                    BlockPos pos = payload.position();

                    context.queue(() -> {
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.chest.client.TutorialChestRenderer"
                            );
                            rendererClass.getMethod("removeChestPosition", BlockPos.class)
                                    .invoke(null, pos);
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to remove chest position on client", e);
                        }
                    });
                }
        );

        // Client receives clear all chests
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                ChestClearPayload.TYPE,
                ChestClearPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.chest.client.TutorialChestRenderer"
                            );
                            rendererClass.getMethod("clearChestPositions").invoke(null);
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to clear chest positions on client", e);
                        }
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial chest networking initialized");
    }

    /**
     * Sends all unconsumed chest positions to a player.
     *
     * @param player the player to send to
     * @param chests list of unconsumed chests
     */
    public static void sendChestsToPlayer(ServerPlayer player, List<TutorialChest> chests) {
        BlockPos[] positions = new BlockPos[chests.size()];
        for (int i = 0; i < chests.size(); i++) {
            positions[i] = chests.get(i).getPosition();
        }
        NetworkManager.sendToPlayer(player, new ChestSyncPayload(positions));
    }

    /**
     * Notifies a player that a chest was consumed.
     *
     * @param player   the player to send to
     * @param position the chest position
     */
    public static void sendChestConsumed(ServerPlayer player, BlockPos position) {
        NetworkManager.sendToPlayer(player, new ChestConsumedPayload(position));
    }

    /**
     * Clears all chest positions on a player's client.
     *
     * @param player the player to send to
     */
    public static void sendClearToPlayer(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new ChestClearPayload());
    }

    /**
     * Syncs all unconsumed chests from the manager to a player.
     * <p>
     * This is useful after a reset to restore chest particles.
     *
     * @param player the player to sync to
     */
    public static void syncAllChestsToPlayer(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }

        List<TutorialChest> unconsumedChests =
                com.ovrtechnology.tutorial.chest.TutorialChestManager.getUnconsumedChests(level);

        sendChestsToPlayer(player, unconsumedChests);
    }
}
