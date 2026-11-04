package com.ovrtechnology.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class RespawnCompat {

    public record Point(ResourceKey<Level> dimension, BlockPos pos) {}

    private RespawnCompat() {}

    @Nullable
    public static Point get(ServerPlayer player) {
        //? if >=26 {
        var config = player.getRespawnConfig();
        if (config == null || config.respawnData() == null) {
            return null;
        }
        var data = config.respawnData();
        return new Point(data.dimension(), data.pos());
        //?} else {
        /*BlockPos pos = player.getRespawnPosition();
        if (pos == null) {
            return null;
        }
        return new Point(player.getRespawnDimension(), pos);
        *///?}
    }
}
