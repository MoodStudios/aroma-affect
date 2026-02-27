package com.ovrtechnology.tutorial.chest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages tutorial chests for a world.
 * <p>
 * Chests are stored per-world and persist across server restarts.
 */
public class TutorialChestManager extends SavedData {

    private final Map<String, TutorialChest> chests = new HashMap<>();

    // Codec for ItemStack (simplified - stores item ID and count)
    private static final Codec<ItemStackData> ITEM_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("item").forGetter(ItemStackData::itemId),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(ItemStackData::count),
                    Codec.STRING.optionalFieldOf("nbt").forGetter(d -> Optional.ofNullable(d.nbt))
            ).apply(instance, (itemId, count, nbt) -> new ItemStackData(itemId, count, nbt.orElse(null)))
    );

    // Codec for ChestData
    private static final Codec<ChestData> CHEST_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(ChestData::id),
                    BlockPos.CODEC.fieldOf("position").forGetter(ChestData::position),
                    ITEM_CODEC.listOf().fieldOf("rewards").forGetter(ChestData::rewards),
                    Codec.STRING.optionalFieldOf("activateWaypointId").forGetter(d -> Optional.ofNullable(d.activateWaypointId)),
                    Codec.STRING.optionalFieldOf("activateCinematicId").forGetter(d -> Optional.ofNullable(d.activateCinematicId)),
                    Codec.BOOL.optionalFieldOf("consumed", false).forGetter(ChestData::consumed)
            ).apply(instance, (id, position, rewards, waypointId, cinematicId, consumed) ->
                    new ChestData(id, position, rewards, waypointId.orElse(null), cinematicId.orElse(null), consumed))
    );

    // Manager codec
    private static final Codec<TutorialChestManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CHEST_CODEC.listOf().fieldOf("chests").forGetter(TutorialChestManager::getChestDataList)
            ).apply(instance, TutorialChestManager::new)
    );

    static final SavedDataType<TutorialChestManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_chests",
            TutorialChestManager::new,
            CODEC,
            null
    );

    public TutorialChestManager() {
    }

    private TutorialChestManager(List<ChestData> chestDataList) {
        for (ChestData data : chestDataList) {
            List<ItemStack> rewards = new ArrayList<>();
            for (ItemStackData itemData : data.rewards) {
                ItemStack stack = itemData.toItemStack();
                if (!stack.isEmpty()) {
                    rewards.add(stack);
                }
            }

            TutorialChest chest = new TutorialChest(
                    data.id,
                    data.position,
                    rewards,
                    data.activateWaypointId,
                    data.activateCinematicId,
                    data.consumed
            );
            chests.put(data.id, chest);
        }
    }

    private List<ChestData> getChestDataList() {
        List<ChestData> list = new ArrayList<>();
        for (TutorialChest chest : chests.values()) {
            List<ItemStackData> rewards = new ArrayList<>();
            for (ItemStack stack : chest.getRewards()) {
                rewards.add(ItemStackData.fromItemStack(stack));
            }

            list.add(new ChestData(
                    chest.getId(),
                    chest.getPosition(),
                    rewards,
                    chest.getActivateWaypointId(),
                    chest.getActivateCinematicId(),
                    chest.isConsumed()
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Chest CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new chest at the given position.
     *
     * @param level    the server level
     * @param id       the chest ID
     * @param position the position
     * @return true if created, false if already exists
     */
    public static boolean createChest(ServerLevel level, String id, BlockPos position) {
        TutorialChestManager manager = get(level);
        if (manager.chests.containsKey(id)) {
            return false;
        }
        TutorialChest chest = new TutorialChest(id);
        chest.setPosition(position);
        manager.chests.put(id, chest);
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial chest {} at {}", id, position);
        return true;
    }

    /**
     * Deletes a chest.
     *
     * @param level the server level
     * @param id    the chest ID
     * @return true if deleted, false if not found
     */
    public static boolean deleteChest(ServerLevel level, String id) {
        TutorialChestManager manager = get(level);
        if (manager.chests.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial chest {}", id);
            return true;
        }
        return false;
    }

    /**
     * Gets a chest by ID.
     *
     * @param level the server level
     * @param id    the chest ID
     * @return Optional containing the chest, or empty if not found
     */
    public static Optional<TutorialChest> getChest(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).chests.get(id));
    }

    /**
     * Gets a chest at a specific position.
     *
     * @param level    the server level
     * @param position the position
     * @return Optional containing the chest, or empty if not found
     */
    public static Optional<TutorialChest> getChestAt(ServerLevel level, BlockPos position) {
        for (TutorialChest chest : get(level).chests.values()) {
            if (chest.getPosition().equals(position)) {
                return Optional.of(chest);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all chest IDs.
     *
     * @param level the server level
     * @return set of chest IDs
     */
    public static Set<String> getAllChestIds(ServerLevel level) {
        return new HashSet<>(get(level).chests.keySet());
    }

    /**
     * Gets all chests.
     *
     * @param level the server level
     * @return list of all chests
     */
    public static List<TutorialChest> getAllChests(ServerLevel level) {
        return new ArrayList<>(get(level).chests.values());
    }

    /**
     * Gets all unconsumed chests.
     *
     * @param level the server level
     * @return list of unconsumed chests
     */
    public static List<TutorialChest> getUnconsumedChests(ServerLevel level) {
        List<TutorialChest> result = new ArrayList<>();
        for (TutorialChest chest : get(level).chests.values()) {
            if (!chest.isConsumed()) {
                result.add(chest);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Chest Modification
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the position of a chest.
     *
     * @param level    the server level
     * @param id       the chest ID
     * @param position the new position
     * @return true if set, false if chest not found
     */
    public static boolean setPosition(ServerLevel level, String id, BlockPos position) {
        TutorialChestManager manager = get(level);
        TutorialChest chest = manager.chests.get(id);
        if (chest == null) {
            return false;
        }
        chest.setPosition(position);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set chest {} position to {}", id, position);
        return true;
    }

    /**
     * Adds a reward to a chest.
     *
     * @param level the server level
     * @param id    the chest ID
     * @param item  the item to add
     * @return true if added, false if chest not found
     */
    public static boolean addReward(ServerLevel level, String id, ItemStack item) {
        TutorialChestManager manager = get(level);
        TutorialChest chest = manager.chests.get(id);
        if (chest == null) {
            return false;
        }
        chest.addReward(item);
        manager.setDirty();
        AromaAffect.LOGGER.info("Added reward to chest {}: {}", id, item);
        return true;
    }

    /**
     * Clears all rewards from a chest.
     *
     * @param level the server level
     * @param id    the chest ID
     * @return true if cleared, false if chest not found
     */
    public static boolean clearRewards(ServerLevel level, String id) {
        TutorialChestManager manager = get(level);
        TutorialChest chest = manager.chests.get(id);
        if (chest == null) {
            return false;
        }
        chest.clearRewards();
        manager.setDirty();
        AromaAffect.LOGGER.info("Cleared rewards from chest {}", id);
        return true;
    }

    /**
     * Sets the waypoint to activate when chest is opened.
     *
     * @param level      the server level
     * @param id         the chest ID
     * @param waypointId the waypoint ID (null to clear)
     * @return true if set, false if chest not found
     */
    public static boolean setActivateWaypoint(ServerLevel level, String id, String waypointId) {
        TutorialChestManager manager = get(level);
        TutorialChest chest = manager.chests.get(id);
        if (chest == null) {
            return false;
        }
        chest.setActivateWaypointId(waypointId);
        manager.setDirty();
        if (waypointId != null) {
            AromaAffect.LOGGER.info("Set chest {} activate waypoint: {}", id, waypointId);
        } else {
            AromaAffect.LOGGER.info("Cleared chest {} activate waypoint", id);
        }
        return true;
    }

    /**
     * Sets the cinematic to activate when chest is opened.
     *
     * @param level       the server level
     * @param id          the chest ID
     * @param cinematicId the cinematic ID (null to clear)
     * @return true if set, false if chest not found
     */
    public static boolean setActivateCinematic(ServerLevel level, String id, String cinematicId) {
        TutorialChestManager manager = get(level);
        TutorialChest chest = manager.chests.get(id);
        if (chest == null) {
            return false;
        }
        chest.setActivateCinematicId(cinematicId);
        manager.setDirty();
        if (cinematicId != null) {
            AromaAffect.LOGGER.info("Set chest {} activate cinematic: {}", id, cinematicId);
        } else {
            AromaAffect.LOGGER.info("Cleared chest {} activate cinematic", id);
        }
        return true;
    }

    /**
     * Marks a chest as consumed.
     *
     * @param level the server level
     * @param id    the chest ID
     * @return true if consumed, false if chest not found
     */
    public static boolean consumeChest(ServerLevel level, String id) {
        TutorialChestManager manager = get(level);
        TutorialChest chest = manager.chests.get(id);
        if (chest == null) {
            return false;
        }
        chest.consume();
        manager.setDirty();
        AromaAffect.LOGGER.info("Consumed chest {}", id);
        return true;
    }

    /**
     * Resets a chest to unconsumed state.
     *
     * @param level the server level
     * @param id    the chest ID
     * @return true if reset, false if chest not found
     */
    public static boolean resetChest(ServerLevel level, String id) {
        TutorialChestManager manager = get(level);
        TutorialChest chest = manager.chests.get(id);
        if (chest == null) {
            return false;
        }
        chest.reset();
        manager.setDirty();
        AromaAffect.LOGGER.info("Reset chest {}", id);
        return true;
    }

    /**
     * Resets all chests to unconsumed state.
     *
     * @param level the server level
     * @return number of chests reset
     */
    public static int resetAllChests(ServerLevel level) {
        TutorialChestManager manager = get(level);
        int count = 0;
        for (TutorialChest chest : manager.chests.values()) {
            if (chest.isConsumed()) {
                chest.reset();
                count++;
            }
        }
        if (count > 0) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Reset {} chests", count);
        }
        return count;
    }

    private static TutorialChestManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data records
    // ─────────────────────────────────────────────────────────────────────────────

    private record ItemStackData(String itemId, int count, String nbt) {
        static ItemStackData fromItemStack(ItemStack stack) {
            String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
            // For now, we only store item ID and count
            // NBT/components would require more complex serialization
            return new ItemStackData(itemId, stack.getCount(), null);
        }

        ItemStack toItemStack() {
            try {
                var itemOpt = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getOptional(net.minecraft.resources.ResourceLocation.parse(itemId));
                if (itemOpt.isPresent()) {
                    return new ItemStack(itemOpt.get(), count);
                }
            } catch (Exception e) {
                AromaAffect.LOGGER.warn("Failed to deserialize item: {}", itemId, e);
            }
            return ItemStack.EMPTY;
        }
    }

    private record ChestData(
            String id,
            BlockPos position,
            List<ItemStackData> rewards,
            String activateWaypointId,
            String activateCinematicId,
            boolean consumed
    ) {}
}
