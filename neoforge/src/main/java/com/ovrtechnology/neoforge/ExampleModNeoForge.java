package com.ovrtechnology.neoforge;

import net.neoforged.fml.common.Mod;

import com.ovrtechnology.AromaAffect;

@Mod(AromaAffect.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        AromaAffect.init();
    }
}
