package com.ovrtechnology.nose;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Utility class for detecting and working with the player's equipped nose.
 *
 * <p>Noses are worn in the HEAD equipment slot. This helper provides methods to:
 * <ul>
 *   <li>Check if a player has a nose equipped</li>
 *   <li>Get the equipped nose's definition and abilities</li>
 *   <li>Check detection capabilities for blocks, biomes, structures, and flowers</li>
 * </ul>
 */
public final class EquippedNoseHelper {

    private EquippedNoseHelper() {
        // Utility class
    }

    /**
     * Gets the NoseItem equipped by the player, if any.
     *
     * @param player the player to check
     * @return Optional containing the NoseItem, or empty if no nose is equipped
     */
    public static Optional<NoseItem> getEquippedNose(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.isEmpty()) {
            return Optional.empty();
        }

        if (headStack.getItem() instanceof NoseItem noseItem) {
            return Optional.of(noseItem);
        }

        return Optional.empty();
    }

    /**
     * Gets the NoseDefinition of the equipped nose, if any.
     *
     * @param player the player to check
     * @return Optional containing the NoseDefinition, or empty if no nose is equipped
     */
    public static Optional<NoseDefinition> getEquippedNoseDefinition(Player player) {
        return getEquippedNose(player).map(NoseItem::getDefinition);
    }

    /**
     * Gets the resolved abilities of the equipped nose (including inherited abilities).
     *
     * @param player the player to check
     * @return the resolved abilities, or EMPTY if no nose is equipped
     */
    public static NoseAbilityResolver.ResolvedAbilities getEquippedAbilities(Player player) {
        return getEquippedNose(player)
                .map(NoseItem::getResolvedAbilities)
                .orElse(NoseAbilityResolver.ResolvedAbilities.EMPTY);
    }

    /**
     * Checks if the player has any nose equipped.
     *
     * @param player the player to check
     * @return true if a nose is equipped
     */
    public static boolean hasNoseEquipped(Player player) {
        return getEquippedNose(player).isPresent();
    }

    /**
     * Checks if the player's equipped nose can detect a specific block.
     *
     * @param player  the player to check
     * @param blockId the block ID (e.g., "minecraft:diamond_ore")
     * @return true if the nose can detect this block
     */
    public static boolean canDetectBlock(Player player, String blockId) {
        return getEquippedAbilities(player).canDetectBlock(blockId);
    }

    /**
     * Checks if the player's equipped nose can detect a specific biome.
     *
     * @param player  the player to check
     * @param biomeId the biome ID (e.g., "minecraft:plains")
     * @return true if the nose can detect this biome
     */
    public static boolean canDetectBiome(Player player, String biomeId) {
        return getEquippedAbilities(player).canDetectBiome(biomeId);
    }

    /**
     * Checks if the player's equipped nose can detect a specific structure.
     *
     * @param player      the player to check
     * @param structureId the structure ID (e.g., "minecraft:village_plains")
     * @return true if the nose can detect this structure
     */
    public static boolean canDetectStructure(Player player, String structureId) {
        return getEquippedAbilities(player).canDetectStructure(structureId);
    }

    /**
     * Checks if the player's equipped nose can detect a specific flower.
     *
     * @param player   the player to check
     * @param flowerId the flower block ID (e.g., "minecraft:dandelion")
     * @return true if the nose can detect this flower
     */
    public static boolean canDetectFlower(Player player, String flowerId) {
        return getEquippedAbilities(player).canDetectFlower(flowerId);
    }

    /**
     * Gets the tier of the equipped nose.
     *
     * @param player the player to check
     * @return the nose tier (1-6), or 0 if no nose is equipped
     */
    public static int getEquippedNoseTier(Player player) {
        return getEquippedNose(player)
                .map(NoseItem::getTier)
                .orElse(0);
    }

    /**
     * Gets the ID of the equipped nose.
     *
     * @param player the player to check
     * @return Optional containing the nose ID, or empty if no nose is equipped
     */
    public static Optional<String> getEquippedNoseId(Player player) {
        return getEquippedNoseDefinition(player).map(NoseDefinition::getId);
    }
}
