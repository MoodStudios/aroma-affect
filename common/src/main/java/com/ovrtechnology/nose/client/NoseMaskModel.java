package com.ovrtechnology.nose.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;

public final class NoseMaskModel extends HumanoidModel<LivingEntity> {
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 32;

    private final ModelPart noseMask;
    private final ModelPart strap;

    public NoseMaskModel(ModelPart root) {
        super(root, RenderType::armorCutoutNoCull);
        this.noseMask = this.head.getChild("nose_mask");
        this.strap = this.noseMask.getChild("strap");
    }

    public void setNoseMaskVisible(boolean visible) {
        noseMask.visible = visible;
    }

    public void setStrapVisible(boolean visible) {
        strap.visible = visible;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head =
                partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition noseMask =
                head.addOrReplaceChild(
                        "nose_mask",
                        CubeListBuilder.create()
                                .texOffs(0, 8)
                                .addBox(
                                        -1.0F,
                                        -4.0F,
                                        -6.0F,
                                        2.0F,
                                        4.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition strap =
                noseMask.addOrReplaceChild(
                        "strap",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -8.5F,
                                        -1.0F,
                                        4.5F,
                                        8.0F,
                                        1.0F,
                                        1.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(0, 0)
                                .addBox(
                                        -8.5F,
                                        -1.0F,
                                        -4.5F,
                                        8.0F,
                                        1.0F,
                                        1.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offset(4.5F, -2.0F, -0.5F));

        strap.addOrReplaceChild(
                "strap_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.5F, -1.0F, -0.5F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        strap.addOrReplaceChild(
                "strap_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.5F, -1.0F, -0.5F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-9.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition extras =
                noseMask.addOrReplaceChild(
                        "extras", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        extras.addOrReplaceChild(
                "detail_bottom",
                CubeListBuilder.create()
                        .texOffs(8, 14)
                        .addBox(0.0F, -1.0F, 0.0F, 1.0F, 1.0F, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.25F, 0.0F, -6.2F, 0.0F, -0.2182F, 0.0F));

        extras.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create()
                        .texOffs(16, 8)
                        .addBox(0.0F, -5.0F, 0.0F, 4.0F, 6.0F, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.05F, -1.0F, -5.6F, 0.0F, -2.9583F, 0.0F));

        extras.addOrReplaceChild(
                "right_back_wing",
                CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(0.0F, -5.0F, 0.1F, 4.0F, 6.0F, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.7F, -1.0F, -5.8F, 0.0F, -1.0472F, 0.0F));

        extras.addOrReplaceChild(
                "left_back_wing",
                CubeListBuilder.create()
                        .texOffs(0, 19)
                        .addBox(0.0F, -5.0F, 0.0F, 4.0F, 6.0F, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.65F, -2.0F, -5.7F, 0.0F, -2.138F, 0.0F));

        extras.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create()
                        .texOffs(8, 8)
                        .addBox(0.0F, -5.0F, 0.0F, 4.0F, 6.0F, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.95F, -1.0F, -5.6F, 0.0F, -0.2182F, 0.0F));

        return LayerDefinition.create(meshdefinition, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
