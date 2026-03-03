package com.ovrtechnology.tutorial.dream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Optional;

/**
 * Persists the dream end configuration (teleport destination after dragon kill).
 */
public class TutorialDreamEndManager extends SavedData {

    private BlockPos endPos;
    private float endYaw;

    private static final Codec<TutorialDreamEndManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("endX", Integer.MIN_VALUE).forGetter(m -> m.endPos != null ? m.endPos.getX() : Integer.MIN_VALUE),
                    Codec.INT.optionalFieldOf("endY", Integer.MIN_VALUE).forGetter(m -> m.endPos != null ? m.endPos.getY() : Integer.MIN_VALUE),
                    Codec.INT.optionalFieldOf("endZ", Integer.MIN_VALUE).forGetter(m -> m.endPos != null ? m.endPos.getZ() : Integer.MIN_VALUE),
                    Codec.FLOAT.optionalFieldOf("endYaw", 0.0f).forGetter(m -> m.endYaw)
            ).apply(instance, TutorialDreamEndManager::new)
    );

    static final SavedDataType<TutorialDreamEndManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_dream_end",
            TutorialDreamEndManager::new,
            CODEC,
            null
    );

    public TutorialDreamEndManager() {
        this.endPos = null;
        this.endYaw = 0.0f;
    }

    private TutorialDreamEndManager(int x, int y, int z, float yaw) {
        if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE && z != Integer.MIN_VALUE) {
            this.endPos = new BlockPos(x, y, z);
        } else {
            this.endPos = null;
        }
        this.endYaw = yaw;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API
    // ─────────────────────────────────────────────────────────────────────────────

    public static Optional<BlockPos> getEndPos(ServerLevel level) {
        return Optional.ofNullable(get(level).endPos);
    }

    public static float getEndYaw(ServerLevel level) {
        return get(level).endYaw;
    }

    public static void setEndPos(ServerLevel level, BlockPos pos) {
        TutorialDreamEndManager manager = get(level);
        manager.endPos = pos;
        manager.setDirty();
        AromaAffect.LOGGER.info("Set dream end position to {}", pos);
    }

    public static void setEndYaw(ServerLevel level, float yaw) {
        TutorialDreamEndManager manager = get(level);
        manager.endYaw = yaw;
        manager.setDirty();
        AromaAffect.LOGGER.info("Set dream end yaw to {}", yaw);
    }

    public static boolean isConfigured(ServerLevel level) {
        return get(level).endPos != null;
    }

    private static TutorialDreamEndManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
