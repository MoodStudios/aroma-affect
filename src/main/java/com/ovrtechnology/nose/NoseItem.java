package com.ovrtechnology.nose;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.accessory.NoseAccessory;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, itemId)
        ));

        // Set durability
        properties.durability(definition.getDurability());

        // Set stack size to 1 (noses are unique equipment)
        properties.stacksTo(1);

        // Set rarity based on tier
        properties.rarity(getRarityForTier(definition.getTier()));

        // Equippable HEAD slot: noses always work in the vanilla helmet slot.
        // On NeoForge, Curios provides an additional "face" accessory slot when
        // installed; NoseAccessory routes equip/lookup to whichever is appropriate.
        ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> equipmentAsset =
                getEquipmentAssetFromModel(definition.getModel());
        Equippable equippable = Equippable.builder(EquipmentSlot.HEAD)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                .setAsset(equipmentAsset)
                .setSwappable(true)
                .setDamageOnHurt(true)
                .build();
        properties.component(DataComponents.EQUIPPABLE, equippable);

        // Add Repairable component for anvil repair
        String repairId = definition.getRepair();
        if (repairId != null && !repairId.isEmpty()) {
            Identifier repairLoc = Identifier.parse(repairId);
            BuiltInRegistries.ITEM.getOptional(repairLoc).ifPresent(repairItem -> {
                Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(repairItem);
                properties.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(holder)));
            });
        }

        return properties;
    }

    /**
     * Map a helmet-style model id (e.g. "minecraft:iron_helmet") to its equipment asset key.
     */
    private static ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> getEquipmentAssetFromModel(String model) {
        if (model == null || model.isEmpty()) return EquipmentAssets.IRON;
        String lower = model.toLowerCase();
        if (lower.contains("diamond")) return EquipmentAssets.DIAMOND;
        if (lower.contains("gold")) return EquipmentAssets.GOLD;
        if (lower.contains("netherite")) return EquipmentAssets.NETHERITE;
        if (lower.contains("leather")) return EquipmentAssets.LEATHER;
        if (lower.contains("chain")) return EquipmentAssets.CHAINMAIL;
        return EquipmentAssets.IRON;
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
     * Right-click routes the nose into the platform-specific accessory slot
     * (Curios face slot on NeoForge, vanilla HEAD slot on Fabric).
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);

        if (!NoseAccessory.hasSlot(player)) {
            return InteractionResult.PASS;
        }

        ItemStack previous = NoseAccessory.equip(player, heldStack.copy());
        player.setItemInHand(hand, previous);

        if (!level.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
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
