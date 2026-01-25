package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class NoseModelLayers {
    public static final ModelLayerLocation NOSE_MASK =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_mask"), "main");

    private NoseModelLayers() {
    }
}
