package com.ovrtechnology.fabric.client;

import com.ovrtechnology.AromaAffectClient;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferScreen;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.omara.OmaraDeviceScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class AromaAffectFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        NoseClient.init();

        AromaAffectClient.init();

        MenuScreens.register(SnifferMenuRegistry.SNIFFER_MENU.get(), SnifferScreen::new);

        MenuScreens.register(OmaraDeviceRegistry.OMARA_DEVICE_MENU.get(), OmaraDeviceScreen::new);

        NoseRenderingFabric.init();

        BlockOutlineRendererFabric.init();
    }
}
