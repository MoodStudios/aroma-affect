package com.ovrtechnology.tutorial.oliver.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.oliver.TutorialOliverRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import lombok.experimental.UtilityClass;

/**
 * Client-side registry for Tutorial Oliver entity rendering.
 * <p>
 * This must be called during client initialization.
 */
@UtilityClass
public final class TutorialOliverClientRegistry {

    private static boolean initialized = false;

    /**
     * Initialize client-side entity rendering for Tutorial Oliver.
     * <p>
     * Must be called during client initialization.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        EntityRendererRegistry.register(
                TutorialOliverRegistry.TUTORIAL_OLIVER,
                TutorialOliverRenderer::new
        );

        initialized = true;
        AromaAffect.LOGGER.debug("Tutorial Oliver client registry initialized");
    }
}
