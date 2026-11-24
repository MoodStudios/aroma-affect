package com.ovrtechnology.tutorial.intro.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialIntroNetworking;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.tutorial.demo.DemoConfig;
import com.ovrtechnology.tutorial.demo.DemoIdleVideo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Start screen of the station.
 * <p>
 * Without a video: the Aroma Affect logo in the middle with the button under it, over the intro flight.
 * With a video ({@link DemoIdleVideo}): the video in a column on the left, sized to keep its proportions
 * (a vertical video fills the height), the OVR logo on top of it, and the logo and the button centered
 * in the rest of the screen.
 * <p>
 * The station shows a single Start button that goes to the free walkaround; the guided demo comes back
 * next to it with {@code showPlayDemo} in the demo config.
 */
public class TutorialIntroScreen extends Screen {

    private static final ResourceLocation MAIN_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/ovr_logo_map.png");

    private static final ResourceLocation PARTNER_LOGO =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/guide/ovr_logo.png");

    private static final float LOGO_ASPECT = 735.0f / 659.0f;
    private static final float PARTNER_ASPECT = 881.0f / 396.0f;
    private static final int LOGO_WIDTH = 180;
    private static final int PARTNER_WIDTH = 70;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 6;
    private static final int GAP = 12;

    /** The video column takes at most this share of the screen width. */
    private static final float MAX_VIDEO_SHARE = 0.45f;
    /** Proportions assumed for the column until the first frame says otherwise. */
    private static final float DEFAULT_VIDEO_ASPECT = 9.0f / 16.0f;

    // Fade-in animation: 1.5 seconds = 30 ticks
    private static final int FADE_IN_TICKS = 30;

    private int ticksOpen = 0;
    private Button primaryButton;
    private Button walkaroundButton;

    // Looping video in the left column, when the station has one in its game folder
    private DemoIdleVideo video;
    private boolean videoChecked = false;
    private float laidOutAspect = -1.0f;

    // Current layout, in GUI pixels
    private int videoX, videoY, videoW, videoH;
    private int columnW;
    private int logoX, logoY, logoW, logoH;

    public TutorialIntroScreen() {
        super(Component.literal("Tutorial Intro"));
    }

    @Override
    protected void init() {
        super.init();

        if (!videoChecked) {
            videoChecked = true;
            video = DemoIdleVideo.openIfPresent();
        }

        if (DemoConfig.get().showPlayDemo()) {
            primaryButton = Button.builder(Component.literal("§l PLAY DEMO"), btn -> onPlayDemoClicked())
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
            walkaroundButton = Button.builder(Component.literal("§f§l WALKAROUND"), btn -> onWalkaroundClicked())
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        } else {
            primaryButton = Button.builder(Component.literal("§l START"), btn -> onWalkaroundClicked())
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
            walkaroundButton = null;
        }

        // Buttons start invisible, fade in with the rest
        for (Button button : buttons()) {
            button.active = false;
            button.visible = false;
            addRenderableWidget(button);
        }

        layout();
    }

    private Button[] buttons() {
        return walkaroundButton == null ? new Button[]{primaryButton} : new Button[]{primaryButton, walkaroundButton};
    }

    /** Places the video column, the logo and the buttons for the current screen and video. */
    private void layout() {
        int areaX = 0;
        int areaW = this.width;

        columnW = 0;
        videoW = videoH = 0;
        if (video != null) {
            float aspect = video.aspect() > 0 ? video.aspect() : DEFAULT_VIDEO_ASPECT;
            laidOutAspect = aspect;
            int maxW = Math.round(this.width * MAX_VIDEO_SHARE);
            videoH = this.height;
            videoW = Math.round(videoH * aspect);
            if (videoW > maxW) {
                videoW = maxW;
                videoH = Math.round(videoW / aspect);
            }
            videoX = 0;
            videoY = (this.height - videoH) / 2;
            columnW = videoW;
            areaX = columnW;
            areaW = this.width - columnW;
        }

        boolean stacked = walkaroundButton != null && (video != null || areaW < BUTTON_WIDTH * 2 + 30);
        int buttonW = Math.min(BUTTON_WIDTH, areaW - 20);
        int buttonsH = stacked ? BUTTON_HEIGHT * 2 + BUTTON_SPACING : BUTTON_HEIGHT;

        // The logo takes what is left, without growing past its usual size by much
        int maxLogoH = this.height - buttonsH - GAP - 40;
        logoW = Math.min(Math.round(LOGO_WIDTH * 1.25f), Math.round(areaW * 0.7f));
        logoW = Math.max(40, Math.min(logoW, Math.round(maxLogoH * LOGO_ASPECT)));
        logoH = Math.round(logoW / LOGO_ASPECT);

        int totalH = logoH + GAP + buttonsH;
        logoX = areaX + (areaW - logoW) / 2;
        logoY = (this.height - totalH) / 2;
        int buttonY = logoY + logoH + GAP;

        if (walkaroundButton == null) {
            primaryButton.setRectangle(buttonW, BUTTON_HEIGHT, areaX + (areaW - buttonW) / 2, buttonY);
        } else if (stacked) {
            int x = areaX + (areaW - buttonW) / 2;
            primaryButton.setRectangle(buttonW, BUTTON_HEIGHT, x, buttonY);
            walkaroundButton.setRectangle(buttonW, BUTTON_HEIGHT, x, buttonY + BUTTON_HEIGHT + BUTTON_SPACING);
        } else {
            int x = areaX + (areaW - (BUTTON_WIDTH * 2 + 10)) / 2;
            primaryButton.setRectangle(BUTTON_WIDTH, BUTTON_HEIGHT, x, buttonY);
            walkaroundButton.setRectangle(BUTTON_WIDTH, BUTTON_HEIGHT, x + BUTTON_WIDTH + 10, buttonY);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        if (video != null && video.isBroken()) {
            // The file cannot be played: back to the screen without video
            video.close();
            video = null;
            layout();
        } else if (video != null && video.aspect() > 0 && Math.abs(video.aspect() - laidOutAspect) > 0.001f) {
            // First frame: the column takes the real proportions of the video
            layout();
        }

        // Enable buttons after fade-in completes
        if (ticksOpen >= FADE_IN_TICKS) {
            for (Button button : buttons()) {
                button.active = true;
                button.visible = true;
            }
        }
    }

    @Override
    public void removed() {
        if (video != null) {
            video.close();
            video = null;
        }
        videoChecked = false;
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float time = ticksOpen + partialTick;
        float fade = Math.min(1.0f, time / FADE_IN_TICKS);
        float eased = fade * fade * (3.0f - 2.0f * fade);

        // The intro flight shows through, darkened, behind the logo
        graphics.fill(-100, -100, this.width + 100, this.height + 100, 0xAA << 24);

        if (video != null) {
            graphics.fill(0, 0, columnW, this.height, 0xFF000000);
            if (video.render(graphics, videoX, videoY, videoW, videoH)) {
                // A thin edge between the column and the rest, and a shade at the top so the logo reads
                graphics.fill(columnW, 0, columnW + 1, this.height, 0x40FFFFFF);
                graphics.fillGradient(0, 0, columnW, 48, 0x90000000, 0x00000000);
            }
            int partnerW = Math.max(50, Math.min(110, Math.round(columnW * 0.55f)));
            blitWhole(graphics, PARTNER_LOGO, 8, 8, partnerW, Math.round(partnerW / PARTNER_ASPECT));
        } else {
            blitWhole(graphics, PARTNER_LOGO, 10, 10, PARTNER_WIDTH, Math.round(PARTNER_WIDTH / PARTNER_ASPECT));
        }

        // The logo floats gently, so the screen never looks frozen
        int bob = Math.round((float) Math.sin(time * 0.08f) * 3.0f);
        blitWhole(graphics, MAIN_LOGO, logoX, logoY + bob, logoW, logoH);

        // Emerge from black
        int black = (int) (0xFF * (1.0f - eased));
        if (black > 0) {
            graphics.fill(-100, -100, this.width + 100, this.height + 100, black << 24);
        }
    }

    /** Draws the whole texture stretched to the given size. */
    private static void blitWhole(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, w, h, w, h);
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
