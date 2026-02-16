package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.history.BlacklistEntry;
import com.ovrtechnology.history.HistoryEntry;
import com.ovrtechnology.history.TrackingHistoryData;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.trigger.client.PathTrackingMaskOverlay;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles server-to-client networking for path tracking scent triggers.
 *
 * <p>Since the path tracking runs on the server but the OVR WebSocket client
 * runs on the client, we need to send scent trigger commands from server to client.</p>
 */
public final class PathScentNetworking {

    private static final ResourceLocation PATH_SCENT_TRIGGER_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_scent_trigger");

    private static final ResourceLocation PATH_DISTANCE_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_distance");

    private static final ResourceLocation PATH_STATUS_FOUND_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_status_found");

    private static final ResourceLocation PATH_STATUS_NOT_FOUND_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_status_not_found");

    private static final ResourceLocation PATH_STATUS_ARRIVED_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_status_arrived");

    private static final ResourceLocation PATH_BLACKLIST_SYNC_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_blacklist_sync");

    /**
     * Default duration for path tracking scent triggers (in ticks).
     * 5 seconds = 100 ticks.
     */
    private static final int PATH_SCENT_DURATION_TICKS = 100;

    private static boolean initialized = false;

    private PathScentNetworking() {
    }

    /**
     * Initializes the networking handler.
     * Must be called on both client and server during mod initialization.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register client-side receiver for scent trigger packets (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PATH_SCENT_TRIGGER_PACKET_ID, (buf, context) -> {
            String scentName = buf.readUtf();
            double intensity = buf.readDouble();
            int priorityOrdinal = buf.readVarInt();
            int durationTicks = buf.readVarInt();

            context.queue(() -> {
                ScentPriority priority = ScentPriority.values()[priorityOrdinal];

                ScentTrigger trigger = ScentTrigger.create(
                        scentName,
                        ScentTriggerSource.PATH_TRACKING,
                        priority,
                        durationTicks,
                        intensity
                );

                boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);
                if (triggered) {
                    PathTrackingMaskOverlay.onPathScentPuff(scentName, intensity);
                }
                AromaAffect.LOGGER.debug("Received path scent trigger from server: {} (triggered: {})",
                        scentName, triggered);
            });
        });

        // Register client-side receiver for distance update packets (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PATH_DISTANCE_PACKET_ID, (buf, context) -> {
            int distance = buf.readVarInt();

            context.queue(() -> {
                ActiveTrackingState.setDistance(distance);
            });
        });

        // Register client-side receiver for path found status (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PATH_STATUS_FOUND_PACKET_ID, (buf, context) -> {
            int distance = buf.readVarInt();
            BlockPos destination = buf.readBlockPos();

            context.queue(() -> {
                AromaAffect.LOGGER.debug("Path status: found (distance: {}, destination: {})", distance, destination);

                ActiveTrackingState.setTracking(distance, destination);

                // Capture to tracking history
                if (ActiveTrackingState.getTargetId() != null && ActiveTrackingState.getCategory() != null) {
                    String dimension = Minecraft.getInstance().level != null
                            ? Minecraft.getInstance().level.dimension().location().toString()
                            : "minecraft:overworld";
                    TrackingHistoryData.getInstance().addHistoryEntry(new HistoryEntry(
                            ActiveTrackingState.getTargetId().toString(),
                            ActiveTrackingState.getDisplayName() != null
                                    ? ActiveTrackingState.getDisplayName().getString() : "",
                            ActiveTrackingState.getCategory().getId(),
                            destination.getX(), destination.getY(), destination.getZ(),
                            dimension,
                            System.currentTimeMillis()
                    ));
                }
            });
        });

        // Register client-side receiver for path not found status (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PATH_STATUS_NOT_FOUND_PACKET_ID, (buf, context) -> {
            String reason = buf.readUtf();

            context.queue(() -> {
                ActiveTrackingState.setFailed(reason, false);
                AromaAffect.LOGGER.debug("Path status: not found (reason: {})", reason);
            });
        });

        // Register client-side receiver for arrived status (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PATH_STATUS_ARRIVED_PACKET_ID, (buf, context) -> {
            context.queue(() -> {
                ActiveTrackingState.setArrived();
                AromaAffect.LOGGER.debug("Path status: arrived");
            });
        });

        // Register server-side receiver for blacklist sync (C2S)
        // Processed immediately (not deferred via context.queue) so the blacklist
        // is guaranteed to be updated before the path command that follows.
        // BlacklistSyncManager uses ConcurrentHashMap — safe from any thread.
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PATH_BLACKLIST_SYNC_PACKET_ID, (buf, context) -> {
            int count = buf.readVarInt();
            List<BlacklistSyncManager.ExcludedPosition> positions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String targetId = buf.readUtf();
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                positions.add(new BlacklistSyncManager.ExcludedPosition(targetId, x, y, z));
            }

            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                BlacklistSyncManager.getInstance().setExclusions(serverPlayer.getUUID(), positions);
                AromaAffect.LOGGER.debug("Received blacklist sync from {}: {} entries",
                        serverPlayer.getName().getString(), positions.size());
            }
        });

        AromaAffect.LOGGER.info("PathScentNetworking initialized");
    }

    /**
     * Sends a scent trigger from the server to a specific client.
     *
     * @param player    the player to send the trigger to
     * @param scentName the scent name to trigger
     * @param intensity the scent intensity (0.0 to 1.0)
     * @param priority  the scent priority
     */
    public static void sendScentTrigger(ServerPlayer player, String scentName, double intensity, ScentPriority priority) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeUtf(scentName);
        buf.writeDouble(intensity);
        buf.writeVarInt(priority.ordinal());
        buf.writeVarInt(PATH_SCENT_DURATION_TICKS);

        if (!NetworkManager.canPlayerReceive(player, PATH_SCENT_TRIGGER_PACKET_ID)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, PATH_SCENT_TRIGGER_PACKET_ID, buf);

        AromaAffect.LOGGER.debug("Sent path scent trigger to {}: {} (intensity: {}, priority: {})",
                player.getName().getString(), scentName, intensity, priority);
    }

    /**
     * Sends the current distance to the tracked target from server to client.
     *
     * @param player   the player to send the distance to
     * @param distance the distance in blocks
     */
    public static void sendDistanceUpdate(ServerPlayer player, int distance) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeVarInt(distance);

        if (!NetworkManager.canPlayerReceive(player, PATH_DISTANCE_PACKET_ID)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, PATH_DISTANCE_PACKET_ID, buf);
    }

    /**
     * Sends a "path found" status to the client when a path is successfully created.
     *
     * @param player   the player to notify
     * @param distance initial distance in blocks
     */
    public static void sendPathFound(ServerPlayer player, int distance, BlockPos destination) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeVarInt(distance);
        buf.writeBlockPos(destination);

        if (!NetworkManager.canPlayerReceive(player, PATH_STATUS_FOUND_PACKET_ID)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, PATH_STATUS_FOUND_PACKET_ID, buf);
    }

    /**
     * Sends a "path not found" status to the client when a search fails.
     *
     * @param player the player to notify
     * @param reason human-readable failure reason
     */
    public static void sendPathNotFound(ServerPlayer player, String reason) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeUtf(reason);

        if (!NetworkManager.canPlayerReceive(player, PATH_STATUS_NOT_FOUND_PACKET_ID)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, PATH_STATUS_NOT_FOUND_PACKET_ID, buf);
    }

    /**
     * Sends an "arrived" status to the client when the player reaches the destination.
     *
     * @param player the player to notify
     */
    public static void sendPathArrived(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        if (!NetworkManager.canPlayerReceive(player, PATH_STATUS_ARRIVED_PACKET_ID)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, PATH_STATUS_ARRIVED_PACKET_ID, buf);
    }

    /**
     * Sends the client's blacklist to the server before a path command.
     * Call this from the client before sending any path tracking command.
     *
     * @param registryAccess the client's registry access
     */
    public static void sendBlacklistSync(RegistryAccess registryAccess) {
        List<BlacklistEntry> blacklist = TrackingHistoryData.getInstance().getBlacklist();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registryAccess);

        buf.writeVarInt(blacklist.size());
        for (BlacklistEntry entry : blacklist) {
            buf.writeUtf(entry.targetId);
            buf.writeInt(entry.x);
            buf.writeInt(entry.y);
            buf.writeInt(entry.z);
        }

        NetworkManager.sendToServer(PATH_BLACKLIST_SYNC_PACKET_ID, buf);
        AromaAffect.LOGGER.debug("Sent blacklist sync to server: {} entries", blacklist.size());
    }
}
