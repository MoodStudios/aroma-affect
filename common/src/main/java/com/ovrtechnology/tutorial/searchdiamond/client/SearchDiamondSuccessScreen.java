package com.ovrtechnology.tutorial.searchdiamond.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Success screen shown when player finds the diamond.
 */
public class SearchDiamondSuccessScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;

    private int ticksOpen = 0;

    public SearchDiamondSuccessScreen() {
        super(Component.literal("Diamond Found!"));
    }

    @Override
    protected void init() {
        super.init();

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = this.height / 2 + 40;

        Button continueButton = Button.builder(
                Component.literal("Continue"),
                btn -> this.onClose()
        ).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        addRenderableWidget(continueButton);
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;
        // Auto-close after 3 seconds (60 ticks)
        if (ticksOpen > 60) {
            this.onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent dark overlay
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        var font = Minecraft.getInstance().font;

        // Success title with green color (centered, larger via shadow effect)
        String title = "Diamond Found!";
        int titleWidth = font.width(title);
        int titleX = (this.width - titleWidth) / 2;
        int titleY = this.height / 2 - 40;

        // Draw title with shadow for emphasis
        graphics.drawString(font, title, titleX + 1, titleY + 1, 0xFF003300, false);
        graphics.drawString(font, title, titleX, titleY, 0xFF55FF55, true);

        // Subtitle
        String subtitle = "Great job using your nose!";
        int subtitleWidth = font.width(subtitle);
        int subtitleX = (this.width - subtitleWidth) / 2;
        graphics.drawString(font, subtitle, subtitleX, this.height / 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
