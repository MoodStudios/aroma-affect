package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.history.BlacklistEntry;
import com.ovrtechnology.history.HistoryEntry;
import com.ovrtechnology.history.TrackingHistoryData;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import com.ovrtechnology.trigger.client.PathTrackingMaskOverlay;
import com.ovrtechnology.util.Ids;
import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PathScentNetworking {

    public record PathScentTriggerS2C(
            String scentName, double intensity, int priorityOrdinal, int durationTicks)
            implements CustomPacketPayload {
        public static final Type<PathScentTriggerS2C> TYPE =
                new Type<>(Ids.mod("path_scent_trigger"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathScentTriggerS2C> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeUtf(payload.scentName);
                            buf.writeDouble(payload.intensity);
                            buf.writeVarInt(payload.priorityOrdinal);
                            buf.writeVarInt(payload.durationTicks);
                        },
                        buf ->
                                new PathScentTriggerS2C(
                                        buf.readUtf(),
                                        buf.readDouble(),
                                        buf.readVarInt(),
                                        buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PathDistanceS2C(int distance) implements CustomPacketPayload {
        public static final Type<PathDistanceS2C> TYPE = new Type<>(Ids.mod("path_distance"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathDistanceS2C> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeVarInt(payload.distance),
                        buf -> new PathDistanceS2C(buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PathStatusFoundS2C(int distance, BlockPos destination)
            implements CustomPacketPayload {
        public static final Type<PathStatusFoundS2C> TYPE =
                new Type<>(Ids.mod("path_status_found"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathStatusFoundS2C> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.distance);
                            buf.writeBlockPos(payload.destination);
                        },
                        buf -> new PathStatusFoundS2C(buf.readVarInt(), buf.readBlockPos()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PathStatusNotFoundS2C(String reason) implements CustomPacketPayload {
        public static final Type<PathStatusNotFoundS2C> TYPE =
                new Type<>(Ids.mod("path_status_not_found"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathStatusNotFoundS2C>
                STREAM_CODEC =
                        StreamCodec.of(
                                (buf, payload) -> buf.writeUtf(payload.reason),
                                buf -> new PathStatusNotFoundS2C(buf.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PathStatusArrivedS2C() implements CustomPacketPayload {
        public static final Type<PathStatusArrivedS2C> TYPE =
                new Type<>(Ids.mod("path_status_arrived"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathStatusArrivedS2C>
                STREAM_CODEC = StreamCodec.unit(new PathStatusArrivedS2C());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StructureSyncS2C(String structureId) implements CustomPacketPayload {
        public static final Type<StructureSyncS2C> TYPE = new Type<>(Ids.mod("structure_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StructureSyncS2C> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeBoolean(payload.structureId != null);
                            if (payload.structureId != null) buf.writeUtf(payload.structureId);
                        },
                        buf -> {
                            boolean hasStructure = buf.readBoolean();
                            return new StructureSyncS2C(hasStructure ? buf.readUtf() : null);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PathBlacklistSyncC2S(List<BlacklistSyncManager.ExcludedPosition> positions)
            implements CustomPacketPayload {
        public static final Type<PathBlacklistSyncC2S> TYPE =
                new Type<>(Ids.mod("path_blacklist_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathBlacklistSyncC2S>
                STREAM_CODEC =
                        StreamCodec.of(
                                (buf, payload) -> {
                                    buf.writeVarInt(payload.positions.size());
                                    for (BlacklistSyncManager.ExcludedPosition pos :
                                            payload.positions) {
                                        buf.writeUtf(pos.targetId());
                                        buf.writeInt(pos.x());
                                        buf.writeInt(pos.y());
                                        buf.writeInt(pos.z());
                                    }
                                },
                                buf -> {
                                    int count = buf.readVarInt();
                                    List<BlacklistSyncManager.ExcludedPosition> positions =
                                            new ArrayList<>();
                                    for (int i = 0; i < count; i++) {
                                        String targetId = buf.readUtf();
                                        int x = buf.readInt();
                                        int y = buf.readInt();
                                        int z = buf.readInt();
                                        positions.add(
                                                new BlacklistSyncManager.ExcludedPosition(
                                                        targetId, x, y, z));
                                    }
                                    return new PathBlacklistSyncC2S(positions);
                                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final int PATH_SCENT_DURATION_TICKS = 100;

    private static boolean initialized = false;

    private PathScentNetworking() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                PathScentTriggerS2C.TYPE,
                PathScentTriggerS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                ScentPriority priority =
                                        ScentPriority.values()[payload.priorityOrdinal()];

                                ScentTrigger trigger =
                                        ScentTrigger.create(
                                                payload.scentName(),
                                                ScentTriggerSource.PATH_TRACKING,
                                                priority,
                                                payload.durationTicks(),
                                                payload.intensity());

                                boolean triggered =
                                        ScentTriggerManager.getInstance().trigger(trigger);
                                if (triggered) {
                                    PathTrackingMaskOverlay.onPathScentPuff(
                                            payload.scentName(), payload.intensity());
                                }
                                AromaAffect.LOGGER.debug(
                                        "Received path scent trigger from server: {} (triggered: {})",
                                        payload.scentName(),
                                        triggered);
                            });
                });

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                PathDistanceS2C.TYPE,
                PathDistanceS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                ActiveTrackingState.setDistance(payload.distance());
                            });
                });

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                PathStatusFoundS2C.TYPE,
                PathStatusFoundS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                AromaAffect.LOGGER.debug(
                                        "Path status: found (distance: {}, destination: {})",
                                        payload.distance(),
                                        payload.destination());

                                ActiveTrackingState.setTracking(
                                        payload.distance(), payload.destination());

                                if (ActiveTrackingState.getTargetId() != null
                                        && ActiveTrackingState.getCategory() != null) {
                                    String dimension =
                                            Minecraft.getInstance().level != null
                                                    ? Minecraft.getInstance()
                                                            .level
                                                            .dimension()
                                                            .location()
                                                            .toString()
                                                    : "minecraft:overworld";
                                    TrackingHistoryData.getInstance()
                                            .addHistoryEntry(
                                                    new HistoryEntry(
                                                            ActiveTrackingState.getTargetId()
                                                                    .toString(),
                                                            ActiveTrackingState.getDisplayName()
                                                                            != null
                                                                    ? ActiveTrackingState
                                                                            .getDisplayName()
                                                                            .getString()
                                                                    : "",
                                                            ActiveTrackingState.getCategory()
                                                                    .getId(),
                                                            payload.destination().getX(),
                                                            payload.destination().getY(),
                                                            payload.destination().getZ(),
                                                            dimension,
                                                            System.currentTimeMillis()));
                                }
                            });
                });

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                PathStatusNotFoundS2C.TYPE,
                PathStatusNotFoundS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                ActiveTrackingState.setFailed(payload.reason(), false);
                                AromaAffect.LOGGER.debug(
                                        "Path status: not found (reason: {})", payload.reason());
                            });
                });

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                PathStatusArrivedS2C.TYPE,
                PathStatusArrivedS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                ActiveTrackingState.setArrived();
                                AromaAffect.LOGGER.debug("Path status: arrived");
                            });
                });

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                StructureSyncS2C.TYPE,
                StructureSyncS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                PassiveModeManager.setServerStructureId(payload.structureId());
                                AromaAffect.LOGGER.debug(
                                        "Received structure sync from server: {}",
                                        payload.structureId());
                            });
                });

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                PathBlacklistSyncC2S.TYPE,
                PathBlacklistSyncC2S.STREAM_CODEC,
                (payload, context) -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        BlacklistSyncManager.getInstance()
                                .setExclusions(serverPlayer.getUUID(), payload.positions());
                        AromaAffect.LOGGER.debug(
                                "Received blacklist sync from {}: {} entries",
                                serverPlayer.getName().getString(),
                                payload.positions().size());
                    }
                });

        AromaAffect.LOGGER.info("PathScentNetworking initialized");
    }

    public static void sendScentTrigger(
            ServerPlayer player, String scentName, double intensity, ScentPriority priority) {
        if (!NetworkManager.canPlayerReceive(player, PathScentTriggerS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(
                player,
                new PathScentTriggerS2C(
                        scentName, intensity, priority.ordinal(), PATH_SCENT_DURATION_TICKS));

        AromaAffect.LOGGER.debug(
                "Sent path scent trigger to {}: {} (intensity: {}, priority: {})",
                player.getName().getString(),
                scentName,
                intensity,
                priority);
    }

    public static void sendDistanceUpdate(ServerPlayer player, int distance) {
        if (!NetworkManager.canPlayerReceive(player, PathDistanceS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathDistanceS2C(distance));
    }

    public static void sendPathFound(ServerPlayer player, int distance, BlockPos destination) {
        if (!NetworkManager.canPlayerReceive(player, PathStatusFoundS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathStatusFoundS2C(distance, destination));
    }

    public static void sendPathNotFound(ServerPlayer player, String reason) {
        if (!NetworkManager.canPlayerReceive(player, PathStatusNotFoundS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathStatusNotFoundS2C(reason));
    }

    public static void sendPathArrived(ServerPlayer player) {
        if (!NetworkManager.canPlayerReceive(player, PathStatusArrivedS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new PathStatusArrivedS2C());
    }

    public static void sendStructureSync(ServerPlayer player, String structureId) {
        if (!NetworkManager.canPlayerReceive(player, StructureSyncS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new StructureSyncS2C(structureId));
    }

    public static void sendBlacklistSync(RegistryAccess registryAccess) {
        List<BlacklistEntry> blacklist = TrackingHistoryData.getInstance().getBlacklist();
        List<BlacklistSyncManager.ExcludedPosition> positions = new ArrayList<>();
        for (BlacklistEntry entry : blacklist) {
            positions.add(
                    new BlacklistSyncManager.ExcludedPosition(
                            entry.targetId, entry.x, entry.y, entry.z));
        }

        NetworkManager.sendToServer(new PathBlacklistSyncC2S(positions));
        AromaAffect.LOGGER.debug("Sent blacklist sync to server: {} entries", blacklist.size());
    }
}
