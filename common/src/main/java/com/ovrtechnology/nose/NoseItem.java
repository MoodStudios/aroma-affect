package com.ovrtechnology.nose;

import com.ovrtechnology.nose.accessory.NoseAccessory;
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

/**
 * Base item class for all nose equipment in Aroma Affect.
 *
 * <p>Implements {@link Equipable} so noses always work in the vanilla helmet
 * slot. On NeoForge, Curios provides an additional "face" accessory slot when
 * installed; {@link NoseAccessory} routes equip/lookup to whichever is
 * appropriate.</p>
 */
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

    /**
     * Right-click routes the nose into the platform-specific accessory slot
     * (Curios face slot when present on NeoForge, vanilla HEAD slot otherwise).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);

        if (!NoseAccessory.hasSlot(player)) {
            return InteractionResultHolder.pass(heldStack);
        }

        ItemStack previous = NoseAccessory.equip(player, heldStack.copy());
        player.setItemInHand(hand, previous);

        if (!level.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        return InteractionResultHolder.success(player.getItemInHand(hand));
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
