package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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

            context.queue(() -> {
                ActiveTrackingState.setTracking(distance);
                AromaAffect.LOGGER.debug("Path status: found (distance: {})", distance);
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

        NetworkManager.sendToPlayer(player, PATH_DISTANCE_PACKET_ID, buf);
    }

    /**
     * Sends a "path found" status to the client when a path is successfully created.
     *
     * @param player   the player to notify
     * @param distance initial distance in blocks
     */
    public static void sendPathFound(ServerPlayer player, int distance) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeVarInt(distance);

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

        NetworkManager.sendToPlayer(player, PATH_STATUS_ARRIVED_PACKET_ID, buf);
    }
}
