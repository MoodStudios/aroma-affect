package com.ovrtechnology.entity.irongolem;

// Render states only exist from 1.21.2; on older versions the renderers read the entity directly
//? if >=26 {
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;

/**
 * Extended render state for Iron Golems that tracks whether the nose is present.
 */
public class IronGolemNoseRenderState extends IronGolemRenderState {
    public boolean hasNose = true;
}
//?}
