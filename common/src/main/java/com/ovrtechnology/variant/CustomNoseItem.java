package com.ovrtechnology.variant;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.util.Ids;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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

public class CustomNoseItem extends Item {

    public static final String ITEM_ID = "custom_nose";
    public static final int DEFAULT_DURABILITY = 250;

    public CustomNoseItem() {
        super(baseProperties());
    }

    private static Properties baseProperties() {
        Properties properties = new Properties();
        properties.setId(ResourceKey.create(Registries.ITEM, Ids.mod(ITEM_ID)));
        properties.stacksTo(1);
        properties.durability(DEFAULT_DURABILITY);
        properties.rarity(Rarity.COMMON);
        properties.component(
                DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.HEAD)
                        .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                        .setAsset(EquipmentAssets.IRON)
                        .setSwappable(true)
                        .setDamageOnHurt(true)
                        .build());
        return properties;
    }

    public static ItemStack stackFor(Item item, ResourceLocation variantId, NoseVariant variant) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.NOSE_VARIANT.get(), variantId);
        stack.set(DataComponents.ITEM_NAME, resolveVariantName(variantId, variant));
        stack.set(DataComponents.RARITY, parseRarity(variant.getRarity()));
        stack.set(DataComponents.MAX_DAMAGE, variant.getDurability());
        stack.set(DataComponents.MAX_STACK_SIZE, 1);

        String repairId = variant.getRepair();
        if (repairId != null && !repairId.isEmpty()) {
            ResourceLocation repairLoc = Ids.parse(repairId);
            BuiltInRegistries.ITEM
                    .getOptional(repairLoc)
                    .ifPresent(
                            repairItem -> {
                                Holder<Item> holder =
                                        BuiltInRegistries.ITEM.wrapAsHolder(repairItem);
                                stack.set(
                                        DataComponents.REPAIRABLE,
                                        new Repairable(HolderSet.direct(holder)));
                            });
        }

        return stack;
    }

    private static Component resolveVariantName(ResourceLocation variantId, NoseVariant variant) {
        String key = variant.getTranslationKey();
        if (key != null && !key.isEmpty()) {
            return Component.translatable(key);
        }
        String display = variant.getDisplayName();
        if (display != null && !display.isEmpty() && !display.equals(variantId.toString())) {
            return Component.literal(display);
        }
        return Component.translatable(
                "nose." + variantId.getNamespace() + "." + variantId.getPath());
    }

    public static Optional<NoseVariant> getVariant(ItemStack stack) {
        ResourceLocation id = stack.get(ModDataComponents.NOSE_VARIANT.get());
        if (id == null) return Optional.empty();
        return NoseVariantRegistry.get(id);
    }

    public static Optional<ResourceLocation> getVariantId(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModDataComponents.NOSE_VARIANT.get()));
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation variantId = stack.get(ModDataComponents.NOSE_VARIANT.get());
        if (variantId == null) return super.getName(stack);
        NoseVariant variant = NoseVariantRegistry.get(variantId).orElse(null);
        if (variant == null) return super.getName(stack);
        return resolveVariantName(variantId, variant);
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
        }
        player.setItemSlot(EquipmentSlot.HEAD, heldStack.copy());
        player.setItemInHand(hand, headStack.copy());
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static Rarity parseRarity(String name) {
        try {
            return Rarity.valueOf(name);
        } catch (IllegalArgumentException e) {
            AromaAffect.LOGGER.warn("Unknown rarity '{}', defaulting to COMMON", name);
            return Rarity.COMMON;
        }
    }
}
