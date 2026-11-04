package com.ovrtechnology.legacy.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Vector2f;
import org.joml.Vector3f;

public final class Pose2D {

    private final PoseStack stack;

    Pose2D(PoseStack stack) {
        this.stack = stack;
    }

    public PoseStack poseStack() {
        return stack;
    }

    public Pose2D pushMatrix() {
        stack.pushPose();
        return this;
    }

    public Pose2D popMatrix() {
        stack.popPose();
        return this;
    }

    public Pose2D translate(float x, float y) {
        stack.translate(x, y, 0.0F);
        return this;
    }

    public Pose2D scale(float x, float y) {
        stack.scale(x, y, 1.0F);
        return this;
    }

    public Pose2D scale(float xy) {
        return scale(xy, xy);
    }

    public Pose2D rotate(float radians) {
        stack.mulPose(Axis.ZP.rotation(radians));
        return this;
    }

    public Vector2f transformPosition(float x, float y, Vector2f dest) {
        Vector3f v = stack.last().pose().transformPosition(x, y, 0.0F, new Vector3f());
        return dest.set(v.x, v.y);
    }
}
