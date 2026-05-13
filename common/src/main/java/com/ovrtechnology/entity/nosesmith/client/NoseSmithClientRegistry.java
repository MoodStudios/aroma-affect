package com.ovrtechnology.entity.nosesmith.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.client.renderer.entity.BalmEntityRendererRegistrar;
import net.minecraft.world.entity.EntityType;

/**
 * Client-side registrar for Nose Smith entity rendering.
 *
 * <p>Wire from {@code AromaAffectClient.initialize} via
 * {@code clientRegistrars.entityRenderers(NoseSmithClientRegistry::register)}.</p>
 */
@UtilityClass
public final class NoseSmithClientRegistry {

    public static void register(BalmEntityRendererRegistrar entityRenderers) {
        EntityType<?> type = NoseSmithRegistry.getNoseSmithType();
        if (type == null) {
            AromaAffect.LOGGER.warn("NoseSmithClientRegistry.register() called before NoseSmithRegistry was bound");
            return;
        }
        entityRenderers.register(NoseSmithRegistry.NOSE_SMITH_ID, () -> type, NoseSmithRenderer::new);
        AromaAffect.LOGGER.info("NoseSmithClientRegistry initialized successfully!");
    }
}
