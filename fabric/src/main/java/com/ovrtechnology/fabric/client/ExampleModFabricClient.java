package com.ovrtechnology.fabric.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.AromaAffectClient;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.fabricmc.api.ClientModInitializer;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(AromaAffect.MOD_ID, FabricLoadContext.INSTANCE, AromaAffectClient::initialize);
    }
}
