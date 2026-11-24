package com.ovrtechnology.tutorial.finishscreen.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.demo.DemoConfig;
import com.ovrtechnology.tutorial.demo.DemoWorldManager;
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
    private static final int QR_SIZE = 56;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CONTINUE_WIDTH = 170;
    private static final int START_OVER_WIDTH = 110;
    private static final int BUTTON_GAP = 4;
    private static final int KEEP_PLAYING_WIDTH = 140;
    private static final int NEW_GAME_WIDTH = 140;

    // Fade-in animation: 1.5 seconds = 30 ticks
    private static final int FADE_IN_TICKS = 30;

    // Animation state
    private int ticksOpen = 0;
    private Button thanksButton;
    private Button newGameButton;
    private Button startOverButton;
    private Button keepPlayingButton;
    private boolean restartRequested = false;

    private static boolean timeExpired = false;

    public TutorialFinishScreen() {
        super(Component.literal("Thanks for Playing"));
    }

    /**
     * Sets whether the session is over: the time ran out or the player ended the demo.
     * When true the screen offers "Continue playing" (a new countdown in the same world) and, on a
     * demo station, a new game, which it starts by itself after {@link DemoConfig#finishScreenSeconds()}.
     */
    public static void setTimeExpired(boolean expired) {
        timeExpired = expired;
    }

    public static boolean isTimeExpired() {
        return timeExpired;
    }

    @Override
    protected void init() {
        super.init();

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = this.height - BUTTON_HEIGHT - 10;

        if (timeExpired) {
            // Session over. The visitor can keep playing with a new countdown; on a station the
            // screen also offers the next game and starts it by itself if nobody chooses.
            // Without a template (dev world) there is no new game: only "Continue playing".
            boolean station = DemoWorldManager.isStationSession();
            int rowWidth = station ? KEEP_PLAYING_WIDTH + BUTTON_GAP + NEW_GAME_WIDTH : KEEP_PLAYING_WIDTH;
            int rowX = (this.width - rowWidth) / 2;
            keepPlayingButton = Button.builder(
                    Component.literal("Continue playing"),
                    btn -> requestKeepPlaying()
            ).bounds(rowX, buttonY, KEEP_PLAYING_WIDTH, BUTTON_HEIGHT).build();
            keepPlayingButton.active = false;
            keepPlayingButton.visible = false;
            addRenderableWidget(keepPlayingButton);
            if (station) {
                newGameButton = Button.builder(
                        newGameLabel(),
                        btn -> requestRestart()
                ).bounds(rowX + KEEP_PLAYING_WIDTH + BUTTON_GAP, buttonY, NEW_GAME_WIDTH, BUTTON_HEIGHT).build();
                newGameButton.active = false;
                newGameButton.visible = false;
                addRenderableWidget(newGameButton);
            }
            return;
        }

        // The route is done and the visitor may keep exploring. On a station the next visitor
        // may be the one looking at this screen, so it also offers a fresh game.
        boolean station = DemoWorldManager.isStationSession();
        int continueWidth = station ? CONTINUE_WIDTH : BUTTON_WIDTH;
        int continueX = station
                ? (this.width - CONTINUE_WIDTH - BUTTON_GAP - START_OVER_WIDTH) / 2
                : buttonX;

        thanksButton = Button.builder(
                Component.literal("Continue discovering scents"),
                btn -> this.onClose()
        ).bounds(continueX, buttonY, continueWidth, BUTTON_HEIGHT).build();

        thanksButton.active = false;
        thanksButton.visible = false;

        addRenderableWidget(thanksButton);

        if (station) {
            startOverButton = Button.builder(
                    Component.literal("Start a new game"),
                    btn -> requestRestart()
            ).bounds(continueX + CONTINUE_WIDTH + BUTTON_GAP, buttonY, START_OVER_WIDTH, BUTTON_HEIGHT).build();
            startOverButton.active = false;
            startOverButton.visible = false;
            addRenderableWidget(startOverButton);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Enable button after fade-in completes
        if (ticksOpen >= FADE_IN_TICKS && thanksButton != null) {
            thanksButton.active = !restartRequested;
            thanksButton.visible = true;
        }
        if (ticksOpen >= FADE_IN_TICKS && startOverButton != null) {
            startOverButton.active = !restartRequested;
            startOverButton.visible = true;
        }

        if (keepPlayingButton != null && ticksOpen >= FADE_IN_TICKS) {
            keepPlayingButton.active = !restartRequested;
            keepPlayingButton.visible = true;
        }

        if (newGameButton != null) {
            if (ticksOpen >= FADE_IN_TICKS && !restartRequested) {
                newGameButton.active = true;
                newGameButton.visible = true;
            }
            newGameButton.setMessage(newGameLabel());
            if (secondsUntilRestart() <= 0) {
                requestRestart();
            }
        }
    }

    private int secondsUntilRestart() {
        return DemoConfig.get().finishScreenSeconds() - ticksOpen / 20;
    }

    private Component newGameLabel() {
        return Component.literal("Start a new game (" + Math.max(0, secondsUntilRestart()) + ")");
    }

    private void requestRestart() {
        if (restartRequested) return;
        restartRequested = true;
        if (newGameButton != null) {
            newGameButton.active = false;
        }
        if (keepPlayingButton != null) {
            keepPlayingButton.active = false;
        }
        DemoWorldManager.restartSession();
    }

    /** Back into the same world; the server starts a new countdown. */
    private void requestKeepPlaying() {
        if (restartRequested) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            com.ovrtechnology.network.TutorialFinishNetworking.sendContinueToServer(mc.player.registryAccess());
        }
        timeExpired = false;
        super.onClose();
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

        // Calculate total content height to center vertically
        float ovrScale = (float) OVR_DISPLAY_WIDTH / OVR_TEX_WIDTH;
        int ovrDisplayHeight = (int) (OVR_TEX_HEIGHT * ovrScale);
        int gap = 8;
        int labelHeight = 12;
        int totalContentHeight = ovrDisplayHeight + gap + labelHeight + QR_SIZE + gap + BUTTON_HEIGHT;
        int startY = (this.height - totalContentHeight) / 2;

        // Main OVR logo (center) - scaled
        int logoX = (this.width - OVR_DISPLAY_WIDTH) / 2;
        int logoY = startY;
        graphics.pose().pushMatrix();
        graphics.pose().translate(logoX, logoY);
        graphics.pose().scale(ovrScale, ovrScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVR_LOGO,
                0, 0, 0.0f, 0.0f,
                OVR_TEX_WIDTH, OVR_TEX_HEIGHT, OVR_TEX_WIDTH, OVR_TEX_HEIGHT);
        graphics.pose().popMatrix();

        // Single QR code centered below OVR logo
        int qrLabelY = logoY + ovrDisplayHeight + gap;
        int qrY = qrLabelY + labelHeight;
        int qrX = (this.width - QR_SIZE) / 2;

        String qrLabel = "ovrtechnology.com/aroma-affect";
        int qrLabelWidth = Minecraft.getInstance().font.width(qrLabel);
        int qrLabelX = (this.width - qrLabelWidth) / 2;
        graphics.drawString(Minecraft.getInstance().font, qrLabel, qrLabelX, qrLabelY, 0xFFFFFFFF, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, QR_LEFT,
                qrX, qrY, 0.0f, 0.0f,
                QR_SIZE, QR_SIZE, QR_SIZE, QR_SIZE);

        // "Thanks for playing" text above the button
        String thanksText = "Thanks for playing";
        int thanksTextWidth = Minecraft.getInstance().font.width(thanksText);
        int thanksTextX = (this.width - thanksTextWidth) / 2;
        int thanksTextY = this.height - BUTTON_HEIGHT - 22;
        graphics.drawString(Minecraft.getInstance().font, thanksText, thanksTextX, thanksTextY, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        if (timeExpired || restartRequested) {
            return;
        }
        super.onClose();
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
