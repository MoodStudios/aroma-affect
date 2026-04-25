package com.ovrtechnology.variant;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.accessory.NoseAccessory;
import com.ovrtechnology.util.Ids;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

public class CustomNoseItem extends Item {

    public static final String ITEM_ID = "custom_nose";
    public static final int DEFAULT_DURABILITY = 250;

    public CustomNoseItem() {
        super(baseProperties());
    }

    private static Properties baseProperties() {
        Properties properties = new Properties();
        properties.stacksTo(1);
        properties.durability(DEFAULT_DURABILITY);
        properties.rarity(Rarity.COMMON);
        return properties;
    }

    public static ItemStack stackFor(Item item, ResourceLocation variantId, NoseVariant variant) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.NOSE_VARIANT.get(), variantId);
        stack.set(DataComponents.ITEM_NAME, resolveVariantName(variantId, variant));
        stack.set(DataComponents.RARITY, parseRarity(variant.getRarity()));
        stack.set(DataComponents.MAX_DAMAGE, variant.getDurability());
        stack.set(DataComponents.MAX_STACK_SIZE, 1);

        if (variant.getCustomModelData() > 0) {
            stack.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(variant.getCustomModelData()));
        }

        return stack;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        ResourceLocation variantId = stack.get(ModDataComponents.NOSE_VARIANT.get());
        if (variantId == null) return super.isValidRepairItem(stack, ingredient);
        NoseVariant variant = NoseVariantRegistry.get(variantId).orElse(null);
        if (variant == null) return super.isValidRepairItem(stack, ingredient);
        String repairId = variant.getRepair();
        if (repairId == null || repairId.isEmpty()) return false;
        ResourceLocation repairLoc = Ids.parse(repairId);
        return BuiltInRegistries.ITEM.getOptional(repairLoc).map(ingredient::is).orElse(false);
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

    private static Rarity parseRarity(String name) {
        try {
            return Rarity.valueOf(name);
        } catch (IllegalArgumentException e) {
            AromaAffect.LOGGER.warn("Unknown rarity '{}', defaulting to COMMON", name);
            return Rarity.COMMON;
        }
    }
}
