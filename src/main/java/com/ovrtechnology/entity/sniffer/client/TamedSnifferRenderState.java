package com.ovrtechnology.entity.sniffer.client;

import net.minecraft.client.renderer.entity.state.SnifferRenderState;

/**
 * Extended render state for tamed Sniffers that tracks equipment.
 */
public class TamedSnifferRenderState extends SnifferRenderState {
    public boolean hasSaddle = false;
    public boolean hasNose = false;
    public boolean isSwimmingMode = false;
}
