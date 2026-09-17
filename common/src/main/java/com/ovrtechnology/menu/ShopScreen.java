package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.net.URI;

/**
 * Shop screen displaying the Omara scent display product.
 * Features an animated product showcase with info and buy actions.
 */
public class ShopScreen extends BaseMenuScreen {


    private static final Identifier OMARA_COVER = Identifier.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/shop/omara_cover.png");
    private static final Identifier ICON_BACK = Identifier.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_back.png");

    private static final String BUY_URL = "https://omara.ovrtechnology.com/?ref=aromaaffect-mod";

    // Colors
    private static final int COL_BG_PANEL = 0xDD1A1A2E;
    private static final int COL_ACCENT = 0xFF9A7CFF;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_HOVER = 0x40FFFFFF;
    private static final int COL_GREEN = 0xFF44DD44;
    private static final int COL_PURPLE = 0xFF9A7CFF;

    // Actual texture dimensions (must match the PNG file)
    private static final int OMARA_TEX_SIZE = 512;

    // Layout
    private static final int BACK_BUTTON_SIZE = 24;
    private static final int BACK_BUTTON_PADDING = 8;

    // State
    private enum ViewState { PRODUCT, INFO }
    private ViewState viewState = ViewState.PRODUCT;

    // Animation for view transition (0 = PRODUCT, 1 = INFO)
    private float infoTransition = 0f;
    private float targetInfoTransition = 0f;

    // Hover states
    private boolean isHoveringBack = false;
    private boolean isHoveringInfo = false;
    private boolean isHoveringBuy = false;
    private boolean isHoveringInfoBack = false;

    // Floating animation tick
    private int tickCount = 0;
    private double infoScroll;
    private int maxInfoScroll;

    public ShopScreen() {
        super(Component.translatable("shop.aromaaffect.title"));
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;

        // Smooth transition
        float speed = 0.12f;
        if (infoTransition < targetInfoTransition) {
            infoTransition = Math.min(targetInfoTransition, infoTransition + speed);
        } else if (infoTransition > targetInfoTransition) {
            infoTransition = Math.max(targetInfoTransition, infoTransition - speed);
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  float partialTick, float animationProgress) {
        float a = animationProgress;
        int centerX = width / 2;
        int centerY = height / 2;

        // Panel
        int panelW = Math.min(420, width - 40);
        int panelH = Math.min(320, height - 40);
        int panelLeft = centerX - panelW / 2;
        int panelTop = centerY - panelH / 2;
        int panelRight = panelLeft + panelW;
        int panelBottom = panelTop + panelH;

        // Panel background with scale-in animation
        float scaleA = easeOutBack(Math.min(1f, a * 1.2f));
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scaleA, scaleA);
        graphics.pose().translate(-centerX, -centerY);

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, MenuRenderUtils.withAlpha(COL_BG_PANEL, a));
        MenuRenderUtils.renderOutline(graphics, panelLeft, panelTop, panelW, panelH, MenuRenderUtils.withAlpha(0x66FFFFFF, a));

        // Accent top bar
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 3, MenuRenderUtils.withAlpha(COL_ACCENT, a));

        // Title
        Component title = Component.translatable("shop.aromaaffect.title");
        graphics.centeredText(font, title, centerX, panelTop + 10, MenuRenderUtils.withAlpha(COL_TEXT, a));

        // Subtitle
        Component subtitle = Component.translatable("shop.aromaaffect.subtitle");
        graphics.centeredText(font, subtitle, centerX, panelTop + 24, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));

        // Content area
        int contentTop = panelTop + 40;
        int contentH = panelBottom - contentTop - 40;

        // Interpolated transition value for smooth lerp
        float t = easeOutCubic(infoTransition);

        // Image area — slides from center to left
        int imgMaxW = Math.max(32, Math.min(contentH - 12, Math.min(160, panelW / 2 - 20)));
        int imgH = (int) (imgMaxW * 1.0f); // square-ish
        int imgCenterX = centerX; // product view: centered
        int imgInfoX = panelLeft + 20 + imgMaxW / 2; // info view: left side

        int imgDrawX = (int) Mth.lerp(t, imgCenterX - imgMaxW / 2, imgInfoX - imgMaxW / 2);
        int imgDrawY = contentTop + (contentH - imgH) / 2;

        // Floating effect
        float floatOffset = (float) Math.sin((tickCount + partialTick) * 0.08) * 3f;
        imgDrawY += (int) ((1f - t) * floatOffset); // only float in product view

        if (panelW >= 380 || t < 0.5f) {
        // Subtle glow behind image
        int glowPad = 6;
        int glowAlpha = (int) (40 * a * (1f - t * 0.5f));
        int glowColor = (glowAlpha << 24) | 0x9A7CFF;
        graphics.fill(imgDrawX - glowPad, imgDrawY - glowPad,
                imgDrawX + imgMaxW + glowPad, imgDrawY + imgH + glowPad, glowColor);

        // Draw the omara cover image (pass render size as texture size so UVs span full image)
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                OMARA_COVER,
                imgDrawX, imgDrawY,
                0.0f, 0.0f,
                imgMaxW, imgH,
                imgMaxW, imgH
        );

        }

        // Render info text on the right side (fades in with transition)
        if (t > 0.05f) {
            renderInfoPanel(graphics, panelLeft + (panelW < 380 ? 12 : panelW / 2 + 10), contentTop + 8,
                    panelW < 380 ? panelW - 24 : panelW / 2 - 30, contentH - 16, t, a);
        }

        // Buttons at the bottom (fade based on view state)
        int btnY = panelBottom - 32;
        int btnH = 22;

        if (t < 0.95f) {
            // Product view buttons: "View Info" and "Buy Now"
            float btnAlpha = (1f - t) * a;
            int btnW = 90;
            int btnGap = 12;
            int totalBtnW = btnW * 2 + btnGap;
            int btn1X = centerX - totalBtnW / 2;
            int btn2X = btn1X + btnW + btnGap;

            // View Info button
            isHoveringInfo = mouseX >= btn1X && mouseX < btn1X + btnW
                    && mouseY >= btnY && mouseY < btnY + btnH && t < 0.5f;
            int infoBg = isHoveringInfo ? MenuRenderUtils.withAlpha(0xDD9A7CFF, btnAlpha) : MenuRenderUtils.withAlpha(0x889A7CFF, btnAlpha);
            graphics.fill(btn1X, btnY, btn1X + btnW, btnY + btnH, infoBg);
            MenuRenderUtils.renderOutline(graphics, btn1X, btnY, btnW, btnH, MenuRenderUtils.withAlpha(0x66FFFFFF, btnAlpha));
            Component infoLabel = Component.translatable("shop.aromaaffect.view_info");
            graphics.centeredText(font, infoLabel, btn1X + btnW / 2, btnY + 7,
                    MenuRenderUtils.withAlpha(COL_TEXT, btnAlpha));

            // Buy Now button with pulsing glow
            isHoveringBuy = mouseX >= btn2X && mouseX < btn2X + btnW
                    && mouseY >= btnY && mouseY < btnY + btnH && t < 0.5f;
            float pulse = (float) (0.7f + 0.3f * Math.sin((tickCount + partialTick) * 0.15));
            int buyGlow = MenuRenderUtils.withAlpha(0x4444DD44, btnAlpha * pulse);
            graphics.fill(btn2X - 2, btnY - 2, btn2X + btnW + 2, btnY + btnH + 2, buyGlow);
            int buyBg = isHoveringBuy ? MenuRenderUtils.withAlpha(0xDD44DD44, btnAlpha) : MenuRenderUtils.withAlpha(0x9944BB44, btnAlpha);
            graphics.fill(btn2X, btnY, btn2X + btnW, btnY + btnH, buyBg);
            MenuRenderUtils.renderOutline(graphics, btn2X, btnY, btnW, btnH, MenuRenderUtils.withAlpha(0x66FFFFFF, btnAlpha));
            Component buyLabel = Component.translatable("shop.aromaaffect.buy_now");
            graphics.centeredText(font, buyLabel, btn2X + btnW / 2, btnY + 7,
                    MenuRenderUtils.withAlpha(COL_TEXT, btnAlpha));
        }

        // Info view: back arrow button (top-left of info area)
        if (t > 0.3f) {
            float backAlpha = (t - 0.3f) / 0.7f * a;
            int backX = panelLeft + (panelW < 380 ? 12 : panelW / 2 + 10);
            int backY = contentTop + 8;
            int backSize = 16;

            isHoveringInfoBack = mouseX >= backX && mouseX < backX + backSize + 4
                    && mouseY >= backY && mouseY < backY + backSize + 4 && t > 0.5f;

            if (isHoveringInfoBack) {
                int hoverBg = MenuRenderUtils.withAlpha(COL_HOVER, backAlpha);
                graphics.fill(backX - 2, backY - 2, backX + backSize + 6, backY + backSize + 6, hoverBg);
            }

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    ICON_BACK,
                    backX + 2, backY + 2,
                    0.0f, 0.0f,
                    backSize, backSize,
                    backSize, backSize
            );

            // Also show buy button in info view
            int buyBtnW = 90;
            int buyBtnX = panelLeft + panelW / 2 + 10;
            int buyBtnY = panelBottom - 32;

            isHoveringBuy = (t > 0.5f) && mouseX >= buyBtnX && mouseX < buyBtnX + buyBtnW
                    && mouseY >= buyBtnY && mouseY < buyBtnY + btnH;

            float pulse = (float) (0.7f + 0.3f * Math.sin((tickCount + partialTick) * 0.15));
            int buyGlow = MenuRenderUtils.withAlpha(0x4444DD44, backAlpha * pulse);
            graphics.fill(buyBtnX - 2, buyBtnY - 2, buyBtnX + buyBtnW + 2, buyBtnY + btnH + 2, buyGlow);
            int buyBg = isHoveringBuy ? MenuRenderUtils.withAlpha(0xDD44DD44, backAlpha) : MenuRenderUtils.withAlpha(0x9944BB44, backAlpha);
            graphics.fill(buyBtnX, buyBtnY, buyBtnX + buyBtnW, buyBtnY + btnH, buyBg);
            MenuRenderUtils.renderOutline(graphics, buyBtnX, buyBtnY, buyBtnW, btnH, MenuRenderUtils.withAlpha(0x66FFFFFF, backAlpha));
            Component buyLabel = Component.translatable("shop.aromaaffect.buy_now");
            graphics.centeredText(font, buyLabel, buyBtnX + buyBtnW / 2, buyBtnY + 7,
                    MenuRenderUtils.withAlpha(COL_TEXT, backAlpha));
        }

        graphics.pose().popMatrix();

        // Main back button (top-left, outside panel scale)
        renderBackButton(graphics, mouseX, mouseY, a);
    }

    private void renderInfoPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, float t, float a) {
        float alpha = t * a;
        int textColor = MenuRenderUtils.withAlpha(COL_TEXT, alpha);
        int dimColor = MenuRenderUtils.withAlpha(COL_TEXT_DIM, alpha);
        int accentColor = MenuRenderUtils.withAlpha(COL_ACCENT, alpha);

        int textX = x + 24;
        int textW = Math.max(30, w - 24);
        String[] keys = {"product_name", "tagline", "feature1", "feature2", "feature3",
                "feature4", "feature5", "feature6", "compatible"};
        int totalHeight = 0;
        for (String key : keys) totalHeight += font.split(Component.translatable("shop.aromaaffect." + key), textW).size() * font.lineHeight + 6;
        maxInfoScroll = Math.max(0, totalHeight - h);
        infoScroll = Mth.clamp(infoScroll, 0, maxInfoScroll);
        graphics.enableScissor(textX, y, x + w, y + h);
        int textY = y - (int) infoScroll;
        for (String key : keys) {
            for (var line : font.split(Component.translatable("shop.aromaaffect." + key), textW)) {
                graphics.text(font, line, textX, textY, key.equals("product_name") ? accentColor : dimColor, false);
                textY += font.lineHeight;
            }
            textY += 6;
        }
        graphics.disableScissor();
        if (maxInfoScroll > 0) {
            int barH = Math.max(10, h * h / totalHeight);
            int barY = y + (int) (infoScroll / maxInfoScroll * (h - barH));
            graphics.fill(x + w - 2, barY, x + w, barY + barH, accentColor);
        }
    }

    @Override
    protected boolean handleMouseScroll(double x, double y, double dx, double dy) {
        if (viewState != ViewState.INFO) return false;
        infoScroll = Mth.clamp(infoScroll - dy * 18, 0, maxInfoScroll);
        return true;
    }

    private void renderBackButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        float appear = Math.max(0f, (a - 0.2f) / 0.8f);
        if (appear <= 0f) return;

        int bx = BACK_BUTTON_PADDING;
        int by = BACK_BUTTON_PADDING;
        int bSize = BACK_BUTTON_SIZE + 8;

        isHoveringBack = mouseX >= bx && mouseX < bx + bSize
                && mouseY >= by && mouseY < by + bSize;

        if (isHoveringBack) {
            int bgColor = MenuRenderUtils.withAlpha(0x809A7CFF, appear);
            graphics.fill(bx, by, bx + bSize, by + bSize, bgColor);
            int borderColor = MenuRenderUtils.withAlpha(0x88FFFFFF, appear);
            MenuRenderUtils.renderOutline(graphics, bx, by, bSize, bSize, borderColor);
        }

        float scale = isHoveringBack ? 1.1f : 1.0f;
        int iconSize = (int) (BACK_BUTTON_SIZE * scale * appear);
        int iconOffset = (bSize - iconSize) / 2;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ICON_BACK,
                bx + iconOffset, by + iconOffset,
                0.0f, 0.0f,
                iconSize, iconSize,
                iconSize, iconSize
        );
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (isHoveringBack) {
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (isHoveringBuy) {
            Util.getPlatform().openUri(URI.create(ModpackConfig.appendModpackParam(BUY_URL)));
            return true;
        }

        if (isHoveringInfo && viewState == ViewState.PRODUCT) {
            viewState = ViewState.INFO;
            targetInfoTransition = 1f;
            return true;
        }

        if (isHoveringInfoBack && viewState == ViewState.INFO) {
            viewState = ViewState.PRODUCT;
            targetInfoTransition = 0f;
            return true;
        }

        return false;
    }

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            if (viewState == ViewState.INFO) {
                viewState = ViewState.PRODUCT;
                targetInfoTransition = 0f;
                return true;
            }
            MenuManager.returnToRadialMenu();
            return true;
        }
        return false;
    }

}
