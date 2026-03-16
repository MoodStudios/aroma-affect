package com.ovrtechnology.tutorial.intro.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialIntroNetworking;
import com.ovrtechnology.trigger.config.ClientConfig;
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

    // Logo actual: 735x659 pixels
    private static final int LOGO_TEX_WIDTH = 735;
    private static final int LOGO_TEX_HEIGHT = 659;
    private static final int LOGO_WIDTH = 180;
    private static final int LOGO_HEIGHT = (int) (LOGO_WIDTH * ((float) LOGO_TEX_HEIGHT / LOGO_TEX_WIDTH)); // ~161

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
    private Button playDemoButton;
    private Button walkaroundButton;

    public TutorialIntroScreen() {
        super(Component.literal("Tutorial Intro"));
    }

    @Override
    protected void init() {
        super.init();

        // Two buttons side by side
        int buttonSpacing = 10;
        int totalButtonsWidth = BUTTON_WIDTH * 2 + buttonSpacing;
        int totalHeight = LOGO_HEIGHT + GAP + BUTTON_HEIGHT;
        int topY = (this.height - totalHeight) / 2;
        int buttonY = topY + LOGO_HEIGHT + GAP;
        int leftButtonX = (this.width - totalButtonsWidth) / 2;
        int rightButtonX = leftButtonX + BUTTON_WIDTH + buttonSpacing;

        // PLAY DEMO button (starts tutorial)
        playDemoButton = Button.builder(
                Component.literal("\u00a7l PLAY DEMO"),
                btn -> onPlayDemoClicked()
        ).bounds(leftButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        // WALKAROUND button (free exploration)
        walkaroundButton = Button.builder(
                Component.literal("\u00a7f\u00a7l WALKAROUND"),
                btn -> onWalkaroundClicked()
        ).bounds(rightButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        // Buttons start invisible, fade in with the rest
        playDemoButton.active = false;
        playDemoButton.visible = false;
        walkaroundButton.active = false;
        walkaroundButton.visible = false;

        addRenderableWidget(playDemoButton);
        addRenderableWidget(walkaroundButton);
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Enable buttons after fade-in completes
        if (ticksOpen >= FADE_IN_TICKS) {
            if (playDemoButton != null) {
                playDemoButton.active = true;
                playDemoButton.visible = true;
            }
            if (walkaroundButton != null) {
                walkaroundButton.active = true;
                walkaroundButton.visible = true;
            }
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
        // Scale the logo to fit LOGO_WIDTH x LOGO_HEIGHT
        float scale = (float) LOGO_WIDTH / LOGO_TEX_WIDTH;
        graphics.pose().pushMatrix();
        graphics.pose().translate(logoX, logoY);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MAIN_LOGO,
                0, 0, 0.0f, 0.0f,
                LOGO_TEX_WIDTH, LOGO_TEX_HEIGHT, LOGO_TEX_WIDTH, LOGO_TEX_HEIGHT);
        graphics.pose().popMatrix();

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

    private void onPlayDemoClicked() {
        TutorialIntroNetworking.sendPlayDemoToServer(Minecraft.getInstance().player.registryAccess());
        this.onClose();
    }

    private void onWalkaroundClicked() {
        // Set minimum cooldowns for walkaround mode (1 second each)
        ClientConfig config = ClientConfig.getInstance();
        config.setPassiveBlockCooldownMs(1000);
        config.setPassiveMobCooldownMs(1000);
        config.setPassivePassiveMobCooldownMs(1000);
        config.save();

        TutorialIntroNetworking.sendWalkaroundToServer(Minecraft.getInstance().player.registryAccess());
        this.onClose();
    }
}
