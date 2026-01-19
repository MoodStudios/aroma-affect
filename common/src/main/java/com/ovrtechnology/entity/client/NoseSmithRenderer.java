package com.ovrtechnology.entity.client;

import com.ovrtechnology.entity.NoseSmithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;

/**
 * Renderer for the Nose Smith entity.
 * Uses the standard Villager model and textures.
 * 
 * Note: A custom texture can be added later at:
 * assets/aromacraft/textures/entity/nose_smith.png
 */
public class NoseSmithRenderer extends VillagerRenderer {
    
    public NoseSmithRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    // Uses default villager rendering
    // Custom texture override can be added when VillagerRenderState API is properly understood
}
