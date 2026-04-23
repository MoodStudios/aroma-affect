package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.AromaAffectClient;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferScreen;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.omara.OmaraDeviceScreen;
import dev.architectury.registry.registries.RegistrySupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@Mod(value = AromaAffect.MOD_ID, dist = Dist.CLIENT)
public final class AromaAffectNeoForgeClient {

    public AromaAffectNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {

        NoseClient.init();

        AromaAffectClient.init();

        modEventBus.addListener(this::onRegisterClientExtensions);
        modEventBus.addListener(this::onRegisterMenuScreens);
        modEventBus.addListener(this::onAddLayers);
        modEventBus.addListener(this::onClientSetup);
    }

    private void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skinModel :
                net.minecraft.client.resources.PlayerSkin.Model.values()) {
            net.minecraft.client.renderer.entity.LivingEntityRenderer<
                            net.minecraft.client.player.AbstractClientPlayer,
                            net.minecraft.client.model.PlayerModel<
                                    net.minecraft.client.player.AbstractClientPlayer>>
                    renderer = event.getSkin(skinModel);
            if (renderer
                    instanceof
                    net.minecraft.client.renderer.entity.player.PlayerRenderer
                    playerRenderer) {
                playerRenderer.addLayer(new NoseLayer(playerRenderer));
            }
        }
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SnifferMenuRegistry.SNIFFER_MENU.get(), SnifferScreen::new);
        event.register(OmaraDeviceRegistry.OMARA_DEVICE_MENU.get(), OmaraDeviceScreen::new);

        BlockOutlineRendererNeoForge.init();
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(AromaAffectClient::registerCompassProperty);
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        NoseItemClientExtensions extensions = new NoseItemClientExtensions();
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            event.registerItem(extensions, supplier.get());
        }
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getLegacyItems()) {
            event.registerItem(extensions, supplier.get());
        }
        if (com.ovrtechnology.variant.CustomNoseRegistry.getCUSTOM_NOSE().isPresent()) {
            event.registerItem(
                    extensions,
                    com.ovrtechnology.variant.CustomNoseRegistry.getCUSTOM_NOSE().get());
        }
    }
}
