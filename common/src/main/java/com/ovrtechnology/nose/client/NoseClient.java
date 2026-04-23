package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseIdRemapper;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.variant.CustomNoseItem;
import com.ovrtechnology.variant.NoseVariant;
import com.ovrtechnology.variant.NoseVariantRegistry;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class NoseClient {
    private static boolean initialized = false;

    private static final Map<String, Integer> ANIMATED_FRAME_COUNTS =
            Map.of(
                    "dimensional_nose", 7,
                    "ancient_nose", 6,
                    "dragon_nose", 4);

    private static final int TICKS_PER_FRAME = 4;

    private static final Map<String, ResourceLocation[]> frameCache = new HashMap<>();

    private NoseClient() {}

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

            return Ids.mod("textures/entity/nose/" + resolvedId + ".png");
        }

        if (stack.getItem() instanceof CustomNoseItem) {
            Optional<ResourceLocation> vid = CustomNoseItem.getVariantId(stack);
            if (vid.isPresent()) {
                ResourceLocation variantId = vid.get();
                Optional<NoseVariant> variantOpt = NoseVariantRegistry.get(variantId);
                if (variantOpt.isPresent()) {
                    NoseVariant variant = variantOpt.get();
                    NoseVariant.Animation anim = variant.getAnimation();
                    if (anim != null && anim.isAnimated()) {
                        return getVariantAnimatedFrame(variantId, anim);
                    }
                    String explicit = variant.getEntityTexture();
                    if (explicit != null && !explicit.isEmpty()) {
                        ResourceLocation parsed = ResourceLocation.tryParse(explicit);
                        if (parsed != null) {
                            return parsed.getPath().startsWith("textures/")
                                    ? parsed
                                    : ResourceLocation.fromNamespaceAndPath(
                                            parsed.getNamespace(),
                                            "textures/entity/nose/" + parsed.getPath() + ".png");
                        }
                    }
                }
                return ResourceLocation.fromNamespaceAndPath(
                        variantId.getNamespace(),
                        "textures/entity/nose/" + variantId.getPath() + ".png");
            }
            return Ids.mod("textures/entity/nose/foragers_nose.png");
        }

        return Ids.mod("textures/entity/nose/foragers_nose.png");
    }

    private static final Map<ResourceLocation, ResourceLocation[]> variantFrameCache =
            new HashMap<>();

    private static ResourceLocation getVariantAnimatedFrame(
            ResourceLocation variantId, NoseVariant.Animation anim) {
        int frameCount = anim.getFrames();
        int ticksPer = anim.getTicksPerFrame();
        ResourceLocation[] frames =
                variantFrameCache.computeIfAbsent(
                        variantId,
                        id -> {
                            ResourceLocation[] arr = new ResourceLocation[frameCount];
                            for (int i = 0; i < frameCount; i++) {
                                arr[i] =
                                        ResourceLocation.fromNamespaceAndPath(
                                                id.getNamespace(),
                                                "textures/entity/nose/"
                                                        + id.getPath()
                                                        + "_"
                                                        + i
                                                        + ".png");
                            }
                            return arr;
                        });

        Minecraft mc = Minecraft.getInstance();
        long ticks = mc.level != null ? mc.level.getGameTime() : 0;
        int frameIndex = (int) ((ticks / ticksPer) % frameCount);
        return frames[frameIndex];
    }

    private static ResourceLocation getAnimatedFrame(String noseId, int frameCount) {
        ResourceLocation[] frames =
                frameCache.computeIfAbsent(
                        noseId,
                        id -> {
                            ResourceLocation[] arr = new ResourceLocation[frameCount];
                            for (int i = 0; i < frameCount; i++) {
                                arr[i] = Ids.mod("textures/entity/nose/" + id + "_" + i + ".png");
                            }
                            return arr;
                        });

        Minecraft mc = Minecraft.getInstance();
        long ticks = mc.level != null ? mc.level.getGameTime() : 0;
        int frameIndex = (int) ((ticks / TICKS_PER_FRAME) % frameCount);
        return frames[frameIndex];
    }
}
