package com.ovrtechnology.search;

import com.mojang.blaze3d.platform.InputConstants;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.keybind.AromaAffectKeyCategory;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class SearchKeyBindings {

    public static final KeyMapping TOGGLE_SEARCH =
            new KeyMapping(
                    "key.aromaaffect.toggle_search",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    AromaAffectKeyCategory.CATEGORY);

    private static boolean initialized = false;

    private SearchKeyBindings() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("SearchKeyBindings.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing search keybindings...");

        SearchManager.init();

        KeyMappingRegistry.register(TOGGLE_SEARCH);

        ClientTickEvent.CLIENT_POST.register(
                instance -> {
                    handleKeyInputs();
                });

        initialized = true;
        AromaAffect.LOGGER.info("Search keybindings initialized (default: V key)");
    }

    private static void handleKeyInputs() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        if (minecraft.screen != null) {
            return;
        }

        while (TOGGLE_SEARCH.consumeClick()) {
            SearchManager.toggleSearch();
        }
    }
}
