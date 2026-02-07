package com.ovrtechnology.nose.client;

import java.util.UUID;

/**
 * Holds the UUID of the entity currently being rendered.
 * <p>Set by a mixin on {@code EntityRenderer.extractRenderState()},
 * read by armor renderers to look up per-player nose preferences.</p>
 * <p>Safe because rendering is single-threaded on the render thread.</p>
 */
public final class NoseRenderContext {
    private static UUID currentEntityUuid;

    private NoseRenderContext() {
    }

    public static void setCurrentEntityUuid(UUID uuid) {
        currentEntityUuid = uuid;
    }

    public static UUID getCurrentEntityUuid() {
        return currentEntityUuid;
    }
}
