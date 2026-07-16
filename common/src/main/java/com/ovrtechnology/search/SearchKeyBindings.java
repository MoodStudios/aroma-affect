package com.ovrtechnology.search;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.keybind.AromaAffectKeyCategory;
import net.blay09.mods.balm.client.BalmKeyMappingRegistrar;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinding registration and input polling for the Aroma Affect search system.
 *
 * <p>Mirror of {@link com.ovrtechnology.menu.MenuKeyBindings}: registration is
 * driven through Balm's {@link BalmKeyMappingRegistrar} and the tick polling
 * is wired separately.</p>
 */
public final class SearchKeyBindings {

    public static final KeyMapping TOGGLE_SEARCH = new KeyMapping(
            "key.aromaaffect.toggle_search",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            AromaAffectKeyCategory.CATEGORY
    );

    private static boolean tickHandlerInitialized = false;

    private SearchKeyBindings() {}

    public static void register(BalmKeyMappingRegistrar keyMappings) {
        keyMappings.register(TOGGLE_SEARCH);
        AromaAffect.LOGGER.info("Search keybinding registered (default: V key)");
    }

    public static void initTickHandler() {
        if (tickHandlerInitialized) {
            return;
        }
        tickHandlerInitialized = true;

        SearchManager.init();
        ClientTickCallback.AFTER.register(instance -> handleKeyInputs());
    }

    private static void handleKeyInputs() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.gui.screen() != null) {
            return;
        }
        while (TOGGLE_SEARCH.consumeClick()) {
            SearchManager.toggleSearch();
        }
    }

    public static boolean isToggleSearchKeyDown() {
        return TOGGLE_SEARCH.isDown();
    }

    public static String getToggleSearchKeyName() {
        return TOGGLE_SEARCH.getTranslatedKeyMessage().getString();
    }
}
