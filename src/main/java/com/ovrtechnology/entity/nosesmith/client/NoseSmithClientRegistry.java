package com.ovrtechnology.entity.nosesmith.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.client.renderer.entity.BalmEntityRendererRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;

/**
 * Client-side registrar for Nose Smith entity rendering.
 */
@UtilityClass
public final class NoseSmithClientRegistry {

    @SuppressWarnings("unchecked")
    public static void register(BalmEntityRendererRegistrar entityRenderers) {
        Holder<EntityType<?>> holder = NoseSmithRegistry.getNoseSmith();
        if (holder == null) {
            AromaAffect.LOGGER.warn("NoseSmithClientRegistry.register() called before NoseSmithRegistry was bound");
            return;
        }
        Holder<EntityType<NoseSmithEntity>> typed = (Holder<EntityType<NoseSmithEntity>>) (Holder<?>) holder;
        entityRenderers.register(typed, NoseSmithRenderer::new);
        AromaAffect.LOGGER.info("NoseSmithClientRegistry initialized successfully!");
    }
}
