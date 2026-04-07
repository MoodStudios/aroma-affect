package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.history.BlacklistEntry;
import com.ovrtechnology.history.HistoryEntry;
import com.ovrtechnology.history.TrackingHistoryData;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.trigger.client.PathTrackingMaskOverlay;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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

    // ── Payload records ────────────────────────────────────────────────

    public record PathScentTriggerS2C(String scentName, double intensity, int priorityOrdinal, int durationTicks) implements CustomPacketPayload {
        public static final Type<PathScentTriggerS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_scent_trigger"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathScentTriggerS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.scentName);
                    buf.writeDouble(payload.intensity);
                    buf.writeVarInt(payload.priorityOrdinal);
                    buf.writeVarInt(payload.durationTicks);
                },
                buf -> new PathScentTriggerS2C(buf.readUtf(), buf.readDouble(), buf.readVarInt(), buf.readVarInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PathDistanceS2C(int distance) implements CustomPacketPayload {
        public static final Type<PathDistanceS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_distance"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathDistanceS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.distance),
                buf -> new PathDistanceS2C(buf.readVarInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PathStatusFoundS2C(int distance, BlockPos destination) implements CustomPacketPayload {
        public static final Type<PathStatusFoundS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_status_found"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathStatusFoundS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.distance);
                    buf.writeBlockPos(payload.destination);
                },
                buf -> new PathStatusFoundS2C(buf.readVarInt(), buf.readBlockPos())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PathStatusNotFoundS2C(String reason) implements CustomPacketPayload {
        public static final Type<PathStatusNotFoundS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_status_not_found"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathStatusNotFoundS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.reason),
                buf -> new PathStatusNotFoundS2C(buf.readUtf())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PathStatusArrivedS2C() implements CustomPacketPayload {
        public static final Type<PathStatusArrivedS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_status_arrived"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathStatusArrivedS2C> STREAM_CODEC =
                StreamCodec.unit(new PathStatusArrivedS2C());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StructureSyncS2C(String structureId) implements CustomPacketPayload {
        public static final Type<StructureSyncS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "structure_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StructureSyncS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.structureId != null);
                    if (payload.structureId != null) buf.writeUtf(payload.structureId);
                },
                buf -> {
                    boolean hasStructure = buf.readBoolean();
                    return new StructureSyncS2C(hasStructure ? buf.readUtf() : null);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PathBlacklistSyncC2S(List<BlacklistSyncManager.ExcludedPosition> positions) implements CustomPacketPayload {
        public static final Type<PathBlacklistSyncC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_blacklist_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathBlacklistSyncC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.positions.size());
                    for (BlacklistSyncManager.ExcludedPosition pos : payload.positions) {
                        buf.writeUtf(pos.targetId());
                        buf.writeInt(pos.x());
                        buf.writeInt(pos.y());
                        buf.writeInt(pos.z());
                    }
                },
                buf -> {
                    int count = buf.readVarInt();
                    List<BlacklistSyncManager.ExcludedPosition> positions = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        String targetId = buf.readUtf();
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        positions.add(new BlacklistSyncManager.ExcludedPosition(targetId, x, y, z));
                    }
                    return new PathBlacklistSyncC2S(positions);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Constants ──────────────────────────────────────────────────────

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
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PathScentTriggerS2C.TYPE, PathScentTriggerS2C.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                ScentPriority priority = ScentPriority.values()[payload.priorityOrdinal()];

                ScentTrigger trigger = ScentTrigger.create(
                        payload.scentName(),
                        ScentTriggerSource.PATH_TRACKING,
                        priority,
                        payload.durationTicks(),
                        payload.intensity()
                );

                boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);
                if (triggered) {
                    PathTrackingMaskOverlay.onPathScentPuff(payload.scentName(), payload.intensity());
                }
                AromaAffect.LOGGER.debug("Received path scent trigger from server: {} (triggered: {})",
                        payload.scentName(), triggered);
            });
        });

        // Register client-side receiver for distance update packets (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PathDistanceS2C.TYPE, PathDistanceS2C.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                ActiveTrackingState.setDistance(payload.distance());
            });
        });

        // Register client-side receiver for path found status (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PathStatusFoundS2C.TYPE, PathStatusFoundS2C.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                AromaAffect.LOGGER.debug("Path status: found (distance: {}, destination: {})", payload.distance(), payload.destination());

                ActiveTrackingState.setTracking(payload.distance(), payload.destination());

                // Capture to tracking history
                if (ActiveTrackingState.getTargetId() != null && ActiveTrackingState.getCategory() != null) {
                    String dimension = Minecraft.getInstance().level != null
                            ? Minecraft.getInstance().level.dimension().identifier().toString()
                            : "minecraft:overworld";
                    TrackingHistoryData.getInstance().addHistoryEntry(new HistoryEntry(
                            ActiveTrackingState.getTargetId().toString(),
                            ActiveTrackingState.getDisplayName() != null
                                    ? ActiveTrackingState.getDisplayName().getString() : "",
                            ActiveTrackingState.getCategory().getId(),
                            payload.destination().getX(), payload.destination().getY(), payload.destination().getZ(),
                            dimension,
                            System.currentTimeMillis()
                    ));
                }
            });
        });

        // Register client-side receiver for path not found status (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PathStatusNotFoundS2C.TYPE, PathStatusNotFoundS2C.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                ActiveTrackingState.setFailed(payload.reason(), false);
                AromaAffect.LOGGER.debug("Path status: not found (reason: {})", payload.reason());
            });
        });

        // Register client-side receiver for arrived status (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PathStatusArrivedS2C.TYPE, PathStatusArrivedS2C.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                ActiveTrackingState.setArrived();
                AromaAffect.LOGGER.debug("Path status: arrived");
            });
        });

        // Register client-side receiver for structure sync packets (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StructureSyncS2C.TYPE, StructureSyncS2C.STREAM_CODEC, (payload, context) -> {
            context.queue(() -> {
                PassiveModeManager.setServerStructureId(payload.structureId());
                AromaAffect.LOGGER.debug("Received structure sync from server: {}", payload.structureId());
            });
        });

        // Register server-side receiver for blacklist sync (C2S)
        // Processed immediately (not deferred via context.queue) so the blacklist
        // is guaranteed to be updated before the path command that follows.
        // BlacklistSyncManager uses ConcurrentHashMap — safe from any thread.
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PathBlacklistSyncC2S.TYPE, PathBlacklistSyncC2S.STREAM_CODEC, (payload, context) -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                BlacklistSyncManager.getInstance().setExclusions(serverPlayer.getUUID(), payload.positions());
                AromaAffect.LOGGER.debug("Received blacklist sync from {}: {} entries",
                        serverPlayer.getName().getString(), payload.positions().size());
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
        if (!NetworkManager.canPlayerReceive(player, PathScentTriggerS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathScentTriggerS2C(scentName, intensity, priority.ordinal(), PATH_SCENT_DURATION_TICKS));

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
        if (!NetworkManager.canPlayerReceive(player, PathDistanceS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathDistanceS2C(distance));
    }

    /**
     * Sends a "path found" status to the client when a path is successfully created.
     *
     * @param player   the player to notify
     * @param distance initial distance in blocks
     */
    public static void sendPathFound(ServerPlayer player, int distance, BlockPos destination) {
        if (!NetworkManager.canPlayerReceive(player, PathStatusFoundS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathStatusFoundS2C(distance, destination));
    }

    /**
     * Sends a "path not found" status to the client when a search fails.
     *
     * @param player the player to notify
     * @param reason human-readable failure reason
     */
    public static void sendPathNotFound(ServerPlayer player, String reason) {
        if (!NetworkManager.canPlayerReceive(player, PathStatusNotFoundS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathStatusNotFoundS2C(reason));
    }

    /**
     * Sends an "arrived" status to the client when the player reaches the destination.
     *
     * @param player the player to notify
     */
    public static void sendPathArrived(ServerPlayer player) {
        if (!NetworkManager.canPlayerReceive(player, PathStatusArrivedS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathStatusArrivedS2C());
    }

    /**
     * Sends the current structure the player is inside (or null) from the server to the client.
     * Used by {@link com.ovrtechnology.trigger.StructureSyncHandler} to keep passive-mode
     * structure triggers working in multiplayer.
     *
     * @param player      the player to send the sync to
     * @param structureId the structure ID the player is inside, or null if none
     */
    public static void sendStructureSync(ServerPlayer player, String structureId) {
        if (!NetworkManager.canPlayerReceive(player, StructureSyncS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new StructureSyncS2C(structureId));
    }

    /**
     * Sends the client's blacklist to the server before a path command.
     * Call this from the client before sending any path tracking command.
     *
     * @param registryAccess the client's registry access
     */
    public static void sendBlacklistSync(RegistryAccess registryAccess) {
        List<BlacklistEntry> blacklist = TrackingHistoryData.getInstance().getBlacklist();
        List<BlacklistSyncManager.ExcludedPosition> positions = new ArrayList<>();
        for (BlacklistEntry entry : blacklist) {
            positions.add(new BlacklistSyncManager.ExcludedPosition(entry.targetId, entry.x, entry.y, entry.z));
        }

        NetworkManager.sendToServer(new PathBlacklistSyncC2S(positions));
        AromaAffect.LOGGER.debug("Sent blacklist sync to server: {} entries", blacklist.size());
    }
}
