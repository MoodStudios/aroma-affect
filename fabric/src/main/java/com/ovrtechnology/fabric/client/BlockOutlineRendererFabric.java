package com.ovrtechnology.fabric.client;

import com.ovrtechnology.AromaAffect;
// TODO(balm-26.1): Fabric API 26.1 removed/relocated WorldRenderEvents from
// net.fabricmc.fabric.api.client.rendering.v1 (and from .v1.world). The
// X-ray block outline + path trail hooks are temporarily stubbed; restore
// once the new Fabric 26.1 rendering event is identified (likely a
// LevelRenderer extension via ClientLifecycleEvents or a new render-stage
// callback).

public final class BlockOutlineRendererFabric {

    private BlockOutlineRendererFabric() {}

    public static void init() {
        AromaAffect.LOGGER.warn("BlockOutlineRendererFabric stubbed -- Fabric 26.1 render-event API still being resolved");
    }
}
