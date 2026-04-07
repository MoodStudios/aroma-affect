package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class NoseStrapToggleButton extends ImageButton {
    public static final int SIZE = 12;

    private static final Identifier STRAP_ON = Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose/strap_on");
    private static final Identifier STRAP_OFF = Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose/strap_off");
    private static final WidgetSprites PLACEHOLDER_SPRITES = new WidgetSprites(STRAP_OFF);

    public NoseStrapToggleButton(int x, int y, OnPress onPress) {
        super(
                x,
                y,
                SIZE,
                SIZE,
                PLACEHOLDER_SPRITES,
                onPress,
                Component.translatable("gui.aromaaffect.nose_strap.tooltip")
        );
        setTooltip(Tooltip.create(Component.translatable("gui.aromaaffect.nose_strap.tooltip")));
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Identifier sprite = NoseRenderToggles.isStrapEnabled() ? STRAP_ON : STRAP_OFF;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), width, height);
    }
}
