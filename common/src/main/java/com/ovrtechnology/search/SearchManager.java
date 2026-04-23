package com.ovrtechnology.search;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseTags;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SearchManager {

    @Getter private static boolean searchActive = false;

    private static boolean initialized = false;

    private SearchManager() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("SearchManager.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing SearchManager...");

        searchActive = false;

        initialized = true;
        AromaAffect.LOGGER.info("SearchManager initialized");
    }

    public static boolean toggleSearch() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null) {
            AromaAffect.LOGGER.debug("Cannot toggle search: No player instance");
            return false;
        }

        if (!isNoseEquipped(player)) {
            AromaAffect.LOGGER.info(
                    "[Aroma Affect Search] Cannot activate search - No Nose equipped!");
            return false;
        }

        searchActive = !searchActive;

        if (searchActive) {
            onSearchActivated(player);
        } else {
            onSearchDeactivated(player);
        }

        return true;
    }

    private static void onSearchActivated(Player player) {
        String noseName = EquippedNoseHelper.getEquippedNoseId(player).orElse("Unknown");
        AromaAffect.LOGGER.info("[Aroma Affect Search] Search mode ACTIVATED with: {}", noseName);
    }

    private static void onSearchDeactivated(Player player) {
        AromaAffect.LOGGER.info("[Aroma Affect Search] Search mode DEACTIVATED");
    }

    public static boolean isNoseEquipped(Player player) {
        if (player == null) return false;
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        return !headStack.isEmpty() && headStack.is(NoseTags.NOSES);
    }
}
