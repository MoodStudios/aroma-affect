package com.ovrtechnology.entity.nosesmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.util.Ids;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;

public class NoseSmithRenderer
        extends MobRenderer<NoseSmithEntity, VillagerModel<NoseSmithEntity>> {

    private static final ResourceLocation NOSE_SMITH_TEXTURE =
            Ids.mod("textures/entity/nose_smith.png");

    @SuppressWarnings("this-escape")
    public NoseSmithRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(
                new CustomHeadLayer<>(
                        this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(NoseSmithEntity entity) {
        return NOSE_SMITH_TEXTURE;
    }

    @Override
    public void render(
            NoseSmithEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        setNoseVisible(this.getModel(), entity.hasNose());
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void setNoseVisible(VillagerModel<?> model, boolean visible) {
        var head = model.getHead();
        if (!head.hasChild("nose")) {
            return;
        }
        head.getChild("nose").visible = visible;
    }
}
