package com.ovrtechnology.tracking;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.PathScentNetworking;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.storage.LevelData;

@UtilityClass
public final class RespawnSyncHandler {

    public static void init() {
        ServerPlayerCallback.Join.EVENT.register(RespawnSyncHandler::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        server.execute(() -> sync(player));
    }

    public static void sync(ServerPlayer player) {
        try {
            ServerPlayer.RespawnConfig config = player.getRespawnConfig();
            if (config == null || config.respawnData() == null) {
                PathScentNetworking.sendRespawnCleared(player);
                return;
            }
            LevelData.RespawnData data = config.respawnData();
            MinecraftServer server = player.level().getServer();
            ServerLevel level = server != null ? server.getLevel(data.dimension()) : null;
            if (level == null) {
                PathScentNetworking.sendRespawnCleared(player);
                return;
            }
            BlockPos pos = data.pos();
            Block block = level.getBlockState(pos).getBlock();
            if (!(block instanceof BedBlock) && !(block instanceof RespawnAnchorBlock)) {
                PathScentNetworking.sendRespawnCleared(player);
                return;
            }
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
            String dimensionId = data.dimension().identifier().toString();
            PathScentNetworking.sendRespawnSync(player, dimensionId, pos, blockId);
        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Could not sync respawn point for {}: {}", player.getName().getString(), e.getMessage());
        }
    }
}
