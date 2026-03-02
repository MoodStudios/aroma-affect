package com.ovrtechnology.tutorial.regenarea;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
 * Manages tutorial regeneration areas for a world.
 * <p>
 * Regen areas are stored per-world and persist across server restarts.
 */
public class TutorialRegenAreaManager extends SavedData {

    private final Map<String, TutorialRegenArea> regenAreas = new HashMap<>();

    // Codec for SavedBlockData
    private static final Codec<SavedBlockData> SAVED_BLOCK_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("x").forGetter(SavedBlockData::x),
                    Codec.INT.fieldOf("y").forGetter(SavedBlockData::y),
                    Codec.INT.fieldOf("z").forGetter(SavedBlockData::z),
                    Codec.STRING.fieldOf("blockState").forGetter(SavedBlockData::blockState)
            ).apply(instance, SavedBlockData::new)
    );

    // Codec for RegenAreaData
    private static final Codec<RegenAreaData> REGEN_AREA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(RegenAreaData::id),
                    BlockPos.CODEC.optionalFieldOf("corner1").forGetter(d -> Optional.ofNullable(d.corner1)),
                    BlockPos.CODEC.optionalFieldOf("corner2").forGetter(d -> Optional.ofNullable(d.corner2)),
                    Codec.INT.optionalFieldOf("regenDelayTicks", TutorialRegenArea.DEFAULT_REGEN_DELAY_TICKS)
                            .forGetter(RegenAreaData::regenDelayTicks),
                    Codec.BOOL.optionalFieldOf("enabled", true).forGetter(RegenAreaData::enabled),
                    SAVED_BLOCK_CODEC.listOf().optionalFieldOf("savedBlocks", List.of())
                            .forGetter(RegenAreaData::savedBlocks)
            ).apply(instance, (id, corner1, corner2, regenDelayTicks, enabled, savedBlocks) ->
                    new RegenAreaData(id, corner1.orElse(null), corner2.orElse(null),
                            regenDelayTicks, enabled, savedBlocks))
    );

    // Manager codec
    private static final Codec<TutorialRegenAreaManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    REGEN_AREA_CODEC.listOf().fieldOf("regenAreas")
                            .forGetter(TutorialRegenAreaManager::getRegenAreaDataList)
            ).apply(instance, TutorialRegenAreaManager::new)
    );

    static final SavedDataType<TutorialRegenAreaManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_regen_areas",
            TutorialRegenAreaManager::new,
            CODEC,
            null
    );

    public TutorialRegenAreaManager() {
    }

    private TutorialRegenAreaManager(List<RegenAreaData> regenAreaDataList) {
        for (RegenAreaData data : regenAreaDataList) {
            Map<BlockPos, String> savedBlocksMap = new HashMap<>();
            for (SavedBlockData sbd : data.savedBlocks) {
                savedBlocksMap.put(new BlockPos(sbd.x, sbd.y, sbd.z), sbd.blockState);
            }

            TutorialRegenArea area = new TutorialRegenArea(
                    data.id,
                    data.corner1,
                    data.corner2,
                    data.regenDelayTicks,
                    data.enabled,
                    savedBlocksMap
            );
            regenAreas.put(data.id, area);
        }
    }

    private List<RegenAreaData> getRegenAreaDataList() {
        List<RegenAreaData> list = new ArrayList<>();
        for (TutorialRegenArea area : regenAreas.values()) {
            List<SavedBlockData> savedBlockDataList = area.getSavedBlocks().entrySet().stream()
                    .map(e -> new SavedBlockData(
                            e.getKey().getX(), e.getKey().getY(), e.getKey().getZ(),
                            e.getValue()))
                    .collect(Collectors.toList());

            list.add(new RegenAreaData(
                    area.getId(),
                    area.getCorner1(),
                    area.getCorner2(),
                    area.getRegenDelayTicks(),
                    area.isEnabled(),
                    savedBlockDataList
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - RegenArea CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new regen area.
     *
     * @param level the server level
     * @param id    the regen area ID
     * @return true if created, false if already exists
     */
    public static boolean createRegenArea(ServerLevel level, String id) {
        TutorialRegenAreaManager manager = get(level);
        if (manager.regenAreas.containsKey(id)) {
            return false;
        }
        TutorialRegenArea area = new TutorialRegenArea(id);
        manager.regenAreas.put(id, area);
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial regen area {}", id);
        return true;
    }

    /**
     * Deletes a regen area.
     *
     * @param level the server level
     * @param id    the regen area ID
     * @return true if deleted, false if not found
     */
    public static boolean deleteRegenArea(ServerLevel level, String id) {
        TutorialRegenAreaManager manager = get(level);
        if (manager.regenAreas.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial regen area {}", id);
            return true;
        }
        return false;
    }

    /**
     * Gets a regen area by ID.
     *
     * @param level the server level
     * @param id    the regen area ID
     * @return Optional containing the regen area, or empty if not found
     */
    public static Optional<TutorialRegenArea> getRegenArea(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).regenAreas.get(id));
    }

    /**
     * Gets all regen area IDs.
     *
     * @param level the server level
     * @return set of regen area IDs
     */
    public static Set<String> getAllRegenAreaIds(ServerLevel level) {
        return new HashSet<>(get(level).regenAreas.keySet());
    }

    /**
     * Gets all regen areas.
     *
     * @param level the server level
     * @return list of all regen areas
     */
    public static List<TutorialRegenArea> getAllRegenAreas(ServerLevel level) {
        return new ArrayList<>(get(level).regenAreas.values());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - RegenArea Modification
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets corner1 of a regen area.
     */
    public static boolean setCorner1(ServerLevel level, String id, BlockPos pos) {
        TutorialRegenAreaManager manager = get(level);
        TutorialRegenArea area = manager.regenAreas.get(id);
        if (area == null) {
            return false;
        }
        area.setCorner1(pos);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set regen area {} corner1 to {}", id, pos);
        return true;
    }

    /**
     * Sets corner2 of a regen area.
     */
    public static boolean setCorner2(ServerLevel level, String id, BlockPos pos) {
        TutorialRegenAreaManager manager = get(level);
        TutorialRegenArea area = manager.regenAreas.get(id);
        if (area == null) {
            return false;
        }
        area.setCorner2(pos);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set regen area {} corner2 to {}", id, pos);
        return true;
    }

    /**
     * Sets the regeneration delay for a regen area.
     */
    public static boolean setRegenDelay(ServerLevel level, String id, int ticks) {
        TutorialRegenAreaManager manager = get(level);
        TutorialRegenArea area = manager.regenAreas.get(id);
        if (area == null) {
            return false;
        }
        area.setRegenDelayTicks(ticks);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set regen area {} delay to {} ticks", id, ticks);
        return true;
    }

    /**
     * Enables or disables a regen area.
     */
    public static boolean setEnabled(ServerLevel level, String id, boolean enabled) {
        TutorialRegenAreaManager manager = get(level);
        TutorialRegenArea area = manager.regenAreas.get(id);
        if (area == null) {
            return false;
        }
        area.setEnabled(enabled);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set regen area {} enabled: {}", id, enabled);
        return true;
    }

    /**
     * Snapshots all non-air blocks in a regen area for restoration.
     */
    public static boolean snapshotBlocks(ServerLevel level, String id) {
        TutorialRegenAreaManager manager = get(level);
        TutorialRegenArea area = manager.regenAreas.get(id);
        if (area == null || !area.isComplete()) {
            return false;
        }

        BlockPos corner1 = area.getCorner1();
        BlockPos corner2 = area.getCorner2();

        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        Map<BlockPos, String> snapshot = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        snapshot.put(pos, serializeBlockState(state));
                    }
                }
            }
        }

        area.setSavedBlocks(snapshot);
        manager.setDirty();
        AromaAffect.LOGGER.info("Snapshotted {} blocks for regen area {}", snapshot.size(), id);
        return true;
    }

    /**
     * Restores all blocks in a regen area from the snapshot.
     */
    public static int restoreAllBlocks(ServerLevel level, String id) {
        TutorialRegenAreaManager manager = get(level);
        TutorialRegenArea area = manager.regenAreas.get(id);
        if (area == null || !area.hasSavedBlocks()) {
            return 0;
        }

        int restored = 0;
        for (Map.Entry<BlockPos, String> entry : area.getSavedBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            String stateString = entry.getValue();

            // Only restore if the block is currently air
            if (level.getBlockState(pos).isAir()) {
                try {
                    BlockState blockState = BlockStateParser.parseForBlock(
                            level.holderLookup(Registries.BLOCK),
                            stateString,
                            false
                    ).blockState();
                    level.setBlock(pos, blockState, Block.UPDATE_ALL);
                    restored++;
                } catch (Exception e) {
                    AromaAffect.LOGGER.warn("Failed to restore block at {} with state '{}': {}",
                            pos, stateString, e.getMessage());
                }
            }
        }

        if (restored > 0) {
            AromaAffect.LOGGER.info("Restored {} blocks for regen area {}", restored, id);
        }
        return restored;
    }

    /**
     * Restores all blocks in ALL regen areas from their snapshots.
     * Used during tutorial reset.
     *
     * @param level the server level
     * @return total number of blocks restored
     */
    public static int restoreAllAreas(ServerLevel level) {
        int totalRestored = 0;
        for (String id : getAllRegenAreaIds(level)) {
            totalRestored += restoreAllBlocks(level, id);
        }
        if (totalRestored > 0) {
            AromaAffect.LOGGER.info("Tutorial reset: restored {} total blocks across all regen areas", totalRestored);
        }
        return totalRestored;
    }

    /**
     * Finds the regen area that contains a given position.
     *
     * @param level the server level
     * @param pos   the position to check
     * @return Optional containing the regen area, or empty if not found
     */
    public static Optional<TutorialRegenArea> findAreaContaining(ServerLevel level, BlockPos pos) {
        for (TutorialRegenArea area : get(level).regenAreas.values()) {
            if (area.isEnabled() && area.isComplete() && area.isInsideArea(pos)) {
                return Optional.of(area);
            }
        }
        return Optional.empty();
    }

    /**
     * Marks the manager as dirty (needs saving).
     */
    public static void markDirty(ServerLevel level) {
        get(level).setDirty();
    }

    private static TutorialRegenAreaManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data records
    // ─────────────────────────────────────────────────────────────────────────────

    private record RegenAreaData(
            String id,
            BlockPos corner1,
            BlockPos corner2,
            int regenDelayTicks,
            boolean enabled,
            List<SavedBlockData> savedBlocks
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
