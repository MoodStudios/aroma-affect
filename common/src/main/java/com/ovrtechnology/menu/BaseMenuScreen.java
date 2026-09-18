package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Base class for all Aroma Affect menu screens.
 * Provides common functionality and styling for menu screens.
 */
public abstract class BaseMenuScreen extends Screen {

    /**
     * Standard animation duration in ticks (1 second = 20 ticks).
     */
    protected static final int ANIMATION_DURATION_TICKS = 6;

    /**
     * Standard padding for menu elements.
     */
    protected static final int PADDING = 8;

    /**
     * The current animation progress (0.0 to 1.0).
     */
    protected float animationProgress = 0.0f;

    /**
     * Whether the menu is currently animating in.
     */
    protected boolean isAnimatingIn = true;

    /**
     * Whether the menu is currently animating out (closing).
     */
    protected boolean isAnimatingOut = false;

    /**
     * Tick counter for animations.
     */
    protected int animationTicks = 0;

    /**
     * The background overlay color (ARGB format).
     */
    protected int backgroundColor = 0x80000000;

    // â”€â”€ Notification System â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Duration in milliseconds before notifications auto-hide.
     */
    private static final long NOTIFICATION_DURATION_MS = 3000;

    /**
     * Current notification message (null if none).
     */
    private Component notificationMessage = null;

    /**
     * Timestamp when the notification was shown.
     */
    private long notificationTimestamp = 0;

    /**
     * Whether this is an error notification (red) or info (blue).
     */
    private boolean notificationIsError = false;
    
    private boolean layoutReady;

    protected final int screenX(double x) { return (int) Math.round(x); }
    protected final int screenY(double y) { return (int) Math.round(y); }


    protected BaseMenuScreen(Component title) {
        super(title);
    }
    
    @Override
    protected void init() {
        super.init();
        layoutReady = false;
        setDragging(false);
        // Reset animation state when screen is opened
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
            animationProgress = Math.max(0.0f, 1.0f - (float) animationTicks / ANIMATION_DURATION_TICKS);
            if (animationProgress <= 0.0f) {
                isAnimatingOut = false;
                onAnimationOutComplete();
            }
        }
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int logicalMouseX = mouseX;
        int logicalMouseY = mouseY;
        graphics.pose().pushMatrix();
        try {
            float smoothProgress = getSmoothAnimationProgress(partialTick);
            renderMenuBackground(graphics, smoothProgress);
            renderContent(graphics, logicalMouseX, logicalMouseY, partialTick, smoothProgress);
            renderNotification(graphics);
            super.extractRenderState(graphics, logicalMouseX, logicalMouseY, partialTick);
            layoutReady = true;
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected void repositionElements() {
        int focusedIndex = children().indexOf(getFocused());
        float progress = animationProgress;
        boolean opening = isAnimatingIn;
        boolean closing = isAnimatingOut;
        int ticks = animationTicks;
        super.repositionElements();
        if (focusedIndex >= 0 && focusedIndex < children().size()) {
            setFocused(children().get(focusedIndex));
        }
        animationProgress = progress;
        isAnimatingIn = opening;
        isAnimatingOut = closing;
        animationTicks = ticks;
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean isValidClickButton) {
        if (!layoutReady) return false;
        MouseButtonEvent local = event;
        if (handleMouseClick(local.x(), local.y(), local.button())) return true;
        return super.mouseClicked(local, isValidClickButton);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!layoutReady) return false;
        double x = mouseX, y = mouseY;
        if (handleMouseScroll(x, y, scrollX, scrollY)) return true;
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!layoutReady) return false;
        MouseButtonEvent local = event;
        return handleMouseDrag(local, dx, dy);
    }

    protected boolean handleMouseDrag(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        return handleMouseRelease(event);
    }

    protected boolean handleMouseRelease(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (handleKeyPress(event.key(), event.scancode(), event.modifiers())) {
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * Handles mouse click events for this menu.
     * Override in subclasses to implement custom click behavior.
     */
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Handles mouse scroll events for this menu.
     * Override in subclasses to implement custom scrolling behavior.
     */
    protected boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    /**
     * Handles key press events for this menu.
     * Override in subclasses to implement custom key behavior.
     */
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        return false;
    }
    
    /**
     * Renders the semi-transparent background overlay.
     */
    protected void renderMenuBackground(GuiGraphicsExtractor graphics, float animationProgress) {
        int alpha = (int) (128 * animationProgress);
        int color = (alpha << 24);
        graphics.fill(0, 0, width, height, color);
    }
    
    /**
     * Override this to render the menu's main content.
     * 
     * @param graphics the graphics context
     * @param mouseX the mouse X position
     * @param mouseY the mouse Y position  
     * @param partialTick the partial tick for smooth rendering
     * @param animationProgress the current animation progress (0.0 to 1.0)
     */
    protected abstract void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                          float partialTick, float animationProgress);

    // â”€â”€ Notification Methods â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Shows an error notification at the top of the screen.
     *
     * @param message the message to display
     */
    protected void showErrorNotification(Component message) {
        this.notificationMessage = message;
        this.notificationTimestamp = System.currentTimeMillis();
        this.notificationIsError = true;
    }

    /**
     * Shows an info notification at the top of the screen.
     *
     * @param message the message to display
     */
    protected void showInfoNotification(Component message) {
        this.notificationMessage = message;
        this.notificationTimestamp = System.currentTimeMillis();
        this.notificationIsError = false;
    }

    /**
     * Renders the notification banner if one is active.
     */
    private void renderNotification(GuiGraphicsExtractor graphics) {
        if (notificationMessage == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - notificationTimestamp;
        if (elapsed > NOTIFICATION_DURATION_MS) {
            notificationMessage = null;
            return;
        }

        // Calculate fade animation
        float fadeIn = Math.min(1.0f, elapsed / 200.0f);
        float fadeOut = elapsed > (NOTIFICATION_DURATION_MS - 500)
                ? 1.0f - (elapsed - (NOTIFICATION_DURATION_MS - 500)) / 500.0f
                : 1.0f;
        float alpha = fadeIn * fadeOut;

        if (alpha <= 0) {
            return;
        }

        // Calculate dimensions
        int padding = 12;
        int bannerWidth = Math.min(width - 24, font.width(notificationMessage) + padding * 2);
        var lines = font.split(notificationMessage, bannerWidth - padding * 2);
        int bannerHeight = Math.max(24, lines.size() * font.lineHeight + 12);
        int bannerX = (width - bannerWidth) / 2;
        int bannerY = 20;

        // Slide in from top
        float slideProgress = easeOutCubic(Math.min(1.0f, elapsed / 150.0f));
        bannerY = (int) (bannerY * slideProgress - bannerHeight * (1 - slideProgress));

        // Colors based on error/info
        int bgColor = notificationIsError
                ? MenuRenderUtils.withAlpha(0xDD442222, alpha)
                : MenuRenderUtils.withAlpha(0xDD224444, alpha);
        int borderColor = notificationIsError
                ? MenuRenderUtils.withAlpha(0xFFFF6B6B, alpha)
                : MenuRenderUtils.withAlpha(0xFF6B9FFF, alpha);
        int textColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, alpha);

        // Draw background
        graphics.fill(bannerX, bannerY, bannerX + bannerWidth, bannerY + bannerHeight, bgColor);

        // Draw border
        MenuRenderUtils.renderOutline(graphics, bannerX, bannerY, bannerWidth, bannerHeight, borderColor);

        // Draw left accent bar
        int accentColor = notificationIsError
                ? MenuRenderUtils.withAlpha(0xFFFF4444, alpha)
                : MenuRenderUtils.withAlpha(0xFF4488FF, alpha);
        graphics.fill(bannerX, bannerY, bannerX + 3, bannerY + bannerHeight, accentColor);

        // Draw text centered
        int textX = bannerX + padding;
        int textY = bannerY + 6;
        for (var line : lines) {
            graphics.text(font, line, textX, textY, textColor, false);
            textY += font.lineHeight;
        }
    }

    /**
     * Called when the opening animation completes.
     */
    protected void onAnimationInComplete() {
        // Override in subclasses if needed
    }
    
    /**
     * Called when the closing animation completes.
     * Default behavior closes the screen.
     */
    protected void onAnimationOutComplete() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(null);
        }
    }
    
    /**
     * Starts the closing animation.
     * The screen will be closed when the animation completes.
     */
    protected void startClosingAnimation() {
        if (!isAnimatingOut) {
            isAnimatingOut = true;
            isAnimatingIn = false;
            animationTicks = 0;
        }
    }
    
    /**
     * Gets the interpolated animation progress for smooth rendering.
     */
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
    
    /**
     * Cubic ease-out function for smooth animations.
     */
    protected static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3);
    }
    
    /**
     * Cubic ease-in function for smooth animations.
     */
    protected static float easeInCubic(float t) {
        return t * t * t;
    }
    
    /**
     * Elastic ease-out function for bouncy animations.
     */
    protected static float easeOutElastic(float t) {
        if (t == 0 || t == 1) return t;
        double p = 0.3;
        double s = p / 4;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t - s) * (2 * Math.PI) / p) + 1);
    }
    
    /**
     * Back ease-out function for overshoot animations.
     */
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
            // Start closing animation instead of immediately closing
            startClosingAnimation();
        } else if (!isAnimatingOut) {
            super.onClose();
        }
    }
    
    /**
     * Creates a resource location for Aroma Affect assets.
     */
    protected static Identifier aromaLocation(String path) {
        return Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, path);
    }
}
