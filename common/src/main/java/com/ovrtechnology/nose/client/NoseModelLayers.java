package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaCraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class NoseModelLayers {
    public static final ModelLayerLocation NOSE_MASK =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AromaCraft.MOD_ID, "nose_mask"), "main");

    private NoseModelLayers() {
    }
}
