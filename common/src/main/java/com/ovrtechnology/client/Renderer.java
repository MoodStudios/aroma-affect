package com.ovrtechnology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.client.handler.RenderHandler;
import com.ovrtechnology.client.model.HeadModel;
import com.ovrtechnology.nose.NoseRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public class Renderer {

    private static final Set<RenderHandler> RENDERING_HANDLERS = new LinkedHashSet<>();

    public static void register() {
        NoseRegistry.getAllNoses().forEach(nose -> register(nose.get(), () ->
                new GenericRenderer("plastic_drinking_hat", new HeadModel(bakeLayer(createLayerLocation("drinking_hat"))))));
    }

    public static void register(RenderHandler integration) {
        RENDERING_HANDLERS.add(integration);
    }

    public static void registerRenderer(Item item, Supplier<HumanoidModelRenderer> rendererFactory) {
        for (RenderHandler handler : RENDERING_HANDLERS) {
            handler.registerArtifactRenderer(item, rendererFactory);
        }
    }

    @Nullable
    public static HumanoidModelRenderer getArtifactRenderer(Item item) {
        for (RenderHandler handler : RENDERING_HANDLERS) {
            HumanoidModelRenderer renderer = handler.getArtifactRenderer(item);

            if (renderer != null) {
                return renderer;
            }
        }

        return null;
    }

    public static void renderArm(PoseStack matrixStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, HumanoidArm side) {
        for (RenderHandler handler : RENDERING_HANDLERS) {
            handler.renderArm(matrixStack, buffer, light, player, side);
        }
    }

    public static ModelPart bakeLayer(ModelLayerLocation layerLocation) {
        return Minecraft.getInstance().getEntityModels().bakeLayer(layerLocation);
    }

    public static void register(Item item, Supplier<HumanoidModelRenderer> rendererFactory) {
        registerRenderer(item, rendererFactory);
    }

    public static ModelLayerLocation createLayerLocation(String name) {
        return new ModelLayerLocation(AromaCraft.id(name), name);
    }
}
