package com.ovrtechnology.nose;

import com.ovrtechnology.util.Ids;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class NoseItem extends Item implements Equipable {

    @Getter private final NoseDefinition definition;

    @Getter private final String itemId;

    public NoseItem(NoseDefinition definition, String itemId) {
        super(createProperties(definition));
        this.definition = definition;
        this.itemId = itemId;
    }

    private static Properties createProperties(NoseDefinition definition) {
        Properties properties = new Properties();
        properties.durability(definition.getDurability());
        properties.stacksTo(1);
        properties.rarity(getRarityForTier(definition.getTier()));
        return properties;
    }

    private static Rarity getRarityForTier(int tier) {
        return switch (tier) {
            case 1 -> Rarity.COMMON;
            case 2 -> Rarity.UNCOMMON;
            case 3 -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        String repairId = definition.getRepair();
        if (repairId == null || repairId.isEmpty()) return false;
        ResourceLocation repairLoc = Ids.parse(repairId);
        return BuiltInRegistries.ITEM.getOptional(repairLoc).map(ingredient::is).orElse(false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);

        if (headStack.isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, heldStack.copy());
            if (!level.isClientSide()) {
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            heldStack.setCount(0);
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            return InteractionResultHolder.success(heldStack);
        } else {
            player.setItemSlot(EquipmentSlot.HEAD, heldStack.copy());
            player.setItemInHand(hand, headStack.copy());
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            return InteractionResultHolder.success(heldStack);
        }
    }

    public int getTier() {
        return definition.getTier();
    }

    public NoseUnlock getUnlock() {
        return definition.getUnlock();
    }

    public NoseAbilityResolver.ResolvedAbilities getResolvedAbilities() {
        return NoseAbilityResolver.getResolvedAbilities(definition.getId());
    }

    public boolean canDetectBlock(String blockId) {
        return NoseAbilityResolver.canDetectBlock(definition.getId(), blockId);
    }

    public boolean canDetectBiome(String biomeId) {
        return NoseAbilityResolver.canDetectBiome(definition.getId(), biomeId);
    }

    public boolean canDetectStructure(String structureId) {
        return NoseAbilityResolver.canDetectStructure(definition.getId(), structureId);
    }

    public boolean canDetectFlower(String flowerId) {
        return NoseAbilityResolver.canDetectFlower(definition.getId(), flowerId);
    }

    public boolean hasAbility(String abilityId) {
        return NoseAbilityResolver.hasAbility(definition.getId(), abilityId);
    }
}
