package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.history.BlacklistEntry;
import com.ovrtechnology.history.HistoryEntry;
import com.ovrtechnology.history.SavedEntry;
import com.ovrtechnology.history.TrackingHistoryData;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.tracking.TrackingConfig;
import com.ovrtechnology.trigger.PassiveModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * History screen with 3 tabs: History, Saved, Blacklist.
 * Allows reviewing past tracking results, saving favorites, and blacklisting positions.
 */
public class HistoryMenuScreen extends BaseMenuScreen {

    private enum Tab { HISTORY, SAVED, BLACKLIST }

    // ── Layout constants ─────────────────────────────────────────────────

    private static final int MAX_LIST_WIDTH = 380;
    private static final int ROW_HEIGHT = 36;
    private static final int ROW_PADDING = 3;
    private static final int SEARCH_BOX_HEIGHT = 20;
    private static final int TAB_HEIGHT = 16;
    private static final int TAB_GAP = 4;
    private static final int ICON_BTN_SIZE = 14;
    private static final int ICON_BTN_GAP = 2;
    private static final int GO_BTN_W = 28;
    private static final int ZIGZAG_W = 6;
    private static final int BACK_BUTTON_SIZE = 24;
    private static final int BACK_BUTTON_PADDING = 8;

    private static final ResourceLocation ICON_BACK = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_back.png");

    // ── Tab colors ───────────────────────────────────────────────────────

    private static final int TAB_HISTORY_COLOR = 0xFF6B8CFF;
    private static final int TAB_SAVED_COLOR = 0xFFFFCC44;
    private static final int TAB_BLACKLIST_COLOR = 0xFFFF6B6B;

    private static final int ROW_COLOR = 0xB0222222;
    private static final int ROW_HOVER_COLOR = 0xE0444488;

    // ── Category accent colors ───────────────────────────────────────────

    private static final int BADGE_BLOCKS = 0xFF4488CC;
    private static final int BADGE_BIOMES = 0xFF44AA44;
    private static final int BADGE_STRUCTURES = 0xFFCC8844;
    private static final int BADGE_FLOWERS = 0xFFCC44AA;

    // ── State ────────────────────────────────────────────────────────────

    private Tab activeTab = Tab.HISTORY;
    private EditBox searchBox;
    private String searchQuery = "";
    private int listScrollOffset = 0;
    private int hoveredRowIndex = -1;
    private int hoveredActionIndex = -1;
    private boolean isHoveringBackButton = false;

    // Tab button hover states
    private int hoveredTabIndex = -1;

    // Name popup state
    private boolean showNamePopup = false;
    private EditBox nameEditBox;
    private int namePopupSourceIndex = -1;
    private boolean namePopupIsRename = false;

    // Tooltip state (deferred rendering after scissor)
    private Component pendingTooltip = null;
    private int pendingTooltipX, pendingTooltipY;

    // Filtered display lists (rebuilt on tab switch / search change)
    private final List<Integer> filteredIndices = new ArrayList<>();

    public HistoryMenuScreen() {
        super(Component.translatable("history.aromaaffect.title"));
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int searchX = (width - listWidth) / 2;
        int searchY = 56;

        searchBox = new EditBox(font, searchX, searchY, listWidth, SEARCH_BOX_HEIGHT,
                Component.translatable("history.aromaaffect.search"));
        searchBox.setHint(Component.translatable("history.aromaaffect.search"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(query -> {
            searchQuery = query;
            rebuildFilteredList();
        });
        addWidget(searchBox);

        rebuildFilteredList();
    }

    // ── Filtering ────────────────────────────────────────────────────────

    private void rebuildFilteredList() {
        filteredIndices.clear();
        listScrollOffset = 0;
        String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
        TrackingHistoryData data = TrackingHistoryData.getInstance();

        switch (activeTab) {
            case HISTORY -> {
                for (int i = 0; i < data.getHistory().size(); i++) {
                    HistoryEntry e = data.getHistory().get(i);
                    if (matchesQuery(e.targetId, e.displayName, lowerQuery)) {
                        filteredIndices.add(i);
                    }
                }
            }
            case SAVED -> {
                for (int i = 0; i < data.getSaved().size(); i++) {
                    SavedEntry e = data.getSaved().get(i);
                    if (matchesQuery(e.targetId, e.customName, lowerQuery)) {
                        filteredIndices.add(i);
                    }
                }
            }
            case BLACKLIST -> {
                for (int i = 0; i < data.getBlacklist().size(); i++) {
                    BlacklistEntry e = data.getBlacklist().get(i);
                    if (matchesQuery(e.targetId, e.displayName, lowerQuery)) {
                        filteredIndices.add(i);
                    }
                }
            }
        }
    }

    private boolean matchesQuery(String targetId, String displayName, String lowerQuery) {
        if (lowerQuery.isEmpty()) return true;
        if (targetId.toLowerCase(Locale.ROOT).contains(lowerQuery)) return true;
        return displayName != null && displayName.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    // ── Rendering ────────────────────────────────────────────────────────

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY,
                                  float partialTick, float animationProgress) {
        int centerX = width / 2;
        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int listX = (width - listWidth) / 2;

        // Title
        int titleColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, animationProgress);
        g.drawCenteredString(font, getTitle(), centerX, 10, titleColor);

        // Back button
        renderBackButton(g, mouseX, mouseY, animationProgress);

        // Tab bar
        int tabY = 26;
        renderTabs(g, listX, tabY, listWidth, mouseX, mouseY, animationProgress);

        // Search box
        int searchY = 56;
        searchBox.setX(listX);
        searchBox.setY(searchY);
        searchBox.setWidth(listWidth);
        searchBox.render(g, mouseX, mouseY, partialTick);

        // List
        int listTop = searchY + SEARCH_BOX_HEIGHT + 6;
        int listBottom = height - 10;
        pendingTooltip = null;
        renderList(g, listX, listTop, listWidth, listBottom, mouseX, mouseY, animationProgress);

        // Deferred tooltip rendering (outside scissor)
        if (pendingTooltip != null) {
            renderTooltip(g, pendingTooltip, pendingTooltipX, pendingTooltipY, animationProgress);
        }

        // Name popup overlay
        if (showNamePopup) {
            renderNamePopup(g, mouseX, mouseY, partialTick, animationProgress);
        }
    }

    private void renderTooltip(GuiGraphics g, Component text, int x, int y, float ap) {
        int tw = font.width(text) + 8;
        int th = 14;
        // Ensure tooltip stays on screen
        int tx = Math.max(2, Math.min(x, width - tw - 2));
        int ty = y - th - 2;
        if (ty < 2) ty = y + 16;

        g.fill(tx, ty, tx + tw, ty + th, MenuRenderUtils.withAlpha(0xF0181820, ap));
        MenuRenderUtils.renderOutline(g, tx, ty, tw, th, MenuRenderUtils.withAlpha(0x889A7CFF, ap));
        g.drawString(font, text, tx + 4, ty + 3, MenuRenderUtils.withAlpha(0xFFDDDDDD, ap));
    }

    private void renderBackButton(GuiGraphics g, int mouseX, int mouseY, float ap) {
        float appear = Math.max(0.0f, (ap - 0.2f) / 0.8f);
        if (appear <= 0.0f) return;

        int bx = BACK_BUTTON_PADDING;
        int by = BACK_BUTTON_PADDING;
        int bSize = BACK_BUTTON_SIZE + 8;

        isHoveringBackButton = !showNamePopup
                && mouseX >= bx && mouseX < bx + bSize
                && mouseY >= by && mouseY < by + bSize;

        if (isHoveringBackButton) {
            int bgColor = MenuRenderUtils.withAlpha(0x809A7CFF, appear);
            g.fill(bx, by, bx + bSize, by + bSize, bgColor);
            MenuRenderUtils.renderOutline(g, bx, by, bSize, bSize,
                    MenuRenderUtils.withAlpha(0x88FFFFFF, appear));
        }

        float scale = isHoveringBackButton ? 1.1f : 1.0f;
        int iconSize = (int) (BACK_BUTTON_SIZE * scale * appear);
        int iconOffset = (bSize - iconSize) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON_BACK,
                bx + iconOffset, by + iconOffset,
                0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
    }

    private void renderTabs(GuiGraphics g, int listX, int tabY, int listWidth,
                            int mouseX, int mouseY, float ap) {
        hoveredTabIndex = -1;
        Tab[] tabs = Tab.values();
        String[] labels = {
                Component.translatable("history.aromaaffect.tab.history").getString(),
                Component.translatable("history.aromaaffect.tab.saved").getString(),
                Component.translatable("history.aromaaffect.tab.blacklist").getString()
        };
        int[] colors = { TAB_HISTORY_COLOR, TAB_SAVED_COLOR, TAB_BLACKLIST_COLOR };

        int totalGaps = (tabs.length - 1) * TAB_GAP;
        int tabWidth = (listWidth - totalGaps) / tabs.length;

        for (int i = 0; i < tabs.length; i++) {
            int tx = listX + i * (tabWidth + TAB_GAP);
            boolean active = tabs[i] == activeTab;
            boolean hovered = !showNamePopup
                    && mouseX >= tx && mouseX < tx + tabWidth
                    && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT;
            if (hovered) hoveredTabIndex = i;

            int bgColor;
            if (active) {
                bgColor = MenuRenderUtils.withAlpha(colors[i], ap);
            } else if (hovered) {
                bgColor = MenuRenderUtils.withAlpha(0x80888888, ap);
            } else {
                bgColor = MenuRenderUtils.withAlpha(0x60444444, ap);
            }
            g.fill(tx, tabY, tx + tabWidth, tabY + TAB_HEIGHT, bgColor);

            if (active) {
                MenuRenderUtils.renderOutline(g, tx, tabY, tabWidth, TAB_HEIGHT,
                        MenuRenderUtils.withAlpha(0xAAFFFFFF, ap));
            }

            int textColor = active
                    ? MenuRenderUtils.withAlpha(0xFFFFFFFF, ap)
                    : MenuRenderUtils.withAlpha(0xFFAAAAAA, ap);
            g.drawCenteredString(font, labels[i],
                    tx + tabWidth / 2, tabY + (TAB_HEIGHT - 8) / 2, textColor);
        }
    }

    private void renderList(GuiGraphics g, int listX, int listTop, int listWidth,
                            int listBottom, int mouseX, int mouseY, float ap) {
        hoveredRowIndex = -1;
        hoveredActionIndex = -1;

        if (filteredIndices.isEmpty()) {
            int textColor = MenuRenderUtils.withAlpha(0xFFAAAAAA, ap);
            Component emptyMsg = switch (activeTab) {
                case HISTORY -> Component.translatable("history.aromaaffect.empty.history");
                case SAVED -> Component.translatable("history.aromaaffect.empty.saved");
                case BLACKLIST -> Component.translatable("history.aromaaffect.empty.blacklist");
            };
            g.drawCenteredString(font, emptyMsg, width / 2, listTop + 20, textColor);
            return;
        }

        g.enableScissor(listX, listTop, listX + listWidth, listBottom);

        for (int i = 0; i < filteredIndices.size(); i++) {
            int rowY = listTop + i * (ROW_HEIGHT + ROW_PADDING) - listScrollOffset;
            if (rowY + ROW_HEIGHT < listTop || rowY > listBottom) continue;

            boolean hovered = !showNamePopup
                    && mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
                    && mouseY >= listTop && mouseY < listBottom;
            if (hovered) hoveredRowIndex = i;

            int rowBg = hovered ? MenuRenderUtils.withAlpha(ROW_HOVER_COLOR, ap)
                    : MenuRenderUtils.withAlpha(ROW_COLOR, ap);
            g.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT, rowBg);

            int dataIndex = filteredIndices.get(i);

            switch (activeTab) {
                case HISTORY -> renderHistoryRow(g, dataIndex, listX, rowY, listWidth, hovered, ap, mouseX, mouseY, i);
                case SAVED -> renderSavedRow(g, dataIndex, listX, rowY, listWidth, hovered, ap, mouseX, mouseY, i);
                case BLACKLIST -> renderBlacklistRow(g, dataIndex, listX, rowY, listWidth, hovered, ap, mouseX, mouseY, i);
            }
        }

        g.disableScissor();
    }

    // ── History Row ──────────────────────────────────────────────────────

    private void renderHistoryRow(GuiGraphics g, int dataIndex, int x, int y, int w,
                                  boolean hovered, float ap, int mouseX, int mouseY, int filteredIdx) {
        TrackingHistoryData data = TrackingHistoryData.getInstance();
        HistoryEntry entry = data.getHistory().get(dataIndex);

        boolean isSaved = data.isSaved(entry.targetId, entry.x, entry.y, entry.z);
        boolean isBlacklisted = data.isBlacklisted(entry.targetId, entry.x, entry.y, entry.z);

        // Icon: category header as main, specific item as badge overlay
        int iconX = x + 4;
        int iconY = y + (ROW_HEIGHT - 16) / 2;
        ItemStack icon = getItemForTarget(entry.targetId, entry.categoryId);
        renderEntryIcon(g, iconX, iconY, icon, entry.categoryId, ap);

        // Name
        int textX = x + 28;
        g.drawString(font, entry.displayName, textX, y + 4,
                MenuRenderUtils.withAlpha(0xFFFFFFFF, ap));

        // Category badge + coords
        int badgeColor = getCategoryBadgeColor(entry.categoryId);
        String catLabel = MenuRenderUtils.capitalizeWords(entry.categoryId);
        g.drawString(font, catLabel, textX, y + 15,
                MenuRenderUtils.withAlpha(badgeColor, ap));

        String coords = String.format("(%d, %d, %d)", entry.x, entry.y, entry.z);
        int coordsX = textX + font.width(catLabel) + 4;
        g.drawString(font, coords, coordsX, y + 15,
                MenuRenderUtils.withAlpha(0xFF888888, ap));

        // Dimension label after coords
        String dimLabel = formatDimension(entry.dimension);
        int dimColor = getDimensionColor(entry.dimension);
        int dimX = coordsX + font.width(coords) + 4;
        g.drawString(font, dimLabel, dimX, y + 15,
                MenuRenderUtils.withAlpha(dimColor, ap));

        // Status badges after dimension
        int statusX = dimX + font.width(dimLabel) + 4;
        if (isSaved) {
            statusX = renderBadge(g, statusX, y + 14, "SAVED", 0xFFFFCC44, 0x60FFCC44, ap);
        }
        if (isBlacklisted) {
            renderBadge(g, statusX, y + 14, "BLOCKED", 0xFFFF6B6B, 0x60FF4444, ap);
        }

        // Time ago
        g.drawString(font, formatTimeAgo(entry.timestamp), textX, y + 26,
                MenuRenderUtils.withAlpha(0xFF666666, ap));

        // Action buttons when hovered
        if (hovered) {
            // Cross-dimension check: disable Go button if player is in wrong dimension
            boolean wrongDimension = entry.dimension != null
                    && !entry.dimension.equals(getCurrentDimension());

            // Go button (far right, with zigzag separator)
            int goRight = x + w;
            renderGoButton(g, goRight - GO_BTN_W, y, GO_BTN_W, ROW_HEIGHT,
                    entry.targetId, entry.categoryId, mouseX, mouseY, ap, filteredIdx, 2, wrongDimension);
            renderZigzagSeparator(g, goRight - GO_BTN_W - ZIGZAG_W, y, ZIGZAG_W, ROW_HEIGHT, ap);

            // Icon buttons (left of zigzag): Delete, TP, and conditionally Blacklist/Save
            int btnX = goRight - GO_BTN_W - ZIGZAG_W - 4;
            int btnY = y + (ROW_HEIGHT - ICON_BTN_SIZE) / 2;

            btnX = renderIconButton(g, btnX, btnY, IconType.DELETE, 0xFFFF4444,
                    Component.translatable("history.aromaaffect.action.delete"),
                    mouseX, mouseY, ap, filteredIdx, 4);
            btnX = renderTeleportIconButton(g, btnX, btnY, mouseX, mouseY, ap, filteredIdx, 3);
            if (!isBlacklisted) {
                btnX = renderIconButton(g, btnX, btnY, IconType.BLACKLIST, 0xFFFF6B6B,
                        Component.translatable("history.aromaaffect.action.blacklist"),
                        mouseX, mouseY, ap, filteredIdx, 1);
            }
            if (!isSaved) {
                renderIconButton(g, btnX, btnY, IconType.SAVE, 0xFFFFCC44,
                        Component.translatable("history.aromaaffect.action.save"),
                        mouseX, mouseY, ap, filteredIdx, 0);
            }
        }
    }

    // ── Saved Row ────────────────────────────────────────────────────────

    private void renderSavedRow(GuiGraphics g, int dataIndex, int x, int y, int w,
                                boolean hovered, float ap, int mouseX, int mouseY, int filteredIdx) {
        TrackingHistoryData data = TrackingHistoryData.getInstance();
        SavedEntry entry = data.getSaved().get(dataIndex);
        boolean isBlacklisted = data.isBlacklisted(entry.targetId, entry.x, entry.y, entry.z);

        // Icon: category header as main, specific item as badge overlay
        int iconX = x + 4;
        int iconY = y + (ROW_HEIGHT - 16) / 2;
        ItemStack icon = getItemForTarget(entry.targetId, entry.categoryId);
        renderEntryIcon(g, iconX, iconY, icon, entry.categoryId, ap);

        int textX = x + 28;
        g.drawString(font, entry.customName, textX, y + 4,
                MenuRenderUtils.withAlpha(0xFFFFCC44, ap));

        int badgeColor = getCategoryBadgeColor(entry.categoryId);
        String catLabel = MenuRenderUtils.capitalizeWords(entry.categoryId);
        g.drawString(font, catLabel, textX, y + 15,
                MenuRenderUtils.withAlpha(badgeColor, ap));

        String coords = String.format("(%d, %d, %d)", entry.x, entry.y, entry.z);
        int coordsX = textX + font.width(catLabel) + 4;
        g.drawString(font, coords, coordsX, y + 15,
                MenuRenderUtils.withAlpha(0xFF888888, ap));

        // Dimension label after coords
        String dimLabel = formatDimension(entry.dimension);
        int dimColor = getDimensionColor(entry.dimension);
        int dimX = coordsX + font.width(coords) + 4;
        g.drawString(font, dimLabel, dimX, y + 15,
                MenuRenderUtils.withAlpha(dimColor, ap));

        // Status badge if blacklisted
        if (isBlacklisted) {
            renderBadge(g, dimX + font.width(dimLabel) + 4, y + 14,
                    "BLOCKED", 0xFFFF6B6B, 0x60FF4444, ap);
        }

        if (hovered) {
            boolean wrongDimension = entry.dimension != null
                    && !entry.dimension.equals(getCurrentDimension());

            int goRight = x + w;
            renderGoButton(g, goRight - GO_BTN_W, y, GO_BTN_W, ROW_HEIGHT,
                    entry.targetId, entry.categoryId, mouseX, mouseY, ap, filteredIdx, 0, wrongDimension);
            renderZigzagSeparator(g, goRight - GO_BTN_W - ZIGZAG_W, y, ZIGZAG_W, ROW_HEIGHT, ap);

            int btnX = goRight - GO_BTN_W - ZIGZAG_W - 4;
            int btnY = y + (ROW_HEIGHT - ICON_BTN_SIZE) / 2;

            btnX = renderIconButton(g, btnX, btnY, IconType.DELETE, 0xFFFF4444,
                    Component.translatable("history.aromaaffect.action.delete"),
                    mouseX, mouseY, ap, filteredIdx, 3);
            btnX = renderIconButton(g, btnX, btnY, IconType.RENAME, 0xFF44AAFF,
                    Component.translatable("history.aromaaffect.action.rename"),
                    mouseX, mouseY, ap, filteredIdx, 2);
            renderTeleportIconButton(g, btnX, btnY, mouseX, mouseY, ap, filteredIdx, 1);
        }
    }

    // ── Blacklist Row ────────────────────────────────────────────────────

    private void renderBlacklistRow(GuiGraphics g, int dataIndex, int x, int y, int w,
                                    boolean hovered, float ap, int mouseX, int mouseY, int filteredIdx) {
        TrackingHistoryData data = TrackingHistoryData.getInstance();
        BlacklistEntry entry = data.getBlacklist().get(dataIndex);

        // Icon: category header as main, specific item as badge overlay
        int iconX = x + 4;
        int iconY = y + (ROW_HEIGHT - 16) / 2;
        ItemStack icon = getItemForTarget(entry.targetId, entry.categoryId);
        renderEntryIcon(g, iconX, iconY, icon, entry.categoryId, ap);

        int textX = x + 28;
        String name = entry.displayName != null ? entry.displayName : entry.targetId;
        g.drawString(font, name, textX, y + 4,
                MenuRenderUtils.withAlpha(0xFFFF8888, ap));

        int badgeColor = getCategoryBadgeColor(entry.categoryId);
        String catLabel = MenuRenderUtils.capitalizeWords(entry.categoryId);
        g.drawString(font, catLabel, textX, y + 15,
                MenuRenderUtils.withAlpha(badgeColor, ap));

        String coords = String.format("(%d, %d, %d)", entry.x, entry.y, entry.z);
        int coordsX = textX + font.width(catLabel) + 4;
        g.drawString(font, coords, coordsX, y + 15,
                MenuRenderUtils.withAlpha(0xFF888888, ap));

        // Dimension label after coords
        String dimLabel = formatDimension(entry.dimension);
        int dimColor = getDimensionColor(entry.dimension);
        g.drawString(font, dimLabel, coordsX + font.width(coords) + 4, y + 15,
                MenuRenderUtils.withAlpha(dimColor, ap));

        if (hovered) {
            int btnX = x + w - 4;
            int btnY = y + (ROW_HEIGHT - ICON_BTN_SIZE) / 2;
            renderIconButton(g, btnX, btnY, IconType.UNBLOCK, 0xFF44CC44,
                    Component.translatable("history.aromaaffect.action.unblacklist"),
                    mouseX, mouseY, ap, filteredIdx, 0);
        }
    }

    // ── Go (Re-track) Button ─────────────────────────────────────────────

    private void renderGoButton(GuiGraphics g, int x, int y, int w, int h,
                                String targetId, String categoryId,
                                int mouseX, int mouseY, float ap,
                                int filteredIdx, int actionIdx, boolean forceDisabled) {
        boolean canRetrack = !forceDisabled && canRetrackTarget(targetId, categoryId);

        boolean hov = mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
        if (hov) {
            hoveredRowIndex = filteredIdx;
            hoveredActionIndex = actionIdx;
        }

        // Background — vibrant green when available, muted gray when disabled
        int baseColor = canRetrack ? 0xFF2D8B2D : 0xFF3A3A3A;
        int hoverColor = canRetrack ? 0xFF3DAF3D : 0xFF4A4A4A;
        int bg = MenuRenderUtils.withAlpha(hov ? hoverColor : baseColor, ap);
        g.fill(x, y, x + w, y + h, bg);

        // Bright edge highlight on left when hovered
        if (hov && canRetrack) {
            g.fill(x, y, x + 1, y + h, MenuRenderUtils.withAlpha(0xFF66FF66, ap));
        }

        // Arrow icon ▶ (triangle pointing right)
        int cx = x + w / 2;
        int cy = y + h / 2;
        int arrowColor = canRetrack
                ? MenuRenderUtils.withAlpha(hov ? 0xFFFFFFFF : 0xFFCCFFCC, ap)
                : MenuRenderUtils.withAlpha(0xFF666666, ap);

        // Draw a right-pointing triangle with fill calls (stepped approximation)
        // 8px tall, 6px wide
        int arrowH = 8;
        int arrowW = 6;
        int aTop = cy - arrowH / 2;
        for (int row = 0; row < arrowH; row++) {
            int halfRow = arrowH / 2;
            int dist = halfRow - Math.abs(row - halfRow);
            int pw = Math.max(1, (dist * arrowW) / halfRow);
            int px = cx - 2;
            int py = aTop + row;
            g.fill(px, py, px + pw, py + 1, arrowColor);
        }

        // Retrack cost number below arrow
        if (canRetrack) {
            int retrackCost = TrackingConfig.getInstance().getHistoryRetrackCost();
            String costStr = String.valueOf(retrackCost);
            int costW = font.width(costStr);
            int costColor = MenuRenderUtils.withAlpha(0xFFFFAA00, ap);
            g.drawString(font, costStr, cx - costW / 2, cy + 6, costColor);
        }

        // Tooltip
        if (hov) {
            Component tip;
            if (forceDisabled) {
                tip = Component.translatable("history.aromaaffect.wrong_dimension");
            } else if (canRetrack) {
                int retrackCost = TrackingConfig.getInstance().getHistoryRetrackCost();
                tip = Component.translatable("history.aromaaffect.action.retrack.cost", retrackCost);
            } else {
                tip = Component.translatable("history.aromaaffect.retrack.disabled");
            }
            pendingTooltip = tip;
            pendingTooltipX = mouseX;
            pendingTooltipY = mouseY;
        }
    }

    // ── Zigzag Torn-Edge Separator ───────────────────────────────────────

    private void renderZigzagSeparator(GuiGraphics g, int x, int y, int w, int h, float ap) {
        // Draws a vertical zigzag torn-paper edge
        // Left half: row background bleeds through, right half: Go button color bleeds
        // Creates triangular teeth alternating left/right

        int toothH = 4; // height of each zigzag tooth
        int teethCount = h / toothH;

        int colorA = MenuRenderUtils.withAlpha(0xCC1A1A2E, ap); // dark "shadow" on tear
        int colorB = MenuRenderUtils.withAlpha(0x882D8B2D, ap); // green tint from Go side
        int edgeColor = MenuRenderUtils.withAlpha(0x66AAAAAA, ap); // subtle edge highlight

        for (int i = 0; i < teethCount; i++) {
            int ty = y + i * toothH;
            boolean pointsRight = (i % 2 == 0);

            // Each tooth: a triangle approximated by 1px-tall rows narrowing to a point
            for (int row = 0; row < toothH; row++) {
                int progress = pointsRight ? row : (toothH - 1 - row);
                int indent = (progress * w) / toothH;

                if (pointsRight) {
                    // Triangle pointing right: left side is dark, right grows
                    g.fill(x, ty + row, x + indent, ty + row + 1, colorA);
                    g.fill(x + indent, ty + row, x + w, ty + row + 1, colorB);
                } else {
                    // Triangle pointing left
                    g.fill(x, ty + row, x + (w - indent), ty + row + 1, colorB);
                    g.fill(x + (w - indent), ty + row, x + w, ty + row + 1, colorA);
                }
            }
        }

        // Subtle edge line along the zigzag path
        for (int i = 0; i < teethCount; i++) {
            int ty = y + i * toothH;
            boolean pointsRight = (i % 2 == 0);
            for (int row = 0; row < toothH; row++) {
                int progress = pointsRight ? row : (toothH - 1 - row);
                int edgeX = x + (progress * w) / toothH;
                g.fill(edgeX, ty + row, edgeX + 1, ty + row + 1, edgeColor);
            }
        }
    }

    // ── Icon Button Types ────────────────────────────────────────────────

    private enum IconType { SAVE, BLACKLIST, DELETE, RENAME, TELEPORT, UNBLOCK }

    /**
     * Renders a square icon button with a procedural icon and hover tooltip.
     * Returns the X position for the next button to the left.
     */
    private int renderIconButton(GuiGraphics g, int rightEdge, int y,
                                 IconType type, int accentColor, Component tooltip,
                                 int mouseX, int mouseY, float ap,
                                 int filteredIdx, int actionIdx) {
        int bx = rightEdge - ICON_BTN_SIZE;
        boolean hov = mouseX >= bx && mouseX < bx + ICON_BTN_SIZE
                && mouseY >= y && mouseY < y + ICON_BTN_SIZE;

        if (hov) {
            hoveredRowIndex = filteredIdx;
            hoveredActionIndex = actionIdx;
        }

        // Background
        int bg = hov
                ? MenuRenderUtils.withAlpha(accentColor, ap * 0.6f)
                : MenuRenderUtils.withAlpha(0xFF333333, ap * 0.7f);
        g.fill(bx, y, bx + ICON_BTN_SIZE, y + ICON_BTN_SIZE, bg);

        // Border (accent on hover, subtle otherwise)
        int border = hov
                ? MenuRenderUtils.withAlpha(accentColor, ap * 0.9f)
                : MenuRenderUtils.withAlpha(0xFF555555, ap * 0.5f);
        MenuRenderUtils.renderOutline(g, bx, y, ICON_BTN_SIZE, ICON_BTN_SIZE, border);

        // Draw procedural icon
        int iconColor = hov
                ? MenuRenderUtils.withAlpha(0xFFFFFFFF, ap)
                : MenuRenderUtils.withAlpha(accentColor, ap * 0.9f);
        int cx = bx + ICON_BTN_SIZE / 2;
        int cy = y + ICON_BTN_SIZE / 2;

        switch (type) {
            case SAVE -> drawStarIcon(g, cx, cy, iconColor);
            case BLACKLIST -> drawBanIcon(g, cx, cy, iconColor);
            case DELETE -> drawTrashIcon(g, cx, cy, iconColor);
            case RENAME -> drawPencilIcon(g, cx, cy, iconColor);
            case TELEPORT -> drawLightningIcon(g, cx, cy, iconColor);
            case UNBLOCK -> drawCheckIcon(g, cx, cy, iconColor);
        }

        // Tooltip on hover
        if (hov) {
            pendingTooltip = tooltip;
            pendingTooltipX = mouseX;
            pendingTooltipY = mouseY;
        }

        return bx - ICON_BTN_GAP;
    }

    private int renderTeleportIconButton(GuiGraphics g, int rightEdge, int y,
                                         int mouseX, int mouseY, float ap,
                                         int filteredIdx, int actionIdx) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isCreative()) {
            return rightEdge;
        }
        return renderIconButton(g, rightEdge, y, IconType.TELEPORT, 0xFF44AAFF,
                Component.translatable("history.aromaaffect.action.teleport"),
                mouseX, mouseY, ap, filteredIdx, actionIdx);
    }

    // ── Procedural Icon Drawing ──────────────────────────────────────────

    /** Star (bookmark/save): 5-pointed star outline */
    private static void drawStarIcon(GuiGraphics g, int cx, int cy, int color) {
        // Simple star: center dot + 4 points
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);  // center
        g.fill(cx, cy - 4, cx + 1, cy - 1, color);        // top spike
        g.fill(cx, cy + 2, cx + 1, cy + 5, color);        // bottom spike
        g.fill(cx - 4, cy, cx - 1, cy + 1, color);        // left spike
        g.fill(cx + 2, cy, cx + 5, cy + 1, color);        // right spike
        // Diagonal accents
        g.fill(cx - 2, cy - 2, cx - 1, cy - 1, color);
        g.fill(cx + 2, cy - 2, cx + 3, cy - 1, color);
        g.fill(cx - 2, cy + 2, cx - 1, cy + 3, color);
        g.fill(cx + 2, cy + 2, cx + 3, cy + 3, color);
    }

    /** Ban/blacklist: circle with diagonal slash */
    private static void drawBanIcon(GuiGraphics g, int cx, int cy, int color) {
        // Circle approximation (octagon-ish)
        g.fill(cx - 2, cy - 4, cx + 3, cy - 3, color); // top
        g.fill(cx - 2, cy + 4, cx + 3, cy + 5, color); // bottom
        g.fill(cx - 4, cy - 2, cx - 3, cy + 3, color); // left
        g.fill(cx + 4, cy - 2, cx + 5, cy + 3, color); // right
        // Corner fills
        g.fill(cx - 3, cy - 3, cx - 2, cy - 2, color);
        g.fill(cx + 3, cy - 3, cx + 4, cy - 2, color);
        g.fill(cx - 3, cy + 3, cx - 2, cy + 4, color);
        g.fill(cx + 3, cy + 3, cx + 4, cy + 4, color);
        // Diagonal slash
        g.fill(cx - 3, cy - 2, cx - 2, cy - 1, color);
        g.fill(cx - 2, cy - 1, cx - 1, cy, color);
        g.fill(cx - 1, cy, cx, cy + 1, color);
        g.fill(cx, cy + 1, cx + 1, cy + 2, color);
        g.fill(cx + 1, cy + 2, cx + 2, cy + 3, color);
        g.fill(cx + 2, cy + 3, cx + 3, cy + 4, color);
    }

    /** Trash can icon */
    private static void drawTrashIcon(GuiGraphics g, int cx, int cy, int color) {
        // Lid
        g.fill(cx - 3, cy - 4, cx + 4, cy - 3, color);
        g.fill(cx - 1, cy - 5, cx + 2, cy - 4, color); // handle
        // Body
        g.fill(cx - 3, cy - 2, cx - 2, cy + 4, color); // left wall
        g.fill(cx + 3, cy - 2, cx + 4, cy + 4, color);  // right wall
        g.fill(cx - 3, cy + 4, cx + 4, cy + 5, color);  // bottom
        // Inner lines
        g.fill(cx - 1, cy - 1, cx, cy + 3, color);
        g.fill(cx + 1, cy - 1, cx + 2, cy + 3, color);
    }

    /** Pencil/edit icon */
    private static void drawPencilIcon(GuiGraphics g, int cx, int cy, int color) {
        // Pencil body (diagonal)
        g.fill(cx - 3, cy + 2, cx - 2, cy + 4, color);   // tip
        g.fill(cx - 2, cy + 1, cx - 1, cy + 3, color);
        g.fill(cx - 1, cy, cx, cy + 2, color);
        g.fill(cx, cy - 1, cx + 1, cy + 1, color);
        g.fill(cx + 1, cy - 2, cx + 2, cy, color);
        g.fill(cx + 2, cy - 3, cx + 3, cy - 1, color);
        g.fill(cx + 3, cy - 4, cx + 4, cy - 2, color);   // eraser end
        // Tip accent
        g.fill(cx - 4, cy + 3, cx - 3, cy + 5, color);
    }

    /** Lightning bolt (teleport) */
    private static void drawLightningIcon(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx, cy - 5, cx + 3, cy - 4, color);
        g.fill(cx - 1, cy - 4, cx + 2, cy - 3, color);
        g.fill(cx - 1, cy - 3, cx + 1, cy - 2, color);
        g.fill(cx - 2, cy - 2, cx + 2, cy - 1, color);  // wide part
        g.fill(cx - 1, cy - 1, cx + 1, cy, color);
        g.fill(cx - 1, cy, cx, cy + 1, color);
        g.fill(cx - 2, cy + 1, cx, cy + 2, color);
        g.fill(cx - 3, cy + 2, cx - 1, cy + 3, color);
        g.fill(cx - 3, cy + 3, cx - 2, cy + 5, color);
    }

    /** Checkmark (un-blacklist) */
    private static void drawCheckIcon(GuiGraphics g, int cx, int cy, int color) {
        // Short leg (going down-right)
        g.fill(cx - 3, cy, cx - 2, cy + 1, color);
        g.fill(cx - 2, cy + 1, cx - 1, cy + 2, color);
        g.fill(cx - 1, cy + 2, cx, cy + 3, color);
        // Long leg (going up-right)
        g.fill(cx, cy + 1, cx + 1, cy + 2, color);
        g.fill(cx + 1, cy, cx + 2, cy + 1, color);
        g.fill(cx + 2, cy - 1, cx + 3, cy, color);
        g.fill(cx + 3, cy - 2, cx + 4, cy - 1, color);
        g.fill(cx + 4, cy - 3, cx + 5, cy - 2, color);
    }

    // ── Status Badge ─────────────────────────────────────────────────────

    /**
     * Renders the entry icon: category header texture as the main 16x16 icon,
     * with the specific target item as a small overlay badge at the bottom-right.
     */
    private void renderEntryIcon(GuiGraphics g, int iconX, int iconY,
                                  ItemStack specificIcon, String categoryId, float ap) {
        // Big icon: category header texture at 16x16
        MenuCategory cat = MenuCategory.fromId(categoryId);
        if (cat != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, cat.getHeaderIcon(),
                    iconX, iconY, 0.0f, 0.0f, 16, 16, 16, 16);
        }

        // Small badge: specific item at bottom-right
        if (specificIcon != null && !specificIcon.isEmpty()) {
            int badgeSize = 10;
            int pad = 1;
            int bx = iconX + 16 - badgeSize + 3;
            int by = iconY + 16 - badgeSize + 3;

            // Dark background
            g.fill(bx - pad, by - pad, bx + badgeSize + pad, by + badgeSize + pad,
                    MenuRenderUtils.withAlpha(0xEE111122, ap));

            // Colored border matching category accent
            int borderColor = MenuRenderUtils.withAlpha(getCategoryBadgeColor(categoryId), ap * 0.7f);
            MenuRenderUtils.renderOutline(g, bx - pad, by - pad,
                    badgeSize + pad * 2, badgeSize + pad * 2, borderColor);

            // Render the specific item small, centered within the badge
            g.pose().pushMatrix();
            float itemSize = badgeSize - 2;
            float offset = (badgeSize - itemSize) / 2.0f;
            g.pose().translate(bx + offset, by + offset);
            float scale = itemSize / 16.0f;
            g.pose().scale(scale, scale);
            g.renderItem(specificIcon, 0, 0);
            g.pose().popMatrix();
        }
    }

    /** Renders a small pill-shaped badge. Returns the X position after the badge for chaining. */
    private int renderBadge(GuiGraphics g, int x, int y, String label, int textColor, int bgColor, float ap) {
        int tw = font.width(label);
        int pillW = tw + 6;
        int pillH = 10;
        g.fill(x, y, x + pillW, y + pillH, MenuRenderUtils.withAlpha(bgColor, ap));
        MenuRenderUtils.renderOutline(g, x, y, pillW, pillH,
                MenuRenderUtils.withAlpha(textColor, ap * 0.4f));
        g.drawString(font, label, x + 3, y + 1,
                MenuRenderUtils.withAlpha(textColor, ap));
        return x + pillW + 3;
    }

    // ── Name Popup ───────────────────────────────────────────────────────

    private void renderNamePopup(GuiGraphics g, int mouseX, int mouseY,
                                 float partialTick, float ap) {
        g.fill(0, 0, width, height, MenuRenderUtils.withAlpha(0xCC000000, ap));

        int popupW = 220;
        int popupH = 80;
        int px = (width - popupW) / 2;
        int py = (height - popupH) / 2;

        g.fill(px, py, px + popupW, py + popupH, MenuRenderUtils.withAlpha(0xEE1A1A2E, ap));
        MenuRenderUtils.renderOutline(g, px, py, popupW, popupH,
                MenuRenderUtils.withAlpha(0xAA9A7CFF, ap));

        Component popupTitle = namePopupIsRename
                ? Component.translatable("history.aromaaffect.popup.rename")
                : Component.translatable("history.aromaaffect.popup.save");
        g.drawCenteredString(font, popupTitle, px + popupW / 2, py + 6,
                MenuRenderUtils.withAlpha(0xFFFFFFFF, ap));

        if (nameEditBox != null) {
            nameEditBox.setX(px + 10);
            nameEditBox.setY(py + 22);
            nameEditBox.setWidth(popupW - 20);
            nameEditBox.render(g, mouseX, mouseY, partialTick);
        }

        int confirmX = px + 10;
        int confirmY = py + popupH - 22;
        int confirmW = (popupW - 30) / 2;
        int confirmH = 16;
        boolean hovConfirm = mouseX >= confirmX && mouseX < confirmX + confirmW
                && mouseY >= confirmY && mouseY < confirmY + confirmH;
        g.fill(confirmX, confirmY, confirmX + confirmW, confirmY + confirmH,
                hovConfirm ? MenuRenderUtils.withAlpha(0xCC44CC44, ap)
                        : MenuRenderUtils.withAlpha(0x8833AA33, ap));
        MenuRenderUtils.renderOutline(g, confirmX, confirmY, confirmW, confirmH,
                MenuRenderUtils.withAlpha(0x8844FF44, ap));
        g.drawCenteredString(font, Component.translatable("history.aromaaffect.popup.confirm"),
                confirmX + confirmW / 2, confirmY + 4,
                MenuRenderUtils.withAlpha(0xFFFFFFFF, ap));

        int cancelX = px + popupW - 10 - confirmW;
        boolean hovCancel = mouseX >= cancelX && mouseX < cancelX + confirmW
                && mouseY >= confirmY && mouseY < confirmY + confirmH;
        g.fill(cancelX, confirmY, cancelX + confirmW, confirmY + confirmH,
                hovCancel ? MenuRenderUtils.withAlpha(0xCCCC4444, ap)
                        : MenuRenderUtils.withAlpha(0x80AA3333, ap));
        MenuRenderUtils.renderOutline(g, cancelX, confirmY, confirmW, confirmH,
                MenuRenderUtils.withAlpha(0x88FF4444, ap));
        g.drawCenteredString(font, Component.translatable("history.aromaaffect.popup.cancel"),
                cancelX + confirmW / 2, confirmY + 4,
                MenuRenderUtils.withAlpha(0xFFFFFFFF, ap));
    }

    // ── Input Handling ───────────────────────────────────────────────────

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (showNamePopup) {
            return handleNamePopupClick((int) mouseX, (int) mouseY);
        }

        if (isHoveringBackButton) {
            MenuRenderUtils.playClickSound();
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (hoveredTabIndex >= 0 && hoveredTabIndex < Tab.values().length) {
            Tab newTab = Tab.values()[hoveredTabIndex];
            if (newTab != activeTab) {
                activeTab = newTab;
                searchQuery = "";
                if (searchBox != null) searchBox.setValue("");
                rebuildFilteredList();
                MenuRenderUtils.playClickSound();
            }
            return true;
        }

        if (hoveredRowIndex >= 0 && hoveredActionIndex >= 0) {
            handleActionClick(hoveredRowIndex, hoveredActionIndex);
            return true;
        }

        return false;
    }

    @Override
    protected boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showNamePopup) return true;
        int maxScroll = Math.max(0, filteredIndices.size() * (ROW_HEIGHT + ROW_PADDING) - (height - 92));
        listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset - (int) (scrollY * 20)));
        return true;
    }

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (showNamePopup) {
                closeNamePopup();
                return true;
            }
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (showNamePopup && keyCode == 257) {
            confirmNamePopup();
            return true;
        }

        return false;
    }

    // ── Action Handling ──────────────────────────────────────────────────

    private void handleActionClick(int filteredIdx, int actionIdx) {
        if (filteredIdx < 0 || filteredIdx >= filteredIndices.size()) return;
        int dataIndex = filteredIndices.get(filteredIdx);
        TrackingHistoryData data = TrackingHistoryData.getInstance();

        switch (activeTab) {
            case HISTORY -> handleHistoryAction(data, dataIndex, actionIdx);
            case SAVED -> handleSavedAction(data, dataIndex, actionIdx);
            case BLACKLIST -> handleBlacklistAction(data, dataIndex, actionIdx);
        }
    }

    private void handleHistoryAction(TrackingHistoryData data, int dataIndex, int actionIdx) {
        // Actions: 0=Save, 1=Blacklist, 2=Re-track, 3=Teleport, 4=Delete
        if (dataIndex >= data.getHistory().size()) return;
        HistoryEntry entry = data.getHistory().get(dataIndex);

        switch (actionIdx) {
            case 0 -> {
                if (data.isSaved(entry.targetId, entry.x, entry.y, entry.z)) return;
                MenuRenderUtils.playClickSound();
                openNamePopup(dataIndex, false, entry.displayName);
            }
            case 1 -> {
                if (data.isBlacklisted(entry.targetId, entry.x, entry.y, entry.z)) return;
                MenuRenderUtils.playClickSound();
                data.addToBlacklist(entry);
                rebuildFilteredList();
            }
            case 2 -> {
                if (canRetrackTarget(entry.targetId, entry.categoryId)) {
                    MenuRenderUtils.playClickSound();
                    executeRetrack(entry.targetId, entry.categoryId, entry.displayName,
                            entry.x, entry.y, entry.z, entry.dimension);
                } else {
                    MenuRenderUtils.playSound(SoundEvents.VILLAGER_NO, 0.5f, 1.2f);
                }
            }
            case 3 -> executeTeleport(entry.x, entry.y, entry.z);
            case 4 -> {
                MenuRenderUtils.playClickSound();
                data.removeHistoryEntry(dataIndex);
                rebuildFilteredList();
            }
        }
    }

    private void handleSavedAction(TrackingHistoryData data, int dataIndex, int actionIdx) {
        // Actions: 0=Re-track, 1=Teleport, 2=Rename, 3=Delete
        if (dataIndex >= data.getSaved().size()) return;
        SavedEntry entry = data.getSaved().get(dataIndex);

        switch (actionIdx) {
            case 0 -> {
                if (canRetrackTarget(entry.targetId, entry.categoryId)) {
                    MenuRenderUtils.playClickSound();
                    executeRetrack(entry.targetId, entry.categoryId, entry.customName,
                            entry.x, entry.y, entry.z, entry.dimension);
                } else {
                    MenuRenderUtils.playSound(SoundEvents.VILLAGER_NO, 0.5f, 1.2f);
                }
            }
            case 1 -> executeTeleport(entry.x, entry.y, entry.z);
            case 2 -> {
                MenuRenderUtils.playClickSound();
                openNamePopup(dataIndex, true, entry.customName);
            }
            case 3 -> {
                MenuRenderUtils.playClickSound();
                data.removeSavedEntry(dataIndex);
                rebuildFilteredList();
            }
        }
    }

    private void handleBlacklistAction(TrackingHistoryData data, int dataIndex, int actionIdx) {
        if (actionIdx == 0 && dataIndex < data.getBlacklist().size()) {
            MenuRenderUtils.playClickSound();
            data.removeFromBlacklist(dataIndex);
            rebuildFilteredList();
        }
    }

    // ── Re-track ─────────────────────────────────────────────────────────

    private boolean canRetrackTarget(String targetId, String categoryId) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;

        return switch (categoryId) {
            case "blocks" -> EquippedNoseHelper.canDetectBlock(player, targetId);
            case "biomes" -> EquippedNoseHelper.canDetectBiome(player, targetId);
            case "structures" -> EquippedNoseHelper.canDetectStructure(player, targetId);
            case "flowers" -> EquippedNoseHelper.canDetectFlower(player, targetId);
            default -> false;
        };
    }

    private void executeRetrack(String targetId, String categoryId, String displayName,
                                int x, int y, int z, String dimension) {
        MenuCategory cat = MenuCategory.fromId(categoryId);
        if (cat == null) return;

        var player = Minecraft.getInstance().player;

        // Check if passive mode is active - cannot use active tracking while passive mode is enabled
        if (PassiveModeManager.isPassiveModeEnabled()) {
            showErrorNotification(Component.translatable("message.aromaaffect.tracking.passive_mode_active"));
            if (player != null) {
                player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
            }
            AromaAffect.LOGGER.info("Cannot start tracking while passive mode is active");
            return;
        }

        // Block re-tracking from wrong dimension
        if (player != null && dimension != null && !dimension.equals(getCurrentDimension())) {
            player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
            AromaAffect.LOGGER.info("Cannot re-track from wrong dimension: entry is in {}, player is in {}",
                    dimension, getCurrentDimension());
            return;
        }

        // Pre-validate durability for recall cost
        if (player != null) {
            int retrackCost = TrackingConfig.getInstance().getHistoryRetrackCost();
            ItemStack headStack = com.ovrtechnology.nose.accessory.NoseAccessory.getEquipped(player);
            if (!headStack.isEmpty() && headStack.isDamageableItem()) {
                int remaining = headStack.getMaxDamage() - headStack.getDamageValue();
                if (remaining < retrackCost) {
                    player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
                    AromaAffect.LOGGER.info("Not enough nose durability for recall: need {}, have {}",
                            retrackCost, remaining);
                    return;
                }
            }
        }

        ResourceLocation targetLoc = ResourceLocation.parse(targetId);
        ItemStack icon = getItemForTarget(targetId, categoryId);
        ActiveTrackingState.set(targetLoc, Component.literal(displayName), icon, cat);

        // Use recall command to go directly to known coordinates (no search needed)
        String dimArg = dimension != null ? dimension : "minecraft:overworld";
        String command = String.format("aromatest path recall %s %d %d %d %s", targetId, x, y, z, dimArg);
        AromaAffect.LOGGER.debug("Re-tracking via recall: {}", command);

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand(command);
        }

        MenuManager.returnToRadialMenu();
    }

    // ── Teleport ─────────────────────────────────────────────────────────

    private void executeTeleport(int x, int y, int z) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isCreative()) return;

        MenuRenderUtils.playClickSound();

        if (Minecraft.getInstance().getConnection() != null) {
            String cmd = String.format("tp @s %d %d %d", x, y + 1, z);
            Minecraft.getInstance().getConnection().sendCommand(cmd);
            AromaAffect.LOGGER.debug("Teleporting to ({}, {}, {})", x, y, z);
        }

        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    // ── Name Popup Logic ─────────────────────────────────────────────────

    private void openNamePopup(int dataIndex, boolean isRename, String prefill) {
        showNamePopup = true;
        namePopupSourceIndex = dataIndex;
        namePopupIsRename = isRename;

        int popupW = 220;
        int px = (width - popupW) / 2;
        int py = (height - 80) / 2;

        nameEditBox = new EditBox(font, px + 10, py + 22, popupW - 20, 18,
                Component.literal("Name"));
        nameEditBox.setMaxLength(64);
        nameEditBox.setValue(prefill != null ? prefill : "");
        nameEditBox.setFocused(true);
        addWidget(nameEditBox);
        this.setFocused(nameEditBox);
    }

    private void closeNamePopup() {
        showNamePopup = false;
        if (nameEditBox != null) {
            removeWidget(nameEditBox);
            nameEditBox = null;
        }
        namePopupSourceIndex = -1;
        this.setFocused(searchBox);
    }

    private void confirmNamePopup() {
        if (nameEditBox == null) return;
        String name = nameEditBox.getValue().trim();
        if (name.isEmpty()) {
            closeNamePopup();
            return;
        }

        TrackingHistoryData data = TrackingHistoryData.getInstance();

        if (namePopupIsRename) {
            data.renameSavedEntry(namePopupSourceIndex, name);
        } else {
            if (namePopupSourceIndex >= 0 && namePopupSourceIndex < data.getHistory().size()) {
                data.saveEntry(data.getHistory().get(namePopupSourceIndex), name);
            }
        }

        closeNamePopup();
        rebuildFilteredList();
    }

    private boolean handleNamePopupClick(int mouseX, int mouseY) {
        int popupW = 220;
        int popupH = 80;
        int px = (width - popupW) / 2;
        int py = (height - popupH) / 2;

        int confirmX = px + 10;
        int confirmY = py + popupH - 22;
        int btnW = (popupW - 30) / 2;
        int btnH = 16;

        // Confirm button
        if (mouseX >= confirmX && mouseX < confirmX + btnW
                && mouseY >= confirmY && mouseY < confirmY + btnH) {
            MenuRenderUtils.playClickSound();
            confirmNamePopup();
            return true;
        }

        // Cancel button
        int cancelX = px + popupW - 10 - btnW;
        if (mouseX >= cancelX && mouseX < cancelX + btnW
                && mouseY >= confirmY && mouseY < confirmY + btnH) {
            MenuRenderUtils.playClickSound();
            closeNamePopup();
            return true;
        }

        // Clicks inside popup: let them propagate so EditBox receives them
        if (mouseX >= px && mouseX < px + popupW && mouseY >= py && mouseY < py + popupH) {
            return false;
        }

        // Clicks outside popup: consume to prevent clicking through
        return true;
    }

    // ── Utilities ────────────────────────────────────────────────────────

    /**
     * Resolves an item icon for the given target using the same icon maps
     * as the selection menus. Falls back to registry lookups and finally
     * the category's default icon.
     */
    private ItemStack getItemForTarget(String targetId, String categoryId) {
        // Check category-specific maps first (most accurate icons)
        switch (categoryId) {
            case "structures" -> {
                var info = StructuresMenuScreen.STRUCTURE_INFO.get(targetId);
                if (info != null) return info.icon().copy();
            }
            case "biomes" -> {
                ItemStack biomeIcon = BiomesMenuScreen.BIOME_ICONS.get(targetId);
                if (biomeIcon != null) return biomeIcon.copy();
            }
            case "flowers" -> {
                ItemStack flowerIcon = FlowersMenuScreen.FLOWER_ICONS.get(targetId);
                if (flowerIcon != null) return flowerIcon.copy();
            }
        }

        // Try as registered item (works for most blocks)
        try {
            ResourceLocation loc = ResourceLocation.parse(targetId);
            var optItem = BuiltInRegistries.ITEM.getOptional(loc);
            if (optItem.isPresent()) {
                ItemStack stack = new ItemStack(optItem.get());
                if (!stack.isEmpty()) return stack;
            }
        } catch (Exception ignored) {}

        // Fallback: use the category's representative icon
        MenuCategory cat = MenuCategory.fromId(categoryId);
        if (cat != null) return cat.getIconItem();
        return new ItemStack(Items.BARRIER);
    }

    private static String getCurrentDimension() {
        return Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.dimension().location().toString()
                : null;
    }

    private static String formatDimension(String dimension) {
        if (dimension == null) return "Unknown";
        return switch (dimension) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> dimension;
        };
    }

    private static int getDimensionColor(String dimension) {
        if (dimension == null) return 0xFF888888;
        return switch (dimension) {
            case "minecraft:overworld" -> 0xFF55FF55;
            case "minecraft:the_nether" -> 0xFFFF5555;
            case "minecraft:the_end" -> 0xFFDD88FF;
            default -> 0xFF888888;
        };
    }

    private static int getCategoryBadgeColor(String categoryId) {
        return switch (categoryId) {
            case "blocks" -> BADGE_BLOCKS;
            case "biomes" -> BADGE_BIOMES;
            case "structures" -> BADGE_STRUCTURES;
            case "flowers" -> BADGE_FLOWERS;
            default -> 0xFF888888;
        };
    }

    private static String formatTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 0) return "just now";

        long seconds = diff / 1000;
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 30) return days + "d ago";
        long months = days / 30;
        return months + "mo ago";
    }
}
