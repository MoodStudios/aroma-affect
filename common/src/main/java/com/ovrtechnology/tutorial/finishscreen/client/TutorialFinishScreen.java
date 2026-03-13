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
    private static final int OVR_DISPLAY_WIDTH = 190;

    // Mood Studios logo (top-right) - 735x289 actual texture size
    private static final ResourceLocation MOOD_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/moodlogo.png");
    private static final int MOOD_TEX_WIDTH = 735;
    private static final int MOOD_TEX_HEIGHT = 289;
    private static final int MOOD_DISPLAY_WIDTH = 50;

    // QR codes - 50x50 display size
    private static final ResourceLocation QR_LEFT =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/qr_left.png");
    private static final ResourceLocation QR_RIGHT =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/qr_right.png");
    private static final int QR_SIZE = 50;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;

    // Fade-in animation: 1.5 seconds = 30 ticks
    private static final int FADE_IN_TICKS = 30;

    // Animation state
    private int ticksOpen = 0;
    private Button thanksButton;

    public TutorialFinishScreen() {
        super(Component.literal("Thanks for Playing"));
    }

    @Override
    protected void init() {
        super.init();

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        // Position button below the "Thanks for playing" text
        int buttonY = this.height - 40;

        thanksButton = Button.builder(
                Component.literal("Continue discovering scents"),
                btn -> this.onClose()
        ).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        // Button starts invisible, fades in with the rest
        thanksButton.active = false;
        thanksButton.visible = false;

        addRenderableWidget(thanksButton);
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Enable button after fade-in completes
        if (ticksOpen >= FADE_IN_TICKS && thanksButton != null) {
            thanksButton.active = true;
            thanksButton.visible = true;
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Calculate fade progress (0.0 to 1.0) with smoothstep
        float fadeProgress = Math.min(1.0f, (ticksOpen + partialTick) / FADE_IN_TICKS);
        float alpha = fadeProgress * fadeProgress * (3.0f - 2.0f * fadeProgress);

        // During fade-in: start fully black (0xFF) and transition to target overlay (0xAA)
        int overlayAlpha;
        if (alpha < 1.0f) {
            overlayAlpha = (int) (0xFF - (0xFF - 0xAA) * alpha) & 0xFF;
        } else {
            overlayAlpha = 0xAA;
        }
        int overlayColor = (overlayAlpha << 24);
        graphics.fill(-100, -100, this.width + 100, this.height + 100, overlayColor);

        // Don't render content until fade has started enough
        if (alpha < 0.05f) return;

        // "Powered by" text + Mood Studios logo (top-right corner)
        float moodScale = (float) MOOD_DISPLAY_WIDTH / MOOD_TEX_WIDTH;
        int moodDisplayHeight = (int) (MOOD_TEX_HEIGHT * moodScale);
        int moodX = this.width - MOOD_DISPLAY_WIDTH - 10;

        // "Powered by" text above Mood logo (smaller font using scale)
        String poweredByText = "Powered by";
        float textScale = 0.7f;
        int textWidth = (int) (Minecraft.getInstance().font.width(poweredByText) * textScale);
        int textX = moodX + (MOOD_DISPLAY_WIDTH - textWidth) / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, 10);
        graphics.pose().scale(textScale, textScale);
        graphics.drawString(Minecraft.getInstance().font, poweredByText, 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popMatrix();

        // Mood Studios logo below text
        graphics.pose().pushMatrix();
        graphics.pose().translate(moodX, 22);
        graphics.pose().scale(moodScale, moodScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MOOD_LOGO,
                0, 0, 0.0f, 0.0f,
                MOOD_TEX_WIDTH, MOOD_TEX_HEIGHT, MOOD_TEX_WIDTH, MOOD_TEX_HEIGHT);
        graphics.pose().popMatrix();

        // Main OVR logo (center) - scaled
        float ovrScale = (float) OVR_DISPLAY_WIDTH / OVR_TEX_WIDTH;
        int ovrDisplayHeight = (int) (OVR_TEX_HEIGHT * ovrScale);
        int logoX = (this.width - OVR_DISPLAY_WIDTH) / 2;
        int logoY = this.height / 2 - ovrDisplayHeight - 20;
        graphics.pose().pushMatrix();
        graphics.pose().translate(logoX, logoY);
        graphics.pose().scale(ovrScale, ovrScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVR_LOGO,
                0, 0, 0.0f, 0.0f,
                OVR_TEX_WIDTH, OVR_TEX_HEIGHT, OVR_TEX_WIDTH, OVR_TEX_HEIGHT);
        graphics.pose().popMatrix();

        // QR codes below OVR logo with labels
        int qrSpacing = 60;
        int totalQrWidth = QR_SIZE * 2 + qrSpacing;
        int qrLeftX = (this.width - totalQrWidth) / 2;
        int qrRightX = qrLeftX + QR_SIZE + qrSpacing;
        int qrLabelY = logoY + ovrDisplayHeight + 10;
        int qrY = qrLabelY + 12;

        // QR Code 1 label and image
        String qr1Label = "QR Code 1";
        int qr1LabelWidth = Minecraft.getInstance().font.width(qr1Label);
        int qr1LabelX = qrLeftX + (QR_SIZE - qr1LabelWidth) / 2;
        graphics.drawString(Minecraft.getInstance().font, qr1Label, qr1LabelX, qrLabelY, 0xFFFFFFFF, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, QR_LEFT,
                qrLeftX, qrY, 0.0f, 0.0f,
                QR_SIZE, QR_SIZE, QR_SIZE, QR_SIZE);

        // QR Code 2 label and image
        String qr2Label = "QR Code 2";
        int qr2LabelWidth = Minecraft.getInstance().font.width(qr2Label);
        int qr2LabelX = qrRightX + (QR_SIZE - qr2LabelWidth) / 2;
        graphics.drawString(Minecraft.getInstance().font, qr2Label, qr2LabelX, qrLabelY, 0xFFFFFFFF, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, QR_RIGHT,
                qrRightX, qrY, 0.0f, 0.0f,
                QR_SIZE, QR_SIZE, QR_SIZE, QR_SIZE);

        // "Thanks for playing" text above the button
        String thanksText = "Thanks for playing";
        int thanksTextWidth = Minecraft.getInstance().font.width(thanksText);
        int thanksTextX = (this.width - thanksTextWidth) / 2;
        int thanksTextY = this.height - 55;
        graphics.drawString(Minecraft.getInstance().font, thanksText, thanksTextX, thanksTextY, 0xFFFFFFFF, true);
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
