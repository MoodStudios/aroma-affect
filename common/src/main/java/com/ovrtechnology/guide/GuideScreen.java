package com.ovrtechnology.guide;

import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.menu.BaseMenuScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The main guide screen with a wiki-style layout: sidebar table of contents on the left,
 * rich content area on the right. Supports scrolling, category expand/collapse, and
 * smooth animations.
 */
public class GuideScreen extends BaseMenuScreen {

    // ── Layout Constants ───────────────────────────────────────────
    private static final int WINDOW_MARGIN = 20;
    private static final int SIDEBAR_WIDTH = 130;
    private static final int SIDEBAR_ITEM_HEIGHT = 16;
    private static final int SIDEBAR_CATEGORY_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 32;
    private static final int CONTENT_PADDING = 14;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int PAGE_TITLE_BAR_HEIGHT = 26;

    // ── Colors ─────────────────────────────────────────────────────
    private static final int COLOR_WINDOW_BG = 0xF0101020;
    private static final int COLOR_SIDEBAR_BG = 0xF0161630;
    private static final int COLOR_HEADER_BG = 0xF01A1A3A;
    private static final int COLOR_CONTENT_BG = 0xF00E0E1E;
    private static final int COLOR_ACCENT = 0xFF6D5EF8;
    private static final int COLOR_ACCENT_DIM = 0xFF4A3DB0;
    private static final int COLOR_BORDER = 0xFF2A2A50;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_SUBTITLE = 0xFFAAAACC;
    private static final int COLOR_SIDEBAR_TEXT = 0xFFCCCCDD;
    private static final int COLOR_SIDEBAR_HOVER = 0x40FFFFFF;
    private static final int COLOR_SIDEBAR_SELECTED = 0x306D5EF8;
    private static final int COLOR_CATEGORY_TEXT = 0xFFDDDDEE;
    private static final int COLOR_SCROLLBAR_BG = 0x40FFFFFF;
    private static final int COLOR_SCROLLBAR_THUMB = 0x80AAAACC;
    private static final int COLOR_PAGE_TITLE_BG = 0xF0181838;
    private static final int COLOR_TIP_BG = 0x30228B22;
    private static final int COLOR_TIP_BORDER = 0xFF2D8B46;
    private static final int COLOR_ITEM_SHOWCASE_BG = 0x20FFFFFF;
    private static final int COLOR_SEPARATOR = 0xFF333355;

    // ── State ──────────────────────────────────────────────────────
    private final GuideBook book;
    @Nullable
    private GuidePage currentPage;
    @Nullable
    private GuideCategory currentCategory;
    private final Set<String> expandedCategories = new HashSet<>();

    private double contentScrollOffset = 0;
    private double contentScrollTarget = 0;
    private int contentTotalHeight = 0;

    private double sidebarScrollOffset = 0;
    private double sidebarScrollTarget = 0;
    private int sidebarTotalHeight = 0;

    private boolean draggingScrollbar = false;
    private int dragScrollbarStartY = 0;
    private double dragScrollbarStartOffset = 0;

    // Cached layout data for sidebar
    private final List<SidebarEntry> sidebarEntries = new ArrayList<>();

    public GuideScreen(GuideBook book) {
        super(book.getTitle());
        this.book = book;
        // Expand all categories by default
        for (GuideCategory cat : book.getCategories()) {
            expandedCategories.add(cat.getId());
        }
    }

    @Override
    protected void init() {
        super.init();
        if (currentPage == null) {
            currentPage = book.getFirstPage();
            if (currentPage != null) {
                currentCategory = book.findCategoryForPage(currentPage);
            }
        }
        rebuildSidebar();
    }

    @Override
    public void tick() {
        super.tick();
        // Smooth scrolling
        contentScrollOffset += (contentScrollTarget - contentScrollOffset) * 0.35;
        sidebarScrollOffset += (sidebarScrollTarget - sidebarScrollOffset) * 0.35;
    }

    // ── Rendering ──────────────────────────────────────────────────

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY,
                                 float partialTick, float animationProgress) {
        if (animationProgress <= 0.01f) return;

        float alpha = animationProgress;
        float scale = 0.9f + 0.1f * animationProgress;

        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN;
        int wRight = width - WINDOW_MARGIN;
        int wBottom = height - WINDOW_MARGIN;

        // Window background
        g.fill(wLeft, wTop, wRight, wBottom, applyAlpha(COLOR_WINDOW_BG, alpha));

        // Outer border with accent
        drawBorder(g, wLeft, wTop, wRight, wBottom, applyAlpha(COLOR_BORDER, alpha));
        // Accent line at top
        g.fill(wLeft, wTop, wRight, wTop + 2, applyAlpha(COLOR_ACCENT, alpha));

        // Header
        renderHeader(g, wLeft, wTop, wRight, alpha);

        int bodyTop = wTop + HEADER_HEIGHT;

        // Sidebar
        int sidebarRight = wLeft + SIDEBAR_WIDTH;
        renderSidebar(g, wLeft, bodyTop, sidebarRight, wBottom, mouseX, mouseY, alpha);

        // Separator between sidebar and content
        g.fill(sidebarRight, bodyTop, sidebarRight + 1, wBottom, applyAlpha(COLOR_BORDER, alpha));

        // Content area
        renderContentArea(g, sidebarRight + 1, bodyTop, wRight, wBottom, mouseX, mouseY, alpha);
    }

    private void renderHeader(GuiGraphics g, int left, int top, int right, float alpha) {
        int bottom = top + HEADER_HEIGHT;
        g.fill(left, top + 2, right, bottom, applyAlpha(COLOR_HEADER_BG, alpha));

        // Title
        Component title = book.getTitle();
        g.drawString(font, title, left + 12, top + 8, applyAlpha(COLOR_TITLE, alpha), true);

        // Subtitle
        Component subtitle = book.getSubtitle();
        if (subtitle != null && !subtitle.getString().isEmpty()) {
            int subtitleX = left + 14 + font.width(title);
            g.drawString(font, subtitle, subtitleX, top + 8, applyAlpha(COLOR_SUBTITLE, alpha), false);
        }

        // Close hint
        Component closeHint = Component.literal("ESC to close");
        int hintX = right - 10 - font.width(closeHint);
        g.drawString(font, closeHint, hintX, top + 8, applyAlpha(0xFF666688, alpha), false);

        // Bottom border
        g.fill(left, bottom - 1, right, bottom, applyAlpha(COLOR_BORDER, alpha));
    }

    // ── Sidebar ────────────────────────────────────────────────────

    private void renderSidebar(GuiGraphics g, int left, int top, int right, int bottom,
                               int mouseX, int mouseY, float alpha) {
        g.fill(left, top, right, bottom, applyAlpha(COLOR_SIDEBAR_BG, alpha));

        int areaHeight = bottom - top;
        int contentStartY = top + 4;

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

        // Sidebar scrollbar
        if (sidebarTotalHeight > areaHeight) {
            renderScrollbar(g, right - SCROLLBAR_WIDTH - 1, top + 1, SCROLLBAR_WIDTH,
                    areaHeight - 2, sidebarScrollOffset, sidebarTotalHeight, areaHeight, alpha);
        }
    }

    private void renderSidebarCategory(GuiGraphics g, int left, int y, int right,
                                       SidebarEntry entry, int mouseX, int mouseY, float alpha) {
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + SIDEBAR_CATEGORY_HEIGHT;

        if (hovered) {
            g.fill(left + 2, y, right - 2, y + SIDEBAR_CATEGORY_HEIGHT, applyAlpha(COLOR_SIDEBAR_HOVER, alpha));
        }

        // Category accent bar
        int accentColor = entry.category != null ? entry.category.getAccentColor() : COLOR_ACCENT;
        g.fill(left + 4, y + 3, left + 6, y + SIDEBAR_CATEGORY_HEIGHT - 3, applyAlpha(accentColor, alpha));

        // Arrow indicator
        boolean expanded = entry.category != null && expandedCategories.contains(entry.category.getId());
        String arrow = expanded ? "\u25BC" : "\u25B6";
        g.drawString(font, arrow, left + 10, y + 6, applyAlpha(0xFF888899, alpha), false);

        // Category title
        String catTitle = entry.text;
        if (font.width(catTitle) > right - left - 30) {
            catTitle = font.plainSubstrByWidth(catTitle, right - left - 34) + "..";
        }
        g.drawString(font, catTitle, left + 20, y + 6, applyAlpha(COLOR_CATEGORY_TEXT, alpha), false);
    }

    private void renderSidebarPage(GuiGraphics g, int left, int y, int right,
                                   SidebarEntry entry, int mouseX, int mouseY, float alpha) {
        boolean selected = entry.page == currentPage;
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + SIDEBAR_ITEM_HEIGHT;

        if (selected) {
            g.fill(left + 2, y, right - 2, y + SIDEBAR_ITEM_HEIGHT, applyAlpha(COLOR_SIDEBAR_SELECTED, alpha));
            // Selected indicator
            g.fill(left + 2, y + 2, left + 4, y + SIDEBAR_ITEM_HEIGHT - 2, applyAlpha(COLOR_ACCENT, alpha));
        } else if (hovered) {
            g.fill(left + 2, y, right - 2, y + SIDEBAR_ITEM_HEIGHT, applyAlpha(COLOR_SIDEBAR_HOVER, alpha * 0.5f));
        }

        // Item icon (small)
        if (entry.page != null && entry.page.getIcon() != null && entry.page.getIcon().isItem()) {
            // We render a tiny item - using pose stack to scale
            g.pose().pushMatrix();
            float iconScale = 0.5f;
            int iconX = left + 14;
            int iconY = y;
            g.pose().translate(iconX, iconY);
            g.pose().scale(iconScale, iconScale);
            g.renderItem(entry.page.getIcon().getItemStack(), 0, 0);
            g.pose().popMatrix();
        }

        String pageTitle = entry.text;
        int textX = left + 26;
        int maxWidth = right - textX - 4;
        if (font.width(pageTitle) > maxWidth) {
            pageTitle = font.plainSubstrByWidth(pageTitle, maxWidth - 8) + "..";
        }
        int textColor = selected ? COLOR_TITLE : COLOR_SIDEBAR_TEXT;
        g.drawString(font, pageTitle, textX, y + 4, applyAlpha(textColor, alpha), false);
    }

    // ── Content Area ───────────────────────────────────────────────

    private void renderContentArea(GuiGraphics g, int left, int top, int right, int bottom,
                                   int mouseX, int mouseY, float alpha) {
        g.fill(left, top, right, bottom, applyAlpha(COLOR_CONTENT_BG, alpha));

        if (currentPage == null) {
            String msg = "Select a page from the sidebar";
            int msgX = left + (right - left - font.width(msg)) / 2;
            int msgY = top + (bottom - top) / 2;
            g.drawString(font, msg, msgX, msgY, applyAlpha(0xFF666688, alpha), false);
            return;
        }

        // Page title bar
        int titleBarBottom = top + PAGE_TITLE_BAR_HEIGHT;
        g.fill(left, top, right, titleBarBottom, applyAlpha(COLOR_PAGE_TITLE_BG, alpha));
        g.fill(left, titleBarBottom - 1, right, titleBarBottom, applyAlpha(COLOR_BORDER, alpha));

        // Page title with category accent
        GuideCategory cat = book.findCategoryForPage(currentPage);
        int accent = cat != null ? cat.getAccentColor() : COLOR_ACCENT;
        g.fill(left + 6, top + 6, left + 9, top + PAGE_TITLE_BAR_HEIGHT - 6, applyAlpha(accent, alpha));
        g.drawString(font, currentPage.getTitle(), left + 14, top + 9, applyAlpha(COLOR_TITLE, alpha), true);

        // Content scroll area
        int contentTop = titleBarBottom;
        int contentRight = right - SCROLLBAR_WIDTH - 2;
        int areaHeight = bottom - contentTop;

        g.enableScissor(left, contentTop, contentRight, bottom);

        int y = contentTop + CONTENT_PADDING - (int) contentScrollOffset;
        int contentWidth = contentRight - left - CONTENT_PADDING * 2;
        int contentLeft = left + CONTENT_PADDING;

        contentTotalHeight = CONTENT_PADDING;

        for (GuideElement element : currentPage.getElements()) {
            int elementHeight = renderElement(g, element, contentLeft, y, contentWidth, alpha);
            y += elementHeight;
            contentTotalHeight += elementHeight;
        }

        contentTotalHeight += CONTENT_PADDING;

        g.disableScissor();

        // Content scrollbar
        if (contentTotalHeight > areaHeight) {
            renderScrollbar(g, right - SCROLLBAR_WIDTH - 1, contentTop + 1, SCROLLBAR_WIDTH,
                    areaHeight - 2, contentScrollOffset, contentTotalHeight, areaHeight, alpha);
        }
    }

    private int renderElement(GuiGraphics g, GuideElement element, int x, int y,
                              int width, float alpha) {
        switch (element.getType()) {
            case HEADER:
                return renderHeaderElement(g, element, x, y, width, alpha);
            case SUBHEADER:
                return renderSubheaderElement(g, element, x, y, width, alpha);
            case TEXT:
                return renderTextElement(g, element, x, y, width, alpha);
            case ITEM_SHOWCASE:
                return renderItemShowcase(g, element, x, y, width, alpha);
            case IMAGE:
                return renderImageElement(g, element, x, y, width, alpha);
            case SEPARATOR:
                return renderSeparator(g, x, y, width, alpha);
            case SPACER:
                return element.getSpacerHeight();
            case TIP:
                return renderTipElement(g, element, x, y, width, alpha);
            default:
                return 0;
        }
    }

    private int renderHeaderElement(GuiGraphics g, GuideElement element, int x, int y,
                                    int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        // Draw scaled header text (1.5x)
        g.pose().pushMatrix();
        float scale = 1.5f;
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.drawString(font, text, 0, 0, applyAlpha(0xFFFFFFFF, alpha), true);
        g.pose().popMatrix();

        int textHeight = (int) (font.lineHeight * scale);

        // Underline with accent gradient
        g.fill(x, y + textHeight + 2, x + width, y + textHeight + 3, applyAlpha(COLOR_ACCENT, alpha));
        g.fill(x, y + textHeight + 3, x + width, y + textHeight + 4, applyAlpha(COLOR_ACCENT_DIM, alpha * 0.5f));

        return textHeight + 8;
    }

    private int renderSubheaderElement(GuiGraphics g, GuideElement element, int x, int y,
                                       int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        // Small accent dot before subheader
        g.fill(x, y + 3, x + 3, y + 3 + font.lineHeight - 3, applyAlpha(COLOR_ACCENT, alpha));
        g.drawString(font, text, x + 7, y + 2, applyAlpha(element.getColor(), alpha), true);

        return font.lineHeight + 6;
    }

    private int renderTextElement(GuiGraphics g, GuideElement element, int x, int y,
                                  int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        List<FormattedCharSequence> lines = font.split(text, width);
        int lineHeight = font.lineHeight + 2;
        int totalHeight = 0;

        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, y + totalHeight, applyAlpha(element.getColor(), alpha), false);
            totalHeight += lineHeight;
        }

        return totalHeight + 2;
    }

    private int renderItemShowcase(GuiGraphics g, GuideElement element, int x, int y,
                                   int width, float alpha) {
        // Background card
        g.fill(x, y, x + width, y + 24, applyAlpha(COLOR_ITEM_SHOWCASE_BG, alpha));
        drawBorder(g, x, y, x + width, y + 24, applyAlpha(COLOR_BORDER, alpha));

        // Render item
        if (element.getItemStack() != null) {
            g.renderItem(element.getItemStack(), x + 4, y + 4);
        }

        // Description text
        if (element.getText() != null) {
            g.drawString(font, element.getText(), x + 26, y + 8,
                    applyAlpha(element.getColor(), alpha), false);
        }

        return 28;
    }

    private int renderImageElement(GuiGraphics g, GuideElement element, int x, int y,
                                   int width, float alpha) {
        if (element.getImageTexture() == null) return 0;

        int imgW = Math.min(element.getImageWidth(), width);
        int imgH = element.getImageHeight();
        if (element.getImageWidth() > width) {
            float ratio = (float) width / element.getImageWidth();
            imgH = (int) (imgH * ratio);
        }

        int imgX = x + (width - imgW) / 2;

        // Border around image
        drawBorder(g, imgX - 1, y - 1, imgX + imgW + 1, y + imgH + 1,
                applyAlpha(COLOR_BORDER, alpha));

        g.blit(element.getImageTexture(), imgX, y, 0, 0, imgW, imgH,
                element.getImageWidth(), element.getImageHeight());

        return imgH + 6;
    }

    private int renderSeparator(GuiGraphics g, int x, int y, int width, float alpha) {
        int midY = y + 4;
        // Dotted-style separator
        for (int dx = 0; dx < width; dx += 4) {
            g.fill(x + dx, midY, x + dx + 2, midY + 1, applyAlpha(COLOR_SEPARATOR, alpha));
        }
        return 10;
    }

    private int renderTipElement(GuiGraphics g, GuideElement element, int x, int y,
                                 int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        List<FormattedCharSequence> lines = font.split(text, width - 20);
        int lineHeight = font.lineHeight + 2;
        int textHeight = lines.size() * lineHeight;
        int boxHeight = textHeight + 10;

        // Tip background
        g.fill(x, y, x + width, y + boxHeight, applyAlpha(COLOR_TIP_BG, alpha));

        // Left accent bar
        g.fill(x, y, x + 3, y + boxHeight, applyAlpha(COLOR_TIP_BORDER, alpha));

        // Tip icon
        g.drawString(font, "\u2714", x + 8, y + 5, applyAlpha(0xFF7BD48A, alpha), false);

        // Text
        int textY = y + 5;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x + 18, textY, applyAlpha(element.getColor(), alpha), false);
            textY += lineHeight;
        }

        return boxHeight + 4;
    }

    // ── Scrollbar ──────────────────────────────────────────────────

    private void renderScrollbar(GuiGraphics g, int x, int y, int w, int h,
                                 double scrollOffset, int totalHeight, int visibleHeight, float alpha) {
        // Track
        g.fill(x, y, x + w, y + h, applyAlpha(COLOR_SCROLLBAR_BG, alpha * 0.3f));

        // Thumb
        if (totalHeight <= visibleHeight) return;
        float thumbRatio = (float) visibleHeight / totalHeight;
        int thumbH = Math.max(20, (int) (h * thumbRatio));
        float scrollRatio = (float) (scrollOffset / (totalHeight - visibleHeight));
        int thumbY = y + (int) ((h - thumbH) * scrollRatio);

        g.fill(x + 1, thumbY, x + w - 1, thumbY + thumbH, applyAlpha(COLOR_SCROLLBAR_THUMB, alpha));
    }

    // ── Input ──────────────────────────────────────────────────────

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN + HEADER_HEIGHT;
        int sidebarRight = wLeft + SIDEBAR_WIDTH;
        int wBottom = height - WINDOW_MARGIN;

        // Check sidebar clicks
        if (mouseX >= wLeft && mouseX < sidebarRight && mouseY >= wTop && mouseY < wBottom) {
            return handleSidebarClick(mouseX, mouseY);
        }

        return false;
    }

    private boolean handleSidebarClick(double mouseX, double mouseY) {
        for (SidebarEntry entry : sidebarEntries) {
            int entryHeight = entry.isCategory ? SIDEBAR_CATEGORY_HEIGHT : SIDEBAR_ITEM_HEIGHT;
            if (mouseY >= entry.renderY && mouseY < entry.renderY + entryHeight) {
                if (entry.isCategory && entry.category != null) {
                    // Toggle category expansion
                    String catId = entry.category.getId();
                    if (expandedCategories.contains(catId)) {
                        expandedCategories.remove(catId);
                    } else {
                        expandedCategories.add(catId);
                    }
                    rebuildSidebar();
                    return true;
                } else if (entry.page != null) {
                    navigateToPage(entry.page);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN + HEADER_HEIGHT;
        int wRight = width - WINDOW_MARGIN;
        int wBottom = height - WINDOW_MARGIN;
        int sidebarRight = wLeft + SIDEBAR_WIDTH;

        double scrollAmount = -scrollY * 16;

        if (mouseX >= wLeft && mouseX < sidebarRight && mouseY >= wTop && mouseY < wBottom) {
            // Sidebar scroll
            int areaHeight = wBottom - wTop;
            sidebarScrollTarget = Mth.clamp(sidebarScrollTarget + scrollAmount,
                    0, Math.max(0, sidebarTotalHeight - areaHeight + 8));
            return true;
        }

        if (mouseX >= sidebarRight && mouseX < wRight && mouseY >= wTop && mouseY < wBottom) {
            // Content scroll
            int contentTop = wTop + PAGE_TITLE_BAR_HEIGHT;
            int areaHeight = wBottom - contentTop;
            contentScrollTarget = Mth.clamp(contentScrollTarget + scrollAmount,
                    0, Math.max(0, contentTotalHeight - areaHeight + 8));
            return true;
        }

        return false;
    }

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    // ── Navigation ─────────────────────────────────────────────────

    private void navigateToPage(GuidePage page) {
        currentPage = page;
        currentCategory = book.findCategoryForPage(page);
        contentScrollOffset = 0;
        contentScrollTarget = 0;
    }

    // ── Sidebar Data ───────────────────────────────────────────────

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

    // ── Utility ────────────────────────────────────────────────────

    private static int applyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        a = (int) (a * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static void drawBorder(GuiGraphics g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    // ── Inner Classes ──────────────────────────────────────────────

    private static final class SidebarEntry {
        final boolean isCategory;
        @Nullable
        final GuideCategory category;
        @Nullable
        final GuidePage page;
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
}
