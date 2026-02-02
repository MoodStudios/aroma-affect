package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import com.ovrtechnology.websocket.ConnectionState;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import com.ovrtechnology.websocket.WebSocketMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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
    private enum ScentSubFilter { BLOCKS, FLOWERS, BIOMES, STRUCTURES }

    private Section activeSection = Section.GENERAL;
    private ScentSubFilter activeScentFilter = ScentSubFilter.BLOCKS;

    // Scroll state for scent values list
    private double scentScrollOffset = 0;
    private double wsScrollOffset = 0;

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
    private enum DragTarget { NONE, INTENSITY, COOLDOWN, PASSIVE_COOLDOWN }
    private DragTarget activeDrag = DragTarget.NONE;

    public ConfigScreen() {
        super(Component.translatable("config.aromaaffect.title"));
    }

    @Override
    protected void init() {
        super.init();
        // Load current config values into NoseRenderToggles on screen open
        NoseRenderToggles.setNoseEnabled(ClientConfig.getInstance().isNoseRenderEnabled());
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
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, withAlpha(COL_BG_PANEL, a));
        renderOutline(graphics, panelLeft, panelTop, panelW, panelH, withAlpha(0x88FFFFFF, a));

        // Title bar
        int titleBarH = 24;
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + titleBarH, withAlpha(0xDD222244, a));
        Component title = Component.translatable("config.aromaaffect.title");
        graphics.drawCenteredString(font, title, panelLeft + panelW / 2, panelTop + 8, withAlpha(COL_TEXT, a));

        // Back button (top-left of title bar)
        int backW = 40;
        int backH = titleBarH - 4;
        int backX = panelLeft + 4;
        int backY = panelTop + 2;
        hoveringBack = mouseX >= backX && mouseX < backX + backW && mouseY >= backY && mouseY < backY + backH;
        int backBg = hoveringBack ? withAlpha(COL_HOVER, a) : withAlpha(0x20FFFFFF, a);
        graphics.fill(backX, backY, backX + backW, backY + backH, backBg);
        graphics.drawCenteredString(font, "< Back", backX + backW / 2, backY + (backH - 8) / 2, withAlpha(COL_TEXT, a));

        // Sidebar
        int sidebarLeft = panelLeft;
        int sidebarTop = panelTop + titleBarH;
        int sidebarBottom = panelBottom;
        graphics.fill(sidebarLeft, sidebarTop, sidebarLeft + SIDEBAR_WIDTH, sidebarBottom, withAlpha(COL_BG_SIDEBAR, a));

        // Section tabs
        int tabY = sidebarTop + 8;
        for (Section section : Section.values()) {
            int tabH = 24;
            boolean isActive = section == activeSection;
            boolean isHovering = mouseX >= sidebarLeft && mouseX < sidebarLeft + SIDEBAR_WIDTH
                    && mouseY >= tabY && mouseY < tabY + tabH;

            if (isActive) {
                graphics.fill(sidebarLeft, tabY, sidebarLeft + 3, tabY + tabH, withAlpha(COL_ACCENT, a));
                graphics.fill(sidebarLeft, tabY, sidebarLeft + SIDEBAR_WIDTH, tabY + tabH, withAlpha(0x30FFFFFF, a));
            } else if (isHovering) {
                graphics.fill(sidebarLeft, tabY, sidebarLeft + SIDEBAR_WIDTH, tabY + tabH, withAlpha(COL_HOVER, a));
            }

            String labelKey = switch (section) {
                case GENERAL -> "config.aromaaffect.section.general";
                case PASSIVE -> "config.aromaaffect.section.passive";
                case SCENT_VALUES -> "config.aromaaffect.section.scent_values";
                case WEBSOCKET -> "config.aromaaffect.section.websocket";
            };
            Component label = Component.translatable(labelKey);
            graphics.drawString(font, label, sidebarLeft + 12, tabY + (tabH - 8) / 2, withAlpha(isActive ? COL_TEXT : COL_TEXT_DIM, a));
            tabY += tabH + 2;
        }

        // Content area
        int contentLeft = sidebarLeft + SIDEBAR_WIDTH + CONTENT_PAD;
        int contentTop = sidebarTop + CONTENT_PAD;
        int contentRight = panelRight - CONTENT_PAD;
        int contentW = contentRight - contentLeft;

        switch (activeSection) {
            case GENERAL -> renderGeneralSection(graphics, contentLeft, contentTop, contentW, mouseX, mouseY, a);
            case PASSIVE -> renderPassiveSection(graphics, contentLeft, contentTop, contentW, mouseX, mouseY, a);
            case SCENT_VALUES -> renderScentValuesSection(graphics, contentLeft, contentTop, contentW, panelBottom - CONTENT_PAD - contentTop, mouseX, mouseY, a);
            case WEBSOCKET -> renderWebSocketSection(graphics, contentLeft, contentTop, contentW, panelBottom - CONTENT_PAD - contentTop, mouseX, mouseY, a);
        }
    }

    private void renderGeneralSection(GuiGraphics graphics, int x, int y, int w, int mx, int my, float a) {
        ClientConfig config = ClientConfig.getInstance();
        int rowY = y;

        // Puff Mode selector
        graphics.drawString(font, Component.translatable("config.aromaaffect.puff_mode"), x, rowY + 6, withAlpha(COL_TEXT, a));
        int selX = x + w - SELECTOR_BTN_W * 2 - 2;
        boolean isAutomatic = "automatic".equals(config.getPuffMode());

        // Automatic button
        int autoBg = isAutomatic ? withAlpha(0xFF2A6B2A, a) : withAlpha(0xFF333333, a);
        boolean hoverAuto = mx >= selX && mx < selX + SELECTOR_BTN_W && my >= rowY && my < rowY + SELECTOR_BTN_H;
        if (hoverAuto && !isAutomatic) autoBg = withAlpha(0xFF444444, a);
        graphics.fill(selX, rowY, selX + SELECTOR_BTN_W, rowY + SELECTOR_BTN_H, autoBg);
        graphics.drawCenteredString(font, Component.translatable("config.aromaaffect.puff_mode.automatic"),
                selX + SELECTOR_BTN_W / 2, rowY + 6, withAlpha(COL_TEXT, a));

        // Manual button
        int manSelX = selX + SELECTOR_BTN_W + 2;
        int manBg = !isAutomatic ? withAlpha(0xFF2A6B2A, a) : withAlpha(0xFF333333, a);
        boolean hoverMan = mx >= manSelX && mx < manSelX + SELECTOR_BTN_W && my >= rowY && my < rowY + SELECTOR_BTN_H;
        if (hoverMan && isAutomatic) manBg = withAlpha(0xFF444444, a);
        graphics.fill(manSelX, rowY, manSelX + SELECTOR_BTN_W, rowY + SELECTOR_BTN_H, manBg);
        graphics.drawCenteredString(font, Component.translatable("config.aromaaffect.puff_mode.manual"),
                manSelX + SELECTOR_BTN_W / 2, rowY + 6, withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 4;

        // Manual Puff Key (only visible in manual mode)
        if (!isAutomatic) {
            graphics.drawString(font, Component.translatable("config.aromaaffect.manual_key"), x, rowY + 6, withAlpha(COL_TEXT, a));
            int keyBtnX = x + w - 80;
            int keyBtnW = 80;
            int keyBtnH = 20;
            int keyBg = capturingKey ? withAlpha(0xFF666622, a) : withAlpha(0xFF333333, a);
            boolean hoverKey = mx >= keyBtnX && mx < keyBtnX + keyBtnW && my >= rowY && my < rowY + keyBtnH;
            if (hoverKey && !capturingKey) keyBg = withAlpha(0xFF444444, a);
            graphics.fill(keyBtnX, rowY, keyBtnX + keyBtnW, rowY + keyBtnH, keyBg);
            renderOutline(graphics, keyBtnX, rowY, keyBtnW, keyBtnH, withAlpha(0x88FFFFFF, a));
            String keyText = capturingKey ? Component.translatable("config.aromaaffect.manual_key.press").getString()
                    : config.getManualPuffKey();
            int keyColor = capturingKey ? withAlpha(COL_KEY_CAPTURE, a) : withAlpha(COL_TEXT, a);
            graphics.drawCenteredString(font, keyText, keyBtnX + keyBtnW / 2, rowY + 6, keyColor);
            rowY += ROW_HEIGHT + 4;
        }

        // Global Intensity slider
        graphics.drawString(font, Component.translatable("config.aromaaffect.intensity"), x, rowY + 2, withAlpha(COL_TEXT, a));
        int sliderX = x + w - SLIDER_W - 40;
        renderSlider(graphics, sliderX, rowY, SLIDER_W, (float) config.getGlobalIntensityMultiplier(), 0f, 1f, a);
        int pct = (int) (config.getGlobalIntensityMultiplier() * 100);
        graphics.drawString(font, pct + "%", sliderX + SLIDER_W + 6, rowY + 2, withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 4;

        // Global Cooldown slider
        graphics.drawString(font, Component.translatable("config.aromaaffect.cooldown"), x, rowY + 2, withAlpha(COL_TEXT, a));
        float cooldownSec = config.getGlobalCooldownMs() / 1000f;
        renderSlider(graphics, sliderX, rowY, SLIDER_W, cooldownSec, 1f, 60f, a);
        graphics.drawString(font, String.format("%.0fs", cooldownSec), sliderX + SLIDER_W + 6, rowY + 2, withAlpha(COL_TEXT, a));
        rowY += ROW_HEIGHT + 4;

        // 3D Nose Render toggle
        graphics.drawString(font, Component.translatable("config.aromaaffect.nose_render"), x, rowY + 4, withAlpha(COL_TEXT, a));
        int toggleX = x + w - TOGGLE_W - 30;
        renderTogglePill(graphics, toggleX, rowY + 1, config.isNoseRenderEnabled(), a);
        Component toggleLabel = config.isNoseRenderEnabled()
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, toggleLabel, toggleX + TOGGLE_W + 6, rowY + 4, withAlpha(COL_TEXT_DIM, a));
    }

    private void renderPassiveSection(GuiGraphics graphics, int x, int y, int w, int mx, int my, float a) {
        int rowY = y;

        // Passive Enabled toggle
        graphics.drawString(font, Component.translatable("config.aromaaffect.passive_enabled"), x, rowY + 4, withAlpha(COL_TEXT, a));
        int toggleX = x + w - TOGGLE_W - 30;
        boolean passiveEnabled = PassiveModeManager.isPassiveModeEnabled();
        renderTogglePill(graphics, toggleX, rowY + 1, passiveEnabled, a);
        Component toggleLabel = passiveEnabled
                ? Component.translatable("config.aromaaffect.on")
                : Component.translatable("config.aromaaffect.off");
        graphics.drawString(font, toggleLabel, toggleX + TOGGLE_W + 6, rowY + 4, withAlpha(COL_TEXT_DIM, a));
        rowY += ROW_HEIGHT + 4;

        // Passive Cooldown slider (5s - 120s)
        graphics.drawString(font, Component.translatable("config.aromaaffect.passive_cooldown"), x, rowY + 2, withAlpha(COL_TEXT, a));
        int sliderX = x + w - SLIDER_W - 40;
        // Use the global cooldown as passive cooldown for now (could be separate config)
        float passiveCooldown = ClientConfig.getInstance().getGlobalCooldownMs() / 1000f;
        float clampedCooldown = Mth.clamp(passiveCooldown, 5f, 120f);
        renderSlider(graphics, sliderX, rowY, SLIDER_W, clampedCooldown, 5f, 120f, a);
        graphics.drawString(font, String.format("%.0fs", clampedCooldown), sliderX + SLIDER_W + 6, rowY + 2, withAlpha(COL_TEXT, a));
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
            };
            Component label = Component.translatable(labelKey);
            int tabW = font.width(label) + 16;
            int tabH = 18;
            boolean isActive = filter == activeScentFilter;
            boolean isHover = mx >= tabX && mx < tabX + tabW && my >= y && my < y + tabH;

            int bg = isActive ? withAlpha(COL_ACCENT, a) : (isHover ? withAlpha(COL_HOVER, a) : withAlpha(0x20FFFFFF, a));
            graphics.fill(tabX, y, tabX + tabW, y + tabH, bg);
            graphics.drawCenteredString(font, label, tabX + tabW / 2, y + 5, withAlpha(COL_TEXT, a));
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
        graphics.drawString(font, "Name", x, listTop, withAlpha(COL_ACCENT, a));
        graphics.drawString(font, "Scent", x + w / 3, listTop, withAlpha(COL_ACCENT, a));
        graphics.drawString(font, "Intensity", x + 2 * w / 3, listTop, withAlpha(COL_ACCENT, a));
        listTop += entryH + 2;
        listH -= entryH + 2;

        // Entries (scissor-clipped so they don't overlap headers)
        graphics.enableScissor(x, listTop, x + w, listTop + listH);
        int drawY = listTop - (int) scentScrollOffset;
        for (String[] entry : entries) {
            if (drawY + entryH >= listTop && drawY < listTop + listH) {
                int textColor = withAlpha(COL_TEXT_DIM, a);
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
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarH, withAlpha(0x88FFFFFF, a));
        }
    }

    private void renderWebSocketSection(GuiGraphics graphics, int x, int y, int w, int h, int mx, int my, float a) {
        OvrWebSocketClient client = OvrWebSocketClient.getInstance();
        ConnectionState connState = client.getState();

        // --- Status Card ---
        int cardH = 55;
        graphics.fill(x, y, x + w, y + cardH, withAlpha(0x30FFFFFF, a));
        renderOutline(graphics, x, y, w, cardH, withAlpha(0x44FFFFFF, a));

        int cardPad = 8;
        int rowY = y + cardPad;

        // Row 1: colored dot + status text
        int dotColor = switch (connState) {
            case CONNECTED -> 0xFF44FF44;
            case CONNECTING, RECONNECTING -> 0xFFFFCC44;
            case DISCONNECTED, CONNECTION_FAILED -> 0xFFFF4444;
        };
        graphics.fill(x + cardPad, rowY + 1, x + cardPad + 8, rowY + 9, withAlpha(dotColor, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.status").append(": " + connState.getDisplayName()),
                x + cardPad + 12, rowY, withAlpha(COL_TEXT, a));
        rowY += 14;

        // Row 2: server URI
        String uri = client.getConfig().getUri();
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.server").append(": " + uri),
                x + cardPad, rowY, withAlpha(COL_TEXT_DIM, a));
        rowY += 14;

        // Row 3: reconnect attempts (only if > 0)
        int attempts = client.getReconnectAttempts();
        if (attempts > 0) {
            graphics.drawString(font, Component.translatable("config.aromaaffect.ws.attempts").append(": " + attempts),
                    x + cardPad, rowY, withAlpha(COL_TEXT_DIM, a));
        }

        // --- Message Log ---
        int logTop = y + cardH + 10;
        int logH = h - cardH - 10;

        // Header
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.messages"), x, logTop, withAlpha(COL_ACCENT, a));
        logTop += 14;
        logH -= 14;

        // Column headers
        int colDir = x;
        int colType = x + 20;
        int colContent = x + 70;
        int colTime = x + w - 50;
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.dir"), colDir, logTop, withAlpha(COL_ACCENT, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.type"), colType, logTop, withAlpha(COL_ACCENT, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.content"), colContent, logTop, withAlpha(COL_ACCENT, a));
        graphics.drawString(font, Component.translatable("config.aromaaffect.ws.time"), colTime, logTop, withAlpha(COL_ACCENT, a));
        logTop += 12;
        logH -= 12;

        List<WebSocketMessage> messages = client.getMessageHistory();

        if (messages.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("config.aromaaffect.ws.no_messages"),
                    x + w / 2, logTop + logH / 2 - 4, withAlpha(COL_TEXT_DIM, a));
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
                    graphics.drawString(font, "\u2192", colDir, drawY, withAlpha(0xFF44FF44, a));
                } else {
                    graphics.drawString(font, "\u2190", colDir, drawY, withAlpha(0xFFBB88FF, a));
                }

                // Type
                graphics.drawString(font, msg.getType(), colType, drawY, withAlpha(COL_TEXT_DIM, a));

                // Content (truncated)
                String payload = msg.getPayload();
                if (font.width(payload) > maxContentW) {
                    while (payload.length() > 3 && font.width(payload + "...") > maxContentW) {
                        payload = payload.substring(0, payload.length() - 1);
                    }
                    payload = payload + "...";
                }
                graphics.drawString(font, payload, colContent, drawY, withAlpha(COL_TEXT_DIM, a));

                // Time
                String time = TIME_FMT.format(msg.getTimestamp());
                graphics.drawString(font, time, colTime, drawY, withAlpha(COL_TEXT_DIM, a));
            }
            drawY += entryH;
        }
        graphics.disableScissor();

        // Scrollbar
        if (totalH > logH && logH > 0) {
            int scrollBarX = x + w - 4;
            int scrollBarH = Math.max(10, (int) ((float) logH / totalH * logH));
            int scrollBarY = logTop + (int) ((float) wsScrollOffset / maxScroll * (logH - scrollBarH));
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarH, withAlpha(0x88FFFFFF, a));
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
        int bgColor = on ? withAlpha(COL_GREEN, a) : withAlpha(COL_GRAY, a);
        graphics.fill(x, y, x + TOGGLE_W, y + TOGGLE_H, bgColor);
        // Rounded effect using border
        renderOutline(graphics, x, y, TOGGLE_W, TOGGLE_H, withAlpha(0x44FFFFFF, a));
        // Circle thumb
        int circleSize = TOGGLE_H - 4;
        int circleX = on ? x + TOGGLE_W - circleSize - 2 : x + 2;
        int circleY = y + 2;
        graphics.fill(circleX, circleY, circleX + circleSize, circleY + circleSize, withAlpha(COL_TEXT, a));
    }

    private void renderSlider(GuiGraphics graphics, int x, int y, int w, float value, float min, float max, float a) {
        int trackY = y + 4;
        // Track background
        graphics.fill(x, trackY, x + w, trackY + SLIDER_H, withAlpha(0xFF333333, a));
        // Filled portion
        float ratio = (value - min) / (max - min);
        ratio = Mth.clamp(ratio, 0f, 1f);
        int filledW = (int) (w * ratio);
        graphics.fill(x, trackY, x + filledW, trackY + SLIDER_H, withAlpha(COL_ACCENT, a));
        // Handle
        int handleX = x + filledW - 4;
        graphics.fill(handleX, trackY - 2, handleX + 8, trackY + SLIDER_H + 2, withAlpha(COL_TEXT, a));
    }

    private static void renderOutline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static int withAlpha(int argb, float alphaMul) {
        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        int na = Mth.clamp((int) (a * alphaMul), 0, 255);
        return (na << 24) | rgb;
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
            case PASSIVE_COOLDOWN -> {
                long ms = (long) (5000 + ratio * 115000);
                config.setGlobalCooldownMs(ms);
                config.save();
            }
        }
    }

    // --- Sounds ---

    private static void playClickSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.0f));
        }
    }

    private static void playToggleSound(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            float pitch = on ? 1.3f : 0.9f;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, pitch));
        }
    }

    private static void playSliderSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.5f));
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
            playClickSound();
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
                playClickSound();
                activeSection = section;
                scentScrollOffset = 0;
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
        int rowY = y;

        // Puff Mode selector
        int selX = x + w - SELECTOR_BTN_W * 2 - 2;
        if (my >= rowY && my < rowY + SELECTOR_BTN_H) {
            if (mx >= selX && mx < selX + SELECTOR_BTN_W) {
                playClickSound();
                config.setPuffMode("automatic");
                config.save();
                capturingKey = false;
                return true;
            }
            int manSelX = selX + SELECTOR_BTN_W + 2;
            if (mx >= manSelX && mx < manSelX + SELECTOR_BTN_W) {
                playClickSound();
                config.setPuffMode("manual");
                config.save();
                return true;
            }
        }
        rowY += ROW_HEIGHT + 4;

        // Manual key (only if manual mode)
        if (!"automatic".equals(config.getPuffMode())) {
            int keyBtnX = x + w - 80;
            if (mx >= keyBtnX && mx < keyBtnX + 80 && my >= rowY && my < rowY + 20) {
                playClickSound();
                capturingKey = true;
                return true;
            }
            rowY += ROW_HEIGHT + 4;
        }

        // Intensity slider
        int sliderX = x + w - SLIDER_W - 40;
        if (mx >= sliderX && mx < sliderX + SLIDER_W && my >= rowY && my < rowY + SLIDER_H + 8) {
            playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            config.setGlobalIntensityMultiplier(Math.round(ratio * 100.0) / 100.0);
            config.save();
            activeDrag = DragTarget.INTENSITY;
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Cooldown slider
        if (mx >= sliderX && mx < sliderX + SLIDER_W && my >= rowY && my < rowY + SLIDER_H + 8) {
            playSliderSound();
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
        if (mx >= toggleX && mx < toggleX + TOGGLE_W && my >= rowY && my < rowY + TOGGLE_H + 2) {
            config.setNoseRenderEnabled(!config.isNoseRenderEnabled());
            NoseRenderToggles.setNoseEnabled(config.isNoseRenderEnabled());
            playToggleSound(config.isNoseRenderEnabled());
            config.save();
            return true;
        }

        return false;
    }

    private boolean handlePassiveClick(int mx, int my, int x, int y, int w) {
        int rowY = y;

        // Passive toggle
        int toggleX = x + w - TOGGLE_W - 30;
        if (mx >= toggleX && mx < toggleX + TOGGLE_W && my >= rowY && my < rowY + TOGGLE_H + 2) {
            PassiveModeManager.togglePassiveMode();
            playToggleSound(PassiveModeManager.isPassiveModeEnabled());
            return true;
        }
        rowY += ROW_HEIGHT + 4;

        // Passive cooldown slider
        int sliderX = x + w - SLIDER_W - 40;
        if (mx >= sliderX && mx < sliderX + SLIDER_W && my >= rowY && my < rowY + SLIDER_H + 8) {
            playSliderSound();
            float ratio = Mth.clamp((float) (mx - sliderX) / SLIDER_W, 0f, 1f);
            long ms = (long) (5000 + ratio * 115000); // 5s to 120s
            ClientConfig.getInstance().setGlobalCooldownMs(ms);
            ClientConfig.getInstance().save();
            activeDrag = DragTarget.PASSIVE_COOLDOWN;
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
            };
            Component label = Component.translatable(labelKey);
            int tabW = font.width(label) + 16;
            if (mx >= tabX && mx < tabX + tabW && my >= y && my < y + 18) {
                playClickSound();
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
            playClickSound();
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
