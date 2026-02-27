package com.ovrtechnology.tutorial.oliver.client;

import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the Tutorial Oliver entity.
 * <p>
 * Uses the standard villager model and texture.
 */
public class TutorialOliverRenderer extends AgeableMobRenderer<TutorialOliverEntity, VillagerRenderState, VillagerModel> {

    private static final ResourceLocation VILLAGER_BASE_SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public TutorialOliverRenderer(EntityRendererProvider.Context context) {
        super(context,
                new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)),
                new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_BABY)),
                0.5F);

        this.addLayer(new CustomHeadLayer<>(
                this,
                context.getModelSet(),
                context.getPlayerSkinRenderCache(),
                VillagerRenderer.CUSTOM_HEAD_TRANSFORMS
        ));
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(TutorialOliverEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.isUnhappy = false;
        state.villagerData = entity.getVillagerData();
    }

    @Override
    public ResourceLocation getTextureLocation(VillagerRenderState state) {
        return VILLAGER_BASE_SKIN;
    }
}
