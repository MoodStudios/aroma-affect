package com.ovrtechnology.keybind;

import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public final class AromaAffectKeyCategory {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Ids.mod("keybinds")
    );
    
    private AromaAffectKeyCategory() {
        // Utility class
    }

    public static void ensureInitialized() {}
}

