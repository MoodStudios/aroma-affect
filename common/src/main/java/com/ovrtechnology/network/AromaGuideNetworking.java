package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.guide.AromaGuideTracker;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation GUIDE_REQUEST_C2S =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "guide_request_c2s");
    private static final ResourceLocation GUIDE_TARGET_S2C =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "guide_target_s2c");

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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GUIDE_REQUEST_C2S, (buf, context) ->
                context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        sendGuideTarget(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, GUIDE_TARGET_S2C, (buf, context) -> {
            boolean hasVillage = buf.readBoolean();
            BlockPos villagePos = hasVillage ? buf.readBlockPos() : null;

            boolean hasTarget = buf.readBoolean();
            BlockPos compassTarget = hasTarget ? buf.readBlockPos() : null;

            context.queue(() -> AromaGuideTracker.applyServerTarget(villagePos, compassTarget));
        });

        AromaAffect.LOGGER.info("AromaGuideNetworking initialized");
    }

    public static void requestGuideTarget(net.minecraft.core.RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        NetworkManager.sendToServer(GUIDE_REQUEST_C2S, buf);
    }

    private static void sendGuideTarget(ServerPlayer player) {
        GuideTarget target = findGuideTarget(player);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );
        buf.writeBoolean(target.nearestVillagePos != null);
        if (target.nearestVillagePos != null) {
            buf.writeBlockPos(target.nearestVillagePos);
        }

        buf.writeBoolean(target.compassTargetPos != null);
        if (target.compassTargetPos != null) {
            buf.writeBlockPos(target.compassTargetPos);
        }

        if (!NetworkManager.canPlayerReceive(player, GUIDE_TARGET_S2C)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, GUIDE_TARGET_S2C, buf);
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
