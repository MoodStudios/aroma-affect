package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.NoseRenderNetworking;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.MobTriggerDefinition;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import com.ovrtechnology.websocket.ConnectionState;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import com.ovrtechnology.websocket.WebSocketMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Full configuration screen for Aroma Affect mod settings.
 * Three sections: General, Passive Mode, Scent Values.
 */
public class ConfigScreen extends BaseMenuScreen {

    private static final ResourceLocation ICON_CONFIG = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_config.png");

    private enum Section { GENERAL, PASSIVE, SCENT_VALUES, WEBSOCKET }
    private enum ScentSubFilter { BLOCKS, FLOWERS, BIOMES, STRUCTURES, MOBS }

    private Section activeSection = Section.GENERAL;
    private ScentSubFilter activeScentFilter = ScentSubFilter.BLOCKS;

    // Scroll state for scrollable sections
    private double generalScrollOffset = 0;
    private double scentScrollOffset = 0;
    private double wsScrollOffset = 0;
    private double passiveScrollOffset = 0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    // Key capture state
    private boolean capturingKey = false;

    // Layout constants
    private static final int SIDEBAR_WIDTH = 120;
    private static final int CONTENT_PAD = 16;
    private static final int ROW_HEIGHT = 28;
    private static final int TOGGLE_W = 40;
    private static final int TOGGLE_H = 18;
    private static final int SLIDER_W = 150;
    private static final int SLIDER_H = 12;
    private static final int SELECTOR_BTN_W = 80;
    private static final int SELECTOR_BTN_H = 20;

    // Colors
    private static final int COL_GREEN = 0xFF9A7CFF;
    private static final int COL_GRAY = 0xFF666666;
    private static final int COL_ACCENT = 0xFF9A7CFF;
    private static final int COL_BG_PANEL = 0xDD1A1A2E;
    private static final int COL_BG_SIDEBAR = 0xDD111122;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_HOVER = 0x40FFFFFF;
    private static final int COL_KEY_CAPTURE = 0xFFFFFF44;

    // Hover tracking
    private boolean hoveringBack = false;

    // Slider drag tracking
    private enum DragTarget {
        NONE, INTENSITY, COOLDOWN,
        PASSIVE_BLOCK_CD, PASSIVE_MOB_CD, PASSIVE_PASSIVE_MOB_CD,
        PASSIVE_BLOCK_RANGE, PASSIVE_MOB_RANGE
    }
    private DragTarget activeDrag = DragTarget.NONE;

    public ConfigScreen() {
        super(Component.translatable("config.aromaaffect.title"));
    }

    @Override
    protected void init() {
        super.init();
        // Load current config values into NoseRenderToggles on screen open
        ClientConfig cfg = ClientConfig.getInstance();
        NoseRenderToggles.setNoseEnabled(cfg.isNoseRenderEnabled());
        NoseRenderToggles.setStrapEnabled(cfg.isNoseRenderEnabled() && cfg.isStrapEnabled());
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                  float partialTick, float animationProgress) {
        // Handle slider dragging: update value each frame while mouse is held
        updateSliderDrag(mouseX);

        float a = animationProgress;

        int panelLeft = 30;
        int panelTop = 20;
        int panelRight = width - 30;
        int panelBottom = height - 20;
        int panelW = panelRight - panelLeft;
        int panelH = panelBottom - panelTop;

        // Panel background
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, MenuRenderUtils.withAlpha(COL_BG_PANEL, a));
        MenuRenderUtils.renderOutline(graphics, panelLeft, panelTop, panelW, panelH, MenuRenderUtils.withAlpha(0x88FFFFFF, a));

        // Title bar
        int titleBarH = 24;
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + titleBarH, MenuRenderUtils.withAlpha(0xDD222244, a));
        Component title = Component.translatable("config.aromaaffect.title");
        graphics.drawCenteredString(font, title, panelLeft + panelW / 2, panelTop + 8, MenuRenderUtils.withAlpha(COL_TEXT, a));

        // Back button (top-left of title bar)
        int backW = 40;
        int backH = titleBarH - 4;
        int backX = panelLeft + 4;
        int backY = panelTop + 2;
        hoveringBack = mouseX >= backX && mouseX < backX + backW && mouseY >= backY && mouseY < backY + backH;
        int backBg = hoveringBack ? MenuRenderUtils.withAlpha(COL_HOVER, a) : MenuRenderUtils.withAlpha(0x20FFFFFF, a);
        graphics.fill(backX, backY, backX + backW, backY + backH, backBg);
        graphics.drawCenteredString(font, "< Back", backX + backW / 2, backY + (backH - 8) / 2, MenuRenderUtils.withAlpha(COL_TEXT, a));

        // Sidebar
        int sidebarLeft = panelLeft;
        int sidebarTop = panelTop + titleBarH;
        int sidebarBottom = panelBottom;
        graphics.fill(sidebarLeft, sidebarTop, sidebarLeft + SIDEBAR_WIDTH, sidebarBottom, MenuRenderUtils.withAlpha(COL_BG_SIDEBAR, a));

        // Section tabs
        int tabY = sidebarTop + 8;
        for (Section section : Section.values()) {
            int tabH = 24;
            boolean isActive = section == activeSection;
            boolean isHovering = mouseX >= sidebarLeft && mouseX < sidebarLeft + SIDEBAR_WIDTH
                    && mouseY >= tabY && mouseY < tabY + tabH;

            if (isActive) {
                graphics.fill(sidebarLeft, tabY, sidebarLeft + 3, tabY + tabH, MenuRenderUtils.withAlpha(COL_ACCENT, a));
                graphics.fill(sidebarLeft, tabY, sidebarLeft + SIDEBAR_WIDTH, tabY + tabH, MenuRenderUtils.withAlpha(0x30FFFFFF, a));
            } else if (isHovering) {
                graphics.fill(sidebarLeft, tabY, sidebarLeft + SIDEBAR_WIDTH, tabY + tabH, MenuRenderUtils.withAlpha(COL_HOVER, a));
            }

            String labelKey = switch (section) {
                case GENERAL -> "config.aromaaffect.section.general";
                case PASSIVE -> "config.aromaaffect.section.passive";
                case SCENT_VALUES -> "config.aromaaffect.section.scent_values";
                case WEBSOCKET -> "config.aromaaffect.section.websocket";
            };
            Component label = Component.translatable(labelKey);
            graphics.drawString(font, label, sidebarLeft + 12, tabY + (tabH - 8) / 2, MenuRenderUtils.withAlpha(isActive ? COL_TEXT : COL_TEXT_DIM, a));
            tabY += tabH + 2;
        }

        // Content area
        int contentLeft = sidebarLeft + SIDEBAR_WIDTH + CONTENT_PAD;
        int contentTop = sidebarTop + CONTENT_PAD;
        int contentRight = panelRight - CONTENT_PAD;
        int contentW = contentRight - contentLeft;

        switch (activeSection) {
            case GENERAL -> renderGeneralSection(graphics, contentLeft, contentTop, contentW, panelBottom - CONTENT_PAD - contentTop, mouseX, mouseY, a);
            case PASSIVE -> renderPassiveSection(graphics, contentLeft, contentTop, contentW, panelBottom - CONTENT_PAD - contentTop, mouseX, mouseY, a);
            case SCENT_VALUES -> renderScentValuesSection(graphics, contentLeft, contentTop, contentW, panelBottom - CONTENT_PAD - contentTop, mouseX, mouseY, a);
            case WEBSOCKET -> renderWebSocketSection(graphics, contentLeft, contentTop, contentW, panelBottom - CONTENT_PAD - contentTop, mouseX, mouseY, a);
        }
    }

    private void renderGeneralSection(GuiGraphics graphics, int x, int y, int w, int h, int mx, int my, float a) {
        ClientConfig config = ClientConfig.getInstance();
        boolean isAutomatic = "automatic".equals(config.getPuffMode());

        // Calculate total content height
        int totalContentH = (ROW_HEIGHT + 4) * 7 + (ROW_HEIGHT + 4); // 8 rows always visible
        if (!isAutomatic) totalContentH += (ROW_HEIGHT + 4); // manual key row

        int maxScroll = Math.max(0, totalContentH - h);
        generalScrollOffset = Mth.clamp(generalScrollOffset, 0, maxScroll);

        // Scissor clip the content area
        graphics.enableScissor(x, y, x + w, y + h);

        int rowY = y - (int) generalScrollOffset;

        // Puff Mode selector
        graphics.drawString(font, Component.translatable("config.aromaaffect.puff_mode"), x, rowY + 6, MenuRenderUtils.withAlpha(COL_TEXT, a));
        int selX = x + w - SELECTOR_BTN_W * 2 - 2;

        // Automatic button
        int autoBg = isAutomatic ? MenuRenderUtils.withAlpha(COL_ACCENT, a) : MenuRenderUtils.withAlpha(0xFF333333, a);
        boolean hoverAuto = mx >= selX && mx < selX + SELECTOR_BTN_W && my >= rowY && my < rowY + SELECTOR_BTN_H;
        if (hoverAuto && !isAutomatic) autoBg = MenuRenderUtils.withAlpha(0xFF444444, a);
        graphics.fill(selX, rowY, selX + SELECTOR_BTN_W, rowY + SELECTOR_BTN_H, autoBg);
        graphics.drawCenteredString(font, Component.translatable("config.aromaaffect.puff_mode.automatic"),
                selX + SELECTOR_BTN_W / 2, rowY + 6, MenuRenderUtils.withAlpha(COL_TEXT, a));

        // Manual button
        int manSelX = selX + SELECTOR_BTN_W + 2;
        int manBg = !isAutomatic ? MenuRenderUtils.withAlpha(COL_ACCENT, a) : MenuRenderUtils.withAlpha(0xFF333333, a);
        boolean hoverMan = mx >= manSelX && mx < manSelX + SELECTOR_BTN_W && my >= rowY && my < rowY + SELECTOR_BTN_H;
        if (hoverMan && isAutomatic) manBg = MenuRenderUtils.withAlpha(0xFF444444, a);
        graphics.fill(manSelX, rowY, manSelX + SELECTOR_BTN_W, rowY + SELECTOR_BTN_H, manBg);
        graphics.drawCenteredString(font, Component.translatable("config.aromaaffect.puff_mode.manual"),
                manSelX + SELECTOR_BTN_W / 2, rowY + 6, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 4;

        // Manual Puff Key (only visible in manual mode)
        if (!isAutomatic) {
            graphics.drawString(font, Component.translatable("config.aromaaffect.manual_key"), x, rowY + 6, MenuRenderUtils.withAlpha(COL_TEXT, a));
            int keyBtnX = x + w - 80;
            int keyBtnW = 80;
            int keyBtnH = 20;
            int keyBg = capturingKey ? MenuRenderUtils.withAlpha(0xFF666622, a) : MenuRenderUtils.withAlpha(0xFF333333, a);
            boolean hoverKey = mx >= keyBtnX && mx < keyBtnX + keyBtnW && my >= rowY && my < rowY + keyBtnH;
            if (hoverKey && !capturingKey) keyBg = MenuRenderUtils.withAlpha(0xFF444444, a);
            graphics.fill(keyBtnX, rowY, keyBtnX + keyBtnW, rowY + keyBtnH, keyBg);
            MenuRenderUtils.renderOutline(graphics, keyBtnX, rowY, keyBtnW, keyBtnH, MenuRenderUtils.withAlpha(0x88FFFFFF, a));
            String keyText = capturingKey ? Component.translatable("config.aromaaffect.manual_key.press").getString()
                    : config.getManualPuffKey();
            int keyColor = capturingKey ? MenuRenderUtils.withAlpha(COL_KEY_CAPTURE, a) : MenuRenderUtils.withAlpha(COL_TEXT, a);
            graphics.drawCenteredString(font, keyText, keyBtnX + keyBtnW / 2, rowY + 6, keyColor);
            rowY += ROW_HEIGHT + 4;
        }

        // Global Intensity slider
        graphics.drawString(font, Component.translatable("config.aromaaffect.intensity"), x, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        int sliderX = x + w - SLIDER_W - 40;
        renderSlider(graphics, sliderX, rowY, SLIDER_W, (float) config.getGlobalIntensityMultiplier(), 0f, 1f, a);
        int pct = (int) (config.getGlobalIntensityMultiplier() * 100);
        graphics.drawString(font, pct + "%", sliderX + SLIDER_W + 6, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 4;

        // Global Cooldown slider
        graphics.drawString(font, Component.translatable("config.aromaaffect.cooldown"), x, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        float cooldownSec = config.getGlobalCooldownMs() / 1000f;
        renderSlider(graphics, sliderX, rowY, SLIDER_W, cooldownSec, 1f, 60f, a);
        graphics.drawString(font, String.format("%.0fs", cooldownSec), sliderX + SLIDER_W + 6, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 4;

        // 3D Nose Render toggle
        graphics.drawString(font, Component.translatable("config.aromaaffect.nose_render"), x, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT, a));
        int toggleX = x + w - TOGGLE_W - 30;
        renderTogglePill(graphics, toggleX, rowY + 1, config.isNoseRenderEnabled(), a);
        Component toggleLabel = config.isNoseRenderEnabled()
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, toggleLabel, toggleX + TOGGLE_W + 6, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
        rowY += ROW_HEIGHT + 4;

        // Nose Strap toggle (only interactive when 3D Nose Render is enabled)
        boolean strapActive = config.isNoseRenderEnabled() && config.isStrapEnabled();
        int strapTextColor = config.isNoseRenderEnabled() ? COL_TEXT : COL_TEXT_DIM;
        graphics.drawString(font, Component.translatable("config.aromaaffect.nose_strap"), x, rowY + 4, MenuRenderUtils.withAlpha(strapTextColor, a));
        renderTogglePill(graphics, toggleX, rowY + 1, strapActive, a);
        Component strapLabel = strapActive
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, strapLabel, toggleX + TOGGLE_W + 6, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
        rowY += ROW_HEIGHT + 4;

        // Tracking Toast (persistent, outside radial menu)
        graphics.drawString(font, Component.translatable("config.aromaaffect.tracking_toast_persistent"), x, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT, a));
        renderTogglePill(graphics, toggleX, rowY + 1, config.isTrackingToastPersistent(), a);
        Component trackingToastLabel = config.isTrackingToastPersistent()
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, trackingToastLabel, toggleX + TOGGLE_W + 6, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
        rowY += ROW_HEIGHT + 4;

        // Passive Puff Overlay toggle
        graphics.drawString(font, Component.translatable("config.aromaaffect.passive_puff_overlay"), x, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT, a));
        renderTogglePill(graphics, toggleX, rowY + 1, config.isPassivePuffOverlay(), a);
        Component puffOverlayLabel = config.isPassivePuffOverlay()
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, puffOverlayLabel, toggleX + TOGGLE_W + 6, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
        rowY += ROW_HEIGHT + 4;

        // Debug Scent Messages toggle
        graphics.drawString(font, Component.translatable("config.aromaaffect.debug_scent_messages"), x, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT, a));
        renderTogglePill(graphics, toggleX, rowY + 1, config.isDebugScentMessages(), a);
        Component debugScentLabel = config.isDebugScentMessages()
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, debugScentLabel, toggleX + TOGGLE_W + 6, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));

        graphics.disableScissor();

        // Scrollbar (only if content overflows)
        if (totalContentH > h && h > 0) {
            int scrollBarX = x + w - 4;
            int scrollBarH = Math.max(10, (int) ((float) h / totalContentH * h));
            int scrollBarY = y + (int) ((float) generalScrollOffset / maxScroll * (h - scrollBarH));
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarH, MenuRenderUtils.withAlpha(0x88FFFFFF, a));
        }
    }

    private void renderPassiveSection(GuiGraphics graphics, int x, int y, int w, int h, int mx, int my, float a) {
        ClientConfig config = ClientConfig.getInstance();
        int sliderX = x + w - SLIDER_W - 40;
        int toggleX = x + w - TOGGLE_W - 30;

        // Calculate total content height to determine if scrolling is needed
        // Toggle(28+8) + header(16) + 3 sliders(28*3) + gap(8) + header(16) + 2 sliders(28*2) + gap(8) + button(20) = 188
        int totalContentH = (ROW_HEIGHT + 8) + 16 + (ROW_HEIGHT * 3) + 8 + 16 + (ROW_HEIGHT * 2) + 8 + 20;
        int maxScroll = Math.max(0, totalContentH - h);
        passiveScrollOffset = Mth.clamp(passiveScrollOffset, 0, maxScroll);

        // Scissor clip the content area
        graphics.enableScissor(x, y, x + w, y + h);

        int rowY = y - (int) passiveScrollOffset;

        // Passive Enabled toggle
        graphics.drawString(font, Component.translatable("config.aromaaffect.passive_enabled"), x, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT, a));
        boolean passiveEnabled = PassiveModeManager.isPassiveModeEnabled();
        renderTogglePill(graphics, toggleX, rowY + 1, passiveEnabled, a);
        Component toggleLabel = passiveEnabled
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, toggleLabel, toggleX + TOGGLE_W + 6, rowY + 4, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
        rowY += ROW_HEIGHT + 8;

        // Cooldowns
        graphics.drawString(font, "Cooldowns", x, rowY + 2, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        rowY += 16;

        // Block Cooldown (1s - 30s)
        graphics.drawString(font, "Block Cooldown", x, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        float blockCd = config.getPassiveBlockCooldownMs() / 1000f;
        renderSlider(graphics, sliderX, rowY, SLIDER_W, blockCd, 1f, 30f, a);
        graphics.drawString(font, String.format("%.0fs", blockCd), sliderX + SLIDER_W + 6, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT;

        // Hostile Mob CD (1s - 30s)
        graphics.drawString(font, "Hostile Mob CD", x, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        float mobCd = config.getPassiveMobCooldownMs() / 1000f;
        renderSlider(graphics, sliderX, rowY, SLIDER_W, mobCd, 1f, 30f, a);
        graphics.drawString(font, String.format("%.0fs", mobCd), sliderX + SLIDER_W + 6, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT;

        // Passive Mob CD (1s - 30s)
        graphics.drawString(font, "Passive Mob CD", x, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        float passiveMobCd = config.getPassivePassiveMobCooldownMs() / 1000f;
        renderSlider(graphics, sliderX, rowY, SLIDER_W, passiveMobCd, 1f, 30f, a);
        graphics.drawString(font, String.format("%.0fs", passiveMobCd), sliderX + SLIDER_W + 6, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 8;

        // Ranges
        graphics.drawString(font, "Ranges", x, rowY + 2, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        rowY += 16;

        // Block Range (1 - 5 blocks)
        graphics.drawString(font, "Block Range", x, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        float blockRange = (float) config.getPassiveBlockRange();
        renderSlider(graphics, sliderX, rowY, SLIDER_W, blockRange, 1f, 5f, a);
        graphics.drawString(font, String.format("%.1f", blockRange), sliderX + SLIDER_W + 6, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT;

        // Mob Range (1 - 15 blocks)
        graphics.drawString(font, "Mob Range", x, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        float mobRange = (float) config.getPassiveMobRange();
        renderSlider(graphics, sliderX, rowY, SLIDER_W, mobRange, 1f, 15f, a);
        graphics.drawString(font, String.format("%.1f", mobRange), sliderX + SLIDER_W + 6, rowY + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 8;

        // Reset Defaults button
        int resetBtnW = 100;
        int resetBtnH = 20;
        int resetBtnX = x + (w - resetBtnW) / 2;
        boolean hoverReset = mx >= resetBtnX && mx < resetBtnX + resetBtnW && my >= rowY && my < rowY + resetBtnH;
        int resetBg = hoverReset ? MenuRenderUtils.withAlpha(0xFF444444, a) : MenuRenderUtils.withAlpha(0xFF333333, a);
        graphics.fill(resetBtnX, rowY, resetBtnX + resetBtnW, rowY + resetBtnH, resetBg);
        MenuRenderUtils.renderOutline(graphics, resetBtnX, rowY, resetBtnW, resetBtnH, MenuRenderUtils.withAlpha(0x88FFFFFF, a));
        graphics.drawCenteredString(font, "Reset Defaults", resetBtnX + resetBtnW / 2, rowY + 6, MenuRenderUtils.withAlpha(COL_TEXT, a));

        graphics.disableScissor();

        // Scrollbar (only if content overflows)
        if (totalContentH > h && h > 0) {
            int scrollBarX = x + w - 4;
            int scrollBarH = Math.max(10, (int) ((float) h / totalContentH * h));
            int scrollBarY = y + (int) ((float) passiveScrollOffset / maxScroll * (h - scrollBarH));
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarH, MenuRenderUtils.withAlpha(0x88FFFFFF, a));
        }
    }

    private void renderScentValuesSection(GuiGraphics graphics, int x, int y, int w, int h, int mx, int my, float a) {
        // Sub-filter tabs
        int tabX = x;
        for (ScentSubFilter filter : ScentSubFilter.values()) {
            String labelKey = switch (filter) {
                case BLOCKS -> "config.aromaaffect.scent_values.blocks";
                case FLOWERS -> "config.aromaaffect.scent_values.flowers";
                case BIOMES -> "config.aromaaffect.scent_values.biomes";
                case STRUCTURES -> "config.aromaaffect.scent_values.structures";
                case MOBS -> "config.aromaaffect.scent_values.mobs";
            };
            Component label = Component.translatable(labelKey);
            int tabW = font.width(label) + 16;
            int tabH = 18;
            boolean isActive = filter == activeScentFilter;
            boolean isHover = mx >= tabX && mx < tabX + tabW && my >= y && my < y + tabH;

            int bg = isActive ? MenuRenderUtils.withAlpha(COL_ACCENT, a) : (isHover ? MenuRenderUtils.withAlpha(COL_HOVER, a) : MenuRenderUtils.withAlpha(0x20FFFFFF, a));
            graphics.fill(tabX, y, tabX + tabW, y + tabH, bg);
            graphics.drawCenteredString(font, label, tabX + tabW / 2, y + 5, MenuRenderUtils.withAlpha(COL_TEXT, a));
            tabX += tabW + 4;
        }

        // List area
        int listTop = y + 24;
        int listH = h - 24;

        List<String[]> entries = getScentValueEntries();
        int entryH = 14;
        int totalH = entries.size() * entryH;

        // Clamp scroll
        int maxScroll = Math.max(0, totalH - listH);
        scentScrollOffset = Mth.clamp(scentScrollOffset, 0, maxScroll);

        // Header
        graphics.drawString(font, "Name", x, listTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        graphics.drawString(font, "Scent", x + w / 3, listTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        graphics.drawString(font, "Intensity", x + 2 * w / 3, listTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        listTop += entryH + 2;
        listH -= entryH + 2;

        // Entries (scissor-clipped so they don't overlap headers)
        graphics.enableScissor(x, listTop, x + w, listTop + listH);
        int drawY = listTop - (int) scentScrollOffset;
        for (String[] entry : entries) {
            if (drawY + entryH >= listTop && drawY < listTop + listH) {
                int textColor = MenuRenderUtils.withAlpha(COL_TEXT_DIM, a);
                graphics.drawString(font, entry[0], x, drawY, textColor);
                graphics.drawString(font, entry[1], x + w / 3, drawY, textColor);
                graphics.drawString(font, entry[2], x + 2 * w / 3, drawY, textColor);
            }
            drawY += entryH;
        }
        graphics.disableScissor();

        // Scrollbar
        if (totalH > listH && listH > 0) {
            int scrollBarX = x + w - 4;
            int scrollBarH = Math.max(10, (int) ((float) listH / totalH * listH));
            int scrollBarY = listTop + (int) ((float) scentScrollOffset / maxScroll * (listH - scrollBarH));
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarH, MenuRenderUtils.withAlpha(0x88FFFFFF, a));
        }
    }

    private void renderWebSocketSection(GuiGraphics graphics, int x, int y, int w, int h, int mx, int my, float a) {
        OvrWebSocketClient client = OvrWebSocketClient.getInstance();
        ConnectionState connState = client.getState();

        // --- Status Card ---
        int cardH = 55;
        graphics.fill(x, y, x + w, y + cardH, MenuRenderUtils.withAlpha(0x30FFFFFF, a));
        MenuRenderUtils.renderOutline(graphics, x, y, w, cardH, MenuRenderUtils.withAlpha(0x44FFFFFF, a));

        int cardPad = 8;
        int rowY = y + cardPad;

        // Row 1: colored dot + status text
        int dotColor = switch (connState) {
            case CONNECTED -> 0xFF44FF44;
            case CONNECTING, RECONNECTING -> 0xFFFFCC44;
            case DISCONNECTED, CONNECTION_FAILED -> 0xFFFF4444;
        };
        graphics.fill(x + cardPad, rowY + 1, x + cardPad + 8, rowY + 9, MenuRenderUtils.withAlpha(dotColor, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.status").append(": " + connState.getDisplayName()),
                x + cardPad + 12, rowY, MenuRenderUtils.withAlpha(COL_TEXT, a));
        rowY += 14;

        // Row 2: server URI
        String uri = client.getConfig().getUri();
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.server").append(": " + uri),
                x + cardPad, rowY, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
        rowY += 14;

        // Row 3: reconnect attempts (only if > 0)
        int attempts = client.getReconnectAttempts();
        if (attempts > 0) {
            graphics.drawString(font, Component.translatable("config.aromaaffect.ws.attempts").append(": " + attempts),
                    x + cardPad, rowY, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
        }

        // --- Message Log ---
        int logTop = y + cardH + 10;
        int logH = h - cardH - 10;

        // Header
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.messages"), x, logTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        logTop += 14;
        logH -= 14;

        // Column headers
        int colDir = x;
        int colType = x + 20;
        int colContent = x + 70;
        int colTime = x + w - 50;
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.dir"), colDir, logTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.type"), colType, logTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.content"), colContent, logTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.time"), colTime, logTop, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        logTop += 12;
        logH -= 12;

        List<WebSocketMessage> messages = client.getMessageHistory();

        if (messages.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("config.aromaaffect.ws.no_messages"),
                    x + w / 2, logTop + logH / 2 - 4, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
            return;
        }

        int entryH = 14;
        int totalH = messages.size() * entryH;
        int maxScroll = Math.max(0, totalH - logH);
        wsScrollOffset = Mth.clamp(wsScrollOffset, 0, maxScroll);

        // Scissor-clipped scrollable area
        graphics.enableScissor(x, logTop, x + w, logTop + logH);
        int drawY = logTop - (int) wsScrollOffset;
        int maxContentW = colTime - colContent - 4;
        for (WebSocketMessage msg : messages) {
            if (drawY + entryH >= logTop && drawY < logTop + logH) {
                // Direction arrow
                if (msg.isOutgoing()) {
                    graphics.drawString(font, "\u2192", colDir, drawY, MenuRenderUtils.withAlpha(0xFF44FF44, a));
                } else {
                    graphics.drawString(font, "\u2190", colDir, drawY, MenuRenderUtils.withAlpha(0xFFBB88FF, a));
                }

                // Type
                graphics.drawString(font, msg.getType(), colType, drawY, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));

                // Content (truncated)
                String payload = msg.getPayload();
                if (font.width(payload) > maxContentW) {
                    while (payload.length() > 3 && font.width(payload + "...") > maxContentW) {
                        payload = payload.substring(0, payload.length() - 1);
                    }
                    payload = payload + "...";
                }
                graphics.drawString(font, payload, colContent, drawY, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));

                // Time
                String time = TIME_FMT.format(msg.getTimestamp());
                graphics.drawString(font, time, colTime, drawY, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
            }
            drawY += entryH;
        }
        graphics.disableScissor();

        // Scrollbar
        if (totalH > logH && logH > 0) {
            int scrollBarX = x + w - 4;
            int scrollBarH = Math.max(10, (int) ((float) logH / totalH * logH));
            int scrollBarY = logTop + (int) ((float) wsScrollOffset / maxScroll * (logH - scrollBarH));
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarH, MenuRenderUtils.withAlpha(0x88FFFFFF, a));
        }
    }

    private static final Set<String> FLOWER_IDS = Set.of(
            "minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid",
            "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip",
            "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip",
            "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley",
            "minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush", "minecraft:peony",
            "minecraft:wither_rose", "minecraft:torchflower", "minecraft:pitcher_plant",
            "minecraft:pink_petals", "minecraft:spore_blossom",
            "minecraft:crimson_fungus", "minecraft:warped_fungus"
    );

    private List<String[]> getScentValueEntries() {
        List<String[]> entries = new ArrayList<>();
        switch (activeScentFilter) {
            case BLOCKS -> {
                for (BlockTriggerDefinition trigger : ScentTriggerConfigLoader.getAllBlockTriggers()) {
                    if (!trigger.isValid()) continue;
                    if (FLOWER_IDS.contains(trigger.getBlockId())) continue;
                    String name = formatId(trigger.getBlockId());
                    String scent = trigger.getScentName();
                    entries.add(new String[]{name, scent, formatIntensity(trigger.getIntensity())});
                }
            }
            case FLOWERS -> {
                for (BlockTriggerDefinition trigger : ScentTriggerConfigLoader.getAllBlockTriggers()) {
                    if (!trigger.isValid()) continue;
                    if (!FLOWER_IDS.contains(trigger.getBlockId())) continue;
                    String name = formatId(trigger.getBlockId());
                    String scent = trigger.getScentName();
                    entries.add(new String[]{name, scent, formatIntensity(trigger.getIntensity())});
                }
            }
            case BIOMES -> {
                for (BiomeTriggerDefinition trigger : ScentTriggerConfigLoader.getAllBiomeTriggers()) {
                    if (!trigger.isValid()) continue;
                    String name = formatId(trigger.getBiomeId());
                    String scent = trigger.getScentName();
                    entries.add(new String[]{name, scent, formatIntensity(trigger.getIntensity())});
                }
            }
            case STRUCTURES -> {
                for (StructureTriggerDefinition trigger : ScentTriggerConfigLoader.getAllStructureTriggers()) {
                    if (!trigger.isValid()) continue;
                    String name = formatId(trigger.getStructureId());
                    String scent = trigger.getScentName();
                    entries.add(new String[]{name, scent, formatIntensity(trigger.getIntensity())});
                }
            }
            case MOBS -> {
                for (MobTriggerDefinition trigger : ScentTriggerConfigLoader.getAllMobTriggers()) {
                    if (!trigger.isValid()) continue;
                    String name = formatId(trigger.getEntityType());
                    String scent = trigger.getScentName();
                    entries.add(new String[]{name, scent, formatIntensity(trigger.getIntensity())});
                }
            }
        }
        return entries;
    }

    private static String formatIntensity(Double intensity) {
        if (intensity == null || intensity <= 0) return "default";
        return String.format("%.0f%%", intensity * 100);
    }

    private static String formatId(String resourceId) {
        try {
            int colonIdx = resourceId.indexOf(':');
            String path = colonIdx >= 0 ? resourceId.substring(colonIdx + 1) : resourceId;
            return path.replace('_', ' ');
        } catch (Exception e) {
            return resourceId;
        }
    }

    // --- Widget rendering ---

    private void renderTogglePill(GuiGraphics graphics, int x, int y, boolean on, float a) {
        int bgColor = on ? MenuRenderUtils.withAlpha(COL_GREEN, a) : MenuRenderUtils.withAlpha(COL_GRAY, a);
        graphics.fill(x, y, x + TOGGLE_W, y + TOGGLE_H, bgColor);
        // Rounded effect using border
        MenuRenderUtils.renderOutline(graphics, x, y, TOGGLE_W, TOGGLE_H, MenuRenderUtils.withAlpha(0x44FFFFFF, a));
        // Circle thumb
        int circleSize = TOGGLE_H - 4;
        int circleX = on ? x + TOGGLE_W - circleSize - 2 : x + 2;
        int circleY = y + 2;
        graphics.fill(circleX, circleY, circleX + circleSize, circleY + circleSize, MenuRenderUtils.withAlpha(COL_TEXT, a));
    }

    private void renderSlider(GuiGraphics graphics, int x, int y, int w, float value, float min, float max, float a) {
        int trackY = y + 4;
        // Track background
        graphics.fill(x, trackY, x + w, trackY + SLIDER_H, MenuRenderUtils.withAlpha(0xFF333333, a));
        // Filled portion
        float ratio = (value - min) / (max - min);
        ratio = Mth.clamp(ratio, 0f, 1f);
        int filledW = (int) (w * ratio);
        graphics.fill(x, trackY, x + filledW, trackY + SLIDER_H, MenuRenderUtils.withAlpha(COL_ACCENT, a));
        // Handle
        int handleX = x + filledW - 4;
        graphics.fill(handleX, trackY - 2, handleX + 8, trackY + SLIDER_H + 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
    }

    // --- Slider drag handling ---

    private int getSliderX() {
        int panelLeft = 30;
        int panelRight = width - 30;
        int contentLeft = panelLeft + SIDEBAR_WIDTH + CONTENT_PAD + (24 /* titleBarH handled via sidebarTop */);
        // Recalculate same as in render
        int contentW = panelRight - CONTENT_PAD - (panelLeft + SIDEBAR_WIDTH + CONTENT_PAD);
        return panelLeft + SIDEBAR_WIDTH + CONTENT_PAD + contentW - SLIDER_W - 40;
    }

    private void updateSliderDrag(int mouseX) {
        if (activeDrag == DragTarget.NONE) return;

        // Check if left mouse button is still held
        long window = Minecraft.getInstance().getWindow().handle();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            activeDrag = DragTarget.NONE;
            return;
        }

        int sliderX = getSliderX();
        float ratio = Mth.clamp((float) (mouseX - sliderX) / SLIDER_W, 0f, 1f);
        ClientConfig config = ClientConfig.getInstance();

        switch (activeDrag) {
            case INTENSITY -> {
                config.setGlobalIntensityMultiplier(Math.round(ratio * 100.0) / 100.0);
                config.save();
            }
            case COOLDOWN -> {
                long ms = (long) (1000 + ratio * 59000);
                config.setGlobalCooldownMs(ms);
                config.save();
            }
            case PASSIVE_BLOCK_CD -> {
                config.setPassiveBlockCooldownMs((long) (1000 + ratio * 29000));
                config.save();
            }
            case PASSIVE_MOB_CD -> {
                config.setPassiveMobCooldownMs((long) (1000 + ratio * 29000));
                config.save();
            }
            case PASSIVE_PASSIVE_MOB_CD -> {
                config.setPassivePassiveMobCooldownMs((long) (1000 + ratio * 29000));
                config.save();
            }
            case PASSIVE_BLOCK_RANGE -> {
                config.setPassiveBlockRange(Math.round((1.0 + ratio * 4.0) * 10.0) / 10.0);
                config.save();
            }
            case PASSIVE_MOB_RANGE -> {
                config.setPassiveMobRange(Math.round((1.0 + ratio * 14.0) * 10.0) / 10.0);
                config.save();
            }
        }
    }

    // --- Input handling ---

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Back button
        if (hoveringBack) {
            MenuRenderUtils.playClickSound();
            MenuManager.returnToRadialMenu();
            return true;
        }

        int panelLeft = 30;
        int panelTop = 20;
        int panelRight = width - 30;
        int titleBarH = 24;
        int sidebarTop = panelTop + titleBarH;

        // Sidebar section tabs
        int tabY = sidebarTop + 8;
        for (Section section : Section.values()) {
            int tabH = 24;
            if (mx >= panelLeft && mx < panelLeft + SIDEBAR_WIDTH && my >= tabY && my < tabY + tabH) {
                MenuRenderUtils.playClickSound();
                activeSection = section;
                generalScrollOffset = 0;
                scentScrollOffset = 0;
                passiveScrollOffset = 0;
                capturingKey = false;
                return true;
            }
            tabY += tabH + 2;
        }

        // Content area interactions
        int contentLeft = panelLeft + SIDEBAR_WIDTH + CONTENT_PAD;
        int contentTop = sidebarTop + CONTENT_PAD;
        int contentW = panelRight - CONTENT_PAD - contentLeft;

        if (activeSection == Section.GENERAL) {
            return handleGeneralClick(mx, my, contentLeft, contentTop, contentW);
        } else if (activeSection == Section.PASSIVE) {
            return handlePassiveClick(mx, my, contentLeft, contentTop, contentW);
        } else if (activeSection == Section.SCENT_VALUES) {
            return handleScentFilterClick(mx, my, contentLeft, contentTop, contentW);
        }

        return false;
    }

    private boolean handleGeneralClick(int mx, int my, int x, int y, int w) {
        ClientConfig config = ClientConfig.getInstance();
        // Adjust mouse Y for scroll offset so clicks match rendered positions
        int adjustedMy = my + (int) generalScrollOffset;
        int rowY = y;

        // Puff Mode selector
        int selX = x + w - SELECTOR_BTN_W * 2 - 2;
        if (adjustedMy >= rowY && adjustedMy < rowY + SELECTOR_BTN_H) {
            if (mx >= selX && mx < selX + SELECTOR_BTN_W) {
                MenuRenderUtils.playClickSound();
                config.setPuffMode("automatic");
                config.save();
                capturingKey = false;
                return true;
            }
            int manSelX = selX + SELECTOR_BTN_W + 2;
            if (mx >= manSelX && mx < manSelX + SELECTOR_BTN_W) {
                MenuRenderUtils.playClickSound();
                config.setPuffMode("manual");
                config.save();
                return true;
            }
        }
        rowY += ROW_HEIGHT + 4;

        // Manual key (only if manual mode)
        if (!"automatic".equals(config.getPuffMode())) {
            int keyBtnX = x + w - 80;
            if (mx >= keyBtnX && mx < keyBtnX + 80 && adjustedMy >= rowY && adjustedMy < rowY + 20) {
                MenuRenderUtils.playClickSound();
                capturingKey = true;
                return true;
            }
            rowY += ROW_HEIGHT + 4;
        }

        // Intensity slider
        int sliderX = x + w - SLIDER_W - 40;
        if (mx >= sliderX && mx < sliderX + SLIDER_W && adjustedMy >= rowY && adjustedMy < rowY + SLIDER_H + 8) {
            MenuRenderUtils.playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            config.setGlobalIntensityMultiplier(Math.round(ratio * 100.0) / 100.0);
            config.save();
            activeDrag = DragTarget.INTENSITY;
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Cooldown slider
        if (mx >= sliderX && mx < sliderX + SLIDER_W && adjustedMy >= rowY && adjustedMy < rowY + SLIDER_H + 8) {
            MenuRenderUtils.playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            long ms = (long) (1000 + ratio * 59000); // 1s to 60s
            config.setGlobalCooldownMs(ms);
            config.save();
            activeDrag = DragTarget.COOLDOWN;
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Nose render toggle
        int toggleX = x + w - TOGGLE_W - 30;
        if (mx >= toggleX && mx < toggleX + TOGGLE_W && adjustedMy >= rowY && adjustedMy < rowY + TOGGLE_H + 2) {
            config.setNoseRenderEnabled(!config.isNoseRenderEnabled());
            NoseRenderToggles.setNoseEnabled(config.isNoseRenderEnabled());
            // If nose render is disabled, also disable strap
            if (!config.isNoseRenderEnabled()) {
                config.setStrapEnabled(false);
                NoseRenderToggles.setStrapEnabled(false);
            }
            MenuRenderUtils.playToggleSound(config.isNoseRenderEnabled());
            config.save();
            syncNosePrefsToServer(config);
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Nose strap toggle (only if nose render is enabled)
        if (config.isNoseRenderEnabled() && mx >= toggleX && mx < toggleX + TOGGLE_W && adjustedMy >= rowY && adjustedMy < rowY + TOGGLE_H + 2) {
            config.setStrapEnabled(!config.isStrapEnabled());
            NoseRenderToggles.setStrapEnabled(config.isStrapEnabled());
            MenuRenderUtils.playToggleSound(config.isStrapEnabled());
            config.save();
            syncNosePrefsToServer(config);
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Tracking toast persistent toggle
        if (mx >= toggleX && mx < toggleX + TOGGLE_W && adjustedMy >= rowY && adjustedMy < rowY + TOGGLE_H + 2) {
            config.setTrackingToastPersistent(!config.isTrackingToastPersistent());
            MenuRenderUtils.playToggleSound(config.isTrackingToastPersistent());
            config.save();
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Passive puff overlay toggle
        if (mx >= toggleX && mx < toggleX + TOGGLE_W && adjustedMy >= rowY && adjustedMy < rowY + TOGGLE_H + 2) {
            config.setPassivePuffOverlay(!config.isPassivePuffOverlay());
            MenuRenderUtils.playToggleSound(config.isPassivePuffOverlay());
            config.save();
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Debug scent messages toggle
        if (mx >= toggleX && mx < toggleX + TOGGLE_W && adjustedMy >= rowY && adjustedMy < rowY + TOGGLE_H + 2) {
            config.setDebugScentMessages(!config.isDebugScentMessages());
            MenuRenderUtils.playToggleSound(config.isDebugScentMessages());
            config.save();
            return true;
        }

        return false;
    }

    private void syncNosePrefsToServer(ClientConfig config) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean noseEnabled = config.isNoseRenderEnabled();
            boolean strapEnabled = config.isNoseRenderEnabled() && config.isStrapEnabled();
            NoseRenderPreferencesManager.setClientPrefs(
                    mc.player.getUUID(), noseEnabled, strapEnabled);
            NoseRenderNetworking.sendPrefsToServer(
                    mc.player.registryAccess(), noseEnabled, strapEnabled);
        }
    }

    private boolean handlePassiveClick(int mx, int my, int x, int y, int w) {
        ClientConfig config = ClientConfig.getInstance();
        // Adjust mouse Y for scroll offset so clicks match rendered positions
        int adjustedMy = my + (int) passiveScrollOffset;
        int rowY = y;
        int sliderX = x + w - SLIDER_W - 40;
        int toggleX = x + w - TOGGLE_W - 30;

        // Passive toggle
        if (mx >= toggleX && mx < toggleX + TOGGLE_W && adjustedMy >= rowY && adjustedMy < rowY + TOGGLE_H + 2) {
            PassiveModeManager.togglePassiveMode();
            MenuRenderUtils.playToggleSound(PassiveModeManager.isPassiveModeEnabled());
            return true;
        }
        rowY += ROW_HEIGHT + 8;

        // "Cooldowns" header
        rowY += 16;

        // Block Cooldown slider (1s - 30s)
        if (mx >= sliderX && mx < sliderX + SLIDER_W && adjustedMy >= rowY && adjustedMy < rowY + SLIDER_H + 8) {
            MenuRenderUtils.playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            config.setPassiveBlockCooldownMs((long) (1000 + ratio * 29000));
            config.save();
            activeDrag = DragTarget.PASSIVE_BLOCK_CD;
            return true;
        }
        rowY += ROW_HEIGHT;

        // Hostile Mob CD slider (1s - 30s)
        if (mx >= sliderX && mx < sliderX + SLIDER_W && adjustedMy >= rowY && adjustedMy < rowY + SLIDER_H + 8) {
            MenuRenderUtils.playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            config.setPassiveMobCooldownMs((long) (1000 + ratio * 29000));
            config.save();
            activeDrag = DragTarget.PASSIVE_MOB_CD;
            return true;
        }
        rowY += ROW_HEIGHT;

        // Passive Mob CD slider (1s - 30s)
        if (mx >= sliderX && mx < sliderX + SLIDER_W && adjustedMy >= rowY && adjustedMy < rowY + SLIDER_H + 8) {
            MenuRenderUtils.playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            config.setPassivePassiveMobCooldownMs((long) (1000 + ratio * 29000));
            config.save();
            activeDrag = DragTarget.PASSIVE_PASSIVE_MOB_CD;
            return true;
        }
        rowY += ROW_HEIGHT + 8;

        // "Ranges" header
        rowY += 16;

        // Block Range slider (1 - 5)
        if (mx >= sliderX && mx < sliderX + SLIDER_W && adjustedMy >= rowY && adjustedMy < rowY + SLIDER_H + 8) {
            MenuRenderUtils.playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            config.setPassiveBlockRange(Math.round((1.0 + ratio * 4.0) * 10.0) / 10.0);
            config.save();
            activeDrag = DragTarget.PASSIVE_BLOCK_RANGE;
            return true;
        }
        rowY += ROW_HEIGHT;

        // Mob Range slider (1 - 15)
        if (mx >= sliderX && mx < sliderX + SLIDER_W && adjustedMy >= rowY && adjustedMy < rowY + SLIDER_H + 8) {
            MenuRenderUtils.playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            config.setPassiveMobRange(Math.round((1.0 + ratio * 14.0) * 10.0) / 10.0);
            config.save();
            activeDrag = DragTarget.PASSIVE_MOB_RANGE;
            return true;
        }
        rowY += ROW_HEIGHT + 8;

        // Reset Defaults button
        int resetBtnW = 100;
        int resetBtnH = 20;
        int resetBtnX = x + (w - resetBtnW) / 2;
        if (mx >= resetBtnX && mx < resetBtnX + resetBtnW && adjustedMy >= rowY && adjustedMy < rowY + resetBtnH) {
            MenuRenderUtils.playClickSound();
            config.resetPassiveDefaults();
            return true;
        }

        return false;
    }

    private boolean handleScentFilterClick(int mx, int my, int x, int y, int w) {
        // Sub-filter tabs
        int tabX = x;
        for (ScentSubFilter filter : ScentSubFilter.values()) {
            String labelKey = switch (filter) {
                case BLOCKS -> "config.aromaaffect.scent_values.blocks";
                case FLOWERS -> "config.aromaaffect.scent_values.flowers";
                case BIOMES -> "config.aromaaffect.scent_values.biomes";
                case STRUCTURES -> "config.aromaaffect.scent_values.structures";
                case MOBS -> "config.aromaaffect.scent_values.mobs";
            };
            Component label = Component.translatable(labelKey);
            int tabW = font.width(label) + 16;
            if (mx >= tabX && mx < tabX + tabW && my >= y && my < y + 18) {
                MenuRenderUtils.playClickSound();
                activeScentFilter = filter;
                scentScrollOffset = 0;
                return true;
            }
            tabX += tabW + 4;
        }
        return false;
    }

    @Override
    protected boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeSection == Section.GENERAL) {
            generalScrollOffset -= scrollY * 14;
            return true;
        }
        if (activeSection == Section.PASSIVE) {
            passiveScrollOffset -= scrollY * 14;
            return true;
        }
        if (activeSection == Section.SCENT_VALUES) {
            scentScrollOffset -= scrollY * 14;
            return true;
        }
        if (activeSection == Section.WEBSOCKET) {
            wsScrollOffset -= scrollY * 14;
            return true;
        }
        return false;
    }


    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (capturingKey) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                String keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
                if (keyName == null) {
                    keyName = "KEY_" + keyCode;
                }
                ClientConfig.getInstance().setManualPuffKey(keyName.toUpperCase());
                ClientConfig.getInstance().save();
            }
            MenuRenderUtils.playClickSound();
            capturingKey = false;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            MenuManager.returnToRadialMenu();
            return true;
        }
        return false;
    }
}
