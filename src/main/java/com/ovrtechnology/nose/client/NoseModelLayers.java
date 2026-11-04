package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class NoseModelLayers {
    public static final ModelLayerLocation NOSE_MASK =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_mask"), "main");

    private NoseModelLayers() {
    }
}
