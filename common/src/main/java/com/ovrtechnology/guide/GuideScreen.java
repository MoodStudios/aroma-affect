package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.menu.BaseMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The main guide screen with a wiki-style layout: sidebar table of contents on the left,
 * rich content area on the right. Supports scrolling, category expand/collapse,
 * click sounds, and smooth animations.
 */
public class GuideScreen extends BaseMenuScreen {

    // ── Layout Constants ───────────────────────────────────────────
    private static final int WINDOW_MARGIN = 16;
    private static final int SIDEBAR_WIDTH = 150;
    private static final int SIDEBAR_ITEM_HEIGHT = 22;
    private static final int SIDEBAR_CATEGORY_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 36;
    private static final int CONTENT_PADDING = 16;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int PAGE_TITLE_BAR_HEIGHT = 28;

    // ── Colors ─────────────────────────────────────────────────────
    private static final int COLOR_WINDOW_BG = 0xF0101020;
    private static final int COLOR_SIDEBAR_BG = 0xF0141430;
    private static final int COLOR_HEADER_BG = 0xF01A1A3A;
    private static final int COLOR_CONTENT_BG = 0xF00D0D1C;
    private static final int COLOR_ACCENT = 0xFF6D5EF8;
    private static final int COLOR_ACCENT_DIM = 0xFF4A3DB0;
    private static final int COLOR_ACCENT_GLOW = 0x306D5EF8;
    private static final int COLOR_BORDER = 0xFF2A2A50;
    private static final int COLOR_BORDER_LIGHT = 0xFF383860;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_SUBTITLE = 0xFFAAAACC;
    private static final int COLOR_SIDEBAR_TEXT = 0xFFBBBBDD;
    private static final int COLOR_SIDEBAR_HOVER = 0x30FFFFFF;
    private static final int COLOR_SIDEBAR_SELECTED = 0x406D5EF8;
    private static final int COLOR_CATEGORY_TEXT = 0xFFDDDDEE;
    private static final int COLOR_SCROLLBAR_BG = 0x20FFFFFF;
    private static final int COLOR_SCROLLBAR_THUMB = 0x60AAAACC;
    private static final int COLOR_SCROLLBAR_THUMB_HOVER = 0x90BBBBDD;
    private static final int COLOR_PAGE_TITLE_BG = 0xF0181838;
    private static final int COLOR_TIP_BG = 0x30228B22;
    private static final int COLOR_TIP_BORDER = 0xFF2D8B46;
    private static final int COLOR_ITEM_SHOWCASE_BG = 0x20FFFFFF;
    private static final int COLOR_SEPARATOR = 0xFF333355;
    private static final int COLOR_IMAGE_BORDER = 0xFF444466;
    private static final int COLOR_IMAGE_SHADOW = 0x40000000;

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

    // Cached layout data for sidebar
    private final List<SidebarEntry> sidebarEntries = new ArrayList<>();

    public GuideScreen(GuideBook book) {
        super(book.getTitle());
        this.book = book;
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
        contentScrollOffset += (contentScrollTarget - contentScrollOffset) * 0.35;
        sidebarScrollOffset += (sidebarScrollTarget - sidebarScrollOffset) * 0.35;
    }

    // ── Rendering ──────────────────────────────────────────────────

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY,
                                 float partialTick, float animationProgress) {
        if (animationProgress <= 0.01f) return;

        float alpha = animationProgress;

        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN;
        int wRight = width - WINDOW_MARGIN;
        int wBottom = height - WINDOW_MARGIN;

        // Window background with subtle gradient effect
        g.fill(wLeft, wTop, wRight, wBottom, applyAlpha(COLOR_WINDOW_BG, alpha));

        // Outer border
        drawBorder(g, wLeft, wTop, wRight, wBottom, applyAlpha(COLOR_BORDER, alpha));

        // Top accent line (gradient-like with two layers)
        g.fill(wLeft, wTop, wRight, wTop + 2, applyAlpha(COLOR_ACCENT, alpha));
        g.fill(wLeft, wTop + 2, wRight, wTop + 3, applyAlpha(COLOR_ACCENT_DIM, alpha * 0.5f));

        // Bottom accent line
        g.fill(wLeft + 1, wBottom - 2, wRight - 1, wBottom - 1, applyAlpha(COLOR_ACCENT_DIM, alpha * 0.3f));

        // Header
        renderHeader(g, wLeft, wTop, wRight, alpha);

        int bodyTop = wTop + HEADER_HEIGHT;

        // Sidebar
        int sidebarRight = wLeft + SIDEBAR_WIDTH;
        renderSidebar(g, wLeft, bodyTop, sidebarRight, wBottom, mouseX, mouseY, alpha);

        // Separator between sidebar and content (double line for depth)
        g.fill(sidebarRight, bodyTop, sidebarRight + 1, wBottom, applyAlpha(COLOR_BORDER, alpha));
        g.fill(sidebarRight + 1, bodyTop, sidebarRight + 2, wBottom, applyAlpha(COLOR_BORDER_LIGHT, alpha * 0.3f));

        // Content area
        renderContentArea(g, sidebarRight + 2, bodyTop, wRight, wBottom, mouseX, mouseY, alpha);
    }

    private void renderHeader(GuiGraphics g, int left, int top, int right, float alpha) {
        int bottom = top + HEADER_HEIGHT;
        g.fill(left, top + 3, right, bottom, applyAlpha(COLOR_HEADER_BG, alpha));

        // Title (slightly larger via scale)
        Component title = book.getTitle();
        g.pose().pushMatrix();
        float titleScale = 1.2f;
        g.pose().translate(left + 14, top + 10);
        g.pose().scale(titleScale, titleScale);
        g.drawString(font, title, 0, 0, applyAlpha(COLOR_TITLE, alpha), true);
        g.pose().popMatrix();

        // Subtitle
        Component subtitle = book.getSubtitle();
        if (subtitle != null && !subtitle.getString().isEmpty()) {
            int subtitleX = left + 16 + (int) (font.width(title) * 1.2f);
            g.drawString(font, subtitle, subtitleX, top + 14, applyAlpha(COLOR_SUBTITLE, alpha), false);
        }

        // Close hint with subtle background
        Component closeHint = Component.literal("\u00D7 ESC");
        int hintW = font.width(closeHint) + 10;
        int hintX = right - hintW - 6;
        g.fill(hintX - 2, top + 10, hintX + hintW + 2, top + 10 + font.lineHeight + 4,
                applyAlpha(0x30FFFFFF, alpha));
        g.drawString(font, closeHint, hintX + 3, top + 12, applyAlpha(0xFF888899, alpha), false);

        // Bottom border
        g.fill(left, bottom - 1, right, bottom, applyAlpha(COLOR_BORDER, alpha));
    }

    // ── Sidebar ────────────────────────────────────────────────────

    private void renderSidebar(GuiGraphics g, int left, int top, int right, int bottom,
                               int mouseX, int mouseY, float alpha) {
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

        if (sidebarTotalHeight > areaHeight) {
            renderScrollbar(g, right - SCROLLBAR_WIDTH - 1, top + 1, SCROLLBAR_WIDTH,
                    areaHeight - 2, sidebarScrollOffset, sidebarTotalHeight, areaHeight, alpha);
        }
    }

    private void renderSidebarCategory(GuiGraphics g, int left, int y, int right,
                                       SidebarEntry entry, int mouseX, int mouseY, float alpha) {
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + SIDEBAR_CATEGORY_HEIGHT;

        if (hovered) {
            g.fill(left + 2, y + 1, right - 2, y + SIDEBAR_CATEGORY_HEIGHT - 1,
                    applyAlpha(COLOR_SIDEBAR_HOVER, alpha));
        }

        // Category accent bar (thicker)
        int accentColor = entry.category != null ? entry.category.getAccentColor() : COLOR_ACCENT;
        g.fill(left + 4, y + 4, left + 7, y + SIDEBAR_CATEGORY_HEIGHT - 4, applyAlpha(accentColor, alpha));

        // Arrow indicator
        boolean expanded = entry.category != null && expandedCategories.contains(entry.category.getId());
        String arrow = expanded ? "\u25BC" : "\u25B6";
        g.drawString(font, arrow, left + 11, y + 8, applyAlpha(accentColor, alpha * 0.8f), false);

        // Category title
        String catTitle = entry.text;
        int maxCatWidth = right - left - 32;
        if (font.width(catTitle) > maxCatWidth) {
            catTitle = font.plainSubstrByWidth(catTitle, maxCatWidth - 8) + "..";
        }
        g.drawString(font, catTitle, left + 22, y + 8, applyAlpha(COLOR_CATEGORY_TEXT, alpha), false);
    }

    private void renderSidebarPage(GuiGraphics g, int left, int y, int right,
                                   SidebarEntry entry, int mouseX, int mouseY, float alpha) {
        boolean selected = entry.page == currentPage;
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + SIDEBAR_ITEM_HEIGHT;

        if (selected) {
            // Selected background with glow
            g.fill(left + 2, y + 1, right - 2, y + SIDEBAR_ITEM_HEIGHT - 1,
                    applyAlpha(COLOR_SIDEBAR_SELECTED, alpha));
            // Left accent bar
            g.fill(left + 2, y + 3, left + 5, y + SIDEBAR_ITEM_HEIGHT - 3,
                    applyAlpha(COLOR_ACCENT, alpha));
        } else if (hovered) {
            g.fill(left + 2, y + 1, right - 2, y + SIDEBAR_ITEM_HEIGHT - 1,
                    applyAlpha(COLOR_SIDEBAR_HOVER, alpha * 0.5f));
        }

        // Item icon at proper size (16x16 rendered at native scale)
        int iconX = left + 10;
        int iconY = y + 3;
        if (entry.page != null && entry.page.getIcon() != null && entry.page.getIcon().isItem()) {
            g.renderItem(entry.page.getIcon().getItemStack(), iconX, iconY);
        }

        // Page title
        String pageTitle = entry.text;
        int textX = left + 30;
        int maxWidth = right - textX - 6;
        if (font.width(pageTitle) > maxWidth) {
            pageTitle = font.plainSubstrByWidth(pageTitle, maxWidth - 8) + "..";
        }
        int textColor = selected ? COLOR_TITLE : COLOR_SIDEBAR_TEXT;
        g.drawString(font, pageTitle, textX, y + 7, applyAlpha(textColor, alpha), selected);
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

        // Accent bar next to title
        g.fill(left + 8, top + 7, left + 11, top + PAGE_TITLE_BAR_HEIGHT - 7, applyAlpha(accent, alpha));

        // Page icon in title bar
        int titleTextX = left + 16;
        if (currentPage.getIcon() != null && currentPage.getIcon().isItem()) {
            g.pose().pushMatrix();
            g.pose().translate(left + 16, top + 6);
            g.pose().scale(0.75f, 0.75f);
            g.renderItem(currentPage.getIcon().getItemStack(), 0, 0);
            g.pose().popMatrix();
            titleTextX = left + 32;
        }

        g.drawString(font, currentPage.getTitle(), titleTextX, top + 10,
                applyAlpha(COLOR_TITLE, alpha), true);

        // Breadcrumb (category > page)
        if (cat != null) {
            String breadcrumb = cat.getTitle().getString() + " \u203A " + currentPage.getTitle().getString();
            int breadcrumbX = right - 10 - font.width(breadcrumb);
            if (breadcrumbX > titleTextX + font.width(currentPage.getTitle()) + 10) {
                g.drawString(font, breadcrumb, breadcrumbX, top + 10,
                        applyAlpha(0xFF555577, alpha), false);
            }
        }

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

        if (contentTotalHeight > areaHeight) {
            renderScrollbar(g, right - SCROLLBAR_WIDTH - 1, contentTop + 1, SCROLLBAR_WIDTH,
                    areaHeight - 2, contentScrollOffset, contentTotalHeight, areaHeight, alpha);
        }
    }

    private int renderElement(GuiGraphics g, GuideElement element, int x, int y,
                              int width, float alpha) {
        return switch (element.getType()) {
            case HEADER -> renderHeaderElement(g, element, x, y, width, alpha);
            case SUBHEADER -> renderSubheaderElement(g, element, x, y, width, alpha);
            case TEXT -> renderTextElement(g, element, x, y, width, alpha);
            case ITEM_SHOWCASE -> renderItemShowcase(g, element, x, y, width, alpha);
            case IMAGE -> renderImageElement(g, element, x, y, width, alpha);
            case SEPARATOR -> renderSeparator(g, x, y, width, alpha);
            case SPACER -> element.getSpacerHeight();
            case TIP -> renderTipElement(g, element, x, y, width, alpha);
        };
    }

    private int renderHeaderElement(GuiGraphics g, GuideElement element, int x, int y,
                                    int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        // Scaled header text
        g.pose().pushMatrix();
        float scale = 1.5f;
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.drawString(font, text, 0, 0, applyAlpha(0xFFFFFFFF, alpha), true);
        g.pose().popMatrix();

        int textHeight = (int) (font.lineHeight * scale);

        // Accent underline with gradient fade
        g.fill(x, y + textHeight + 3, x + width, y + textHeight + 4, applyAlpha(COLOR_ACCENT, alpha));
        g.fill(x, y + textHeight + 4, x + width / 2, y + textHeight + 5,
                applyAlpha(COLOR_ACCENT_DIM, alpha * 0.4f));

        return textHeight + 10;
    }

    private int renderSubheaderElement(GuiGraphics g, GuideElement element, int x, int y,
                                       int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        // Accent block before subheader
        g.fill(x, y + 2, x + 3, y + font.lineHeight, applyAlpha(COLOR_ACCENT, alpha));
        g.drawString(font, text, x + 8, y + 2, applyAlpha(element.getColor(), alpha), true);

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
        int boxHeight = 28;

        // Card background with subtle border
        g.fill(x, y, x + width, y + boxHeight, applyAlpha(COLOR_ITEM_SHOWCASE_BG, alpha));
        drawBorder(g, x, y, x + width, y + boxHeight, applyAlpha(COLOR_BORDER_LIGHT, alpha));

        // Item at native 16x16 size
        if (element.getItemStack() != null) {
            g.renderItem(element.getItemStack(), x + 6, y + 6);
        }

        // Description text
        if (element.getText() != null) {
            g.drawString(font, element.getText(), x + 28, y + 10,
                    applyAlpha(element.getColor(), alpha), false);
        }

        return boxHeight + 4;
    }

    private int renderImageElement(GuiGraphics g, GuideElement element, int x, int y,
                                   int width, float alpha) {
        if (element.getImageTexture() == null) return 0;

        int texW = element.getImageWidth();
        int texH = element.getImageHeight();

        // Render at native texture size (image pre-sized to fit content area)
        int imgW = texW;
        int imgH = texH;
        int imgX = x + (width - imgW) / 2;

        // Drop shadow
        g.fill(imgX + 2, y + 2, imgX + imgW + 2, y + imgH + 2,
                applyAlpha(COLOR_IMAGE_SHADOW, alpha));

        // 10-param blit: width==texW so the full texture UV range (0..1) is sampled
        g.blit(RenderPipelines.GUI_TEXTURED, element.getImageTexture(),
                imgX, y, 0.0f, 0.0f, texW, texH, texW, texH);

        // Border frame
        drawBorder(g, imgX - 1, y - 1, imgX + imgW + 1, y + imgH + 1,
                applyAlpha(COLOR_IMAGE_BORDER, alpha));

        // Corner accents
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
        // Center line
        g.fill(x + 10, midY, x + width - 10, midY + 1, applyAlpha(COLOR_SEPARATOR, alpha));
        // Accent dot in center
        int cx = x + width / 2;
        g.fill(cx - 1, midY - 1, cx + 2, midY + 2, applyAlpha(COLOR_ACCENT_DIM, alpha * 0.6f));
        return 12;
    }

    private int renderTipElement(GuiGraphics g, GuideElement element, int x, int y,
                                 int width, float alpha) {
        Component text = element.getText();
        if (text == null) return 0;

        List<FormattedCharSequence> lines = font.split(text, width - 24);
        int lineHeight = font.lineHeight + 2;
        int textHeight = lines.size() * lineHeight;
        int boxHeight = textHeight + 12;

        // Tip background
        g.fill(x, y, x + width, y + boxHeight, applyAlpha(COLOR_TIP_BG, alpha));

        // Left accent bar
        g.fill(x, y, x + 3, y + boxHeight, applyAlpha(COLOR_TIP_BORDER, alpha));

        // Top accent line
        g.fill(x + 3, y, x + width, y + 1, applyAlpha(COLOR_TIP_BORDER, alpha * 0.3f));

        // Tip icon
        g.drawString(font, "\u2714", x + 8, y + 6, applyAlpha(0xFF7BD48A, alpha), true);

        // Text
        int textY = y + 6;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x + 20, textY, applyAlpha(element.getColor(), alpha), false);
            textY += lineHeight;
        }

        return boxHeight + 6;
    }

    // ── Scrollbar ──────────────────────────────────────────────────

    private void renderScrollbar(GuiGraphics g, int x, int y, int w, int h,
                                 double scrollOffset, int totalHeight, int visibleHeight, float alpha) {
        // Track
        g.fill(x, y, x + w, y + h, applyAlpha(COLOR_SCROLLBAR_BG, alpha));

        if (totalHeight <= visibleHeight) return;
        float thumbRatio = (float) visibleHeight / totalHeight;
        int thumbH = Math.max(20, (int) (h * thumbRatio));
        float scrollRatio = (float) (scrollOffset / (totalHeight - visibleHeight));
        int thumbY = y + (int) ((h - thumbH) * Mth.clamp(scrollRatio, 0, 1));

        // Thumb with rounded-ish look (inset by 1px)
        g.fill(x + 1, thumbY + 1, x + w - 1, thumbY + thumbH - 1,
                applyAlpha(COLOR_SCROLLBAR_THUMB, alpha));
    }

    // ── Input ──────────────────────────────────────────────────────

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN + HEADER_HEIGHT;
        int sidebarRight = wLeft + SIDEBAR_WIDTH;
        int wBottom = height - WINDOW_MARGIN;

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
    protected boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        int wLeft = WINDOW_MARGIN;
        int wTop = WINDOW_MARGIN + HEADER_HEIGHT;
        int wRight = width - WINDOW_MARGIN;
        int wBottom = height - WINDOW_MARGIN;
        int sidebarRight = wLeft + SIDEBAR_WIDTH;

        double scrollAmount = -scrollY * 20;

        if (mouseX >= wLeft && mouseX < sidebarRight && mouseY >= wTop && mouseY < wBottom) {
            int areaHeight = wBottom - wTop;
            sidebarScrollTarget = Mth.clamp(sidebarScrollTarget + scrollAmount,
                    0, Math.max(0, sidebarTotalHeight - areaHeight + 12));
            return true;
        }

        if (mouseX >= sidebarRight && mouseX < wRight && mouseY >= wTop && mouseY < wBottom) {
            int contentTop = wTop + PAGE_TITLE_BAR_HEIGHT;
            int areaHeight = wBottom - contentTop;
            contentScrollTarget = Mth.clamp(contentScrollTarget + scrollAmount,
                    0, Math.max(0, contentTotalHeight - areaHeight + 12));
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

    // ── Sounds ─────────────────────────────────────────────────────

    private void playClickSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.7f));
        }
    }

    private void playPageSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.5f));
        }
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
