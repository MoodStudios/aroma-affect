package com.ovrtechnology.nose;

import com.ovrtechnology.variant.CustomNoseItem;
import com.ovrtechnology.variant.NoseVariant;
import com.ovrtechnology.variant.NoseVariantRegistry;
import net.minecraft.resources.ResourceLocation;
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
            if (!noseItem.getDefinition().isEnabled()) {
                return Optional.empty();
            }
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

    public static NoseAbilityResolver.ResolvedAbilities getEquippedAbilities(Player player) {
        if (player == null) return NoseAbilityResolver.ResolvedAbilities.EMPTY;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return NoseAbilityResolver.ResolvedAbilities.EMPTY;

        if (head.getItem() instanceof NoseItem noseItem) {
            if (!noseItem.getDefinition().isEnabled()) return NoseAbilityResolver.ResolvedAbilities.EMPTY;
            return noseItem.getResolvedAbilities();
        }
        if (head.getItem() instanceof CustomNoseItem) {
            Optional<ResourceLocation> vid = CustomNoseItem.getVariantId(head);
            if (vid.isPresent()) {
                return NoseAbilityResolver.getResolvedAbilities(vid.get().toString());
            }
        }
        return NoseAbilityResolver.ResolvedAbilities.EMPTY;
    }

    public static boolean hasNoseEquipped(Player player) {
        if (player == null) return false;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return false;
        if (head.getItem() instanceof NoseItem noseItem) {
            return noseItem.getDefinition().isEnabled();
        }
        return head.getItem() instanceof CustomNoseItem && CustomNoseItem.getVariantId(head).isPresent();
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

    public static int getEquippedNoseTier(Player player) {
        if (player == null) return 0;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return 0;
        if (head.getItem() instanceof NoseItem noseItem) {
            if (!noseItem.getDefinition().isEnabled()) return 0;
            return noseItem.getTier();
        }
        if (head.getItem() instanceof CustomNoseItem) {
            return CustomNoseItem.getVariant(head).map(NoseVariant::getTier).orElse(0);
        }
        return 0;
    }

    public static Optional<String> getEquippedNoseId(Player player) {
        if (player == null) return Optional.empty();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return Optional.empty();
        if (head.getItem() instanceof NoseItem noseItem) {
            if (!noseItem.getDefinition().isEnabled()) return Optional.empty();
            return Optional.of(noseItem.getDefinition().getId());
        }
        if (head.getItem() instanceof CustomNoseItem) {
            return CustomNoseItem.getVariantId(head).map(ResourceLocation::toString);
        }
        return Optional.empty();
    }
}
