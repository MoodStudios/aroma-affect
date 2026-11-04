package com.ovrtechnology.compat;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

public final class ItemCompat {

    private ItemCompat() {}

    public static boolean isOnCooldown(Player player, ItemStack stack) {
        //? if >=26 {
        return player.getCooldowns().isOnCooldown(stack);
        //?} else {
        /*return player.getCooldowns().isOnCooldown(stack.getItem());
        *///?}
    }

    public static void addCooldown(Player player, ItemStack stack, int ticks) {
        //? if >=26 {
        player.getCooldowns().addCooldown(stack, ticks);
        //?} else {
        /*player.getCooldowns().addCooldown(stack.getItem(), ticks);
        *///?}
    }

    public static CustomModelData customModelData(int value) {
        //? if >=26 {
        return new CustomModelData(List.of((float) value), List.of(), List.of(), List.of());
        //?} else {
        /*return new CustomModelData(value);
        *///?}
    }

    // 26.x stores the repair ingredient as a component; 1.21.1 asks ModItem#repairItemId instead
    public static void applyRepairItem(Item.Properties properties, @Nullable String repairId) {
        //? if >=26 {
        repairHolder(repairId).ifPresent(holder -> properties.component(
                DataComponents.REPAIRABLE,
                new net.minecraft.world.item.enchantment.Repairable(HolderSet.direct(holder))));
        //?}
    }

    public static void applyRepairItem(ItemStack stack, @Nullable String repairId) {
        //? if >=26 {
        repairHolder(repairId).ifPresent(holder -> stack.set(
                DataComponents.REPAIRABLE,
                new net.minecraft.world.item.enchantment.Repairable(HolderSet.direct(holder))));
        //?}
    }

    public static boolean isRepairIngredient(@Nullable String repairId, ItemStack ingredient) {
        return repairHolder(repairId).map(holder -> ingredient.is(holder.value())).orElse(false);
    }

    private static Optional<Holder<Item>> repairHolder(@Nullable String repairId) {
        if (repairId == null || repairId.isEmpty()) {
            return Optional.empty();
        }
        Identifier id = Identifier.tryParse(repairId);
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(id).map(BuiltInRegistries.ITEM::wrapAsHolder);
    }

    // Wearable in the head slot. 1.21.1 gets the same behavior from HeadWearableItem implementing Equipable
    public static void headEquippable(Item.Properties properties, @Nullable String model) {
        //? if >=26 {
        properties.component(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(EquipmentSlot.HEAD)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                .setAsset(equipmentAsset(model))
                .setSwappable(true)
                .setDamageOnHurt(true)
                .build());
        //?}
    }

    //? if >=26 {
    private static ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> equipmentAsset(@Nullable String model) {
        if (model == null || model.isEmpty()) return net.minecraft.world.item.equipment.EquipmentAssets.IRON;
        String lower = model.toLowerCase();
        if (lower.contains("diamond")) return net.minecraft.world.item.equipment.EquipmentAssets.DIAMOND;
        if (lower.contains("gold")) return net.minecraft.world.item.equipment.EquipmentAssets.GOLD;
        if (lower.contains("netherite")) return net.minecraft.world.item.equipment.EquipmentAssets.NETHERITE;
        if (lower.contains("leather")) return net.minecraft.world.item.equipment.EquipmentAssets.LEATHER;
        if (lower.contains("chain")) return net.minecraft.world.item.equipment.EquipmentAssets.CHAINMAIL;
        return net.minecraft.world.item.equipment.EquipmentAssets.IRON;
    }
    //?}
}
