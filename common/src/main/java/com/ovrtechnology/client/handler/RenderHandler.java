package com.ovrtechnology.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.client.HumanoidModelRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface RenderHandler {

    void registerArtifactRenderer(Item item, Supplier<HumanoidModelRenderer> rendererSupplier);

    @Nullable
    HumanoidModelRenderer getArtifactRenderer(Item item);

    void renderArm(PoseStack matrixStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, HumanoidArm side);

}
