package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.AromaAffectClient;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge-specific client initialization for Aroma Affect.
 *
 * <p>Menu screens, entity renderers, model layers, and NoseItemClientExtensions are
 * registered through {@link AromaAffectClient}'s {@code BalmClientRegistrars} callbacks
 * in phase 8. Curios client rendering (NoseCurioRenderer) is restored in phase 9.</p>
 */
@Mod(value = AromaAffect.MOD_ID, dist = Dist.CLIENT)
public final class AromaAffectNeoForgeClient {

    public AromaAffectNeoForgeClient(ModContainer modContainer, IEventBus modEventBus) {
        BalmClient.initializeMod(AromaAffect.MOD_ID, new NeoForgeLoadContext(modContainer, modEventBus), AromaAffectClient::initialize);
    }
}
