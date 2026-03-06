package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseIdRemapper;
import com.ovrtechnology.nose.NoseItem;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class NoseClient {
    private static boolean initialized = false;

    private NoseClient() {
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseClient.init() called multiple times!");
            return;
        }

        EntityModelLayerRegistry.register(NoseModelLayers.NOSE_MASK, NoseMaskModel::createLayer);

        initialized = true;
        AromaAffect.LOGGER.info("Nose client rendering initialized");
    }

    public static ResourceLocation getArmorTexture(ItemStack stack) {
        if (stack.getItem() instanceof NoseItem noseItem) {
            String resolvedId = NoseIdRemapper.resolve(noseItem.getItemId());
            return ResourceLocation.fromNamespaceAndPath(
                    AromaAffect.MOD_ID,
                    "textures/entity/nose/" + resolvedId + ".png"
            );
        }

        return ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/entity/nose/foragers_nose.png");
    }
}
