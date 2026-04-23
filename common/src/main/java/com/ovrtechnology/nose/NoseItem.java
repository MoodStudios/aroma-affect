package com.ovrtechnology.nose;

import com.ovrtechnology.util.Ids;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

public class NoseItem extends Item {

    @Getter private final NoseDefinition definition;

    @Getter private final String itemId;

    public NoseItem(NoseDefinition definition, String itemId) {
        super(createProperties(definition, itemId));
        this.definition = definition;
        this.itemId = itemId;
    }

    private static Properties createProperties(NoseDefinition definition, String itemId) {
        Properties properties = new Properties();

        properties.setId(ResourceKey.create(Registries.ITEM, Ids.mod(itemId)));

        properties.durability(definition.getDurability());

        properties.stacksTo(1);

        properties.rarity(getRarityForTier(definition.getTier()));

        ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> equipmentAsset =
                getEquipmentAssetFromModel(definition.getModel());

        Equippable equippable =
                Equippable.builder(EquipmentSlot.HEAD)
                        .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                        .setAsset(equipmentAsset)
                        .setSwappable(true)
                        .setDamageOnHurt(true)
                        .build();

        properties.component(DataComponents.EQUIPPABLE, equippable);

        String repairId = definition.getRepair();
        if (repairId != null && !repairId.isEmpty()) {
            ResourceLocation repairLoc = Ids.parse(repairId);
            BuiltInRegistries.ITEM
                    .getOptional(repairLoc)
                    .ifPresent(
                            repairItem -> {
                                Holder<Item> holder =
                                        BuiltInRegistries.ITEM.wrapAsHolder(repairItem);
                                properties.component(
                                        DataComponents.REPAIRABLE,
                                        new Repairable(HolderSet.direct(holder)));
                            });
        }

        return properties;
    }

    private static ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset>
            getEquipmentAssetFromModel(String model) {
        if (model == null || model.isEmpty()) {
            return EquipmentAssets.IRON;
        }

        String lowerModel = model.toLowerCase();

        if (lowerModel.contains("diamond")) {
            return EquipmentAssets.DIAMOND;
        } else if (lowerModel.contains("gold")) {
            return EquipmentAssets.GOLD;
        } else if (lowerModel.contains("netherite")) {
            return EquipmentAssets.NETHERITE;
        } else if (lowerModel.contains("leather")) {
            return EquipmentAssets.LEATHER;
        } else if (lowerModel.contains("chain")) {
            return EquipmentAssets.CHAINMAIL;
        } else {

            return EquipmentAssets.IRON;
        }
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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);

        if (headStack.isEmpty()) {

            player.setItemSlot(EquipmentSlot.HEAD, heldStack.copy());
            if (!level.isClientSide()) {
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            heldStack.setCount(0);
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        } else {

            player.setItemSlot(EquipmentSlot.HEAD, heldStack.copy());
            player.setItemInHand(hand, headStack.copy());
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
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
