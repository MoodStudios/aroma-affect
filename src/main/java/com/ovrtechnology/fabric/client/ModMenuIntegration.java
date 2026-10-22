package com.ovrtechnology.fabric.client;

import com.ovrtechnology.menu.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

// Only loaded when Mod Menu is installed (it is the one that reads the "modmenu" entrypoint).
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
