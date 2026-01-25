package com.ovrtechnology.entity.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.NoseSmithEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the Nose Smith entity.
 *
 * <p>Built on the vanilla villager model, but:</p>
 * <ul>
 *   <li>Uses a single custom skin (no profession/type overlays)</li>
 *   <li>Can toggle the villager "nose" model part on/off (test feature)</li>
 * </ul>
 */
public class NoseSmithRenderer extends AgeableMobRenderer<NoseSmithEntity, VillagerRenderState, VillagerModel> {

    private static final ResourceLocation NOSE_SMITH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/entity/nose_smith.png");

    private final VillagerModel adultModel;
    private final VillagerModel babyModel;

    public NoseSmithRenderer(EntityRendererProvider.Context context) {
        this(context, createAdultModel(context), createBabyModel(context));
    }

    private NoseSmithRenderer(EntityRendererProvider.Context context, VillagerModel adultModel, VillagerModel babyModel) {
        super(context, adultModel, babyModel, 0.5F);
        this.adultModel = adultModel;
        this.babyModel = babyModel;

        // Match vanilla VillagerRenderer layers, except profession/type overlays.
        this.addLayer(new CustomHeadLayer<>(
                this,
                context.getModelSet(),
                context.getPlayerSkinRenderCache(),
                VillagerRenderer.CUSTOM_HEAD_TRANSFORMS
        ));
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    private static VillagerModel createAdultModel(EntityRendererProvider.Context context) {
        return new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER));
    }

    private static VillagerModel createBabyModel(EntityRendererProvider.Context context) {
        return new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_BABY));
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new NoseSmithRenderState();
    }

    @Override
    public void extractRenderState(NoseSmithEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.isUnhappy = entity.getUnhappyCounter() > 0;
        state.villagerData = entity.getVillagerData();
        if (state instanceof NoseSmithRenderState noseSmithRenderState) {
            noseSmithRenderState.hasNose = entity.hasNose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VillagerRenderState state) {
        return NOSE_SMITH_TEXTURE;
    }

    @Override
    public void submit(
            VillagerRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraRenderState
    ) {
        boolean hasNose = true;
        if (state instanceof NoseSmithRenderState noseSmithRenderState) {
            hasNose = noseSmithRenderState.hasNose;
        }

        setNoseVisible(adultModel, hasNose);
        setNoseVisible(babyModel, hasNose);

        super.submit(state, poseStack, collector, cameraRenderState);
    }

    private static void setNoseVisible(VillagerModel model, boolean visible) {
        var head = model.getHead();
        if (!head.hasChild("nose")) {
            return;
        }

        head.getChild("nose").visible = visible;
    }
}
