package com.ovrtechnology.entity.nosesmith.client;

// Render states only exist from 1.21.2; on older versions the renderers read the entity directly
//? if >=26 {
import net.minecraft.client.renderer.entity.state.VillagerRenderState;

public final class NoseSmithRenderState extends VillagerRenderState {
    public boolean hasNose = true;
}
//?}
