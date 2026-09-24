package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.sub.PathSubCommand;
import com.ovrtechnology.history.BlacklistEntry;
import com.ovrtechnology.history.HistoryEntry;
import com.ovrtechnology.history.TrackingHistoryData;
import com.ovrtechnology.lookup.LookupType;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.trigger.client.PathTrackingMaskOverlay;
import com.ovrtechnology.tracking.RespawnSyncState;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import com.ovrtechnology.util.SoundRef;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
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
 * <p>Note on threading: under Architectury the {@code PathBlacklistSyncC2S}
 * receiver ran on the netty thread to guarantee the blacklist was updated
 * before the path request that followed. Under Balm every C2S receiver runs on
 * the main server thread in arrival order, so sending the blacklist right before
 * {@code PathTrackC2S} keeps them ordered.</p>
 */
public final class PathScentNetworking {

    // ── Payload records ────────────────────────────────────────────────

    public record PathScentTriggerS2C(String scentName, String perception, double intensity, int priorityOrdinal, int durationTicks) implements CustomPacketPayload {
        public static final Type<PathScentTriggerS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_scent_trigger"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathScentTriggerS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.scentName);
                    buf.writeUtf(payload.perception);
                    buf.writeDouble(payload.intensity);
                    buf.writeVarInt(payload.priorityOrdinal);
                    buf.writeVarInt(payload.durationTicks);
                },
                buf -> new PathScentTriggerS2C(buf.readUtf(), buf.readUtf(), buf.readDouble(), buf.readVarInt(), buf.readVarInt())
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

    public record RespawnSyncS2C(boolean present, String dimension, BlockPos pos, String blockId) implements CustomPacketPayload {
        public static final Type<RespawnSyncS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "respawn_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RespawnSyncS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.present);
                    if (payload.present) {
                        buf.writeUtf(payload.dimension);
                        buf.writeBlockPos(payload.pos);
                        buf.writeUtf(payload.blockId);
                    }
                },
                buf -> {
                    boolean present = buf.readBoolean();
                    if (!present) return new RespawnSyncS2C(false, null, null, null);
                    return new RespawnSyncS2C(true, buf.readUtf(), buf.readBlockPos(), buf.readUtf());
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

    public record PathTrackC2S(String lookupType, Identifier targetId) implements CustomPacketPayload {
        public static final Type<PathTrackC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_track"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathTrackC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.lookupType, 16);
                    Identifier.STREAM_CODEC.encode(buf, payload.targetId);
                },
                buf -> new PathTrackC2S(buf.readUtf(16), Identifier.STREAM_CODEC.decode(buf))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PathRecallC2S(String lookupType, Identifier targetId, BlockPos destination, Identifier dimension)
            implements CustomPacketPayload {
        public static final Type<PathRecallC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_recall"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathRecallC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.lookupType, 16);
                    Identifier.STREAM_CODEC.encode(buf, payload.targetId);
                    buf.writeBlockPos(payload.destination);
                    Identifier.STREAM_CODEC.encode(buf, payload.dimension);
                },
                buf -> new PathRecallC2S(buf.readUtf(16), Identifier.STREAM_CODEC.decode(buf),
                        buf.readBlockPos(), Identifier.STREAM_CODEC.decode(buf))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Default duration for path tracking scent triggers (in ticks). 5 seconds = 100 ticks. */
    private static final int PATH_SCENT_DURATION_TICKS = 100;

    private static boolean initialized = false;

    private PathScentNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        Balm.networking().registerClientboundPacket(
                PathScentTriggerS2C.TYPE,
                PathScentTriggerS2C.class,
                PathScentTriggerS2C.STREAM_CODEC,
                (player, payload) -> {
                    ScentPriority priority = ScentPriority.values()[payload.priorityOrdinal()];

                    ScentTrigger trigger = ScentTrigger.create(
                            payload.scentName(),
                            payload.perception(),
                            null,
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

        Balm.networking().registerClientboundPacket(
                PathDistanceS2C.TYPE,
                PathDistanceS2C.class,
                PathDistanceS2C.STREAM_CODEC,
                (player, payload) -> ActiveTrackingState.setDistance(payload.distance()));

        Balm.networking().registerClientboundPacket(
                PathStatusFoundS2C.TYPE,
                PathStatusFoundS2C.class,
                PathStatusFoundS2C.STREAM_CODEC,
                (player, payload) -> {
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

        Balm.networking().registerClientboundPacket(
                PathStatusNotFoundS2C.TYPE,
                PathStatusNotFoundS2C.class,
                PathStatusNotFoundS2C.STREAM_CODEC,
                (player, payload) -> {
                    ActiveTrackingState.setFailed(payload.reason(), false);
                    AromaAffect.LOGGER.debug("Path status: not found (reason: {})", payload.reason());
                });

        Balm.networking().registerClientboundPacket(
                PathStatusArrivedS2C.TYPE,
                PathStatusArrivedS2C.class,
                PathStatusArrivedS2C.STREAM_CODEC,
                (player, payload) -> {
                    ActiveTrackingState.setArrived();
                    AromaAffect.LOGGER.debug("Path status: arrived");
                });

        Balm.networking().registerClientboundPacket(
                StructureSyncS2C.TYPE,
                StructureSyncS2C.class,
                StructureSyncS2C.STREAM_CODEC,
                (player, payload) -> {
                    PassiveModeManager.setServerStructureId(payload.structureId());
                    AromaAffect.LOGGER.debug("Received structure sync from server: {}", payload.structureId());
                });

        Balm.networking().registerClientboundPacket(
                RespawnSyncS2C.TYPE,
                RespawnSyncS2C.class,
                RespawnSyncS2C.STREAM_CODEC,
                (player, payload) -> {
                    if (payload.present()) {
                        RespawnSyncState.update(payload.dimension(), payload.pos(), payload.blockId());
                    } else {
                        RespawnSyncState.clear();
                    }
                    AromaAffect.LOGGER.debug("Received respawn sync from server: {} at {}", payload.blockId(), payload.pos());
                });

        // Server-side receiver for blacklist sync. Under Balm this runs on the main
        // server thread; BlacklistSyncManager uses ConcurrentHashMap so the update
        // is still safe and ordering is preserved because C2S packets are handled
        // in arrival order, before the PathTrackC2S that follows.
        Balm.networking().registerServerboundPacket(
                PathBlacklistSyncC2S.TYPE,
                PathBlacklistSyncC2S.class,
                PathBlacklistSyncC2S.STREAM_CODEC,
                (serverPlayer, payload) -> {
                    BlacklistSyncManager.getInstance().setExclusions(serverPlayer.getUUID(), payload.positions());
                    AromaAffect.LOGGER.debug("Received blacklist sync from {}: {} entries",
                            serverPlayer.getName().getString(), payload.positions().size());
                });

        // Menu tracking goes through packets instead of /aromatest, which is OP-only
        Balm.networking().registerServerboundPacket(
                PathTrackC2S.TYPE,
                PathTrackC2S.class,
                PathTrackC2S.STREAM_CODEC,
                (serverPlayer, payload) -> PathSubCommand.trackFromMenu(
                        serverPlayer, LookupType.fromId(payload.lookupType()), payload.targetId()));

        Balm.networking().registerServerboundPacket(
                PathRecallC2S.TYPE,
                PathRecallC2S.class,
                PathRecallC2S.STREAM_CODEC,
                (serverPlayer, payload) -> PathSubCommand.recallFromMenu(
                        serverPlayer, LookupType.fromId(payload.lookupType()), payload.targetId(),
                        payload.destination(), payload.dimension()));

        ServerPlayerCallback.Leave.EVENT.register(player -> PathSubCommand.removePlayer(player.getUUID()));

        AromaAffect.LOGGER.info("PathScentNetworking initialized");
    }

    public static void sendScentTrigger(ServerPlayer player, String scentName, String perception, double intensity, ScentPriority priority) {
        Balm.networking().sendTo(player, new PathScentTriggerS2C(scentName, perception, intensity, priority.ordinal(), PATH_SCENT_DURATION_TICKS));
        AromaAffect.LOGGER.debug("Sent path scent trigger to {}: {} (intensity: {}, priority: {})",
                player.getName().getString(), scentName, intensity, priority);
    }

    public static void sendDistanceUpdate(ServerPlayer player, int distance) {
        Balm.networking().sendTo(player, new PathDistanceS2C(distance));
    }

    public static void sendPathFound(ServerPlayer player, int distance, BlockPos destination) {
        Balm.networking().sendTo(player, new PathStatusFoundS2C(distance, destination));
    }

    public static void sendPathNotFound(ServerPlayer player, String reason) {
        Balm.networking().sendTo(player, new PathStatusNotFoundS2C(reason));
    }

    public static void sendPathArrived(ServerPlayer player) {
        Balm.networking().sendTo(player, new PathStatusArrivedS2C());
    }

    public static void sendStructureSync(ServerPlayer player, String structureId) {
        Balm.networking().sendTo(player, new StructureSyncS2C(structureId));
    }

    public static void sendRespawnSync(ServerPlayer player, String dimension, BlockPos pos, String blockId) {
        Balm.networking().sendTo(player, new RespawnSyncS2C(true, dimension, pos, blockId));
    }

    public static void sendRespawnCleared(ServerPlayer player) {
        Balm.networking().sendTo(player, new RespawnSyncS2C(false, null, null, null));
    }

    public static void sendBlacklistSync(RegistryAccess registryAccess) {
        List<BlacklistEntry> blacklist = TrackingHistoryData.getInstance().getBlacklist();
        List<BlacklistSyncManager.ExcludedPosition> positions = new ArrayList<>();
        for (BlacklistEntry entry : blacklist) {
            positions.add(new BlacklistSyncManager.ExcludedPosition(entry.targetId, entry.x, entry.y, entry.z));
        }

        Balm.networking().sendToServer(new PathBlacklistSyncC2S(positions));
        AromaAffect.LOGGER.debug("Sent blacklist sync to server: {} entries", blacklist.size());
    }

    public static void sendTrackRequest(LookupType type, Identifier targetId) {
        Balm.networking().sendToServer(new PathTrackC2S(type.getId(), targetId));
    }

    public static void sendRecallRequest(LookupType type, Identifier targetId, BlockPos destination, Identifier dimension) {
        Balm.networking().sendToServer(new PathRecallC2S(type.getId(), targetId, destination, dimension));
    }
}
