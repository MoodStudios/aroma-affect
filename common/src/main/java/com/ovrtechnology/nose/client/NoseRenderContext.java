package com.ovrtechnology.nose.client;

import java.util.UUID;

public final class NoseRenderContext {
    private static UUID currentEntityUuid;

    private NoseRenderContext() {}

    public static void setCurrentEntityUuid(UUID uuid) {
        currentEntityUuid = uuid;
    }

    public static UUID getCurrentEntityUuid() {
        return currentEntityUuid;
    }
}
