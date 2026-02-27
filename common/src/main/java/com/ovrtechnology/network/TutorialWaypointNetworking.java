package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Networking for tutorial waypoint synchronization.
 * <p>
 * Syncs active waypoints (multi-point paths) from server to client for visual rendering.
 */
public final class TutorialWaypointNetworking {

    private static final ResourceLocation WAYPOINT_SYNC_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_waypoint_sync");

    private static final ResourceLocation WAYPOINT_CLEAR_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_waypoint_clear");

    private static boolean initialized = false;

    private TutorialWaypointNetworking() {
    }

    /**
     * Initializes networking receivers on the client.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Client receives waypoint sync with multiple positions
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                WAYPOINT_SYNC_PACKET_ID,
                (buf, context) -> {
                    String waypointId = buf.readUtf();
                    int positionCount = buf.readVarInt();

                    List<BlockPos> positions = new ArrayList<>(positionCount);
                    for (int i = 0; i < positionCount; i++) {
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        positions.add(new BlockPos(x, y, z));
                    }

                    context.queue(() -> {
                        // Call client-side handler via reflection
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.waypoint.client.TutorialWaypointRenderer"
                            );
                            rendererClass.getMethod("setActiveWaypoint", String.class, List.class)
                                    .invoke(null, waypointId, positions);
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to set waypoint on client", e);
                        }
                    });
                }
        );

        // Client receives waypoint clear
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                WAYPOINT_CLEAR_PACKET_ID,
                (buf, context) -> {
                    context.queue(() -> {
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.waypoint.client.TutorialWaypointRenderer"
                            );
                            rendererClass.getMethod("clearActiveWaypoint").invoke(null);
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to clear waypoint on client", e);
                        }
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial waypoint networking initialized");
    }

    /**
     * Sends a multi-point waypoint to a player.
     *
     * @param player     the player to send to
     * @param waypointId the waypoint ID
     * @param positions  list of positions in order (1, 2, 3...)
     */
    public static void sendWaypointToPlayer(ServerPlayer player, String waypointId, List<BlockPos> positions) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        buf.writeUtf(waypointId);
        buf.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }
        NetworkManager.sendToPlayer(player, WAYPOINT_SYNC_PACKET_ID, buf);
    }

    /**
     * Legacy method for 2-point waypoints.
     */
    @Deprecated
    public static void sendWaypointToPlayer(ServerPlayer player, String waypointId, BlockPos posA, BlockPos posB) {
        sendWaypointToPlayer(player, waypointId, List.of(posA, posB));
    }

    /**
     * Sends a waypoint clear to a player.
     *
     * @param player the player to send to
     */
    public static void sendClearToPlayer(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        NetworkManager.sendToPlayer(player, WAYPOINT_CLEAR_PACKET_ID, buf);
    }
}
