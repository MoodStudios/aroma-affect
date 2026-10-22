package com.ovrtechnology.variant;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.accessory.NoseAccessory;
import com.ovrtechnology.util.Ids;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.Level;

public class CustomNoseItem extends Item {

    public static final String ITEM_ID = "custom_nose";
    public static final int DEFAULT_DURABILITY = 250;

    public CustomNoseItem(Identifier id) {
        super(baseProperties(id));
    }

    private static Properties baseProperties(Identifier id) {
        Properties properties = new Properties();
        properties.setId(ResourceKey.create(Registries.ITEM, id));
        properties.stacksTo(1);
        properties.durability(DEFAULT_DURABILITY);
        properties.rarity(Rarity.COMMON);
        return properties;
    }

    public static ItemStack stackFor(Item item, Identifier variantId, NoseVariant variant) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.noseVariant(), variantId);
        stack.set(DataComponents.ITEM_NAME, resolveVariantName(variantId, variant));
        stack.set(DataComponents.RARITY, parseRarity(variant.getRarity()));
        stack.set(DataComponents.MAX_DAMAGE, variant.getDurability());
        stack.set(DataComponents.MAX_STACK_SIZE, 1);

        String repairId = variant.getRepair();
        if (repairId != null && !repairId.isEmpty()) {
            Identifier repairLoc = Ids.parse(repairId);
            BuiltInRegistries.ITEM.getOptional(repairLoc).ifPresent(repairItem -> {
                Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(repairItem);
                stack.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(holder)));
            });
        }

        if (variant.getCustomModelData() > 0) {
            stack.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(
                            List.of((float) variant.getCustomModelData()),
                            List.of(),
                            List.of(),
                            List.of()));
        }

        return stack;
    }

    private static Component resolveVariantName(Identifier variantId, NoseVariant variant) {
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
        Identifier id = stack.get(ModDataComponents.noseVariant());
        if (id == null) return Optional.empty();
        return NoseVariantRegistry.get(id);
    }

    public static Optional<Identifier> getVariantId(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModDataComponents.noseVariant()));
    }

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

    private static Rarity parseRarity(String name) {
        try {
            return Rarity.valueOf(name);
        } catch (IllegalArgumentException e) {
            AromaAffect.LOGGER.warn("Unknown rarity '{}', defaulting to COMMON", name);
            return Rarity.COMMON;
        }
    }
}
