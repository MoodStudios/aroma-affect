package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaCraft;
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
            AromaCraft.LOGGER.warn("NoseClient.init() called multiple times!");
            return;
        }

        EntityModelLayerRegistry.register(NoseModelLayers.NOSE_MASK, NoseMaskModel::createLayer);

        initialized = true;
        AromaCraft.LOGGER.info("Nose client rendering initialized");
    }

    public static ResourceLocation getArmorTexture(ItemStack stack) {
        if (stack.getItem() instanceof NoseItem noseItem) {
            return ResourceLocation.fromNamespaceAndPath(
                    AromaCraft.MOD_ID,
                    "textures/entity/nose/" + noseItem.getItemId() + ".png"
            );
        }

        return ResourceLocation.fromNamespaceAndPath(AromaCraft.MOD_ID, "textures/entity/nose/basic_nose.png");
    }
}
