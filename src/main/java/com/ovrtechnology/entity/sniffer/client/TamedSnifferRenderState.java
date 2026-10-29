package com.ovrtechnology.entity.sniffer.client;

// Render states only exist from 1.21.2; on older versions the renderers read the entity directly
//? if >=26 {
import net.minecraft.client.renderer.entity.state.SnifferRenderState;

/**
 * Extended render state for tamed Sniffers that tracks equipment.
 */
public class TamedSnifferRenderState extends SnifferRenderState {
    public boolean hasSaddle = false;
    public boolean hasNose = false;
    public boolean isSwimmingMode = false;
}
//?}
