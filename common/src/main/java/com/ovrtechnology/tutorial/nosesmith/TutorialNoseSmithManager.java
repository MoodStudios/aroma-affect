package com.ovrtechnology.tutorial.nosesmith;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Optional;

/**
 * Persists tutorial Nose Smith configuration:
 * - Spawn position + yaw (where the NoseSmith stands)
 * - Flower position (fixed location for the quest flower)
 */
public class TutorialNoseSmithManager extends SavedData {

    private BlockPos spawnPos;
    private float spawnYaw;
    private BlockPos flowerPos;

    private static final Codec<TutorialNoseSmithManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("spawnX", Integer.MIN_VALUE).forGetter(m -> m.spawnPos != null ? m.spawnPos.getX() : Integer.MIN_VALUE),
                    Codec.INT.optionalFieldOf("spawnY", Integer.MIN_VALUE).forGetter(m -> m.spawnPos != null ? m.spawnPos.getY() : Integer.MIN_VALUE),
                    Codec.INT.optionalFieldOf("spawnZ", Integer.MIN_VALUE).forGetter(m -> m.spawnPos != null ? m.spawnPos.getZ() : Integer.MIN_VALUE),
                    Codec.FLOAT.optionalFieldOf("spawnYaw", 0.0f).forGetter(m -> m.spawnYaw),
                    Codec.INT.optionalFieldOf("flowerX", Integer.MIN_VALUE).forGetter(m -> m.flowerPos != null ? m.flowerPos.getX() : Integer.MIN_VALUE),
                    Codec.INT.optionalFieldOf("flowerY", Integer.MIN_VALUE).forGetter(m -> m.flowerPos != null ? m.flowerPos.getY() : Integer.MIN_VALUE),
                    Codec.INT.optionalFieldOf("flowerZ", Integer.MIN_VALUE).forGetter(m -> m.flowerPos != null ? m.flowerPos.getZ() : Integer.MIN_VALUE)
            ).apply(instance, TutorialNoseSmithManager::new)
    );

    private static final SavedDataType<TutorialNoseSmithManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_nosesmith",
            TutorialNoseSmithManager::new,
            CODEC,
            null
    );

    public TutorialNoseSmithManager() {
        this.spawnPos = null;
        this.spawnYaw = 0.0f;
        this.flowerPos = null;
    }

    private TutorialNoseSmithManager(int sx, int sy, int sz, float yaw, int fx, int fy, int fz) {
        if (sx != Integer.MIN_VALUE && sy != Integer.MIN_VALUE && sz != Integer.MIN_VALUE) {
            this.spawnPos = new BlockPos(sx, sy, sz);
        }
        this.spawnYaw = yaw;
        if (fx != Integer.MIN_VALUE && fy != Integer.MIN_VALUE && fz != Integer.MIN_VALUE) {
            this.flowerPos = new BlockPos(fx, fy, fz);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API
    // ─────────────────────────────────────────────────────────────────────────────

    public static TutorialNoseSmithManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static Optional<BlockPos> getSpawnPos(ServerLevel level) {
        return Optional.ofNullable(get(level).spawnPos);
    }

    public static float getSpawnYaw(ServerLevel level) {
        return get(level).spawnYaw;
    }

    public static Optional<BlockPos> getFlowerPos(ServerLevel level) {
        return Optional.ofNullable(get(level).flowerPos);
    }

    public static void setSpawnPos(ServerLevel level, BlockPos pos, float yaw) {
        TutorialNoseSmithManager manager = get(level);
        manager.spawnPos = pos;
        manager.spawnYaw = yaw;
        manager.setDirty();
        AromaAffect.LOGGER.info("Set Nose Smith spawn to {} (yaw: {})", pos, yaw);
    }

    public static void setFlowerPos(ServerLevel level, BlockPos pos) {
        TutorialNoseSmithManager manager = get(level);
        manager.flowerPos = pos;
        manager.setDirty();
        AromaAffect.LOGGER.info("Set Nose Smith flower position to {}", pos);
    }

    public static boolean hasSpawn(ServerLevel level) {
        return get(level).spawnPos != null;
    }

    public static boolean hasFlowerPos(ServerLevel level) {
        return get(level).flowerPos != null;
    }
}
