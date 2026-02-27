package com.ovrtechnology.tutorial.animation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages tutorial animations for a world.
 * <p>
 * Animations are stored per-world and persist across server restarts.
 */
public class TutorialAnimationManager extends SavedData {

    private final Map<String, TutorialAnimation> animations = new HashMap<>();

    // Codec for SavedBlockData
    private static final Codec<SavedBlockData> SAVED_BLOCK_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("x").forGetter(SavedBlockData::x),
                    Codec.INT.fieldOf("y").forGetter(SavedBlockData::y),
                    Codec.INT.fieldOf("z").forGetter(SavedBlockData::z),
                    Codec.STRING.fieldOf("blockState").forGetter(SavedBlockData::blockState)
            ).apply(instance, SavedBlockData::new)
    );

    // Codec for AnimationData
    private static final Codec<AnimationData> ANIMATION_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(AnimationData::id),
                    Codec.STRING.fieldOf("type").forGetter(AnimationData::type),
                    BlockPos.CODEC.optionalFieldOf("corner1").forGetter(d -> Optional.ofNullable(d.corner1)),
                    BlockPos.CODEC.optionalFieldOf("corner2").forGetter(d -> Optional.ofNullable(d.corner2)),
                    Codec.BOOL.optionalFieldOf("played", false).forGetter(AnimationData::played),
                    SAVED_BLOCK_CODEC.listOf().optionalFieldOf("savedBlocks", List.of())
                            .forGetter(AnimationData::savedBlocks),
                    Codec.STRING.optionalFieldOf("onCompleteWaypointId").forGetter(d -> Optional.ofNullable(d.onCompleteWaypointId)),
                    Codec.STRING.optionalFieldOf("onCompleteCinematicId").forGetter(d -> Optional.ofNullable(d.onCompleteCinematicId)),
                    Codec.STRING.optionalFieldOf("onCompleteOliverAction").forGetter(d -> Optional.ofNullable(d.onCompleteOliverAction))
            ).apply(instance, (id, type, corner1, corner2, played, savedBlocks, waypointId, cinematicId, oliverAction) ->
                    new AnimationData(id, type, corner1.orElse(null), corner2.orElse(null), played, savedBlocks,
                            waypointId.orElse(null), cinematicId.orElse(null), oliverAction.orElse(null)))
    );

    // Manager codec
    private static final Codec<TutorialAnimationManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ANIMATION_CODEC.listOf().fieldOf("animations")
                            .forGetter(TutorialAnimationManager::getAnimationDataList)
            ).apply(instance, TutorialAnimationManager::new)
    );

    static final SavedDataType<TutorialAnimationManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_animations",
            TutorialAnimationManager::new,
            CODEC,
            null
    );

    public TutorialAnimationManager() {
    }

    private TutorialAnimationManager(List<AnimationData> animationDataList) {
        for (AnimationData data : animationDataList) {
            TutorialAnimationType type = TutorialAnimationType.byName(data.type);
            if (type == null) {
                type = TutorialAnimationType.WALL_BREAK;
            }

            Map<BlockPos, String> savedBlocksMap = new HashMap<>();
            for (SavedBlockData sbd : data.savedBlocks) {
                savedBlocksMap.put(new BlockPos(sbd.x, sbd.y, sbd.z), sbd.blockState);
            }

            TutorialAnimation animation = new TutorialAnimation(
                    data.id,
                    type,
                    data.corner1,
                    data.corner2,
                    data.played,
                    savedBlocksMap,
                    data.onCompleteWaypointId,
                    data.onCompleteCinematicId,
                    data.onCompleteOliverAction
            );
            animations.put(data.id, animation);
        }
    }

    private List<AnimationData> getAnimationDataList() {
        List<AnimationData> list = new ArrayList<>();
        for (TutorialAnimation anim : animations.values()) {
            List<SavedBlockData> savedBlockDataList = anim.getSavedBlocks().entrySet().stream()
                    .map(e -> new SavedBlockData(
                            e.getKey().getX(), e.getKey().getY(), e.getKey().getZ(),
                            e.getValue()))
                    .collect(Collectors.toList());

            list.add(new AnimationData(
                    anim.getId(),
                    anim.getType().name(),
                    anim.getCorner1(),
                    anim.getCorner2(),
                    anim.isPlayed(),
                    savedBlockDataList,
                    anim.getOnCompleteWaypointId(),
                    anim.getOnCompleteCinematicId(),
                    anim.getOnCompleteOliverAction()
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Animation CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new animation.
     *
     * @param level the server level
     * @param id    the animation ID
     * @param type  the animation type
     * @return true if created, false if already exists
     */
    public static boolean createAnimation(ServerLevel level, String id, TutorialAnimationType type) {
        TutorialAnimationManager manager = get(level);
        if (manager.animations.containsKey(id)) {
            return false;
        }
        TutorialAnimation animation = new TutorialAnimation(id);
        animation.setType(type);
        manager.animations.put(id, animation);
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial animation {} (type: {})", id, type);
        return true;
    }

    /**
     * Deletes an animation.
     *
     * @param level the server level
     * @param id    the animation ID
     * @return true if deleted, false if not found
     */
    public static boolean deleteAnimation(ServerLevel level, String id) {
        TutorialAnimationManager manager = get(level);
        if (manager.animations.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial animation {}", id);
            return true;
        }
        return false;
    }

    /**
     * Gets an animation by ID.
     *
     * @param level the server level
     * @param id    the animation ID
     * @return Optional containing the animation, or empty if not found
     */
    public static Optional<TutorialAnimation> getAnimation(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).animations.get(id));
    }

    /**
     * Gets all animation IDs.
     *
     * @param level the server level
     * @return set of animation IDs
     */
    public static Set<String> getAllAnimationIds(ServerLevel level) {
        return new HashSet<>(get(level).animations.keySet());
    }

    /**
     * Gets all animations.
     *
     * @param level the server level
     * @return list of all animations
     */
    public static List<TutorialAnimation> getAllAnimations(ServerLevel level) {
        return new ArrayList<>(get(level).animations.values());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Animation Modification
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the type of an animation.
     *
     * @param level the server level
     * @param id    the animation ID
     * @param type  the new type
     * @return true if set, false if animation not found
     */
    public static boolean setType(ServerLevel level, String id, TutorialAnimationType type) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.setType(type);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set animation {} type to {}", id, type);
        return true;
    }

    /**
     * Sets corner1 of an animation.
     *
     * @param level the server level
     * @param id    the animation ID
     * @param pos   the position
     * @return true if set, false if animation not found
     */
    public static boolean setCorner1(ServerLevel level, String id, BlockPos pos) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.setCorner1(pos);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set animation {} corner1 to {}", id, pos);
        return true;
    }

    /**
     * Sets corner2 of an animation.
     *
     * @param level the server level
     * @param id    the animation ID
     * @param pos   the position
     * @return true if set, false if animation not found
     */
    public static boolean setCorner2(ServerLevel level, String id, BlockPos pos) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.setCorner2(pos);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set animation {} corner2 to {}", id, pos);
        return true;
    }

    /**
     * Marks an animation as played.
     *
     * @param level the server level
     * @param id    the animation ID
     * @return true if marked, false if animation not found
     */
    public static boolean markPlayed(ServerLevel level, String id) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.markPlayed();
        manager.setDirty();
        AromaAffect.LOGGER.info("Marked animation {} as played", id);
        return true;
    }

    /**
     * Saves the block snapshot for an animation (called before blocks are removed).
     *
     * @param level      the server level
     * @param id         the animation ID
     * @param savedBlocks map of block positions to their serialized block state strings
     * @return true if saved, false if animation not found
     */
    public static boolean setSavedBlocks(ServerLevel level, String id, Map<BlockPos, String> savedBlocks) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.setSavedBlocks(savedBlocks);
        manager.setDirty();
        AromaAffect.LOGGER.info("Saved {} block states for animation {}", savedBlocks.size(), id);
        return true;
    }

    /**
     * Resets an animation to unplayed state and restores any saved blocks.
     *
     * @param level the server level
     * @param id    the animation ID
     * @return true if reset, false if animation not found
     */
    public static boolean resetAnimation(ServerLevel level, String id) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        restoreBlocks(level, animation);
        animation.reset();
        manager.setDirty();
        AromaAffect.LOGGER.info("Reset animation {}", id);
        return true;
    }

    /**
     * Resets all animations to unplayed state and restores any saved blocks.
     * <p>
     * This also stops any actively playing animations first, and resets
     * animations that are mid-play (have savedBlocks but not yet marked played).
     *
     * @param level the server level
     * @return number of animations reset
     */
    public static int resetAllAnimations(ServerLevel level) {
        // First, stop any actively playing animations so they don't
        // continue destroying blocks after we restore them
        TutorialAnimationHandler.stopAllActiveAnimations(level);

        TutorialAnimationManager manager = get(level);
        int count = 0;
        for (TutorialAnimation animation : manager.animations.values()) {
            // Reset if played OR if has savedBlocks (mid-play animation that
            // was stopped before completing)
            if (animation.isPlayed() || animation.hasSavedBlocks()) {
                restoreBlocks(level, animation);
                animation.reset();
                count++;
            }
        }
        if (count > 0) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Reset {} animations", count);
        }
        return count;
    }

    /**
     * Restores saved blocks for an animation and clears the snapshot.
     */
    private static void restoreBlocks(ServerLevel level, TutorialAnimation animation) {
        if (!animation.hasSavedBlocks()) {
            return;
        }

        int restored = 0;
        int failed = 0;
        for (Map.Entry<BlockPos, String> entry : animation.getSavedBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            String stateString = entry.getValue();

            try {
                BlockState blockState = BlockStateParser.parseForBlock(
                        level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),
                        stateString,
                        false
                ).blockState();
                level.setBlock(pos, blockState, Block.UPDATE_ALL);
                restored++;
            } catch (Exception e) {
                failed++;
                AromaAffect.LOGGER.warn("Failed to restore block at {} with state '{}': {}",
                        pos, stateString, e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Restored {} blocks for animation {}", restored, animation.getId());

        // Only clear saved blocks if all were restored successfully.
        // On partial failure, keep the snapshot so it can be retried.
        if (failed == 0) {
            animation.clearSavedBlocks();
        } else {
            AromaAffect.LOGGER.warn("Kept saved blocks for animation {} due to {} restoration failures",
                    animation.getId(), failed);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - On Complete Actions
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the waypoint to activate when an animation completes.
     *
     * @param level      the server level
     * @param id         the animation ID
     * @param waypointId the waypoint ID (null to clear)
     * @return true if set, false if animation not found
     */
    public static boolean setOnCompleteWaypoint(ServerLevel level, String id, String waypointId) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.setOnCompleteWaypointId(waypointId);
        manager.setDirty();
        if (waypointId != null) {
            AromaAffect.LOGGER.info("Set animation {} onComplete waypoint: {}", id, waypointId);
        } else {
            AromaAffect.LOGGER.info("Cleared animation {} onComplete waypoint", id);
        }
        return true;
    }

    /**
     * Sets the cinematic to start when an animation completes.
     *
     * @param level       the server level
     * @param id          the animation ID
     * @param cinematicId the cinematic ID (null to clear)
     * @return true if set, false if animation not found
     */
    public static boolean setOnCompleteCinematic(ServerLevel level, String id, String cinematicId) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.setOnCompleteCinematicId(cinematicId);
        manager.setDirty();
        if (cinematicId != null) {
            AromaAffect.LOGGER.info("Set animation {} onComplete cinematic: {}", id, cinematicId);
        } else {
            AromaAffect.LOGGER.info("Cleared animation {} onComplete cinematic", id);
        }
        return true;
    }

    /**
     * Sets the Oliver action to execute when an animation completes.
     *
     * @param level  the server level
     * @param id     the animation ID
     * @param action the action (null to clear)
     * @return true if set, false if animation not found
     */
    public static boolean setOnCompleteOliverAction(ServerLevel level, String id, String action) {
        TutorialAnimationManager manager = get(level);
        TutorialAnimation animation = manager.animations.get(id);
        if (animation == null) {
            return false;
        }
        animation.setOnCompleteOliverAction(action);
        manager.setDirty();
        if (action != null) {
            AromaAffect.LOGGER.info("Set animation {} onComplete oliver: {}", id, action);
        } else {
            AromaAffect.LOGGER.info("Cleared animation {} onComplete oliver", id);
        }
        return true;
    }

    private static TutorialAnimationManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data record
    // ─────────────────────────────────────────────────────────────────────────────

    private record AnimationData(
            String id,
            String type,
            BlockPos corner1,
            BlockPos corner2,
            boolean played,
            List<SavedBlockData> savedBlocks,
            String onCompleteWaypointId,
            String onCompleteCinematicId,
            String onCompleteOliverAction
    ) {}

    private record SavedBlockData(int x, int y, int z, String blockState) {}

    /**
     * Serializes a BlockState to a string like "minecraft:oak_stairs[facing=east,half=bottom]".
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static String serializeBlockState(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        StringBuilder sb = new StringBuilder(key.toString());

        if (!state.getValues().isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                Property prop = entry.getKey();
                sb.append(prop.getName()).append('=').append(prop.getName(entry.getValue()));
                first = false;
            }
            sb.append(']');
        }

        return sb.toString();
    }
}
