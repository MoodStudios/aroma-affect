package com.ovrtechnology.menu;

import com.ovrtechnology.util.Colors;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import java.net.URI;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ShopScreen extends BaseMenuScreen {

    private static final ResourceLocation OMARA_COVER =
            Ids.mod("textures/gui/sprites/shop/omara_cover.png");
    private static final ResourceLocation ICON_BACK =
            Ids.mod("textures/gui/sprites/radial/icon_back.png");

    private static final String BUY_URL = "https://omara.ovrtechnology.com/?ref=aromaaffect-mod";

    private static final int COL_BG_PANEL = Colors.BG_MENU_BACKDROP;
    private static final int COL_ACCENT = Colors.ACCENT_PURPLE_LIGHT;
    private static final int COL_TEXT = Colors.WHITE;
    private static final int COL_TEXT_DIM = Colors.TEXT_MUTED;
    private static final int COL_HOVER = Colors.OVERLAY_WHITE_STRONG;
    private static final int COL_GREEN = 0xFF44DD44;
    private static final int COL_PURPLE = Colors.ACCENT_PURPLE_LIGHT;

    private static final int OMARA_TEX_SIZE = 512;

    private static final int BACK_BUTTON_SIZE = 24;
    private static final int BACK_BUTTON_PADDING = 8;

    private enum ViewState {
        PRODUCT,
        INFO
    }

    private ViewState viewState = ViewState.PRODUCT;

    private float infoTransition = 0f;
    private float targetInfoTransition = 0f;

    private boolean isHoveringBack = false;
    private boolean isHoveringInfo = false;
    private boolean isHoveringBuy = false;
    private boolean isHoveringInfoBack = false;

    private int tickCount = 0;

    public ShopScreen() {
        super(Texts.tr("shop.aromaaffect.title"));
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;

        float speed = 0.12f;
        if (infoTransition < targetInfoTransition) {
            infoTransition = Math.min(targetInfoTransition, infoTransition + speed);
        } else if (infoTransition > targetInfoTransition) {
            infoTransition = Math.max(targetInfoTransition, infoTransition - speed);
        }
    }

    @Override
    protected void renderContent(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            float animationProgress) {
        float a = animationProgress;
        int centerX = width / 2;
        int centerY = height / 2;

        int panelW = Math.min(420, width - 40);
        int panelH = Math.min(320, height - 40);
        int panelLeft = centerX - panelW / 2;
        int panelTop = centerY - panelH / 2;
        int panelRight = panelLeft + panelW;
        int panelBottom = panelTop + panelH;

        float scaleA = easeOutBack(Math.min(1f, a * 1.2f));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(scaleA, scaleA, 1);
        graphics.pose().translate(-centerX, -centerY, 0);

        graphics.fill(
                panelLeft,
                panelTop,
                panelRight,
                panelBottom,
                MenuRenderUtils.withAlpha(COL_BG_PANEL, a));
        MenuRenderUtils.renderOutline(
                graphics,
                panelLeft,
                panelTop,
                panelW,
                panelH,
                MenuRenderUtils.withAlpha(Colors.OVERLAY_WHITE_HALF, a));

        graphics.fill(
                panelLeft,
                panelTop,
                panelRight,
                panelTop + 3,
                MenuRenderUtils.withAlpha(COL_ACCENT, a));

        Component title = Texts.tr("shop.aromaaffect.title");
        graphics.drawCenteredString(
                font, title, centerX, panelTop + 10, MenuRenderUtils.withAlpha(COL_TEXT, a));

        Component subtitle = Texts.tr("shop.aromaaffect.subtitle");
        graphics.drawCenteredString(
                font, subtitle, centerX, panelTop + 24, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));

        int contentTop = panelTop + 40;
        int contentH = panelBottom - contentTop - 10;

        float t = easeOutCubic(infoTransition);

        int imgMaxW = Math.min(160, panelW / 2 - 20);
        int imgH = (int) (imgMaxW * 1.0f);
        int imgCenterX = centerX;
        int imgInfoX = panelLeft + 20 + imgMaxW / 2;

        int imgDrawX = (int) Mth.lerp(t, imgCenterX - imgMaxW / 2, imgInfoX - imgMaxW / 2);
        int imgDrawY = contentTop + (contentH - imgH) / 2;

        float floatOffset = (float) Math.sin((tickCount + partialTick) * 0.08) * 3f;
        imgDrawY += (int) ((1f - t) * floatOffset);

        int glowPad = 6;
        int glowAlpha = (int) (40 * a * (1f - t * 0.5f));
        int glowColor = (glowAlpha << 24) | Colors.TRACK_PURPLE_RGB;
        graphics.fill(
                imgDrawX - glowPad,
                imgDrawY - glowPad,
                imgDrawX + imgMaxW + glowPad,
                imgDrawY + imgH + glowPad,
                glowColor);

        graphics.blit(OMARA_COVER, imgDrawX, imgDrawY, 0.0f, 0.0f, imgMaxW, imgH, imgMaxW, imgH);

        if (t > 0.05f) {
            renderInfoPanel(
                    graphics,
                    panelLeft + panelW / 2 + 10,
                    contentTop + 8,
                    panelW / 2 - 30,
                    contentH - 16,
                    t,
                    a);
        }

        int btnY = panelBottom - 32;
        int btnH = 22;

        if (t < 0.95f) {

            float btnAlpha = (1f - t) * a;
            int btnW = 90;
            int btnGap = 12;
            int totalBtnW = btnW * 2 + btnGap;
            int btn1X = centerX - totalBtnW / 2;
            int btn2X = btn1X + btnW + btnGap;

            isHoveringInfo =
                    mouseX >= btn1X
                            && mouseX < btn1X + btnW
                            && mouseY >= btnY
                            && mouseY < btnY + btnH
                            && t < 0.5f;
            int infoBg =
                    isHoveringInfo
                            ? MenuRenderUtils.withAlpha(0xDD9A7CFF, btnAlpha)
                            : MenuRenderUtils.withAlpha(Colors.TRACK_PURPLE_TOOLTIP, btnAlpha);
            graphics.fill(btn1X, btnY, btn1X + btnW, btnY + btnH, infoBg);
            MenuRenderUtils.renderOutline(
                    graphics,
                    btn1X,
                    btnY,
                    btnW,
                    btnH,
                    MenuRenderUtils.withAlpha(Colors.OVERLAY_WHITE_HALF, btnAlpha));
            Component infoLabel = Texts.tr("shop.aromaaffect.view_info");
            graphics.drawCenteredString(
                    font,
                    infoLabel,
                    btn1X + btnW / 2,
                    btnY + 7,
                    MenuRenderUtils.withAlpha(COL_TEXT, btnAlpha));

            isHoveringBuy =
                    mouseX >= btn2X
                            && mouseX < btn2X + btnW
                            && mouseY >= btnY
                            && mouseY < btnY + btnH
                            && t < 0.5f;
            float pulse = (float) (0.7f + 0.3f * Math.sin((tickCount + partialTick) * 0.15));
            int buyGlow = MenuRenderUtils.withAlpha(Colors.TRACK_GREEN_ACCENT, btnAlpha * pulse);
            graphics.fill(btn2X - 2, btnY - 2, btn2X + btnW + 2, btnY + btnH + 2, buyGlow);
            int buyBg =
                    isHoveringBuy
                            ? MenuRenderUtils.withAlpha(Colors.TRACK_GREEN_STRONG, btnAlpha)
                            : MenuRenderUtils.withAlpha(Colors.TRACK_GREEN_HOVER, btnAlpha);
            graphics.fill(btn2X, btnY, btn2X + btnW, btnY + btnH, buyBg);
            MenuRenderUtils.renderOutline(
                    graphics,
                    btn2X,
                    btnY,
                    btnW,
                    btnH,
                    MenuRenderUtils.withAlpha(Colors.OVERLAY_WHITE_HALF, btnAlpha));
            Component buyLabel = Texts.tr("shop.aromaaffect.buy_now");
            graphics.drawCenteredString(
                    font,
                    buyLabel,
                    btn2X + btnW / 2,
                    btnY + 7,
                    MenuRenderUtils.withAlpha(COL_TEXT, btnAlpha));
        }

        if (t > 0.3f) {
            float backAlpha = (t - 0.3f) / 0.7f * a;
            int backX = panelLeft + panelW / 2 + 10;
            int backY = contentTop + 8;
            int backSize = 16;

            isHoveringInfoBack =
                    mouseX >= backX
                            && mouseX < backX + backSize + 4
                            && mouseY >= backY
                            && mouseY < backY + backSize + 4
                            && t > 0.5f;

            if (isHoveringInfoBack) {
                int hoverBg = MenuRenderUtils.withAlpha(COL_HOVER, backAlpha);
                graphics.fill(
                        backX - 2, backY - 2, backX + backSize + 6, backY + backSize + 6, hoverBg);
            }

            graphics.blit(
                    ICON_BACK, backX + 2, backY + 2, 0.0f, 0.0f, backSize, backSize, backSize,
                    backSize);

            int buyBtnW = 90;
            int buyBtnX = panelLeft + panelW / 2 + 10;
            int buyBtnY = panelBottom - 32;

            isHoveringBuy =
                    (t > 0.5f)
                            && mouseX >= buyBtnX
                            && mouseX < buyBtnX + buyBtnW
                            && mouseY >= buyBtnY
                            && mouseY < buyBtnY + btnH;

            float pulse = (float) (0.7f + 0.3f * Math.sin((tickCount + partialTick) * 0.15));
            int buyGlow = MenuRenderUtils.withAlpha(Colors.TRACK_GREEN_ACCENT, backAlpha * pulse);
            graphics.fill(
                    buyBtnX - 2, buyBtnY - 2, buyBtnX + buyBtnW + 2, buyBtnY + btnH + 2, buyGlow);
            int buyBg =
                    isHoveringBuy
                            ? MenuRenderUtils.withAlpha(Colors.TRACK_GREEN_STRONG, backAlpha)
                            : MenuRenderUtils.withAlpha(Colors.TRACK_GREEN_HOVER, backAlpha);
            graphics.fill(buyBtnX, buyBtnY, buyBtnX + buyBtnW, buyBtnY + btnH, buyBg);
            MenuRenderUtils.renderOutline(
                    graphics,
                    buyBtnX,
                    buyBtnY,
                    buyBtnW,
                    btnH,
                    MenuRenderUtils.withAlpha(Colors.OVERLAY_WHITE_HALF, backAlpha));
            Component buyLabel = Texts.tr("shop.aromaaffect.buy_now");
            graphics.drawCenteredString(
                    font,
                    buyLabel,
                    buyBtnX + buyBtnW / 2,
                    buyBtnY + 7,
                    MenuRenderUtils.withAlpha(COL_TEXT, backAlpha));
        }

        graphics.pose().popPose();

        renderBackButton(graphics, mouseX, mouseY, a);
    }

    private void renderInfoPanel(
            GuiGraphics graphics, int x, int y, int w, int h, float t, float a) {
        float alpha = t * a;
        int textColor = MenuRenderUtils.withAlpha(COL_TEXT, alpha);
        int dimColor = MenuRenderUtils.withAlpha(COL_TEXT_DIM, alpha);
        int accentColor = MenuRenderUtils.withAlpha(COL_ACCENT, alpha);

        int textX = x + 24;
        int textY = y + 4;

        Component name = Texts.tr("shop.aromaaffect.product_name");
        graphics.drawString(font, name, textX, textY, accentColor);
        textY += 14;

        Component tagline = Texts.tr("shop.aromaaffect.tagline");
        graphics.drawString(font, tagline, textX, textY, textColor);
        textY += 16;

        String[] featureKeys = {
            "shop.aromaaffect.feature1",
            "shop.aromaaffect.feature2",
            "shop.aromaaffect.feature3",
            "shop.aromaaffect.feature4",
            "shop.aromaaffect.feature5",
            "shop.aromaaffect.feature6"
        };

        for (String key : featureKeys) {
            Component feature = Texts.tr(key);

            graphics.drawString(font, "\u2022", textX, textY, accentColor);
            graphics.drawString(font, feature, textX + 10, textY, dimColor);
            textY += 12;
        }

        textY += 6;

        Component compatible = Texts.tr("shop.aromaaffect.compatible");
        graphics.drawString(
                font, compatible, textX, textY, MenuRenderUtils.withAlpha(COL_GREEN, alpha));
    }

    private void renderBackButton(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        float appear = Math.max(0f, (a - 0.2f) / 0.8f);
        if (appear <= 0f) return;

        int bx = BACK_BUTTON_PADDING;
        int by = BACK_BUTTON_PADDING;
        int bSize = BACK_BUTTON_SIZE + 8;

        isHoveringBack = mouseX >= bx && mouseX < bx + bSize && mouseY >= by && mouseY < by + bSize;

        if (isHoveringBack) {
            int bgColor = MenuRenderUtils.withAlpha(Colors.TRACK_PURPLE_FADE, appear);
            graphics.fill(bx, by, bx + bSize, by + bSize, bgColor);
            int borderColor = MenuRenderUtils.withAlpha(Colors.OVERLAY_WHITE_TOOLTIP, appear);
            MenuRenderUtils.renderOutline(graphics, bx, by, bSize, bSize, borderColor);
        }

        float scale = isHoveringBack ? 1.1f : 1.0f;
        int iconSize = (int) (BACK_BUTTON_SIZE * scale * appear);
        int iconOffset = (bSize - iconSize) / 2;
        graphics.blit(
                ICON_BACK,
                bx + iconOffset,
                by + iconOffset,
                0.0f,
                0.0f,
                iconSize,
                iconSize,
                iconSize,
                iconSize);
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (isHoveringBack) {
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (isHoveringBuy) {
            Util.getPlatform().openUri(URI.create(BUY_URL));
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
        if (keyCode == 256) {
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
