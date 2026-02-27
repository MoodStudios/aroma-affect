package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.chest.TutorialChest;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Networking for tutorial chest synchronization.
 * <p>
 * Syncs chest positions to clients for particle rendering.
 */
public final class TutorialChestNetworking {

    private static final ResourceLocation CHEST_SYNC_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_chest_sync");

    private static final ResourceLocation CHEST_CONSUMED_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_chest_consumed");

    private static final ResourceLocation CHEST_CLEAR_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_chest_clear");

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
                CHEST_SYNC_PACKET_ID,
                (buf, context) -> {
                    int chestCount = buf.readVarInt();
                    BlockPos[] positions = new BlockPos[chestCount];

                    for (int i = 0; i < chestCount; i++) {
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        positions[i] = new BlockPos(x, y, z);
                    }

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
                CHEST_CONSUMED_PACKET_ID,
                (buf, context) -> {
                    int x = buf.readInt();
                    int y = buf.readInt();
                    int z = buf.readInt();
                    BlockPos pos = new BlockPos(x, y, z);

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
                CHEST_CLEAR_PACKET_ID,
                (buf, context) -> {
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
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        buf.writeVarInt(chests.size());
        for (TutorialChest chest : chests) {
            BlockPos pos = chest.getPosition();
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }
        NetworkManager.sendToPlayer(player, CHEST_SYNC_PACKET_ID, buf);
    }

    /**
     * Notifies a player that a chest was consumed.
     *
     * @param player   the player to send to
     * @param position the chest position
     */
    public static void sendChestConsumed(ServerPlayer player, BlockPos position) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        buf.writeInt(position.getX());
        buf.writeInt(position.getY());
        buf.writeInt(position.getZ());
        NetworkManager.sendToPlayer(player, CHEST_CONSUMED_PACKET_ID, buf);
    }

    /**
     * Clears all chest positions on a player's client.
     *
     * @param player the player to send to
     */
    public static void sendClearToPlayer(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        NetworkManager.sendToPlayer(player, CHEST_CLEAR_PACKET_ID, buf);
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
