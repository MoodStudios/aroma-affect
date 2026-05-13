package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.AromaAffectClient;
import com.ovrtechnology.nose.NoseRegistry;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge-specific client initialization for Aroma Affect.
 *
 * <p>Most client wiring (model layers, entity renderers, menu screens, key
 * mappings, HUD overlays, OVR WebSocket) goes through Balm via
 * {@link AromaAffectClient#initialize}. The hooks that have no Balm wrapper
 * (NeoForge {@code IClientItemExtensions} for armor model providers and the
 * {@code RenderLevelStageEvent.AfterTranslucentBlocks} listener for the path
 * trail / block outline overlays) are wired below.</p>
 */
@Mod(value = AromaAffect.MOD_ID, dist = Dist.CLIENT)
public final class AromaAffectNeoForgeClient {

    public AromaAffectNeoForgeClient(ModContainer modContainer, IEventBus modEventBus) {
        BalmClient.initializeMod(AromaAffect.MOD_ID, new NeoForgeLoadContext(modContainer, modEventBus), AromaAffectClient::initialize);

        modEventBus.addListener(this::onRegisterClientExtensions);

        // BlockOutlineRendererNeoForge listens to RenderLevelStageEvent.AfterTranslucentBlocks
        // on NeoForge's main event bus; matches the pre-migration behaviour.
        BlockOutlineRendererNeoForge.init();
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        NoseItemClientExtensions extensions = new NoseItemClientExtensions();
        for (Holder<Item> holder : NoseRegistry.getAllNoses()) {
            if (holder.isBound()) {
                event.registerItem(extensions, holder.value());
            }
        }
        for (Holder<Item> holder : NoseRegistry.getLegacyItems()) {
            if (holder.isBound()) {
                event.registerItem(extensions, holder.value());
            }
        }
    }
}
