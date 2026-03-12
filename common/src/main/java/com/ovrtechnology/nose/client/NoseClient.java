package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseIdRemapper;
import com.ovrtechnology.nose.NoseItem;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class NoseClient {
    private static boolean initialized = false;

    /** Animated entity texture frame counts (nose ID → number of frames). */
    private static final Map<String, Integer> ANIMATED_FRAME_COUNTS = Map.of(
            "dimensional_nose", 7,
            "ancient_nose", 6,
            "dragon_nose", 4
    );

    /** Ticks per animation frame (matches item texture frametime). */
    private static final int TICKS_PER_FRAME = 4;

    /** Cached ResourceLocation arrays per animated nose to avoid allocation every frame. */
    private static final Map<String, ResourceLocation[]> frameCache = new HashMap<>();

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

            Integer frameCount = ANIMATED_FRAME_COUNTS.get(resolvedId);
            if (frameCount != null) {
                return getAnimatedFrame(resolvedId, frameCount);
            }

            return ResourceLocation.fromNamespaceAndPath(
                    AromaAffect.MOD_ID,
                    "textures/entity/nose/" + resolvedId + ".png"
            );
        }

        return ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/entity/nose/foragers_nose.png");
    }

    private static ResourceLocation getAnimatedFrame(String noseId, int frameCount) {
        ResourceLocation[] frames = frameCache.computeIfAbsent(noseId, id -> {
            ResourceLocation[] arr = new ResourceLocation[frameCount];
            for (int i = 0; i < frameCount; i++) {
                arr[i] = ResourceLocation.fromNamespaceAndPath(
                        AromaAffect.MOD_ID,
                        "textures/entity/nose/" + id + "_" + i + ".png"
                );
            }
            return arr;
        });

        Minecraft mc = Minecraft.getInstance();
        long ticks = mc.level != null ? mc.level.getGameTime() : 0;
        int frameIndex = (int) ((ticks / TICKS_PER_FRAME) % frameCount);
        return frames[frameIndex];
    }
}
