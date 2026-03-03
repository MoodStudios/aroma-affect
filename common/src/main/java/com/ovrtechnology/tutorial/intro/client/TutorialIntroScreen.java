package com.ovrtechnology.tutorial.intro.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialIntroNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TutorialIntroScreen extends Screen {

    private static final ResourceLocation MAIN_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/ovr_logo_map.png");

    private static final ResourceLocation PARTNER_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/guide/ovr_logo.png");

    private static final int LOGO_WIDTH = 180;
    private static final int LOGO_HEIGHT = 97;  // 735:396 aspect ratio

    // Partner logo: 881x396 -> display 70x31
    private static final int PARTNER_WIDTH = 70;
    private static final int PARTNER_HEIGHT = 31;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 12;

    // Fade-in animation: 1.5 seconds = 30 ticks
    private static final int FADE_IN_TICKS = 30;

    // Animation state
    private int ticksOpen = 0;
    private Button startButton;

    public TutorialIntroScreen() {
        super(Component.literal("Tutorial Intro"));
    }

    @Override
    protected void init() {
        super.init();

        int totalHeight = LOGO_HEIGHT + GAP + BUTTON_HEIGHT;
        int topY = (this.height - totalHeight) / 2;
        int buttonY = topY + LOGO_HEIGHT + GAP;

        startButton = Button.builder(
                Component.literal("\u00a7l START EXPERIENCE"),
                btn -> onStartClicked()
        ).bounds((this.width - BUTTON_WIDTH) / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        // Button starts invisible, fades in with the rest
        startButton.active = false;
        startButton.visible = false;
        addRenderableWidget(startButton);
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Enable button after fade-in completes
        if (startButton != null && ticksOpen >= FADE_IN_TICKS) {
            startButton.active = true;
            startButton.visible = true;
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Calculate fade progress (0.0 to 1.0) with smoothstep
        float fadeProgress = Math.min(1.0f, (ticksOpen + partialTick) / FADE_IN_TICKS);
        float alpha = fadeProgress * fadeProgress * (3.0f - 2.0f * fadeProgress);

        // During fade-in: start fully black (0xFF) and transition to target overlay (0xAA)
        // This creates a "emerge from black" effect that naturally reveals the logo
        int overlayAlpha;
        if (alpha < 1.0f) {
            // Lerp from fully opaque (0xFF) to target (0xAA)
            overlayAlpha = (int) (0xFF - (0xFF - 0xAA) * alpha) & 0xFF;
        } else {
            overlayAlpha = 0xAA;
        }
        int overlayColor = (overlayAlpha << 24);
        graphics.fill(-100, -100, this.width + 100, this.height + 100, overlayColor);

        // Don't render content until fade has started enough
        if (alpha < 0.05f) return;

        int totalHeight = LOGO_HEIGHT + GAP + BUTTON_HEIGHT;
        int logoY = (this.height - totalHeight) / 2;
        int logoX = (this.width - LOGO_WIDTH) / 2;

        // Logo (rendered behind overlay — becomes visible as overlay fades from black to semi-transparent)
        graphics.blit(RenderPipelines.GUI_TEXTURED, MAIN_LOGO,
                logoX, logoY, 0.0f, 0.0f,
                LOGO_WIDTH, LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);

        // Partner logo (top-left corner)
        graphics.blit(RenderPipelines.GUI_TEXTURED, PARTNER_LOGO,
                10, 10, 0.0f, 0.0f,
                PARTNER_WIDTH, PARTNER_HEIGHT, PARTNER_WIDTH, PARTNER_HEIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Block ESC - player must click the button
        if (event.isEscape()) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void onStartClicked() {
        TutorialIntroNetworking.sendStartToServer(Minecraft.getInstance().player.registryAccess());
        this.onClose();
    }
}
