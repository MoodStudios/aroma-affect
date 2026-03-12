package com.ovrtechnology.tutorial.finishscreen.client;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TutorialFinishScreen extends Screen {

    // Main OVR logo (center) - 881x396 actual texture size
    private static final ResourceLocation OVR_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/ovr_logo.png");
    private static final int OVR_TEX_WIDTH = 881;
    private static final int OVR_TEX_HEIGHT = 396;
    private static final int OVR_DISPLAY_WIDTH = 250;

    // Mood Studios logo (top-right) - 735x289 actual texture size
    private static final ResourceLocation MOOD_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/moodlogo.png");
    private static final int MOOD_TEX_WIDTH = 735;
    private static final int MOOD_TEX_HEIGHT = 289;
    private static final int MOOD_DISPLAY_WIDTH = 60;

    // QR codes - 200x200 texture size
    private static final ResourceLocation QR_LEFT =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/qr_left.png");
    private static final ResourceLocation QR_RIGHT =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/qr_right.png");
    private static final int QR_SIZE = 64;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;

    private Button thanksButton;

    public TutorialFinishScreen() {
        super(Component.literal("Thanks for Playing"));
    }

    @Override
    protected void init() {
        super.init();

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        // Position button below the QR codes
        int buttonY = this.height / 2 + QR_SIZE + 30;

        thanksButton = Button.builder(
                Component.literal("Thanks for playing!"),
                btn -> this.onClose()
        ).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        addRenderableWidget(thanksButton);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark overlay
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        // "Powered by" text + Mood Studios logo (top-right corner)
        float moodScale = (float) MOOD_DISPLAY_WIDTH / MOOD_TEX_WIDTH;
        int moodDisplayHeight = (int) (MOOD_TEX_HEIGHT * moodScale);
        int moodX = this.width - MOOD_DISPLAY_WIDTH - 10;

        // "Powered by" text above Mood logo
        String poweredByText = "Powered by";
        int textWidth = Minecraft.getInstance().font.width(poweredByText);
        int textX = moodX + (MOOD_DISPLAY_WIDTH - textWidth) / 2;
        graphics.drawString(Minecraft.getInstance().font, poweredByText, textX, 8, 0xFFFFFFFF, false);

        // Mood Studios logo below text
        graphics.pose().pushMatrix();
        graphics.pose().translate(moodX, 20);
        graphics.pose().scale(moodScale, moodScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MOOD_LOGO,
                0, 0, 0.0f, 0.0f,
                MOOD_TEX_WIDTH, MOOD_TEX_HEIGHT, MOOD_TEX_WIDTH, MOOD_TEX_HEIGHT);
        graphics.pose().popMatrix();

        // Main OVR logo (center) - scaled
        float ovrScale = (float) OVR_DISPLAY_WIDTH / OVR_TEX_WIDTH;
        int ovrDisplayHeight = (int) (OVR_TEX_HEIGHT * ovrScale);
        int logoX = (this.width - OVR_DISPLAY_WIDTH) / 2;
        int logoY = this.height / 2 - ovrDisplayHeight - 10;
        graphics.pose().pushMatrix();
        graphics.pose().translate(logoX, logoY);
        graphics.pose().scale(ovrScale, ovrScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVR_LOGO,
                0, 0, 0.0f, 0.0f,
                OVR_TEX_WIDTH, OVR_TEX_HEIGHT, OVR_TEX_WIDTH, OVR_TEX_HEIGHT);
        graphics.pose().popMatrix();

        // QR codes below OVR logo
        int qrY = logoY + ovrDisplayHeight + 15;
        int qrSpacing = 20;
        int totalQrWidth = QR_SIZE * 2 + qrSpacing;
        int qrLeftX = (this.width - totalQrWidth) / 2;
        int qrRightX = qrLeftX + QR_SIZE + qrSpacing;

        graphics.blit(RenderPipelines.GUI_TEXTURED, QR_LEFT,
                qrLeftX, qrY, 0.0f, 0.0f,
                QR_SIZE, QR_SIZE, QR_SIZE, QR_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, QR_RIGHT,
                qrRightX, qrY, 0.0f, 0.0f,
                QR_SIZE, QR_SIZE, QR_SIZE, QR_SIZE);
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
