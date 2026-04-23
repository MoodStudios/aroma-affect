package com.ovrtechnology.entity.nosesmith.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class NoseSmithClientRegistry {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseSmithClientRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing NoseSmithClientRegistry...");

        EntityRendererRegistry.register(NoseSmithRegistry.getNOSE_SMITH(), NoseSmithRenderer::new);

        initialized = true;
        AromaAffect.LOGGER.info("NoseSmithClientRegistry initialized successfully!");
    }
}
