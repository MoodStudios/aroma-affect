package com.ovrtechnology.menu;

import com.ovrtechnology.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class BaseMenuScreen extends Screen {

    protected static final int ANIMATION_DURATION_TICKS = 6;

    protected static final int PADDING = 8;

    protected float animationProgress = 0.0f;

    protected boolean isAnimatingIn = true;

    protected boolean isAnimatingOut = false;

    protected int animationTicks = 0;

    protected int backgroundColor = Colors.OVERLAY_DARK_STRONG;

    private static final long NOTIFICATION_DURATION_MS = 3000;

    private Component notificationMessage = null;

    private long notificationTimestamp = 0;

    private boolean notificationIsError = false;

    protected BaseMenuScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        animationProgress = 0.0f;
        isAnimatingIn = true;
        isAnimatingOut = false;
        animationTicks = 0;
    }

    @Override
    public void tick() {
        super.tick();

        if (isAnimatingIn) {
            animationTicks++;
            animationProgress = Math.min(1.0f, (float) animationTicks / ANIMATION_DURATION_TICKS);
            if (animationProgress >= 1.0f) {
                isAnimatingIn = false;
                onAnimationInComplete();
            }
        } else if (isAnimatingOut) {
            animationTicks++;
            animationProgress =
                    Math.max(0.0f, 1.0f - (float) animationTicks / ANIMATION_DURATION_TICKS);
            if (animationProgress <= 0.0f) {
                isAnimatingOut = false;
                onAnimationOutComplete();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        float smoothProgress = getSmoothAnimationProgress(partialTick);

        renderMenuBackground(graphics, smoothProgress);

        renderContent(graphics, mouseX, mouseY, partialTick, smoothProgress);

        renderNotification(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isValidClickButton) {
        if (handleMouseClick(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, isValidClickButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (handleMouseScroll(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (handleKeyPress(event.key(), event.scancode(), event.modifiers())) {
            return true;
        }
        return super.keyPressed(event);
    }

    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean handleMouseScroll(
            double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    protected void renderMenuBackground(GuiGraphics graphics, float animationProgress) {
        int alpha = (int) (128 * animationProgress);
        int color = (alpha << 24);
        graphics.fill(0, 0, width, height, color);
    }

    protected abstract void renderContent(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            float animationProgress);

    protected void showErrorNotification(Component message) {
        this.notificationMessage = message;
        this.notificationTimestamp = System.currentTimeMillis();
        this.notificationIsError = true;
    }

    private void renderNotification(GuiGraphics graphics) {
        if (notificationMessage == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - notificationTimestamp;
        if (elapsed > NOTIFICATION_DURATION_MS) {
            notificationMessage = null;
            return;
        }

        float fadeIn = Math.min(1.0f, elapsed / 200.0f);
        float fadeOut =
                elapsed > (NOTIFICATION_DURATION_MS - 500)
                        ? 1.0f - (elapsed - (NOTIFICATION_DURATION_MS - 500)) / 500.0f
                        : 1.0f;
        float alpha = fadeIn * fadeOut;

        if (alpha <= 0) {
            return;
        }

        int textWidth = font.width(notificationMessage);
        int padding = 12;
        int bannerWidth = textWidth + padding * 2;
        int bannerHeight = 24;
        int bannerX = (width - bannerWidth) / 2;
        int bannerY = 20;

        float slideProgress = easeOutCubic(Math.min(1.0f, elapsed / 150.0f));
        bannerY = (int) (bannerY * slideProgress - bannerHeight * (1 - slideProgress));

        int bgColor =
                notificationIsError
                        ? MenuRenderUtils.withAlpha(0xDD442222, alpha)
                        : MenuRenderUtils.withAlpha(0xDD224444, alpha);
        int borderColor =
                notificationIsError
                        ? MenuRenderUtils.withAlpha(Colors.ERROR_RED_PASTEL, alpha)
                        : MenuRenderUtils.withAlpha(0xFF6B9FFF, alpha);
        int textColor = MenuRenderUtils.withAlpha(Colors.WHITE, alpha);

        graphics.fill(bannerX, bannerY, bannerX + bannerWidth, bannerY + bannerHeight, bgColor);

        MenuRenderUtils.renderOutline(
                graphics, bannerX, bannerY, bannerWidth, bannerHeight, borderColor);

        int accentColor =
                notificationIsError
                        ? MenuRenderUtils.withAlpha(Colors.ERROR_RED, alpha)
                        : MenuRenderUtils.withAlpha(0xFF4488FF, alpha);
        graphics.fill(bannerX, bannerY, bannerX + 3, bannerY + bannerHeight, accentColor);

        int textX = bannerX + padding;
        int textY = bannerY + (bannerHeight - 8) / 2;
        graphics.drawString(font, notificationMessage, textX, textY, textColor, false);
    }

    protected void onAnimationInComplete() {}

    protected void onAnimationOutComplete() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    protected void startClosingAnimation() {
        if (!isAnimatingOut) {
            isAnimatingOut = true;
            isAnimatingIn = false;
            animationTicks = 0;
        }
    }

    protected float getSmoothAnimationProgress(float partialTick) {
        if (isAnimatingIn) {
            float tickProgress = (animationTicks + partialTick) / ANIMATION_DURATION_TICKS;
            return easeOutCubic(Math.min(1.0f, tickProgress));
        } else if (isAnimatingOut) {
            float tickProgress = (animationTicks + partialTick) / ANIMATION_DURATION_TICKS;
            return easeInCubic(Math.max(0.0f, 1.0f - tickProgress));
        }
        return animationProgress;
    }

    protected static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3);
    }

    protected static float easeInCubic(float t) {
        return t * t * t;
    }

    protected static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!isAnimatingOut && animationProgress > 0) {

            startClosingAnimation();
        } else if (!isAnimatingOut) {
            super.onClose();
        }
    }
}
