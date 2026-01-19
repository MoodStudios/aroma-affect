package com.ovrtechnology.entity.client;

import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.entity.NoseSmithRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import lombok.experimental.UtilityClass;

/**
 * Client-side registry for Nose Smith entity rendering.
 * This must be called during client initialization on both platforms.
 */
@UtilityClass
public final class NoseSmithClientRegistry {
    
    private static boolean initialized = false;
    
    /**
     * Initialize client-side entity rendering.
     * Must be called during client initialization.
     */
    public static void init() {
        if (initialized) {
            AromaCraft.LOGGER.warn("NoseSmithClientRegistry.init() called multiple times!");
            return;
        }
        
        AromaCraft.LOGGER.info("Initializing NoseSmithClientRegistry...");
        
        // Register the entity renderer
        EntityRendererRegistry.register(NoseSmithRegistry.getNOSE_SMITH(), NoseSmithRenderer::new);
        
        initialized = true;
        AromaCraft.LOGGER.info("NoseSmithClientRegistry initialized successfully!");
    }
}
