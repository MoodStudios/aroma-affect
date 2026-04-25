package com.ovrtechnology.nose;

import com.ovrtechnology.nose.accessory.NoseAccessory;
import com.ovrtechnology.variant.CustomNoseItem;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EquippedNoseHelper {

    private EquippedNoseHelper() {}

    public static ItemStack getEquippedStack(Player player) {
        if (player == null) return ItemStack.EMPTY;
        return NoseAccessory.getEquipped(player);
    }

    public static Optional<NoseItem> getEquippedNose(Player player) {
        ItemStack stack = getEquippedStack(player);
        if (stack.isEmpty()) return Optional.empty();

        if (stack.getItem() instanceof NoseItem noseItem) {
            if (!noseItem.getDefinition().isEnabled()) {
                return Optional.empty();
            }
            return Optional.of(noseItem);
        }
        return Optional.empty();
    }

    public static NoseAbilityResolver.ResolvedAbilities getEquippedAbilities(Player player) {
        ItemStack stack = getEquippedStack(player);
        if (stack.isEmpty()) return NoseAbilityResolver.ResolvedAbilities.EMPTY;

        if (stack.getItem() instanceof NoseItem noseItem) {
            if (!noseItem.getDefinition().isEnabled())
                return NoseAbilityResolver.ResolvedAbilities.EMPTY;
            return noseItem.getResolvedAbilities();
        }
        if (stack.getItem() instanceof CustomNoseItem) {
            Optional<ResourceLocation> vid = CustomNoseItem.getVariantId(stack);
            if (vid.isPresent()) {
                return NoseAbilityResolver.getResolvedAbilities(vid.get().toString());
            }
        }
        return NoseAbilityResolver.ResolvedAbilities.EMPTY;
    }

    public static boolean hasNoseEquipped(Player player) {
        ItemStack stack = getEquippedStack(player);
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof NoseItem noseItem) {
            return noseItem.getDefinition().isEnabled();
        }
        return stack.getItem() instanceof CustomNoseItem
                && CustomNoseItem.getVariantId(stack).isPresent();
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
        ItemStack stack = getEquippedStack(player);
        if (stack.isEmpty()) return Optional.empty();
        if (stack.getItem() instanceof NoseItem noseItem) {
            if (!noseItem.getDefinition().isEnabled()) return Optional.empty();
            return Optional.of(noseItem.getDefinition().getId());
        }
        if (stack.getItem() instanceof CustomNoseItem) {
            return CustomNoseItem.getVariantId(stack).map(ResourceLocation::toString);
        }
        return Optional.empty();
    }
}
