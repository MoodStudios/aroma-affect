package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

    public record WaypointSyncS2C(String waypointId, List<BlockPos> positions) implements CustomPacketPayload {
        public static final Type<WaypointSyncS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_waypoint_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WaypointSyncS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.waypointId);
                    buf.writeVarInt(payload.positions.size());
                    for (BlockPos pos : payload.positions) {
                        buf.writeInt(pos.getX());
                        buf.writeInt(pos.getY());
                        buf.writeInt(pos.getZ());
                    }
                },
                buf -> {
                    String waypointId = buf.readUtf();
                    int positionCount = buf.readVarInt();
                    List<BlockPos> positions = new ArrayList<>(positionCount);
                    for (int i = 0; i < positionCount; i++) {
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        positions.add(new BlockPos(x, y, z));
                    }
                    return new WaypointSyncS2C(waypointId, positions);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record WaypointClearS2C() implements CustomPacketPayload {
        public static final Type<WaypointClearS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_waypoint_clear"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WaypointClearS2C> STREAM_CODEC =
                StreamCodec.unit(new WaypointClearS2C());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

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
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, WaypointSyncS2C.TYPE, WaypointSyncS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        // Call client-side handler via reflection
                        try {
                            Class<?> rendererClass = Class.forName(
                                    "com.ovrtechnology.tutorial.waypoint.client.TutorialWaypointRenderer"
                            );
                            rendererClass.getMethod("setActiveWaypoint", String.class, List.class)
                                    .invoke(null, payload.waypointId(), payload.positions());
                        } catch (ReflectiveOperationException e) {
                            AromaAffect.LOGGER.debug("Failed to set waypoint on client", e);
                        }
                    });
                }
        );

        // Client receives waypoint clear
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, WaypointClearS2C.TYPE, WaypointClearS2C.STREAM_CODEC,
                (payload, context) -> {
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
        NetworkManager.sendToPlayer(player, new WaypointSyncS2C(waypointId, positions));
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
        NetworkManager.sendToPlayer(player, new WaypointClearS2C());
    }
}
