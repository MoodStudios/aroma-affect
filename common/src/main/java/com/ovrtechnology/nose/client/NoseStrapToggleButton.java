package com.ovrtechnology.nose.client;

import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;

public final class NoseStrapToggleButton extends ImageButton {
    public static final int SIZE = 12;

    private static final ResourceLocation STRAP_ON = Ids.mod("nose/strap_on");
    private static final ResourceLocation STRAP_OFF = Ids.mod("nose/strap_off");
    private static final WidgetSprites PLACEHOLDER_SPRITES =
            new WidgetSprites(STRAP_OFF, STRAP_OFF);

    public NoseStrapToggleButton(int x, int y, OnPress onPress) {
        super(
                x,
                y,
                SIZE,
                SIZE,
                PLACEHOLDER_SPRITES,
                onPress,
                Texts.tr("gui.aromaaffect.nose_strap.tooltip"));
        setTooltip(Tooltip.create(Texts.tr("gui.aromaaffect.nose_strap.tooltip")));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ResourceLocation sprite = NoseRenderToggles.isStrapEnabled() ? STRAP_ON : STRAP_OFF;
        graphics.blitSprite(sprite, getX(), getY(), width, height);
    }
}
