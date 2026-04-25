package com.ovrtechnology.trigger.event;

import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;

public final class PlayerItemUseEventDispatcher {

    private PlayerItemUseEventDispatcher() {}

    public static void onItemUseFinished(LocalPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;

        EventTriggersConfig config = EventTriggersConfig.getInstance();
        if (!config.isEventTriggersEnabled()) return;

        boolean isPotion = stack.getItem() instanceof PotionItem;
        FoodProperties food = stack.get(DataComponents.FOOD);

        if (isPotion) {
            dispatchPotion(stack, config);
            return;
        }

        if (food != null) {
            dispatchFood(stack, config);
        }
    }

    private static void dispatchFood(ItemStack stack, EventTriggersConfig config) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return;
        String itemKey = itemId.toString();

        boolean fired =
                PlayerStateTickHandler.fireOneShotByTriggerType(
                        PlayerStateTickHandler.TT_PLAYER_FOOD_EATEN,
                        config,
                        def -> matchesItem(def.getConditions(), itemKey),
                        -1.0);
        if (!fired) {
            AromaAffect.LOGGER.debug(
                    "[Events] no food event matched item {}", itemKey);
        }
    }

    private static void dispatchPotion(ItemStack stack, EventTriggersConfig config) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return;

        Holder<Potion> potionHolder = contents.potion().orElse(null);
        if (potionHolder == null) return;

        ResourceLocation primaryEffectId = null;
        for (MobEffectInstance effect : potionHolder.value().getEffects()) {
            ResourceLocation rl = mobEffectId(effect.getEffect());
            if (rl != null) {
                primaryEffectId = rl;
                break;
            }
        }

        ResourceLocation potionId = potionHolder.unwrapKey().map(k -> k.location()).orElse(null);
        final String potionKey = potionId != null ? potionId.toString() : null;
        final String effectKey = primaryEffectId != null ? primaryEffectId.toString() : null;

        boolean fired =
                PlayerStateTickHandler.fireOneShotByTriggerType(
                        PlayerStateTickHandler.TT_PLAYER_POTION_DRUNK,
                        config,
                        def -> matchesPotion(def.getConditions(), potionKey, effectKey),
                        -1.0);
        if (!fired) {
            AromaAffect.LOGGER.debug(
                    "[Events] no potion event matched (potion: {}, primary effect: {})",
                    potionKey,
                    effectKey);
        }
    }

    private static ResourceLocation mobEffectId(Holder<MobEffect> effectHolder) {
        if (effectHolder == null) return null;
        return effectHolder.unwrapKey().map(k -> k.location()).orElse(null);
    }

    private static boolean matchesItem(JsonObject conditions, String itemKey) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "item_ids");
        if (!ids.isEmpty()) {
            return ids.contains(itemKey);
        }
        return EventConditionUtils.getBoolean(conditions, "default", false);
    }

    private static boolean matchesPotion(
            JsonObject conditions, String potionKey, String effectKey) {
        List<String> potionIds = EventConditionUtils.getStringArray(conditions, "potion_ids");
        if (potionKey != null && !potionIds.isEmpty() && potionIds.contains(potionKey)) {
            return true;
        }
        List<String> effectIds = EventConditionUtils.getStringArray(conditions, "effect_ids");
        if (effectKey != null && !effectIds.isEmpty() && effectIds.contains(effectKey)) {
            return true;
        }
        return EventConditionUtils.getBoolean(conditions, "default", false);
    }
}
