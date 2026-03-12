package com.ovrtechnology.tutorial.finishscreen.client;

import com.ovrtechnology.AromaAffect;
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
    private static final int MOOD_DISPLAY_WIDTH = 90;

    // OVR Technology partner logo (top-left)
    private static final ResourceLocation PARTNER_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/guide/ovr_logo.png");
    private static final int PARTNER_WIDTH = 70;
    private static final int PARTNER_HEIGHT = 31;

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
        int buttonY = this.height / 2 + 50;

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

        // OVR Technology partner logo (top-left corner)
        graphics.blit(RenderPipelines.GUI_TEXTURED, PARTNER_LOGO,
                10, 10, 0.0f, 0.0f,
                PARTNER_WIDTH, PARTNER_HEIGHT, PARTNER_WIDTH, PARTNER_HEIGHT);

        // Mood Studios logo (top-right corner) - scaled
        float moodScale = (float) MOOD_DISPLAY_WIDTH / MOOD_TEX_WIDTH;
        int moodDisplayHeight = (int) (MOOD_TEX_HEIGHT * moodScale);
        int moodX = this.width - MOOD_DISPLAY_WIDTH - 10;
        graphics.pose().pushMatrix();
        graphics.pose().translate(moodX, 10);
        graphics.pose().scale(moodScale, moodScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MOOD_LOGO,
                0, 0, 0.0f, 0.0f,
                MOOD_TEX_WIDTH, MOOD_TEX_HEIGHT, MOOD_TEX_WIDTH, MOOD_TEX_HEIGHT);
        graphics.pose().popMatrix();

        // Main OVR logo (center) - scaled
        float ovrScale = (float) OVR_DISPLAY_WIDTH / OVR_TEX_WIDTH;
        int ovrDisplayHeight = (int) (OVR_TEX_HEIGHT * ovrScale);
        int logoX = (this.width - OVR_DISPLAY_WIDTH) / 2;
        int logoY = this.height / 2 - ovrDisplayHeight / 2 - 20;
        graphics.pose().pushMatrix();
        graphics.pose().translate(logoX, logoY);
        graphics.pose().scale(ovrScale, ovrScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVR_LOGO,
                0, 0, 0.0f, 0.0f,
                OVR_TEX_WIDTH, OVR_TEX_HEIGHT, OVR_TEX_WIDTH, OVR_TEX_HEIGHT);
        graphics.pose().popMatrix();
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
