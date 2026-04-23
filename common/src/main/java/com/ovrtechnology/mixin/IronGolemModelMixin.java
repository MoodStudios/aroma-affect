package com.ovrtechnology.mixin;

import com.ovrtechnology.entity.irongolem.IronGolemNoseRenderState;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolemModel.class)
public class IronGolemModelMixin {

    @Shadow private ModelPart head;

    /**
     * @author aromaaffect
     * @reason rebuilds the iron golem layer definition to include a nose part on the head.
     */
    @Overwrite
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition headPart =
                partDefinition.addOrReplaceChild(
                        "head",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F),
                        PartPose.offset(0.0F, -7.0F, -2.0F));

        headPart.addOrReplaceChild(
                "nose",
                CubeListBuilder.create()
                        .texOffs(24, 0)
                        .addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F),
                PartPose.ZERO);

        partDefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F)
                        .texOffs(0, 70)
                        .addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        partDefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(60, 21)
                        .addBox(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        partDefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(60, 58)
                        .addBox(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        partDefinition.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(37, 0)
                        .addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(-4.0F, 11.0F, 0.0F));
        partDefinition.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(60, 0)
                        .mirror()
                        .addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));

        return LayerDefinition.create(meshDefinition, 128, 128);
    }

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void aromaaffect$toggleNoseVisibility(IronGolemRenderState state, CallbackInfo ci) {
        if (state instanceof IronGolemNoseRenderState noseState) {
            head.getChild("nose").visible = noseState.hasNose;
        }
    }
}
