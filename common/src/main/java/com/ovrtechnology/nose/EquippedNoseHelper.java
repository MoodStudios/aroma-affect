package com.ovrtechnology.nose;

import com.ovrtechnology.variant.CustomNoseItem;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EquippedNoseHelper {

    private EquippedNoseHelper() {}

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

    public static NoseAbilityResolver.ResolvedAbilities getEquippedAbilities(Player player) {
        if (player == null) return NoseAbilityResolver.ResolvedAbilities.EMPTY;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return NoseAbilityResolver.ResolvedAbilities.EMPTY;

        if (head.getItem() instanceof NoseItem noseItem) {
            if (!noseItem.getDefinition().isEnabled())
                return NoseAbilityResolver.ResolvedAbilities.EMPTY;
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
        return head.getItem() instanceof CustomNoseItem
                && CustomNoseItem.getVariantId(head).isPresent();
    }

    public static boolean canDetectBlock(Player player, String blockId) {
        return getEquippedAbilities(player).canDetectBlock(blockId);
    }

    public static boolean canDetectBiome(Player player, String biomeId) {
        return getEquippedAbilities(player).canDetectBiome(biomeId);
    }

    public static boolean canDetectStructure(Player player, String structureId) {
        return getEquippedAbilities(player).canDetectStructure(structureId);
    }

    public static boolean canDetectFlower(Player player, String flowerId) {
        return getEquippedAbilities(player).canDetectFlower(flowerId);
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
