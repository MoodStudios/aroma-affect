package com.ovrtechnology.nose;

import com.ovrtechnology.AromaAffect;
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

/**
 * Base item class for all nose equipment in Aroma Affect.
 * Noses are equippable items that provide scent detection abilities.
 * 
 * The nose is worn in the head slot like a helmet and provides different detection
 * capabilities based on its tier and unlock configuration.
 */
public class NoseItem extends Item {
    
    /**
     * The definition that was used to create this nose item
     */
    @Getter
    private final NoseDefinition definition;
    
    /**
     * The item ID for this nose
     */
    @Getter
    private final String itemId;
    
    /**
     * Create a new nose item from a definition
     * @param definition The nose definition from JSON
     * @param itemId The item ID for this nose (used for ResourceKey)
     */
    public NoseItem(NoseDefinition definition, String itemId) {
        super(createProperties(definition, itemId));
        this.definition = definition;
        this.itemId = itemId;
    }
    
    /**
     * Create item properties from a nose definition
     */
    private static Properties createProperties(NoseDefinition definition, String itemId) {
        Properties properties = new Properties();
        
        // Set the item ID - REQUIRED in Minecraft 1.21.x
        properties.setId(ResourceKey.create(
                Registries.ITEM, 
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, itemId)
        ));
        
        // Set durability
        properties.durability(definition.getDurability());
        
        // Set stack size to 1 (noses are unique equipment)
        properties.stacksTo(1);
        
        // Set rarity based on tier
        properties.rarity(getRarityForTier(definition.getTier()));
        
        // Configure the equippable component with the proper equipment asset
        // This makes the nose show as a helmet when worn, but use the custom icon in inventory
        ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> equipmentAsset = 
                getEquipmentAssetFromModel(definition.getModel());
        
        Equippable equippable = Equippable.builder(EquipmentSlot.HEAD)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                .setAsset(equipmentAsset)
                .setSwappable(false)
                .setDamageOnHurt(true)
                .build();
        
        properties.component(DataComponents.EQUIPPABLE, equippable);

        // Add Repairable component for anvil repair
        String repairId = definition.getRepair();
        if (repairId != null && !repairId.isEmpty()) {
            ResourceLocation repairLoc = ResourceLocation.parse(repairId);
            BuiltInRegistries.ITEM.getOptional(repairLoc).ifPresent(repairItem -> {
                Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(repairItem);
                properties.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(holder)));
            });
        }

        return properties;
    }
    
    /**
     * Convert a model string (e.g., "minecraft:iron_helmet") to an equipment asset key.
     * This maps helmet model names to their corresponding equipment asset.
     */
    private static ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> getEquipmentAssetFromModel(String model) {
        if (model == null || model.isEmpty()) {
            return EquipmentAssets.IRON;
        }
        
        // Extract just the material name from the model
        // e.g., "minecraft:iron_helmet" -> "iron"
        // e.g., "minecraft:golden_helmet" -> "gold"
        // e.g., "minecraft:diamond_helmet" -> "diamond"
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
            // Default to iron
            return EquipmentAssets.IRON;
        }
    }
    
    /**
     * Get the Minecraft rarity based on nose tier
     */
    private static Rarity getRarityForTier(int tier) {
        return switch (tier) {
            case 1 -> Rarity.COMMON;
            case 2 -> Rarity.UNCOMMON;
            case 3 -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }
    
    /**
     * Allow right-click to equip the nose to head slot
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        
        if (headStack.isEmpty()) {
            // Equip to head slot
            player.setItemSlot(EquipmentSlot.HEAD, heldStack.copy());
            if (!level.isClientSide()) {
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            heldStack.setCount(0);
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        } else if (headStack.getItem() instanceof NoseItem) {
            // Swap with current nose
            player.setItemSlot(EquipmentSlot.HEAD, heldStack.copy());
            player.setItemInHand(hand, headStack.copy());
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
    
    // ========== Nose-specific Methods ==========
    
    /**
     * Get the tier of this nose
     */
    public int getTier() {
        return definition.getTier();
    }
    
    /**
     * Get the unlock configuration for this nose
     */
    public NoseUnlock getUnlock() {
        return definition.getUnlock();
    }
    
    /**
     * Get resolved abilities for this nose (including inherited from other noses)
     */
    public NoseAbilityResolver.ResolvedAbilities getResolvedAbilities() {
        return NoseAbilityResolver.getResolvedAbilities(definition.getId());
    }
    
    /**
     * Check if this nose can detect a specific block (including inherited abilities)
     */
    public boolean canDetectBlock(String blockId) {
        return NoseAbilityResolver.canDetectBlock(definition.getId(), blockId);
    }
    
    /**
     * Check if this nose can detect a specific biome (including inherited abilities)
     */
    public boolean canDetectBiome(String biomeId) {
        return NoseAbilityResolver.canDetectBiome(definition.getId(), biomeId);
    }
    
    /**
     * Check if this nose can detect a specific structure (including inherited abilities)
     */
    public boolean canDetectStructure(String structureId) {
        return NoseAbilityResolver.canDetectStructure(definition.getId(), structureId);
    }

    /**
     * Check if this nose can detect a specific flower (including inherited abilities)
     */
    public boolean canDetectFlower(String flowerId) {
        return NoseAbilityResolver.canDetectFlower(definition.getId(), flowerId);
    }

    /**
     * Check if this nose has a specific ability (including inherited abilities)
     */
    public boolean hasAbility(String abilityId) {
        return NoseAbilityResolver.hasAbility(definition.getId(), abilityId);
    }
    
    /**
     * Get the repair item ID for this nose
     */
    public String getRepairItemId() {
        return definition.getRepair();
    }
    
    /**
     * Get the enchantment value based on tier
     * Higher tier = more enchantable
     */
    public int getNoseEnchantmentValue() {
        return 5 + (definition.getTier() * 5);
    }
}
