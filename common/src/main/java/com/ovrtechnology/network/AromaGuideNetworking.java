package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.guide.AromaGuideTracker;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Syncs Aroma Guide targets from server to client.
 *
 * <p>Client requests an update while holding the guide. Server computes nearest
 * village (and nearby Nose Smith target) and sends positions back.</p>
 */
public final class AromaGuideNetworking {

    public record GuideRequestC2S() implements CustomPacketPayload {
        public static final Type<GuideRequestC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "guide_request_c2s"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GuideRequestC2S> STREAM_CODEC =
                StreamCodec.unit(new GuideRequestC2S());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record GuideTargetS2C(@Nullable BlockPos villagePos, @Nullable BlockPos compassTarget) implements CustomPacketPayload {
        public static final Type<GuideTargetS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "guide_target_s2c"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GuideTargetS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.villagePos != null);
                    if (payload.villagePos != null) buf.writeBlockPos(payload.villagePos);
                    buf.writeBoolean(payload.compassTarget != null);
                    if (payload.compassTarget != null) buf.writeBlockPos(payload.compassTarget);
                },
                buf -> {
                    BlockPos v = buf.readBoolean() ? buf.readBlockPos() : null;
                    BlockPos c = buf.readBoolean() ? buf.readBlockPos() : null;
                    return new GuideTargetS2C(v, c);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static final int SEARCH_RADIUS = 100; // in chunks
    private static final double NOSE_SMITH_SEARCH_RADIUS = 100.0;

    private static boolean initialized = false;

    private AromaGuideNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GuideRequestC2S.TYPE, GuideRequestC2S.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        sendGuideTarget(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, GuideTargetS2C.TYPE, GuideTargetS2C.STREAM_CODEC,
                (payload, context) -> context.queue(() ->
                        AromaGuideTracker.applyServerTarget(payload.villagePos(), payload.compassTarget())));

        AromaAffect.LOGGER.info("AromaGuideNetworking initialized");
    }

    public static void requestGuideTarget(net.minecraft.core.RegistryAccess registryAccess) {
        NetworkManager.sendToServer(new GuideRequestC2S());
    }

    private static void sendGuideTarget(ServerPlayer player) {
        GuideTarget target = findGuideTarget(player);

        if (!NetworkManager.canPlayerReceive(player, GuideTargetS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new GuideTargetS2C(target.nearestVillagePos, target.compassTargetPos));
    }

    private static GuideTarget findGuideTarget(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        var result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(
                        level,
                        level.registryAccess()
                                .lookupOrThrow(Registries.STRUCTURE)
                                .getOrThrow(net.minecraft.tags.StructureTags.VILLAGE),
                        playerPos,
                        SEARCH_RADIUS,
                        false
                );

        if (result == null) {
            return new GuideTarget(null, null);
        }

        BlockPos villagePos = result.getFirst();
        BlockPos compassTarget = villagePos;

        double distToVillage = Math.sqrt(playerPos.distSqr(villagePos));
        if (distToVillage <= NOSE_SMITH_SEARCH_RADIUS) {
            BlockPos smithPos = findNoseSmithNear(level, villagePos);
            if (smithPos != null) {
                compassTarget = smithPos;
            }
        }

        return new GuideTarget(villagePos, compassTarget);
    }

    @Nullable
    private static BlockPos findNoseSmithNear(ServerLevel level, BlockPos center) {
        AABB searchBox = new AABB(center).inflate(NOSE_SMITH_SEARCH_RADIUS);
        List<NoseSmithEntity> smiths = level.getEntitiesOfClass(
                NoseSmithEntity.class, searchBox, Entity::isAlive
        );

        if (smiths.isEmpty()) {
            return null;
        }

        NoseSmithEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (NoseSmithEntity smith : smiths) {
            double dist = smith.blockPosition().distSqr(center);
            if (dist < closestDist) {
                closestDist = dist;
                closest = smith;
            }
        }
        return closest != null ? closest.blockPosition() : null;
    }

    private static final class GuideTarget {
        @Nullable
        private final BlockPos nearestVillagePos;
        @Nullable
        private final BlockPos compassTargetPos;

        private GuideTarget(@Nullable BlockPos nearestVillagePos, @Nullable BlockPos compassTargetPos) {
            this.nearestVillagePos = nearestVillagePos;
            this.compassTargetPos = compassTargetPos;
        }
    }
}
