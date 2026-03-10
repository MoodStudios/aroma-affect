package com.ovrtechnology.tutorial.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Optional;

/**
 * Manages the tutorial spawn point for a world.
 * <p>
 * The spawn point is saved in the world data and persists across server restarts.
 * When a player joins a tutorial-enabled world, they are teleported to this
 * spawn point with a cinematic intro sequence.
 */
public class TutorialSpawnManager extends SavedData {

    private BlockPos spawnPos;
    private float spawnYaw;
    private float spawnPitch;
    private String firstWaypointId;
    private String introCinematicId;
    // Walkaround spawn point (free exploration mode)
    private BlockPos walkaroundPos;
    private float walkaroundYaw;
    private float walkaroundPitch;

    private static final Codec<TutorialSpawnManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.optionalFieldOf("spawnPos").forGetter(m -> Optional.ofNullable(m.spawnPos)),
                    Codec.FLOAT.fieldOf("spawnYaw").forGetter(m -> m.spawnYaw),
                    Codec.FLOAT.fieldOf("spawnPitch").forGetter(m -> m.spawnPitch),
                    Codec.STRING.optionalFieldOf("firstWaypointId", "").forGetter(m -> m.firstWaypointId != null ? m.firstWaypointId : ""),
                    Codec.STRING.optionalFieldOf("introCinematicId", "").forGetter(m -> m.introCinematicId != null ? m.introCinematicId : ""),
                    BlockPos.CODEC.optionalFieldOf("walkaroundPos").forGetter(m -> Optional.ofNullable(m.walkaroundPos)),
                    Codec.FLOAT.optionalFieldOf("walkaroundYaw", 0f).forGetter(m -> m.walkaroundYaw),
                    Codec.FLOAT.optionalFieldOf("walkaroundPitch", 0f).forGetter(m -> m.walkaroundPitch)
            ).apply(instance, TutorialSpawnManager::new)
    );

    static final SavedDataType<TutorialSpawnManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_spawn",
            TutorialSpawnManager::new,
            CODEC,
            null
    );

    public TutorialSpawnManager() {
        this.spawnPos = null;
        this.spawnYaw = 0f;
        this.spawnPitch = 0f;
        this.firstWaypointId = "";
        this.introCinematicId = "";
        this.walkaroundPos = null;
        this.walkaroundYaw = 0f;
        this.walkaroundPitch = 0f;
    }

    private TutorialSpawnManager(Optional<BlockPos> spawnPos, float spawnYaw, float spawnPitch,
                                  String firstWaypointId, String introCinematicId,
                                  Optional<BlockPos> walkaroundPos, float walkaroundYaw, float walkaroundPitch) {
        this.spawnPos = spawnPos.orElse(null);
        this.spawnYaw = spawnYaw;
        this.spawnPitch = spawnPitch;
        this.firstWaypointId = firstWaypointId != null ? firstWaypointId : "";
        this.introCinematicId = introCinematicId != null ? introCinematicId : "";
        this.walkaroundPos = walkaroundPos.orElse(null);
        this.walkaroundYaw = walkaroundYaw;
        this.walkaroundPitch = walkaroundPitch;
    }

    /**
     * Sets the tutorial spawn point for the given level.
     *
     * @param level the server level
     * @param pos   the spawn position
     * @param yaw   the spawn yaw (horizontal rotation)
     * @param pitch the spawn pitch (vertical rotation)
     */
    public static void setSpawn(ServerLevel level, BlockPos pos, float yaw, float pitch) {
        TutorialSpawnManager manager = get(level);
        manager.spawnPos = pos;
        manager.spawnYaw = yaw;
        manager.spawnPitch = pitch;
        manager.setDirty();

        AromaAffect.LOGGER.info("Tutorial spawn set to {} (yaw: {}, pitch: {})", pos, yaw, pitch);
    }

    /**
     * Sets the first waypoint ID that will be activated when a player joins.
     */
    public static void setFirstWaypoint(ServerLevel level, String waypointId) {
        TutorialSpawnManager manager = get(level);
        manager.firstWaypointId = waypointId != null ? waypointId : "";
        manager.setDirty();
        AromaAffect.LOGGER.info("Tutorial first waypoint set to '{}'", waypointId);
    }

    /**
     * Gets the first waypoint ID.
     */
    public static Optional<String> getFirstWaypointId(ServerLevel level) {
        TutorialSpawnManager manager = get(level);
        if (manager.firstWaypointId == null || manager.firstWaypointId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(manager.firstWaypointId);
    }

    /**
     * Sets the intro cinematic ID used for the tutorial intro screen.
     */
    public static void setIntroCinematic(ServerLevel level, String cinematicId) {
        TutorialSpawnManager manager = get(level);
        manager.introCinematicId = cinematicId != null ? cinematicId : "";
        manager.setDirty();
        AromaAffect.LOGGER.info("Tutorial intro cinematic set to '{}'", cinematicId);
    }

    /**
     * Gets the intro cinematic ID.
     */
    public static Optional<String> getIntroCinematicId(ServerLevel level) {
        TutorialSpawnManager manager = get(level);
        if (manager.introCinematicId == null || manager.introCinematicId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(manager.introCinematicId);
    }

    /**
     * Gets the tutorial spawn data for the given level.
     *
     * @param level the server level
     * @return optional containing spawn data if set, empty otherwise
     */
    public static Optional<SpawnData> getSpawn(ServerLevel level) {
        TutorialSpawnManager manager = get(level);
        if (manager.spawnPos == null) {
            return Optional.empty();
        }
        return Optional.of(new SpawnData(manager.spawnPos, manager.spawnYaw, manager.spawnPitch));
    }

    /**
     * Checks if a tutorial spawn point is set for the given level.
     *
     * @param level the server level
     * @return true if a spawn point is set
     */
    public static boolean hasSpawn(ServerLevel level) {
        return get(level).spawnPos != null;
    }

    private static TutorialSpawnManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Walkaround spawn point (free exploration mode)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the walkaround spawn point for free exploration mode.
     *
     * @param level the server level
     * @param pos   the walkaround spawn position
     * @param yaw   the spawn yaw (horizontal rotation)
     * @param pitch the spawn pitch (vertical rotation)
     */
    public static void setWalkaroundSpawn(ServerLevel level, BlockPos pos, float yaw, float pitch) {
        TutorialSpawnManager manager = get(level);
        manager.walkaroundPos = pos;
        manager.walkaroundYaw = yaw;
        manager.walkaroundPitch = pitch;
        manager.setDirty();

        AromaAffect.LOGGER.info("Walkaround spawn set to {} (yaw: {}, pitch: {})", pos, yaw, pitch);
    }

    /**
     * Gets the walkaround spawn data for free exploration mode.
     *
     * @param level the server level
     * @return optional containing walkaround spawn data if set, empty otherwise
     */
    public static Optional<SpawnData> getWalkaroundSpawn(ServerLevel level) {
        TutorialSpawnManager manager = get(level);
        if (manager.walkaroundPos == null) {
            return Optional.empty();
        }
        return Optional.of(new SpawnData(manager.walkaroundPos, manager.walkaroundYaw, manager.walkaroundPitch));
    }

    /**
     * Checks if a walkaround spawn point is set.
     *
     * @param level the server level
     * @return true if a walkaround spawn point is set
     */
    public static boolean hasWalkaroundSpawn(ServerLevel level) {
        return get(level).walkaroundPos != null;
    }

    /**
     * Immutable spawn data record.
     */
    public record SpawnData(BlockPos pos, float yaw, float pitch) {}
}
