package com.ovrtechnology.guide;

import com.ovrtechnology.menu.BaseMenuScreen;
import com.ovrtechnology.util.Colors;
import com.ovrtechnology.util.Texts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GuideScreen extends BaseMenuScreen {

    private static final int WINDOW_MARGIN = 16;
    private static final int SIDEBAR_WIDTH = 150;
    private static final int SIDEBAR_ITEM_HEIGHT = 22;
    private static final int SIDEBAR_CATEGORY_HEIGHT = 32;
    private static final int HEADER_HEIGHT = 42;
    private static final int CONTENT_PADDING = 16;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int PAGE_TITLE_BAR_HEIGHT = 28;

    private static final int COLOR_WINDOW_BG = 0xF0101020;
    private static final int COLOR_SIDEBAR_BG = 0xF0141430;
    private static final int COLOR_HEADER_BG = 0xF01A1A3A;
    private static final int COLOR_CONTENT_BG = 0xF00D0D1C;
    private static final int COLOR_ACCENT = Colors.ACCENT_PURPLE;
    private static final int COLOR_ACCENT_DIM = 0xFF4A3DB0;
    private static final int COLOR_ACCENT_GLOW = Colors.ACCENT_PURPLE_ALPHA_SOFT;
    private static final int COLOR_BORDER = 0xFF2A2A50;
    private static final int COLOR_BORDER_LIGHT = 0xFF383860;
    private static final int COLOR_TITLE = Colors.WHITE;
    private static final int COLOR_SUBTITLE = Colors.TEXT_LABEL;
    private static final int COLOR_SIDEBAR_TEXT = 0xFFBBBBDD;
    private static final int COLOR_SIDEBAR_HOVER = Colors.OVERLAY_WHITE_MEDIUM;
    private static final int COLOR_SIDEBAR_SELECTED = Colors.ACCENT_PURPLE_ALPHA_STRONG;
    private static final int COLOR_CATEGORY_TEXT = 0xFFDDDDEE;
    private static final int COLOR_SCROLLBAR_BG = 0xFF1F2030;
    private static final int COLOR_SCROLLBAR_THUMB = 0xFFAAAACC;
    private static final int COLOR_SCROLLBAR_THUMB_HOVER = 0xFFBBBBDD;
    private static final int COLOR_PAGE_TITLE_BG = 0xF0181838;
    private static final int COLOR_TIP_BG = Colors.FOREST_OVERLAY;
    private static final int COLOR_TIP_BORDER = 0xFF2D8B46;
    private static final int COLOR_ITEM_SHOWCASE_BG = Colors.OVERLAY_WHITE_SOFT;
    private static final int COLOR_SEPARATOR = Colors.BORDER_DARK_BLUE;
    private static final int COLOR_IMAGE_BORDER = Colors.SEPARATOR;
    private static final int COLOR_IMAGE_SHADOW = Colors.OVERLAY_DARK_MEDIUM;

    private int closeBtnX, closeBtnY, closeBtnSize;
    private boolean hoveringClose = false;

    private final GuideBook book;
    @Nullable private GuidePage currentPage;
    @Nullable private GuideCategory currentCategory;

    private static final Set<String> expandedCategories = new HashSet<>();
    private static boolean categoriesInitialized = false;

    private double contentScrollOffset = 0;
    private double contentScrollTarget = 0;
    private int contentTotalHeight = 0;

    private double sidebarScrollOffset = 0;
    private double sidebarScrollTarget = 0;
    private int sidebarTotalHeight = 0;

    private final List<ClickableRegion> abilityLinkRegions = new ArrayList<>();

    private final List<ClickableRegion> urlLinkRegions = new ArrayList<>();

    private ItemStack craftingTooltipItem = ItemStack.EMPTY;

    private final List<SidebarEntry> sidebarEntries = new ArrayList<>();

    private final Map<String, Float> chevronAnimation = new HashMap<>();

    private int tickCount = 0;

    private enum DragTarget {
        NONE,
        CONTENT_SCROLLBAR,
        SIDEBAR_SCROLLBAR
    }

    private DragTarget dragging = DragTarget.NONE;
    private double dragStartMouseY = 0;
    private double dragStartScrollOffset = 0;

    private int contentScrollbarX, contentScrollbarY, contentScrollbarW, contentScrollbarH;
    private int contentVisibleHeight;
    private int sidebarScrollbarX, sidebarScrollbarY, sidebarScrollbarW, sidebarScrollbarH;
    private int sidebarVisibleHeight;

    private final Map<String, Double> pageScrollPositions = new HashMap<>();

    @Nullable private static String lastOpenedPageId = null;

    public GuideScreen(GuideBook book) {
        super(book.getTitle());
        this.book = book;
        if (!categoriesInitialized) {
            for (GuideCategory cat : book.getCategories()) {
                expandedCategories.add(cat.getId());
            }
            categoriesInitialized = true;
        }
    }

    @Override
    public void onClose() {

        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (currentPage == null) {

            if (lastOpenedPageId != null) {
                GuidePage restored = book.findPageById(lastOpenedPageId);
                if (restored != null) {
                    currentPage = restored;
                    currentCategory = book.findCategoryForPage(restored);
                    double savedScroll = pageScrollPositions.getOrDefault(lastOpenedPageId, 0.0);
                    contentScrollOffset = savedScroll;
                    contentScrollTarget = savedScroll;
                }
            }
            if (currentPage == null) {
                currentPage = book.getFirstPage();
                if (currentPage != null) {
                    currentCategory = book.findCategoryForPage(currentPage);
                }
            }
        }
        rebuildSidebar();
    }

    @Override
    public void tick() {
        super.tick();
        contentScrollOffset += (contentScrollTarget - contentScrollOffset) * 0.35;
        sidebarScrollOffset += (sidebarScrollTarget - sidebarScrollOffset) * 0.35;
        tickCount++;

        for (GuideCategory cat : book.getCategories()) {
            float target = expandedCategories.contains(cat.getId()) ? 1.0f : 0.0f;
            float current = chevronAnimation.getOrDefault(cat.getId(), target);
            current += (target - current) * 0.5f;
            if (Math.abs(target - current) < 0.01f) current = target;
            chevronAnimation.put(cat.getId(), current);
        }
    }

    @Override
    protected void renderContent(
            GuiGraphics g, int mouseX, int mouseY, float partialTick, float animationProgress) {
        if (animationProgress <= 0.01f) return;

        float alpha = animationProgress;

        int extraMargin = (int) ((1.0f - alpha) * 30);
        int wLeft = WINDOW_MARGIN + extraMargin;
        int wTop = WINDOW_MARGIN + extraMargin;
        int wRight = width - WINDOW_MARGIN - extraMargin;
        int wBottom = height - WINDOW_MARGIN - extraMargin;

        g.fill(wLeft, wTop, wRight, wBottom, applyAlpha(COLOR_WINDOW_BG, alpha));

        drawBorder(g, wLeft, wTop, wRight, wBottom, applyAlpha(COLOR_BORDER, alpha));

        g.fill(wLeft, wTop, wRight, wTop + 2, applyAlpha(COLOR_ACCENT, alpha));
        g.fill(wLeft, wTop + 2, wRight, wTop + 3, applyAlpha(COLOR_ACCENT_DIM, alpha * 0.5f));

        g.fill(
                wLeft + 1,
                wBottom - 2,
                wRight - 1,
                wBottom - 1,
                applyAlpha(COLOR_ACCENT_DIM, alpha * 0.3f));

        renderHeader(g, wLeft, wTop, wRight, alpha, mouseX, mouseY);

        int bodyTop = wTop + HEADER_HEIGHT;

        int sidebarRight = wLeft + SIDEBAR_WIDTH;
        renderSidebar(g, wLeft, bodyTop, sidebarRight, wBottom, mouseX, mouseY, alpha);

        g.fill(sidebarRight, bodyTop, sidebarRight + 1, wBottom, applyAlpha(COLOR_BORDER, alpha));
        g.fill(
                sidebarRight + 1,
                bodyTop,
                sidebarRight + 2,
                wBottom,
                applyAlpha(COLOR_BORDER_LIGHT, alpha * 0.3f));

        renderContentArea(g, sidebarRight + 2, bodyTop, wRight, wBottom, mouseX, mouseY, alpha);
    }

    private void renderHeader(
            GuiGraphics g, int left, int top, int right, float alpha, int mouseX, int mouseY) {
        int bottom = top + HEADER_HEIGHT;
        g.fill(left, top + 3, right, bottom, applyAlpha(COLOR_HEADER_BG, alpha));

        Component title = book.getTitle();
        float titleScale = 1.2f;
        int textLeftX = left + 14;
        int titleY = top + 7;
        g.pose().pushPose();
        g.pose().translate(textLeftX, titleY, 0);
        g.pose().scale(titleScale, titleScale, 1);
        g.drawString(font, title, 0, 0, applyAlpha(COLOR_TITLE, alpha), true);
        g.pose().popPose();

        Component subtitle = book.getSubtitle();
        if (subtitle != null && !subtitle.getString().isEmpty()) {
            int subtitleY = titleY + (int) (font.lineHeight * titleScale) + 2;

            float shimmer = (float) Math.sin(tickCount * 0.08) * 0.15f + 0.5f;
            int baseR = (COLOR_SUBTITLE >> 16) & 0xFF;
            int baseG = (COLOR_SUBTITLE >> 8) & 0xFF;
            int baseB = COLOR_SUBTITLE & 0xFF;
            int r = Mth.clamp((int) (baseR + (255 - baseR) * shimmer * 0.3f), 0, 255);
            int gC = Mth.clamp((int) (baseG + (255 - baseG) * shimmer * 0.3f), 0, 255);
            int b = Mth.clamp((int) (baseB + (255 - baseB) * shimmer * 0.3f), 0, 255);
            int shimmerColor = Colors.BLACK | (r << 16) | (gC << 8) | b;
            g.drawString(
                    font, subtitle, textLeftX, subtitleY, applyAlpha(shimmerColor, alpha), false);
        }

        closeBtnSize = 16;
        closeBtnX = right - closeBtnSize - 10;
        closeBtnY = top + (HEADER_HEIGHT - closeBtnSize) / 2 + 2;
        hoveringClose =
                mouseX >= closeBtnX - 2
                        && mouseX < closeBtnX + closeBtnSize + 2
                        && mouseY >= closeBtnY - 2
                        && mouseY < closeBtnY + closeBtnSize + 2;

        int btnBg =
                hoveringClose
                        ? applyAlpha(0xDDCC3333, alpha)
                        : applyAlpha(Colors.OVERLAY_WHITE_STRONG, alpha);

        g.fill(
                closeBtnX + 1,
                closeBtnY,
                closeBtnX + closeBtnSize - 1,
                closeBtnY + closeBtnSize,
                btnBg);

        g.fill(closeBtnX, closeBtnY + 1, closeBtnX + 1, closeBtnY + closeBtnSize - 1, btnBg);
        g.fill(
                closeBtnX + closeBtnSize - 1,
                closeBtnY + 1,
                closeBtnX + closeBtnSize,
                closeBtnY + closeBtnSize - 1,
                btnBg);

        String xChar = "\u00D7";
        int xColor =
                hoveringClose ? applyAlpha(Colors.WHITE, alpha) : applyAlpha(0xFFBBBBBB, alpha);
        int xTextX = closeBtnX + (closeBtnSize - font.width(xChar)) / 2;
        int xTextY = closeBtnY + (closeBtnSize - font.lineHeight) / 2;

        g.drawString(font, xChar, xTextX, xTextY, xColor, true);
        g.drawString(font, xChar, xTextX + 1, xTextY, xColor, false);

        g.fill(left, bottom - 1, right, bottom, applyAlpha(COLOR_BORDER, alpha));
    }

    private void renderSidebar(
            GuiGraphics g,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY,
            float alpha) {
        g.fill(left, top, right, bottom, applyAlpha(COLOR_SIDEBAR_BG, alpha));

        int areaHeight = bottom - top;
        int contentStartY = top + 6;

        g.enableScissor(left, top, right, bottom);

        int y = contentStartY - (int) sidebarScrollOffset;
        sidebarTotalHeight = 0;

        for (SidebarEntry entry : sidebarEntries) {
            entry.renderY = y;
            int entryHeight = entry.isCategory ? SIDEBAR_CATEGORY_HEIGHT : SIDEBAR_ITEM_HEIGHT;

            if (y + entryHeight > top && y < bottom) {
                if (entry.isCategory) {
                    renderSidebarCategory(g, left, y, right, entry, mouseX, mouseY, alpha);
                } else {
                    renderSidebarPage(g, left, y, right, entry, mouseX, mouseY, alpha);
                }
            }

            y += entryHeight;
            sidebarTotalHeight += entryHeight;
        }

        g.disableScissor();

        sidebarScrollbarX = right - SCROLLBAR_WIDTH - 1;
        sidebarScrollbarY = top + 1;
        sidebarScrollbarW = SCROLLBAR_WIDTH;
        sidebarScrollbarH = areaHeight - 2;
        sidebarVisibleHeight = areaHeight;

        if (sidebarTotalHeight > areaHeight) {
            renderScrollbar(
                    g,
                    sidebarScrollbarX,
                    sidebarScrollbarY,
                    sidebarScrollbarW,
                    sidebarScrollbarH,
                    sidebarScrollOffset,
                    sidebarTotalHeight,
                    areaHeight,
                    alpha);
        }
    }

    private void renderSidebarCategory(
            GuiGraphics g,
            int left,
            int y,
            int right,
            SidebarEntry entry,
            int mouseX,
            int mouseY,
            float alpha) {
        boolean hovered =
                mouseX >= left
                        && mouseX < right
                        && mouseY >= y
                        && mouseY < y + SIDEBAR_CATEGORY_HEIGHT;
        int accentColor = entry.category != null ? entry.category.getAccentColor() : COLOR_ACCENT;
        int h = SIDEBAR_CATEGORY_HEIGHT;
        int margin = 4;

        int bgBase = hovered ? Colors.OVERLAY_WHITE_STRONG : Colors.OVERLAY_WHITE_SOFT;
        int accentTint = (accentColor & Colors.TRANSPARENT) | Colors.OVERLAY_DARK_LIGHT;
        g.fill(left + margin, y + 2, right - margin, y + h - 2, applyAlpha(bgBase, alpha));
        g.fill(left + margin, y + 2, right - margin, y + h - 2, applyAlpha(accentTint, alpha));

        g.fill(left + margin, y + 2, left + margin + 3, y + h - 2, applyAlpha(accentColor, alpha));

        int iconX = left + margin + 8;
        int iconY = y + (h - 16) / 2;
        if (entry.category != null && entry.category.getIcon() != null) {
            GuideIcon icon = entry.category.getIcon();
            if (icon.isItem()) {
                g.pose().pushPose();
                g.pose().translate(iconX, iconY, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.renderItem(icon.getItemStack(), 0, 0);
                g.pose().popPose();
            } else if (icon.isTexture()) {
                int renderSize = 12;
                int texIconY = y + (h - renderSize) / 2;
                g.blit(
                        icon.getTexture(),
                        iconX,
                        texIconY,
                        0.0f,
                        0.0f,
                        renderSize,
                        renderSize,
                        renderSize,
                        renderSize);
            } else if (icon.isSymbol()) {
                int symY = y + (h - font.lineHeight) / 2;
                g.drawString(
                        font,
                        icon.getSymbol(),
                        iconX,
                        symY,
                        applyAlpha(icon.getSymbolColor(), alpha),
                        true);
            }
        }

        int textX = left + margin + 22;
        Component catTitle = Texts.lit(entry.text).withStyle(ChatFormatting.BOLD);
        int maxCatWidth = right - textX - 20;
        String catTitleStr = entry.text;
        if (font.width(catTitle) > maxCatWidth) {
            catTitleStr = font.plainSubstrByWidth(catTitleStr, maxCatWidth - 10) + "..";
            catTitle = Texts.lit(catTitleStr).withStyle(ChatFormatting.BOLD);
        }
        g.drawString(
                font,
                catTitle,
                textX,
                y + (h - font.lineHeight) / 2,
                applyAlpha(COLOR_CATEGORY_TEXT, alpha),
                true);

        float chevronProgress =
                entry.category != null
                        ? chevronAnimation.getOrDefault(entry.category.getId(), 0f)
                        : 0f;
        int chevronX = right - margin - 14;
        int chevronCY = y + h / 2;
        renderChevron(
                g, chevronX, chevronCY, chevronProgress, applyAlpha(accentColor, alpha * 0.9f));
    }

    private void renderChevron(GuiGraphics g, int cx, int cy, float progress, int color) {
        double rad = Math.toRadians(progress * 90.0);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        int s = 3;
        for (int i = 0; i <= s; i++) {

            float tx = -s + i;
            float ty = -s + i;
            int rx1 = cx + Math.round(tx * cos - ty * sin);
            int ry1 = cy + Math.round(tx * sin + ty * cos);
            g.fill(rx1, ry1, rx1 + 2, ry1 + 2, color);

            float bx = -s + i;
            float by = s - i;
            int rx2 = cx + Math.round(bx * cos - by * sin);
            int ry2 = cy + Math.round(bx * sin + by * cos);
            g.fill(rx2, ry2, rx2 + 2, ry2 + 2, color);
        }
    }

    private void renderSidebarPage(
            GuiGraphics g,
            int left,
            int y,
            int right,
            SidebarEntry entry,
            int mouseX,
            int mouseY,
            float alpha) {
        boolean selected = entry.page == currentPage;
        boolean hovered =
                mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + SIDEBAR_ITEM_HEIGHT;
        int indent = 12;

        int accentColor = entry.category != null ? entry.category.getAccentColor() : COLOR_ACCENT;

        if (selected) {

            int selBg = (accentColor & Colors.TRANSPARENT) | Colors.OVERLAY_DARK_SOFT;
            g.fill(
                    left + indent,
                    y + 1,
                    right - 4,
                    y + SIDEBAR_ITEM_HEIGHT - 1,
                    applyAlpha(selBg, alpha));

            g.fill(
                    left + indent,
                    y + 2,
                    left + indent + 2,
                    y + SIDEBAR_ITEM_HEIGHT - 2,
                    applyAlpha(accentColor, alpha));

            g.fill(
                    left + indent + 2,
                    y + 2,
                    left + indent + 4,
                    y + SIDEBAR_ITEM_HEIGHT - 2,
                    applyAlpha(accentColor, alpha * 0.2f));
        } else if (hovered) {
            g.fill(
                    left + indent,
                    y + 1,
                    right - 4,
                    y + SIDEBAR_ITEM_HEIGHT - 1,
                    applyAlpha(COLOR_SIDEBAR_HOVER, alpha * 0.4f));
        }

        int iconX = left + indent + 6;
        int iconRenderSize = 12;
        int iconY = y + (SIDEBAR_ITEM_HEIGHT - iconRenderSize) / 2;
        GuideIcon pageIcon = entry.page != null ? entry.page.getIcon() : null;
        if (pageIcon != null && pageIcon.isItem()) {
            g.pose().pushPose();
            g.pose().translate(iconX, iconY, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.renderItem(pageIcon.getItemStack(), 0, 0);
            g.pose().popPose();
        } else if (pageIcon != null && pageIcon.isSymbol()) {
            int symY = y + (SIDEBAR_ITEM_HEIGHT - font.lineHeight) / 2;
            g.drawString(
                    font,
                    pageIcon.getSymbol(),
                    iconX,
                    symY,
                    applyAlpha(pageIcon.getSymbolColor(), alpha),
                    true);
        } else {

            int dotY = y + SIDEBAR_ITEM_HEIGHT / 2 - 1;
            int dotColor = selected ? accentColor : applyAlpha(Colors.SEPARATOR_BLUE, alpha);
            g.fill(iconX + 3, dotY, iconX + 5, dotY + 2, applyAlpha(dotColor, alpha));
        }

        String pageTitle = entry.text;
        int textX = left + indent + 20;
        int maxWidth = right - textX - 6;
        if (font.width(pageTitle) > maxWidth) {
            pageTitle = font.plainSubstrByWidth(pageTitle, maxWidth - 8) + "..";
        }
        int textColor = selected ? COLOR_TITLE : COLOR_SIDEBAR_TEXT;
        g.drawString(font, pageTitle, textX, y + 7, applyAlpha(textColor, alpha), selected);
    }

    private void renderContentArea(
            GuiGraphics g,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY,
            float alpha) {
        g.fill(left, top, right, bottom, applyAlpha(COLOR_CONTENT_BG, alpha));

        if (currentPage == null) {
            String msg = "Select a page from the sidebar";
            int msgX = left + (right - left - font.width(msg)) / 2;
            int msgY = top + (bottom - top) / 2;
            g.drawString(font, msg, msgX, msgY, applyAlpha(0xFF666688, alpha), false);
            return;
        }

        int titleBarBottom = top + PAGE_TITLE_BAR_HEIGHT;
        g.fill(left, top, right, titleBarBottom, applyAlpha(COLOR_PAGE_TITLE_BG, alpha));
        g.fill(left, titleBarBottom - 1, right, titleBarBottom, applyAlpha(COLOR_BORDER, alpha));

        GuideCategory cat = book.findCategoryForPage(currentPage);
        int accent = cat != null ? cat.getAccentColor() : COLOR_ACCENT;

        g.fill(
                left + 8,
                top + 7,
                left + 11,
                top + PAGE_TITLE_BAR_HEIGHT - 7,
                applyAlpha(accent, alpha));

        int titleTextX = left + 16;
        GuideIcon pageIcon = currentPage.getIcon();
        if (pageIcon != null && pageIcon.isItem()) {
            g.pose().pushPose();
            g.pose().translate(left + 16, top + 8, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.renderItem(pageIcon.getItemStack(), 0, 0);
            g.pose().popPose();
            titleTextX = left + 32;
        } else if (pageIcon != null && pageIcon.isSymbol()) {
            g.drawString(
                    font,
                    pageIcon.getSymbol(),
                    left + 16,
                    top + 10,
                    applyAlpha(pageIcon.getSymbolColor(), alpha),
                    true);
            titleTextX = left + 16 + font.width(pageIcon.getSymbol()) + 4;
        }

        g.drawString(
                font,
                currentPage.getTitle(),
                titleTextX,
                top + 10,
                applyAlpha(COLOR_TITLE, alpha),
                true);

        if (cat != null) {
            String breadcrumb =
                    cat.getTitle().getString() + " \u203A " + currentPage.getTitle().getString();
            int breadcrumbX = right - 10 - font.width(breadcrumb);
            if (breadcrumbX > titleTextX + font.width(currentPage.getTitle()) + 10) {
                g.drawString(
                        font,
                        breadcrumb,
                        breadcrumbX,
                        top + 10,
                        applyAlpha(Colors.SEPARATOR_BLUE, alpha),
                        false);
            }
        }

        int contentTop = titleBarBottom;
        int contentRight = right - SCROLLBAR_WIDTH - 2;
        int areaHeight = bottom - contentTop;

        abilityLinkRegions.clear();
        urlLinkRegions.clear();
        craftingTooltipItem = ItemStack.EMPTY;

        g.enableScissor(left, contentTop, contentRight, bottom);

        int y = contentTop + CONTENT_PADDING - (int) contentScrollOffset;
        int contentWidth = contentRight - left - CONTENT_PADDING * 2;
        int contentLeft = left + CONTENT_PADDING;

        contentTotalHeight = CONTENT_PADDING;

        for (GuideElement element : currentPage.getElements()) {
            int elementHeight =
                    renderElement(g, element, contentLeft, y, contentWidth, mouseX, mouseY, alpha);
            y += elementHeight;
            contentTotalHeight += elementHeight;
        }

        contentTotalHeight += CONTENT_PADDING;

        g.disableScissor();

        contentScrollbarX = right - SCROLLBAR_WIDTH - 1;
        contentScrollbarY = contentTop + 1;
        contentScrollbarW = SCROLLBAR_WIDTH;
        contentScrollbarH = areaHeight - 2;
        contentVisibleHeight = areaHeight;

        if (contentTotalHeight > areaHeight) {
            renderScrollbar(
                    g,
                    contentScrollbarX,
                    contentScrollbarY,
                    contentScrollbarW,
                    contentScrollbarH,
                    contentScrollOffset,
                    contentTotalHeight,
                    areaHeight,
                    alpha);
        }

        if (!craftingTooltipItem.isEmpty()) {
            List<Component> tooltipLines =
                    Screen.getTooltipFromItem(Minecraft.getInstance(), craftingTooltipItem);
            List<net.minecraft.util.FormattedCharSequence> charSequences = new ArrayList<>();
            for (Component line : tooltipLines) {
                charSequences.add(line.getVisualOrderText());
            }
            g.renderTooltip(
                    font,
                    charSequences,
                    net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
                            .INSTANCE,
                    mouseX,
                    mouseY);
        }
    }

    private int renderElement(
            GuiGraphics g,
            GuideElement element,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            float alpha) {
        return switch (element.getType()) {
            case HEADER -> renderHeaderElement(g, element, x, y, width, alpha);
            case SUBHEADER -> renderSubheaderElement(g, element, x, y, width, alpha);
            case TEXT -> renderTextElement(g, element, x, y, width, alpha);
            case ITEM_SHOWCASE -> renderItemShowcase(g, element, x, y, width, alpha);
            case IMAGE -> renderImageElement(g, element, x, y, width, alpha);
            case SEPARATOR -> renderSeparator(g, x, y, width, alpha);
            case SPACER -> element.getSpacerHeight();
            case TIP -> renderTipElement(g, element, x, y, width, alpha);
            case CRAFTING_GRID -> renderCraftingGrid(
                    g, element, x, y, width, mouseX, mouseY, alpha);
            case ABILITY_LINK -> renderAbilityLink(g, element, x, y, width, mouseX, mouseY, alpha);
            case ICON_TEXT -> renderIconText(g, element, x, y, width, alpha);
            case URL_LINK -> renderUrlLink(g, element, x, y, width, mouseX, mouseY, alpha);
        };
    }

    private int renderHeaderElement(
            GuiGraphics g, GuideElement element, int x, int y, int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        g.pose().pushPose();
        float scale = 1.5f;
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, applyAlpha(Colors.WHITE, alpha), true);
        g.pose().popPose();

        int textHeight = (int) (font.lineHeight * scale);

        g.fill(
                x,
                y + textHeight + 3,
                x + width,
                y + textHeight + 4,
                applyAlpha(COLOR_ACCENT, alpha));
        g.fill(
                x,
                y + textHeight + 4,
                x + width / 2,
                y + textHeight + 5,
                applyAlpha(COLOR_ACCENT_DIM, alpha * 0.4f));

        return textHeight + 10;
    }

    private int renderSubheaderElement(
            GuiGraphics g, GuideElement element, int x, int y, int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        String str = text.getString();

        g.fill(x, y + 2, x + 3, y + font.lineHeight, applyAlpha(COLOR_ACCENT, alpha));

        int iconOffset = 0;
        int iconColor = applyAlpha(COLOR_ACCENT, alpha);
        int ix = x + 8;
        int iy = y + 3;

        if (str.equals("Abilities")) {

            g.fill(ix + 3, iy, ix + 4, iy + 1, iconColor);
            g.fill(ix + 1, iy + 2, ix + 6, iy + 3, iconColor);
            g.fill(ix + 2, iy + 1, ix + 5, iy + 2, iconColor);
            g.fill(ix + 2, iy + 3, ix + 5, iy + 4, iconColor);
            g.fill(ix + 3, iy + 4, ix + 4, iy + 5, iconColor);
            g.fill(ix, iy + 2, ix + 1, iy + 3, iconColor);
            g.fill(ix + 6, iy + 2, ix + 7, iy + 3, iconColor);
            iconOffset = 10;
        } else if (str.equals("New Detections") || str.equals("Detects")) {

            g.fill(ix + 2, iy, ix + 5, iy + 1, iconColor);
            g.fill(ix + 1, iy + 1, ix + 2, iy + 2, iconColor);
            g.fill(ix + 5, iy + 1, ix + 6, iy + 2, iconColor);
            g.fill(ix + 3, iy + 1, ix + 4, iy + 3, iconColor);
            g.fill(ix + 1, iy + 3, ix + 2, iy + 4, iconColor);
            g.fill(ix + 5, iy + 3, ix + 6, iy + 4, iconColor);
            g.fill(ix + 2, iy + 4, ix + 5, iy + 5, iconColor);
            iconOffset = 10;
        } else if (str.equals("Recipe")) {

            g.fill(ix, iy, ix + 7, iy + 7, applyAlpha(Colors.BORDER_DARK_BLUE, alpha));
            g.fill(ix + 1, iy + 1, ix + 3, iy + 3, iconColor);
            g.fill(ix + 4, iy + 1, ix + 6, iy + 3, iconColor);
            g.fill(ix + 1, iy + 4, ix + 3, iy + 6, iconColor);
            g.fill(ix + 4, iy + 4, ix + 6, iy + 6, iconColor);
            iconOffset = 10;
        } else if (str.equals("How to Obtain") || str.equals("How to Equip")) {

            g.fill(ix + 2, iy, ix + 5, iy + 1, iconColor);
            g.fill(ix + 3, iy + 1, ix + 4, iy + 4, iconColor);
            g.fill(ix + 1, iy + 3, ix + 6, iy + 4, iconColor);
            g.fill(ix + 2, iy + 4, ix + 5, iy + 5, iconColor);
            g.fill(ix + 3, iy + 5, ix + 4, iy + 6, iconColor);
            iconOffset = 10;
        } else if (str.equals("How to Tame")) {

            g.fill(ix + 1, iy, ix + 3, iy + 1, iconColor);
            g.fill(ix + 4, iy, ix + 6, iy + 1, iconColor);
            g.fill(ix, iy + 1, ix + 7, iy + 3, iconColor);
            g.fill(ix + 1, iy + 3, ix + 6, iy + 4, iconColor);
            g.fill(ix + 2, iy + 4, ix + 5, iy + 5, iconColor);
            g.fill(ix + 3, iy + 5, ix + 4, iy + 6, iconColor);
            iconOffset = 10;
        } else if (str.equals("Riding")) {

            g.fill(ix + 2, iy, ix + 5, iy + 1, iconColor);
            g.fill(ix + 1, iy + 1, ix + 2, iy + 4, iconColor);
            g.fill(ix + 5, iy + 1, ix + 6, iy + 4, iconColor);
            g.fill(ix, iy + 4, ix + 2, iy + 6, iconColor);
            g.fill(ix + 5, iy + 4, ix + 7, iy + 6, iconColor);
            iconOffset = 10;
        } else if (str.equals("Sniffer Inventory")) {

            g.fill(ix, iy + 1, ix + 7, iy + 6, applyAlpha(Colors.BORDER_DARK_BLUE, alpha));
            g.fill(ix, iy + 1, ix + 7, iy + 2, iconColor);
            g.fill(ix, iy + 5, ix + 7, iy + 6, iconColor);
            g.fill(ix, iy + 1, ix + 1, iy + 6, iconColor);
            g.fill(ix + 6, iy + 1, ix + 7, iy + 6, iconColor);
            g.fill(ix + 3, iy + 3, ix + 4, iy + 4, iconColor);
            iconOffset = 10;
        } else if (str.startsWith("What Can") || str.equals("How It Works")) {

            g.fill(ix + 2, iy, ix + 5, iy + 1, iconColor);
            g.fill(ix + 1, iy + 1, ix + 2, iy + 2, iconColor);
            g.fill(ix + 5, iy + 1, ix + 6, iy + 2, iconColor);
            g.fill(ix + 3, iy + 2, ix + 4, iy + 3, iconColor);
            g.fill(ix + 3, iy + 3, ix + 4, iy + 5, iconColor);
            g.fill(ix + 1, iy + 4, ix + 2, iy + 5, iconColor);
            g.fill(ix + 5, iy + 4, ix + 6, iy + 5, iconColor);
            g.fill(ix + 2, iy + 5, ix + 5, iy + 6, iconColor);
            iconOffset = 10;
        } else if (str.equals("Key Ingredients")) {

            g.fill(ix + 1, iy, ix + 4, iy + 1, iconColor);
            g.fill(ix, iy + 1, ix + 2, iy + 2, iconColor);
            g.fill(ix + 3, iy + 1, ix + 5, iy + 2, iconColor);
            g.fill(ix + 2, iy + 2, ix + 3, iy + 6, iconColor);
            g.fill(ix + 1, iy + 5, ix + 4, iy + 6, iconColor);
            g.fill(ix + 1, iy + 3, ix + 4, iy + 4, iconColor);
            iconOffset = 10;
        } else if (str.equals("All Scents")) {

            g.fill(ix, iy, ix + 7, iy + 1, iconColor);
            g.fill(ix, iy + 2, ix + 7, iy + 3, iconColor);
            g.fill(ix, iy + 4, ix + 7, iy + 5, iconColor);

            g.fill(ix, iy, ix + 1, iy + 1, iconColor);
            g.fill(ix, iy + 2, ix + 1, iy + 3, iconColor);
            g.fill(ix, iy + 4, ix + 1, iy + 5, iconColor);
            iconOffset = 10;
        } else if (str.equals("Dimension Scents")) {

            g.fill(ix + 2, iy, ix + 5, iy + 1, iconColor);
            g.fill(ix + 1, iy + 1, ix + 2, iy + 2, iconColor);
            g.fill(ix + 5, iy + 1, ix + 6, iy + 2, iconColor);
            g.fill(ix, iy + 2, ix + 1, iy + 4, iconColor);
            g.fill(ix + 6, iy + 2, ix + 7, iy + 4, iconColor);
            g.fill(ix + 1, iy + 4, ix + 2, iy + 5, iconColor);
            g.fill(ix + 5, iy + 4, ix + 6, iy + 5, iconColor);
            g.fill(ix + 2, iy + 5, ix + 5, iy + 6, iconColor);
            g.fill(ix + 3, iy + 2, ix + 4, iy + 4, iconColor);
            iconOffset = 10;
        }

        Component boldText = Texts.lit(str).withStyle(ChatFormatting.BOLD);
        g.drawString(
                font,
                boldText,
                x + 8 + iconOffset,
                y + 2,
                applyAlpha(element.getColor(), alpha),
                true);

        return font.lineHeight + 6;
    }

    private int renderTextElement(
            GuiGraphics g, GuideElement element, int x, int y, int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        List<FormattedCharSequence> lines = font.split(text, width);
        int lineHeight = font.lineHeight + 2;
        int totalHeight = 0;

        for (FormattedCharSequence line : lines) {
            g.drawString(
                    font, line, x, y + totalHeight, applyAlpha(element.getColor(), alpha), false);
            totalHeight += lineHeight;
        }

        return totalHeight + 2;
    }

    private int renderItemShowcase(
            GuiGraphics g, GuideElement element, int x, int y, int width, float alpha) {
        int boxHeight = 28;

        g.fill(x, y, x + width, y + boxHeight, applyAlpha(COLOR_ITEM_SHOWCASE_BG, alpha));
        drawBorder(g, x, y, x + width, y + boxHeight, applyAlpha(COLOR_BORDER_LIGHT, alpha));

        if (element.getItemStack() != null) {
            g.renderItem(element.getItemStack(), x + 6, y + 6);
        }

        if (element.getText() != null) {
            g.drawString(
                    font,
                    element.getText(),
                    x + 28,
                    y + 10,
                    applyAlpha(element.getColor(), alpha),
                    false);
        }

        return boxHeight + 4;
    }

    private int renderImageElement(
            GuiGraphics g, GuideElement element, int x, int y, int width, float alpha) {
        if (element.getImageTexture() == null) return 0;

        int texW = element.getImageWidth();
        int texH = element.getImageHeight();

        int imgW = texW;
        int imgH = texH;
        int imgX = x + (width - imgW) / 2;

        g.fill(
                imgX + 2,
                y + 2,
                imgX + imgW + 2,
                y + imgH + 2,
                applyAlpha(COLOR_IMAGE_SHADOW, alpha));

        g.blit(element.getImageTexture(), imgX, y, 0.0f, 0.0f, texW, texH, texW, texH);

        drawBorder(
                g,
                imgX - 1,
                y - 1,
                imgX + imgW + 1,
                y + imgH + 1,
                applyAlpha(COLOR_IMAGE_BORDER, alpha));

        int cornerSize = 4;
        int cc = applyAlpha(COLOR_ACCENT, alpha * 0.6f);
        g.fill(imgX - 1, y - 1, imgX - 1 + cornerSize, y, cc);
        g.fill(imgX - 1, y - 1, imgX, y - 1 + cornerSize, cc);
        g.fill(imgX + imgW + 1 - cornerSize, y - 1, imgX + imgW + 1, y, cc);
        g.fill(imgX + imgW, y - 1, imgX + imgW + 1, y - 1 + cornerSize, cc);
        g.fill(imgX - 1, y + imgH, imgX - 1 + cornerSize, y + imgH + 1, cc);
        g.fill(imgX - 1, y + imgH + 1 - cornerSize, imgX, y + imgH + 1, cc);
        g.fill(imgX + imgW + 1 - cornerSize, y + imgH, imgX + imgW + 1, y + imgH + 1, cc);
        g.fill(imgX + imgW, y + imgH + 1 - cornerSize, imgX + imgW + 1, y + imgH + 1, cc);

        return imgH + 8;
    }

    private int renderSeparator(GuiGraphics g, int x, int y, int width, float alpha) {
        int midY = y + 5;

        g.fill(x + 10, midY, x + width - 10, midY + 1, applyAlpha(COLOR_SEPARATOR, alpha));

        int cx = x + width / 2;
        g.fill(cx - 1, midY - 1, cx + 2, midY + 2, applyAlpha(COLOR_ACCENT_DIM, alpha * 0.6f));
        return 12;
    }

    private int renderTipElement(
            GuiGraphics g, GuideElement element, int x, int y, int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        List<FormattedCharSequence> lines = font.split(text, width - 24);
        int lineHeight = font.lineHeight + 2;
        int textHeight = lines.size() * lineHeight;
        int boxHeight = textHeight + 12;

        g.fill(x, y, x + width, y + boxHeight, applyAlpha(COLOR_TIP_BG, alpha));

        g.fill(x, y, x + 3, y + boxHeight, applyAlpha(COLOR_TIP_BORDER, alpha));

        g.fill(x + 3, y, x + width, y + 1, applyAlpha(COLOR_TIP_BORDER, alpha * 0.3f));

        g.drawString(font, "\u2714", x + 8, y + 6, applyAlpha(Colors.ACCENT_GREEN, alpha), true);

        int textY = y + 6;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x + 20, textY, applyAlpha(element.getColor(), alpha), false);
            textY += lineHeight;
        }

        return boxHeight + 6;
    }

    private int renderCraftingGrid(
            GuiGraphics g,
            GuideElement element,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            float alpha) {
        ItemStack[] grid = element.getCraftingGrid();
        ItemStack result = element.getCraftingResult();
        if (grid == null || result == null) return 0;

        int slotSize = 18;
        int gridSize = slotSize * 3;
        int arrowWidth = 24;
        int resultSlotSize = 22;
        int totalWidth = gridSize + arrowWidth + resultSlotSize;
        int startX = x + (width - totalWidth) / 2;

        int labelHeight = 0;
        Component label = element.getText();
        if (label != null && !label.getString().isEmpty()) {
            int labelX = startX + (totalWidth - font.width(label)) / 2;
            g.drawString(font, label, labelX, y, applyAlpha(Colors.TEXT_SECONDARY, alpha), false);
            labelHeight = font.lineHeight + 8;
        }

        int gridY = y + labelHeight;

        int panelPad = 6;
        int panelLeft = startX - panelPad;
        int panelTop = gridY - panelPad;
        int panelRight = startX + totalWidth + panelPad;
        int panelBottom = gridY + gridSize + panelPad;
        g.fill(
                panelLeft,
                panelTop,
                panelRight,
                panelBottom,
                applyAlpha(Colors.OVERLAY_WHITE_MEDIUM, alpha));
        drawBorder(
                g,
                panelLeft,
                panelTop,
                panelRight,
                panelBottom,
                applyAlpha(COLOR_BORDER_LIGHT, alpha));

        ItemStack hoveredItem = ItemStack.EMPTY;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = startX + col * slotSize;
                int slotY = gridY + row * slotSize;

                ItemStack item = grid[row * 3 + col];
                boolean slotHovered =
                        mouseX >= slotX
                                && mouseX < slotX + slotSize - 1
                                && mouseY >= slotY
                                && mouseY < slotY + slotSize - 1;

                g.fill(
                        slotX,
                        slotY,
                        slotX + slotSize - 1,
                        slotY + slotSize - 1,
                        applyAlpha(Colors.OVERLAY_DARK_MEDIUM, alpha));

                drawBorder(
                        g,
                        slotX,
                        slotY,
                        slotX + slotSize - 1,
                        slotY + slotSize - 1,
                        applyAlpha(0x60FFFFFF, alpha));

                if (item != null && !item.isEmpty()) {
                    g.renderItem(item, slotX + 1, slotY + 1);

                    if (slotHovered) {
                        g.fill(
                                slotX + 1,
                                slotY + 1,
                                slotX + 17,
                                slotY + 17,
                                Colors.OVERLAY_WHITE_HEAVY);
                        hoveredItem = item;
                    }
                }
            }
        }

        int arrowX = startX + gridSize + (arrowWidth - font.width("\u2192")) / 2;
        int arrowY = gridY + (gridSize - font.lineHeight) / 2;
        g.drawString(font, "\u2192", arrowX, arrowY, applyAlpha(Colors.WHITE, alpha), true);

        int resultX = startX + gridSize + arrowWidth;
        int resultY = gridY + (gridSize - resultSlotSize) / 2;

        boolean resultHovered =
                mouseX >= resultX
                        && mouseX < resultX + resultSlotSize
                        && mouseY >= resultY
                        && mouseY < resultY + resultSlotSize;

        g.fill(
                resultX,
                resultY,
                resultX + resultSlotSize,
                resultY + resultSlotSize,
                applyAlpha(0x50FFD700, alpha));
        drawBorder(
                g,
                resultX,
                resultY,
                resultX + resultSlotSize,
                resultY + resultSlotSize,
                applyAlpha(0xAAFFD700, alpha));

        g.renderItem(result, resultX + 3, resultY + 3);

        if (resultHovered) {
            g.fill(
                    resultX + 3,
                    resultY + 3,
                    resultX + 19,
                    resultY + 19,
                    Colors.OVERLAY_WHITE_HEAVY);
            hoveredItem = result;
        }

        if (!hoveredItem.isEmpty()) {
            this.craftingTooltipItem = hoveredItem;
        }

        return labelHeight + gridSize + panelPad * 2 + 4;
    }

    private int renderAbilityLink(
            GuiGraphics g,
            GuideElement element,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            float alpha) {
        Component name = element.getText();
        if (name == null) return 0;

        int lineHeight = font.lineHeight + 2;

        String bullet = "\u2022 ";
        int bulletWidth = font.width(bullet);
        g.drawString(font, bullet, x, y, applyAlpha(Colors.TEXT_PRIMARY, alpha), false);

        String nameStr = name.getString();
        int nameWidth = font.width(nameStr);
        g.drawString(
                font, nameStr, x + bulletWidth, y, applyAlpha(Colors.TEXT_PRIMARY, alpha), false);

        Component inherited = Texts.lit(" (inherited)").withStyle(ChatFormatting.BOLD);
        int inheritedWidth = font.width(inherited);
        int inheritedX = x + bulletWidth + nameWidth;
        g.drawString(font, inherited, inheritedX, y, applyAlpha(Colors.TEXT_LABEL, alpha), false);

        int iconX = inheritedX + inheritedWidth + 3;
        int iconW = 10;

        int linkLeft = inheritedX;
        int linkRight = iconX + iconW;
        boolean hovered =
                mouseX >= linkLeft && mouseX < linkRight && mouseY >= y && mouseY < y + lineHeight;

        int iconColor =
                hovered ? applyAlpha(0xFF9D8EFF, alpha) : applyAlpha(Colors.ACCENT_PURPLE, alpha);

        int iy = y + 2;

        g.fill(iconX, iy + 1, iconX + 1, iy + 5, iconColor);
        g.fill(iconX + 1, iy, iconX + 4, iy + 1, iconColor);
        g.fill(iconX + 1, iy + 5, iconX + 4, iy + 6, iconColor);
        g.fill(iconX + 4, iy + 1, iconX + 5, iy + 3, iconColor);

        g.fill(iconX + 5, iy + 3, iconX + 6, iy + 5, iconColor);
        g.fill(iconX + 6, iy + 1, iconX + 9, iy + 2, iconColor);
        g.fill(iconX + 6, iy + 6, iconX + 9, iy + 7, iconColor);
        g.fill(iconX + 9, iy + 2, iconX + 10, iy + 6, iconColor);

        g.fill(iconX + 4, iy + 3, iconX + 6, iy + 4, iconColor);

        if (hovered) {
            g.fill(
                    linkLeft,
                    y + font.lineHeight,
                    linkRight,
                    y + font.lineHeight + 1,
                    applyAlpha(Colors.ACCENT_PURPLE, alpha * 0.6f));
        }

        if (element.getTargetPageId() != null) {
            abilityLinkRegions.add(
                    new ClickableRegion(
                            linkLeft, y, linkRight, y + lineHeight, element.getTargetPageId()));
        }

        return lineHeight;
    }

    private int renderIconText(
            GuiGraphics g, GuideElement element, int x, int y, int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        int iconSize = 10;
        int iconPad = 3;
        int textX = x + iconSize + iconPad;

        ItemStack displayItem = element.getItemStack();
        ItemStack[] icons = element.getCraftingGrid();
        if (icons != null && icons.length > 1) {

            int index = (tickCount / 30) % icons.length;
            displayItem = icons[index];
        }

        if (displayItem != null && !displayItem.isEmpty()) {
            g.pose().pushPose();
            float scale = iconSize / 16.0f;
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1);
            g.renderItem(displayItem, 0, 0);
            g.pose().popPose();
        }

        List<FormattedCharSequence> lines = font.split(text, width - iconSize - iconPad);
        int lineHeight = font.lineHeight + 2;
        int totalHeight = 0;

        for (FormattedCharSequence line : lines) {
            g.drawString(
                    font,
                    line,
                    textX,
                    y + totalHeight + 1,
                    applyAlpha(element.getColor(), alpha),
                    false);
            totalHeight += lineHeight;
        }

        return Math.max(totalHeight, iconSize) + 2;
    }

    private int renderUrlLink(
            GuiGraphics g,
            GuideElement element,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            float alpha) {
        Component label = element.getText();
        if (label == null) return 0;

        int lineHeight = font.lineHeight + 4;
        String labelStr = label.getString();
        int textWidth = font.width(labelStr);

        int iconW = 10;
        int totalW = iconW + 3 + textWidth;
        int startX = x;

        boolean hovered =
                mouseX >= startX
                        && mouseX < startX + totalW
                        && mouseY >= y
                        && mouseY < y + lineHeight;

        int textColor =
                hovered
                        ? applyAlpha(Colors.TEXT_LINK_BLUE, alpha)
                        : applyAlpha(element.getColor(), alpha);
        int iconColor =
                hovered
                        ? applyAlpha(Colors.TEXT_LINK_BLUE, alpha)
                        : applyAlpha(Colors.ACCENT_BLUE, alpha);

        int iy = y + 3;
        g.fill(startX, iy + 1, startX + 1, iy + 5, iconColor);
        g.fill(startX + 1, iy, startX + 4, iy + 1, iconColor);
        g.fill(startX + 1, iy + 5, startX + 4, iy + 6, iconColor);
        g.fill(startX + 4, iy + 1, startX + 5, iy + 3, iconColor);
        g.fill(startX + 5, iy + 3, startX + 6, iy + 5, iconColor);
        g.fill(startX + 6, iy + 1, startX + 9, iy + 2, iconColor);
        g.fill(startX + 6, iy + 6, startX + 9, iy + 7, iconColor);
        g.fill(startX + 9, iy + 2, startX + 10, iy + 6, iconColor);
        g.fill(startX + 4, iy + 3, startX + 6, iy + 4, iconColor);

        int textX = startX + iconW + 3;
        Component underlined = Texts.lit(labelStr).withStyle(ChatFormatting.UNDERLINE);
        g.drawString(font, underlined, textX, y + 1, textColor, false);

        if (element.getTargetPageId() != null) {
            urlLinkRegions.add(
                    new ClickableRegion(
                            startX, y, startX + totalW, y + lineHeight, element.getTargetPageId()));
        }

        return lineHeight;
    }

    private void renderScrollbar(
            GuiGraphics g,
            int x,
            int y,
            int w,
            int h,
            double scrollOffset,
            int totalHeight,
            int visibleHeight,
            float alpha) {

        g.fill(x, y, x + w, y + h, applyAlpha(COLOR_SCROLLBAR_BG, alpha));

        if (totalHeight <= visibleHeight) return;
        float thumbRatio = (float) visibleHeight / totalHeight;
        int thumbH = Math.max(20, (int) (h * thumbRatio));
        float scrollRatio = (float) (scrollOffset / (totalHeight - visibleHeight));
        int thumbY = y + (int) ((h - thumbH) * Mth.clamp(scrollRatio, 0, 1));

        g.fill(
                x + 1,
                thumbY + 1,
                x + w - 1,
                thumbY + thumbH - 1,
                applyAlpha(COLOR_SCROLLBAR_THUMB, alpha));
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (hoveringClose) {
            playClickSound();
            this.onClose();
            return true;
        }

        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN + HEADER_HEIGHT;
        int sidebarRight = wLeft + SIDEBAR_WIDTH;
        int wBottom = height - WINDOW_MARGIN;

        if (mouseX >= wLeft && mouseX < sidebarRight && mouseY >= wTop && mouseY < wBottom) {
            return handleSidebarClick(mouseX, mouseY);
        }

        if (contentTotalHeight > contentVisibleHeight
                && mouseX >= contentScrollbarX
                && mouseX < contentScrollbarX + contentScrollbarW
                && mouseY >= contentScrollbarY
                && mouseY < contentScrollbarY + contentScrollbarH) {
            dragging = DragTarget.CONTENT_SCROLLBAR;
            dragStartMouseY = mouseY;
            dragStartScrollOffset = contentScrollTarget;
            return true;
        }

        if (sidebarTotalHeight > sidebarVisibleHeight
                && mouseX >= sidebarScrollbarX
                && mouseX < sidebarScrollbarX + sidebarScrollbarW
                && mouseY >= sidebarScrollbarY
                && mouseY < sidebarScrollbarY + sidebarScrollbarH) {
            dragging = DragTarget.SIDEBAR_SCROLLBAR;
            dragStartMouseY = mouseY;
            dragStartScrollOffset = sidebarScrollTarget;
            return true;
        }

        for (ClickableRegion region : abilityLinkRegions) {
            if (mouseX >= region.x1
                    && mouseX < region.x2
                    && mouseY >= region.y1
                    && mouseY < region.y2) {
                GuidePage target = book.findPageById(region.pageId);
                if (target != null) {
                    navigateToPage(target);
                    playPageSound();
                    return true;
                }
            }
        }

        for (ClickableRegion region : urlLinkRegions) {
            if (mouseX >= region.x1
                    && mouseX < region.x2
                    && mouseY >= region.y1
                    && mouseY < region.y2) {
                try {
                    Util.getPlatform().openUri(java.net.URI.create(region.pageId));
                    playClickSound();
                } catch (Exception ignored) {
                }
                return true;
            }
        }

        return false;
    }

    private boolean handleSidebarClick(double mouseX, double mouseY) {
        for (SidebarEntry entry : sidebarEntries) {
            int entryHeight = entry.isCategory ? SIDEBAR_CATEGORY_HEIGHT : SIDEBAR_ITEM_HEIGHT;
            if (mouseY >= entry.renderY && mouseY < entry.renderY + entryHeight) {
                if (entry.isCategory && entry.category != null) {
                    String catId = entry.category.getId();
                    if (expandedCategories.contains(catId)) {
                        expandedCategories.remove(catId);
                    } else {
                        expandedCategories.add(catId);
                    }
                    rebuildSidebar();
                    playClickSound();
                    return true;
                } else if (entry.page != null) {
                    if (entry.page != currentPage) {
                        navigateToPage(entry.page);
                        playPageSound();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean handleMouseScroll(
            double mouseX, double mouseY, double scrollX, double scrollY) {
        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN + HEADER_HEIGHT;
        int wRight = width - WINDOW_MARGIN;
        int wBottom = height - WINDOW_MARGIN;
        int sidebarRight = wLeft + SIDEBAR_WIDTH;

        double scrollAmount = -scrollY * 20;

        if (mouseX >= wLeft && mouseX < sidebarRight && mouseY >= wTop && mouseY < wBottom) {
            int areaHeight = wBottom - wTop;
            sidebarScrollTarget =
                    Mth.clamp(
                            sidebarScrollTarget + scrollAmount,
                            0,
                            Math.max(0, sidebarTotalHeight - areaHeight + 12));
            return true;
        }

        if (mouseX >= sidebarRight && mouseX < wRight && mouseY >= wTop && mouseY < wBottom) {
            int contentTop = wTop + PAGE_TITLE_BAR_HEIGHT;
            int areaHeight = wBottom - contentTop;
            contentScrollTarget =
                    Mth.clamp(
                            contentScrollTarget + scrollAmount,
                            0,
                            Math.max(0, contentTotalHeight - areaHeight + 12));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragging != DragTarget.NONE) {
            double deltaY = mouseY - dragStartMouseY;
            if (dragging == DragTarget.CONTENT_SCROLLBAR) {
                int maxScroll = Math.max(0, contentTotalHeight - contentVisibleHeight);
                int thumbH =
                        getThumbHeight(contentScrollbarH, contentTotalHeight, contentVisibleHeight);
                if (contentScrollbarH > thumbH) {
                    double scrollPerPixel = (double) maxScroll / (contentScrollbarH - thumbH);
                    contentScrollTarget =
                            Mth.clamp(
                                    dragStartScrollOffset + deltaY * scrollPerPixel, 0, maxScroll);
                }
            } else if (dragging == DragTarget.SIDEBAR_SCROLLBAR) {
                int maxScroll = Math.max(0, sidebarTotalHeight - sidebarVisibleHeight + 12);
                int thumbH =
                        getThumbHeight(sidebarScrollbarH, sidebarTotalHeight, sidebarVisibleHeight);
                if (sidebarScrollbarH > thumbH) {
                    double scrollPerPixel = (double) maxScroll / (sidebarScrollbarH - thumbH);
                    sidebarScrollTarget =
                            Mth.clamp(
                                    dragStartScrollOffset + deltaY * scrollPerPixel, 0, maxScroll);
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging != DragTarget.NONE) {
            dragging = DragTarget.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int getThumbHeight(int trackHeight, int totalHeight, int visibleHeight) {
        if (totalHeight <= visibleHeight) return trackHeight;
        return Math.max(20, (int) (trackHeight * ((float) visibleHeight / totalHeight)));
    }

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    private void navigateToPage(GuidePage page) {

        if (currentPage != null) {
            pageScrollPositions.put(currentPage.getId(), contentScrollTarget);
        }

        currentPage = page;
        currentCategory = book.findCategoryForPage(page);
        lastOpenedPageId = page.getId();

        double savedScroll = pageScrollPositions.getOrDefault(page.getId(), 0.0);
        contentScrollOffset = savedScroll;
        contentScrollTarget = savedScroll;
    }

    private void playClickSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager()
                    .play(
                            SimpleSoundInstance.forUI(
                                    SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.7f));
        }
    }

    private void playPageSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.5f));
        }
    }

    private void rebuildSidebar() {
        sidebarEntries.clear();
        for (GuideCategory cat : book.getCategories()) {
            sidebarEntries.add(new SidebarEntry(cat));
            if (expandedCategories.contains(cat.getId())) {
                for (GuidePage page : cat.getPages()) {
                    sidebarEntries.add(new SidebarEntry(cat, page));
                }
            }
        }
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        a = (int) (a * alpha);
        return (a << 24) | (color & Colors.TRANSPARENT);
    }

    private static void drawBorder(
            GuiGraphics g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    private static final class SidebarEntry {
        final boolean isCategory;
        @Nullable final GuideCategory category;
        @Nullable final GuidePage page;
        final String text;
        int renderY;

        SidebarEntry(GuideCategory category) {
            this.isCategory = true;
            this.category = category;
            this.page = null;
            this.text = category.getTitle().getString();
        }

        SidebarEntry(GuideCategory category, GuidePage page) {
            this.isCategory = false;
            this.category = category;
            this.page = page;
            this.text = page.getTitle().getString();
        }
    }

    private static final class ClickableRegion {
        final int x1, y1, x2, y2;
        final String pageId;

        ClickableRegion(int x1, int y1, int x2, int y2, String pageId) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.pageId = pageId;
        }
    }
}
