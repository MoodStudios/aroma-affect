package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import com.ovrtechnology.trigger.PassiveModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Radial menu screen for selecting Aroma Affect modules/categories.
 *
 * <p>Design-focused implementation: renders a circular 4-slice radial menu, with an indicator pointing
 * to the currently selected slice (based on mouse angle). The renderer is data-driven so additional
 * slices can be added in the future without changing the rendering logic.</p>
 */
public class RadialMenuScreen extends BaseMenuScreen {

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    /**
     * Starting angle for slice 0 (in radians). -PI aligns slice borders to cardinal axes for 4 slices.
     */
    private static final float START_ANGLE_RAD = (float) -Math.PI;

    private static final int MIN_OUTER_RADIUS = 96;
    private static final int MAX_OUTER_RADIUS = 180;
    private static final float INNER_RADIUS_RATIO = 0.58f;

    private static final float BORDER_THICKNESS_PX = 2.0f;
    private static final float SEPARATOR_THICKNESS_PX = 2.0f;
    private static final float CENTER_ARROW_THICKNESS_PX = 2.5f;

    private static final int COLOR_RING_BASE = 0x66FFFFFF;
    private static final int COLOR_RING_SELECTED = 0x809A7CFF;
    private static final int COLOR_RING_BORDER = 0x88FFFFFF;
    private static final int COLOR_RING_SEPARATOR = 0x80A88CFF;
    private static final int COLOR_INDICATOR = 0xEEFFFFFF;

    private static final TextureSetup NO_TEXTURE = TextureSetup.noTexture();

    private static final Field GUI_RENDER_STATE_FIELD = findGuiRenderStateField();

    private final List<RadialEntry> entries = new ArrayList<>();
    private float[] selectionProgress = new float[0];

    /**
     * Cache of grayscale texture variants keyed by original ResourceLocation.
     * Generated once on first use via {@link #getGrayscaleIcon(ResourceLocation)}.
     */
    private static final Map<ResourceLocation, ResourceLocation> GRAYSCALE_CACHE = new HashMap<>();

    /**
     * Selected slice index (computed during render based on mouse position).
     */
    private int selectedIndex = -1;

    /**
     * Previously hovered slice index, used to trigger hover sound on change.
     */
    private int previousHoverIndex = -1;

    /**
     * Cached locked state per slot, recomputed each frame in renderEntryIcons.
     */
    private boolean[] cachedLockedSlots = new boolean[0];

    /**
     * Per-slot shake animation timer (ticks remaining). Set when clicking a locked slot.
     */
    private float[] shakeTimers = new float[0];

    private static final float SHAKE_DURATION = 8.0f;
    private static final float SHAKE_AMPLITUDE = 3.0f;

    /**
     * Current rotation angle of the center arrow (radians). Smoothly interpolated toward the target.
     * Default points up (-PI/2 in screen space where +Y is down).
     */
    private float arrowAngle = (float) (-Math.PI / 2.0);

    public RadialMenuScreen() {
        super(Component.translatable("menu.aromaaffect.radial.title"));
        initializeEntries();
    }

    // Texture locations for radial menu icons
    private static final ResourceLocation ICON_STRUCTURES = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_structures.png");
    private static final ResourceLocation ICON_BIOMES = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_biomes.png");
    private static final ResourceLocation ICON_BLOCKS = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_blocks.png");
    private static final ResourceLocation ICON_FLOWERS = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_flowers.png");
    private static final ResourceLocation ICON_CONFIG = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_config.png");

    private static final ResourceLocation ICON_PASSIVE = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_passive.png");
    private static final ResourceLocation ICON_CENTER_LOGO = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/ovr_isologo_part1.png");
    private static final ResourceLocation ICON_CENTER_ARROW = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/ovr_isologo_part2.png");

    // Icon display size (will be scaled from high-res textures)
    private static final int ICON_DISPLAY_SIZE = 32;
    private static final int CORNER_ICON_SIZE = 28;
    private static final int CORNER_BUTTON_PADDING = 12;
    // Source texture resolution for the high-res icons (they will be scaled down)
    private static final int ICON_TEXTURE_SIZE = 64;

    private void initializeEntries() {
        entries.clear();

        // 4-slice layout matching the design:
        // 0: structures (top-left), 1: biomes (top-right), 2: blocks (bottom-right), 3: flowers (bottom-left)
        entries.add(new RadialEntry(
                "structures",
                Component.translatable("menu.aromaaffect.category.structures"),
                Component.translatable("menu.aromaaffect.category.structures.description"),
                ICON_STRUCTURES,
                () -> MenuManager.openStructuresMenu()
        ));

        entries.add(new RadialEntry(
                "biomes",
                Component.translatable("menu.aromaaffect.category.biomes"),
                Component.translatable("menu.aromaaffect.category.biomes.description"),
                ICON_BIOMES,
                () -> MenuManager.openBiomesMenu()
        ));

        entries.add(new RadialEntry(
                "blocks",
                Component.translatable("menu.aromaaffect.category.blocks"),
                Component.translatable("menu.aromaaffect.category.blocks.description"),
                ICON_BLOCKS,
                () -> MenuManager.openBlocksMenu()
        ));

        // 4th slice: Flowers/Flora
        entries.add(new RadialEntry(
                "flowers",
                Component.translatable("menu.aromaaffect.category.flowers"),
                Component.translatable("menu.aromaaffect.category.flowers.description"),
                ICON_FLOWERS,
                () -> MenuManager.openFlowersMenu()
        ));
    }

    @Override
    protected void init() {
        super.init();
        selectionProgress = new float[entries.size()];
        shakeTimers = new float[entries.size()];
        cachedLockedSlots = new boolean[entries.size()];
        selectedIndex = -1;
        previousHoverIndex = -1;
    }

    @Override
    public void tick() {
        super.tick();

        if (selectionProgress.length == 0) {
            return;
        }

        for (int i = 0; i < selectionProgress.length; i++) {
            if (i == selectedIndex) {
                selectionProgress[i] = Math.min(1.0f, selectionProgress[i] + 0.25f);
            } else {
                selectionProgress[i] = Math.max(0.0f, selectionProgress[i] - 0.20f);
            }

            // Decay shake timers
            if (shakeTimers[i] > 0) {
                shakeTimers[i] = Math.max(0, shakeTimers[i] - 1.0f);
            }
        }

        // Play hover sound when entering a new slice
        if (selectedIndex != previousHoverIndex) {
            if (selectedIndex >= 0 && selectedIndex < entries.size()) {
                MenuRenderUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.5f);
            }
            previousHoverIndex = selectedIndex;
        }

        // Shop button glow animation
        shopGlowPhase += 0.15f;

        // Smoothly rotate arrow toward selected segment center, or back to default (up)
        float targetAngle;
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            float segmentAngle = TWO_PI / entries.size();
            targetAngle = START_ANGLE_RAD + (selectedIndex + 0.5f) * segmentAngle;
        } else {
            targetAngle = (float) (-Math.PI / 2.0);
        }
        // Normalize the difference to [-PI, PI] for shortest-path rotation
        float diff = targetAngle - arrowAngle;
        diff = (float) ((diff + Math.PI) % TWO_PI - Math.PI);
        arrowAngle += diff * 0.3f;
    }

    // Track hover states for corner buttons
    private boolean isHoveringPassiveToggle = false;
    private boolean isHoveringConfigGear = false;
    private boolean isHoveringGuide = false;
    private boolean isHoveringShop = false;
    private boolean isHoveringHistory = false;
    private boolean isHoveringPanelStop = false;
    private boolean isHoveringPanelTeleport = false;

    // Shop button animation
    private float shopGlowPhase = 0f;

    /** Bounds of the stop button rendered below the tracking panel. */
    private int panelStopX, panelStopY, panelStopW, panelStopH;

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, float animationProgress) {
        if (entries.isEmpty()) {
            return;
        }

        int centerX = width / 2;
        int centerY = height / 2;

        int outerRadius = computeOuterRadiusPx(width, height);
        float innerRadius = outerRadius * INNER_RADIUS_RATIO;

        selectedIndex = computeSelectedIndex(mouseX, mouseY, centerX, centerY, innerRadius, outerRadius, entries.size());

        submitRadialRenderState(graphics, centerX, centerY, innerRadius, outerRadius, animationProgress);
        renderEntryIcons(graphics, centerX, centerY, innerRadius, outerRadius, animationProgress);
        renderCenterLogo(graphics, centerX, centerY, innerRadius, animationProgress);
        renderSelectionText(graphics, centerX, centerY, outerRadius, animationProgress);

        // Render corner buttons (top-left row)
        renderCornerButtons(graphics, mouseX, mouseY, animationProgress);

        // Render active tracking info panel
        renderTrackingPanel(graphics, mouseX, mouseY, animationProgress);
    }

    /**
     * Renders the corner buttons (top-left row: passive toggle, gear, guide, shop).
     */
    private void renderCornerButtons(GuiGraphics graphics, int mouseX, int mouseY, float animationProgress) {
        float appear = Mth.clamp((animationProgress - 0.3f) / 0.7f, 0.0f, 1.0f);
        if (appear <= 0.0f) {
            return;
        }

        // Top-left: Passive mode toggle pill + Gear config button (side by side)
        boolean isPassiveEnabled = PassiveModeManager.isPassiveModeEnabled();
        int toggleW = 36;
        int toggleH = 18;
        int toggleX = CORNER_BUTTON_PADDING;
        int toggleY = CORNER_BUTTON_PADDING;

        isHoveringPassiveToggle = isInBounds(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH);

        // Toggle pill background
        int toggleBg = isPassiveEnabled
                ? MenuRenderUtils.withAlpha(isHoveringPassiveToggle ? 0xDDAA8FFF : 0xCC9A7CFF, appear)
                : MenuRenderUtils.withAlpha(isHoveringPassiveToggle ? 0xDD777777 : 0xCC555555, appear);
        graphics.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, toggleBg);
        MenuRenderUtils.renderOutline(graphics, toggleX, toggleY, toggleW, toggleH, MenuRenderUtils.withAlpha(0x44FFFFFF, appear));

        // Toggle circle thumb
        int circleSize = toggleH - 4;
        int circleX = isPassiveEnabled ? toggleX + toggleW - circleSize - 2 : toggleX + 2;
        int circleY = toggleY + 2;
        graphics.fill(circleX, circleY, circleX + circleSize, circleY + circleSize, MenuRenderUtils.withAlpha(0xFFFFFFFF, appear));

        // Gear config button (right of toggle, same height as toggle)
        int gearX = toggleX + toggleW + 4;
        int gearY = CORNER_BUTTON_PADDING;
        int gearBtnSize = toggleH; // match toggle height for consistent look

        isHoveringConfigGear = isInBounds(mouseX, mouseY, gearX, gearY, gearBtnSize, gearBtnSize);

        int gearBg = isHoveringConfigGear
                ? MenuRenderUtils.withAlpha(COLOR_RING_SELECTED, appear)
                : MenuRenderUtils.withAlpha(0x40FFFFFF, appear);
        graphics.fill(gearX, gearY, gearX + gearBtnSize, gearY + gearBtnSize, gearBg);
        MenuRenderUtils.renderOutline(graphics, gearX, gearY, gearBtnSize, gearBtnSize, MenuRenderUtils.withAlpha(COLOR_RING_BORDER, appear));

        // Render gear icon scaled to fit inside the button with 2px padding
        int gearIconSize = gearBtnSize - 4;
        int gearIconX = gearX + 2;
        int gearIconY = gearY + 2;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ICON_CONFIG,
                gearIconX, gearIconY,
                0.0f, 0.0f,
                gearIconSize, gearIconSize,
                gearIconSize, gearIconSize
        );

        // Guide button (right of gear, same height)
        int guideX = gearX + gearBtnSize + 4;
        int guideY = CORNER_BUTTON_PADDING;
        int guideBtnSize = toggleH;

        isHoveringGuide = isInBounds(mouseX, mouseY, guideX, guideY, guideBtnSize, guideBtnSize);

        int guideBg = isHoveringGuide
                ? MenuRenderUtils.withAlpha(COLOR_RING_SELECTED, appear)
                : MenuRenderUtils.withAlpha(0x40FFFFFF, appear);
        graphics.fill(guideX, guideY, guideX + guideBtnSize, guideY + guideBtnSize, guideBg);
        MenuRenderUtils.renderOutline(graphics, guideX, guideY, guideBtnSize, guideBtnSize, MenuRenderUtils.withAlpha(COLOR_RING_BORDER, appear));

        // Draw an open book icon procedurally
        int bx = guideX + guideBtnSize / 2;
        int by = guideY + guideBtnSize / 2;
        int coverColor = MenuRenderUtils.withAlpha(0xFFCCA654, appear);  // Warm brown cover
        int pageColor = MenuRenderUtils.withAlpha(0xFFF5ECD7, appear);   // Cream pages
        int lineColor = MenuRenderUtils.withAlpha(0xFFAA9060, appear);   // Subtle text lines
        int spineColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, appear);   // White spine highlight
        // Left cover (slightly larger than page for border effect)
        graphics.fill(bx - 7, by - 5, bx - 1, by + 5, coverColor);
        // Right cover
        graphics.fill(bx + 1, by - 5, bx + 7, by + 5, coverColor);
        // Left page (inset from cover)
        graphics.fill(bx - 6, by - 4, bx - 1, by + 4, pageColor);
        // Right page
        graphics.fill(bx + 1, by - 4, bx + 6, by + 4, pageColor);
        // Spine highlight
        graphics.fill(bx - 1, by - 5, bx + 1, by + 5, spineColor);
        // Text lines on left page
        graphics.fill(bx - 5, by - 3, bx - 2, by - 2, lineColor);
        graphics.fill(bx - 5, by - 1, bx - 2, by, lineColor);
        graphics.fill(bx - 5, by + 1, bx - 3, by + 2, lineColor);
        // Text lines on right page
        graphics.fill(bx + 2, by - 3, bx + 5, by - 2, lineColor);
        graphics.fill(bx + 2, by - 1, bx + 5, by, lineColor);
        graphics.fill(bx + 2, by + 1, bx + 4, by + 2, lineColor);

        // Shop button (right of guide, same height)
        int shopX = guideX + guideBtnSize + 4;
        int shopY = CORNER_BUTTON_PADDING;
        int shopBtnSize = toggleH;

        isHoveringShop = isInBounds(mouseX, mouseY, shopX, shopY, shopBtnSize, shopBtnSize);

        // Animated glow behind shop button
        float glowPulse = (float) (0.4f + 0.6f * Math.abs(Math.sin(shopGlowPhase)));
        int glowA = (int) (50 * appear * glowPulse);
        int glowColor = (glowA << 24) | 0x44DD44;
        graphics.fill(shopX - 2, shopY - 2, shopX + shopBtnSize + 2, shopY + shopBtnSize + 2, glowColor);

        int shopBg = isHoveringShop
                ? MenuRenderUtils.withAlpha(0xCC44BB44, appear)
                : MenuRenderUtils.withAlpha(0x8833AA33, appear);
        graphics.fill(shopX, shopY, shopX + shopBtnSize, shopY + shopBtnSize, shopBg);
        MenuRenderUtils.renderOutline(graphics, shopX, shopY, shopBtnSize, shopBtnSize, MenuRenderUtils.withAlpha(0x8844FF44, appear));

        // Draw a cart icon procedurally (small basket shape)
        int cx = shopX + shopBtnSize / 2;
        int cy = shopY + shopBtnSize / 2;
        int iconColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, appear);
        // Cart body
        graphics.fill(cx - 5, cy - 2, cx + 5, cy + 3, iconColor);
        // Cart bottom (narrower)
        graphics.fill(cx - 4, cy + 3, cx + 4, cy + 4, iconColor);
        // Handle
        graphics.fill(cx - 6, cy - 4, cx - 5, cy - 1, iconColor);
        graphics.fill(cx - 6, cy - 4, cx - 2, cy - 3, iconColor);
        // Wheels
        graphics.fill(cx - 3, cy + 5, cx - 1, cy + 7, iconColor);
        graphics.fill(cx + 1, cy + 5, cx + 3, cy + 7, iconColor);

        // History button (right of shop, same height)
        int histX = shopX + shopBtnSize + 4;
        int histY = CORNER_BUTTON_PADDING;
        int histBtnSize = toggleH;

        isHoveringHistory = isInBounds(mouseX, mouseY, histX, histY, histBtnSize, histBtnSize);

        int histBg = isHoveringHistory
                ? MenuRenderUtils.withAlpha(0xCC6688CC, appear)
                : MenuRenderUtils.withAlpha(0x804466AA, appear);
        graphics.fill(histX, histY, histX + histBtnSize, histY + histBtnSize, histBg);
        MenuRenderUtils.renderOutline(graphics, histX, histY, histBtnSize, histBtnSize,
                MenuRenderUtils.withAlpha(0x886B8CFF, appear));

        // Draw a clock icon procedurally (circle + hands)
        int hx = histX + histBtnSize / 2;
        int hy = histY + histBtnSize / 2;
        int clockColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, appear);
        // Circle outline (square approximation)
        graphics.fill(hx - 5, hy - 6, hx + 5, hy - 5, clockColor); // top
        graphics.fill(hx - 5, hy + 5, hx + 5, hy + 6, clockColor); // bottom
        graphics.fill(hx - 6, hy - 5, hx - 5, hy + 5, clockColor); // left
        graphics.fill(hx + 5, hy - 5, hx + 6, hy + 5, clockColor); // right
        // Clock hands — minute (up) + hour (right)
        graphics.fill(hx, hy - 4, hx + 1, hy + 1, clockColor);  // minute hand (up)
        graphics.fill(hx, hy, hx + 3, hy + 1, clockColor);       // hour hand (right)

        // Tooltips
        int tooltipX = histX + histBtnSize + 8;
        if (isHoveringPassiveToggle) {
            Component passiveLabel = isPassiveEnabled
                    ? Component.translatable("menu.aromaaffect.button.passive.on")
                    : Component.translatable("menu.aromaaffect.button.passive.off");
            graphics.drawString(font, passiveLabel,
                    tooltipX,
                    toggleY + toggleH / 2 - 4,
                    MenuRenderUtils.withAlpha(0xFFFFFFFF, appear));
        }
        if (isHoveringConfigGear) {
            graphics.drawString(font, Component.translatable("config.aromaaffect.button.settings"),
                    tooltipX,
                    gearY + gearBtnSize / 2 - 4,
                    MenuRenderUtils.withAlpha(0xFFFFFFFF, appear));
        }
        if (isHoveringGuide) {
            graphics.drawString(font, Component.translatable("guide.aromaaffect.button"),
                    tooltipX,
                    guideY + guideBtnSize / 2 - 4,
                    MenuRenderUtils.withAlpha(0xFFFFFFFF, appear));
        }
        if (isHoveringShop) {
            graphics.drawString(font, Component.translatable("shop.aromaaffect.button"),
                    tooltipX,
                    shopY + shopBtnSize / 2 - 4,
                    MenuRenderUtils.withAlpha(0xFFFFFFFF, appear));
        }
        if (isHoveringHistory) {
            graphics.drawString(font, Component.translatable("history.aromaaffect.button"),
                    tooltipX,
                    histY + histBtnSize / 2 - 4,
                    MenuRenderUtils.withAlpha(0xFFFFFFFF, appear));
        }
    }

    private static boolean isInBounds(double x, double y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        // Check corner buttons first
        if (isHoveringPassiveToggle) {
            AromaAffect.LOGGER.debug("Passive mode toggle clicked");

            // Check if trying to enable passive mode while tracking is active
            if (!PassiveModeManager.isPassiveModeEnabled() && ActiveTrackingState.isTracking()) {
                showErrorNotification(Component.translatable("message.aromaaffect.passive.tracking_active"));
                MenuRenderUtils.playSound(SoundEvents.VILLAGER_NO, 0.5f, 1.0f);
                AromaAffect.LOGGER.info("Cannot enable passive mode while tracking is active");
                return true;
            }

            PassiveModeManager.togglePassiveMode();
            MenuRenderUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, PassiveModeManager.isPassiveModeEnabled() ? 1.3f : 0.9f);
            return true;
        }
        if (isHoveringConfigGear) {
            AromaAffect.LOGGER.debug("Config gear button clicked");
            MenuRenderUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.0f);
            MenuManager.openConfigMenu();
            return true;
        }
        if (isHoveringGuide) {
            AromaAffect.LOGGER.debug("Guide button clicked");
            MenuRenderUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.0f);
            MenuManager.openGuide();
            return true;
        }
        if (isHoveringShop) {
            AromaAffect.LOGGER.debug("Shop button clicked");
            MenuRenderUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.2f);
            MenuManager.openShopMenu();
            return true;
        }
        if (isHoveringHistory) {
            AromaAffect.LOGGER.debug("History button clicked");
            MenuRenderUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.0f);
            MenuManager.openHistoryMenu();
            return true;
        }
        if (isHoveringPanelStop) {
            AromaAffect.LOGGER.debug("Panel stop button clicked");
            executeStopPath();
            return true;
        }
        if (isHoveringPanelTeleport) {
            AromaAffect.LOGGER.debug("Panel teleport button clicked");
            executeTeleport();
            return true;
        }

        if (entries.isEmpty()) {
            return false;
        }

        int centerX = width / 2;
        int centerY = height / 2;
        int outerRadius = computeOuterRadiusPx(width, height);
        float innerRadius = outerRadius * INNER_RADIUS_RATIO;

        int index = computeSelectedIndex(mouseX, mouseY, centerX, centerY, innerRadius, outerRadius, entries.size());
        if (index < 0 || index >= entries.size()) {
            return false;
        }

        // Block interaction on locked slices
        if (index < cachedLockedSlots.length && cachedLockedSlots[index]) {
            shakeTimers[index] = SHAKE_DURATION;
            MenuRenderUtils.playSound(SoundEvents.VILLAGER_NO, 0.5f, 1.2f);
            return true;
        }

        MenuRenderUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.0f);
        onEntrySelected(entries.get(index));
        return true;
    }

    /**
     * Executes the stop path command and closes the menu.
     */
    private void executeStopPath() {
        // Clear client-side tracking state
        ActiveTrackingState.clear();

        // Send the stop path command to the server
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand("aromatest path stop");
            AromaAffect.LOGGER.debug("Sent stop path command");
        }

        // Close the menu
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    /**
     * Teleports the player to the active tracking destination via /tp command.
     * Only works in creative mode.
     */
    private void executeTeleport() {
        BlockPos dest = ActiveTrackingState.getDestination();
        if (dest == null) return;

        if (Minecraft.getInstance().getConnection() != null) {
            String cmd = String.format("tp @s %d %d %d", dest.getX(), dest.getY() + 1, dest.getZ());
            Minecraft.getInstance().getConnection().sendCommand(cmd);
            AromaAffect.LOGGER.debug("Sent teleport command to {}", dest);
        }

        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == GLFW.GLFW_KEY_R) { // Escape or radial toggle key
            onClose();
            return true;
        }
        return false;
    }

    private void onEntrySelected(RadialEntry entry) {
        AromaAffect.LOGGER.debug("Radial menu selected: {}", entry.id);
        entry.onSelect.run();
    }

    private void renderEntryIcons(GuiGraphics graphics, int centerX, int centerY, float innerRadius, int outerRadius, float animationProgress) {
        float appear = Mth.clamp((animationProgress - 0.15f) / 0.85f, 0.0f, 1.0f);
        if (appear <= 0.0f) {
            return;
        }

        // Determine which categories are locked based on equipped nose abilities
        boolean[] lockedSlots = computeLockedSlots();
        cachedLockedSlots = lockedSlots;

        float iconRadius = (innerRadius + outerRadius) * 0.5f;
        float segmentAngle = TWO_PI / entries.size();

        for (int i = 0; i < entries.size(); i++) {
            RadialEntry entry = entries.get(i);
            float angle = START_ANGLE_RAD + (i + 0.5f) * segmentAngle;

            float x = centerX + (float) Math.cos(angle) * iconRadius;
            float y = centerY + (float) Math.sin(angle) * iconRadius;

            // Apply shake offset when a locked slot was clicked
            if (shakeTimers[i] > 0) {
                float shakeProgress = shakeTimers[i] / SHAKE_DURATION;
                float shakeOffset = (float) Math.sin(shakeTimers[i] * 2.5f) * SHAKE_AMPLITUDE * shakeProgress;
                x += shakeOffset;
            }

            // Scale factor for selection feedback and appear animation
            float scale = (1.0f + 0.2f * selectionProgress[i]) * appear;
            int iconSize = (int) (ICON_DISPLAY_SIZE * scale);
            int halfSize = iconSize / 2;

            int drawX = (int)(x - halfSize);
            int drawY = (int)(y - halfSize);

            ResourceLocation icon = lockedSlots[i] ? getGrayscaleIcon(entry.icon()) : entry.icon();

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    icon,
                    drawX, drawY,
                    0.0f, 0.0f,
                    iconSize, iconSize,
                    iconSize, iconSize
            );

            // Draw lock symbol on top of locked icons
            if (lockedSlots[i]) {
                renderLockIcon(graphics, drawX, drawY, iconSize);
            }

            // Draw tracking indicator if this category has an active track (only in TRACKING state)
            if (!lockedSlots[i] && ActiveTrackingState.isActivelyTracking()) {
                String catId = ActiveTrackingState.getCategoryId();
                if (catId != null && catId.equals(entry.id())) {
                    renderTrackingIndicator(graphics, drawX, drawY, iconSize, appear);
                }
            }
        }
    }

    /**
     * Renders a small lock icon at the bottom-right corner of the given icon area.
     * Drawn procedurally using fill calls (shackle arc + body rectangle).
     */
    private static void renderLockIcon(GuiGraphics graphics, int iconX, int iconY, int iconSize) {
        // Position lock at bottom-right of icon
        int lockW = 10;
        int lockH = 8;
        int lx = iconX + iconSize - lockW - 1;
        int ly = iconY + iconSize - lockH - 1;

        // Lock body (filled rectangle)
        graphics.fill(lx, ly + 3, lx + lockW, ly + lockH, 0xDD444444);
        // Lock body border
        graphics.fill(lx, ly + 3, lx + lockW, ly + 4, 0xDD888888);
        graphics.fill(lx, ly + lockH - 1, lx + lockW, ly + lockH, 0xDD888888);
        graphics.fill(lx, ly + 3, lx + 1, ly + lockH, 0xDD888888);
        graphics.fill(lx + lockW - 1, ly + 3, lx + lockW, ly + lockH, 0xDD888888);

        // Shackle (U-shape on top)
        int shackleL = lx + 2;
        int shackleR = lx + lockW - 2;
        graphics.fill(shackleL, ly, shackleL + 1, ly + 4, 0xDD888888);
        graphics.fill(shackleR - 1, ly, shackleR, ly + 4, 0xDD888888);
        graphics.fill(shackleL, ly, shackleR, ly + 1, 0xDD888888);
    }

    /**
     * Renders an animated "online" indicator at the top-right corner of an icon.
     * Smooth pulsing glow around a solid green dot — pure opacity animation,
     * no moving geometry, so no integer-rounding jitter.
     */
    private static void renderTrackingIndicator(GuiGraphics graphics, int iconX, int iconY, int iconSize, float alpha) {
        int dotSize = 6;
        int dx = iconX + iconSize - dotSize;
        int dy = iconY - 1;

        // Continuous time in seconds for smooth per-frame interpolation
        double t = System.nanoTime() / 1_000_000_000.0;

        // Soft glow layers (fixed size, only opacity animates — perfectly smooth)
        // Sine wave with ~3s period, phase-shifted per layer for a gentle ripple
        float glow1 = 0.5f + 0.5f * (float) Math.sin(t * 2.1);        // ~3.0s
        float glow2 = 0.5f + 0.5f * (float) Math.sin(t * 2.1 - 0.8);  // same speed, offset

        int g1Alpha = (int) (45 * alpha * glow1);
        int g2Alpha = (int) (28 * alpha * glow2);

        // Outer glow (larger, fainter)
        if (g2Alpha > 0) {
            graphics.fill(dx - 4, dy - 4, dx + dotSize + 4, dy + dotSize + 4,
                    (g2Alpha << 24) | 0x44FF44);
        }
        // Inner glow (tighter, brighter)
        if (g1Alpha > 0) {
            graphics.fill(dx - 2, dy - 2, dx + dotSize + 2, dy + dotSize + 2,
                    (g1Alpha << 24) | 0x44FF44);
        }

        // Solid dot — subtle brightness breathing
        float breathe = 0.9f + 0.1f * (float) Math.sin(t * 2.1 + 1.6);
        int borderColor = (int) (200 * alpha * breathe) << 24 | 0x226622;
        int dotColor = (int) (230 * alpha * breathe) << 24 | 0x44FF44;

        graphics.fill(dx - 1, dy - 1, dx + dotSize + 1, dy + dotSize + 1, borderColor);
        graphics.fill(dx, dy, dx + dotSize, dy + dotSize, dotColor);
    }

    /**
     * Renders a compact tracking info panel flush to the top-right corner.
     * Content varies based on tracking status state machine.
     */
    private void renderTrackingPanel(GuiGraphics graphics, int mouseX, int mouseY, float animationProgress) {
        isHoveringPanelStop = false;
        isHoveringPanelTeleport = false;
        ActiveTrackingState.TrackingStatus status = ActiveTrackingState.getStatus();
        if (status == ActiveTrackingState.TrackingStatus.IDLE) {
            return;
        }

        float appear = Mth.clamp((animationProgress - 0.4f) / 0.6f, 0.0f, 1.0f);
        if (appear <= 0.0f) {
            return;
        }

        int pad = 6;
        int iconSpace = 20;
        int panelTop = 4;
        int panelRight = width - 4;

        // Choose accent color and content based on status
        int accentArgb;
        int borderArgb;

        switch (status) {
            case SEARCHING -> {
                accentArgb = 0xDDFFCC44;   // yellow/amber
                borderArgb = 0xAAAA8833;
            }
            case TRACKING -> {
                accentArgb = 0xDD44FF44;   // green
                borderArgb = 0xAA44AA44;
            }
            case ARRIVED -> {
                accentArgb = 0xDD44FF44;   // green
                borderArgb = 0xAA44FF44;
            }
            case NOT_FOUND, ERROR -> {
                accentArgb = 0xDDFF6B6B;   // red
                borderArgb = 0xAACC4444;
            }
            default -> {
                accentArgb = 0xDD44FF44;
                borderArgb = 0xAA44AA44;
            }
        }

        // Build header
        MenuCategory cat = ActiveTrackingState.getCategory();
        String headerText;
        switch (status) {
            case SEARCHING -> headerText = Component.translatable("tracking.aromaaffect.status.searching").getString();
            case TRACKING -> {
                headerText = Component.translatable("menu.aromaaffect.tracking.label").getString();
                if (cat != null) {
                    headerText += " · " + cat.getDisplayName().getString();
                }
            }
            case ARRIVED -> headerText = Component.translatable("tracking.aromaaffect.status.arrived").getString();
            case NOT_FOUND -> headerText = Component.translatable("tracking.aromaaffect.status.not_found").getString();
            case ERROR -> headerText = Component.translatable("tracking.aromaaffect.status.error").getString();
            default -> headerText = "";
        }

        // Determine lines to show
        Component targetName = ActiveTrackingState.getDisplayName();
        String targetIdStr = ActiveTrackingState.getTargetId() != null ? ActiveTrackingState.getTargetId().toString() : null;

        int dist = ActiveTrackingState.getDistance();
        String distText = (status == ActiveTrackingState.TrackingStatus.TRACKING && dist >= 0)
                ? dist + " blocks away"
                : null;
        TrackingDirectionIndicator.Kind dirKind = (status == ActiveTrackingState.TrackingStatus.TRACKING && dist >= 0)
                ? TrackingDirectionIndicator.resolve(Minecraft.getInstance(), ActiveTrackingState.getDestination())
                : null;

        String failureReason = (status == ActiveTrackingState.TrackingStatus.NOT_FOUND
                || status == ActiveTrackingState.TrackingStatus.ERROR)
                ? ActiveTrackingState.getStatusMessage() : null;

        // Measure all lines to size the panel
        int maxText = font.width(headerText);
        if (targetName != null) {
            maxText = Math.max(maxText, font.width(targetName));
        }
        if (targetIdStr != null) {
            maxText = Math.max(maxText, font.width(targetIdStr));
        }
        if (distText != null) {
            maxText = Math.max(maxText, TrackingDirectionIndicator.getColumnWidth() + font.width(distText));
        }
        if (failureReason != null) {
            maxText = Math.max(maxText, font.width(failureReason));
        }

        int panelWidth = maxText + iconSpace + pad * 2;

        // Calculate panel height based on content
        int lineCount = 1; // header always present
        if (targetName != null && (status == ActiveTrackingState.TrackingStatus.SEARCHING
                || status == ActiveTrackingState.TrackingStatus.TRACKING)) {
            lineCount++; // target name
        }
        if (targetIdStr != null && status == ActiveTrackingState.TrackingStatus.TRACKING) {
            lineCount++; // target ID
        }
        if (distText != null) {
            lineCount++; // distance
        }
        if (failureReason != null) {
            lineCount++; // failure reason
        }
        int panelHeight = 10 + lineCount * 11;
        int panelLeft = panelRight - panelWidth;

        // Background
        int bgColor = MenuRenderUtils.withAlpha(0xDD1A1A2E, appear);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + panelHeight, bgColor);

        // Border
        int borderColor = MenuRenderUtils.withAlpha(borderArgb, appear);
        MenuRenderUtils.renderOutline(graphics, panelLeft, panelTop, panelWidth, panelHeight, borderColor);

        // Left accent bar
        int accentColor = MenuRenderUtils.withAlpha(accentArgb, appear);
        graphics.fill(panelLeft, panelTop, panelLeft + 2, panelTop + panelHeight, accentColor);

        // Item icon
        int iconX = panelLeft + pad + 1;
        int iconY = panelTop + (panelHeight - 16) / 2;
        if (ActiveTrackingState.getIcon() != null) {
            graphics.renderItem(ActiveTrackingState.getIcon(), iconX, iconY);
        }

        int textX = iconX + iconSpace;
        int currentY = panelTop + 5;

        // Header line
        int labelColor;
        switch (status) {
            case SEARCHING -> labelColor = MenuRenderUtils.withAlpha(0xFFFFCC44, appear);
            case ARRIVED -> labelColor = MenuRenderUtils.withAlpha(0xFF44FF44, appear);
            case NOT_FOUND, ERROR -> labelColor = MenuRenderUtils.withAlpha(0xFFFF6B6B, appear);
            default -> labelColor = MenuRenderUtils.withAlpha(0xFF88CC88, appear);
        }
        graphics.drawString(font, headerText, textX, currentY, labelColor);
        currentY += 11;

        // Target name (for SEARCHING and TRACKING)
        if (targetName != null && (status == ActiveTrackingState.TrackingStatus.SEARCHING
                || status == ActiveTrackingState.TrackingStatus.TRACKING)) {
            int nameColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, appear);
            graphics.drawString(font, targetName, textX, currentY, nameColor);
            currentY += 11;
        }

        // Target ID (TRACKING only)
        if (targetIdStr != null && status == ActiveTrackingState.TrackingStatus.TRACKING) {
            int idColor = MenuRenderUtils.withAlpha(0xFF777777, appear);
            graphics.drawString(font, targetIdStr, textX, currentY, idColor);
            currentY += 11;
        }

        // Distance (TRACKING only)
        if (distText != null && dirKind != null) {
            int distColor = MenuRenderUtils.withAlpha(0xFF44CCFF, appear);
            drawDistanceLine(graphics, textX, currentY, dirKind, distText, distColor);
            currentY += 11;
        }

        // Failure reason
        if (failureReason != null) {
            int reasonColor = MenuRenderUtils.withAlpha(0xFFFFAAAA, appear);
            graphics.drawString(font, failureReason, textX, currentY, reasonColor);
        }

        // Stop button below panel (only for SEARCHING and TRACKING states)
        if (status == ActiveTrackingState.TrackingStatus.SEARCHING
                || status == ActiveTrackingState.TrackingStatus.TRACKING) {
            Component stopLabel = Component.translatable("tracking.aromaaffect.stop");
            int stopTextW = font.width(stopLabel);
            int stopBtnW = stopTextW + 12;
            int stopBtnH = 14;
            int stopBtnX = panelRight - stopBtnW;
            int stopBtnY = panelTop + panelHeight + 2;

            // Store bounds for click detection
            panelStopX = stopBtnX;
            panelStopY = stopBtnY;
            panelStopW = stopBtnW;
            panelStopH = stopBtnH;

            isHoveringPanelStop = isInBounds(mouseX, mouseY, stopBtnX, stopBtnY, stopBtnW, stopBtnH);

            int stopBg = isHoveringPanelStop
                    ? MenuRenderUtils.withAlpha(0xC0FF4444, appear)
                    : MenuRenderUtils.withAlpha(0x80CC3333, appear);
            graphics.fill(stopBtnX, stopBtnY, stopBtnX + stopBtnW, stopBtnY + stopBtnH, stopBg);

            int stopBorder = MenuRenderUtils.withAlpha(isHoveringPanelStop ? 0xEEFF6B6B : 0x88AA4444, appear);
            MenuRenderUtils.renderOutline(graphics, stopBtnX, stopBtnY, stopBtnW, stopBtnH, stopBorder);

            int stopTextColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, appear);
            graphics.drawCenteredString(font, stopLabel, stopBtnX + stopBtnW / 2, stopBtnY + 3, stopTextColor);

            // Teleport button (creative mode only, left of stop button)
            var player = Minecraft.getInstance().player;
            if (player != null && player.isCreative() && ActiveTrackingState.getDestination() != null) {
                Component tpLabel = Component.literal("Teleport");
                int tpTextW = font.width(tpLabel);
                int tpBtnW = tpTextW + 12;
                int tpBtnH = 14;
                int tpBtnX = stopBtnX - tpBtnW - 4;
                int tpBtnY = stopBtnY;

                isHoveringPanelTeleport = isInBounds(mouseX, mouseY, tpBtnX, tpBtnY, tpBtnW, tpBtnH);

                int tpBg = isHoveringPanelTeleport
                        ? MenuRenderUtils.withAlpha(0xC044AAFF, appear)
                        : MenuRenderUtils.withAlpha(0x803388CC, appear);
                graphics.fill(tpBtnX, tpBtnY, tpBtnX + tpBtnW, tpBtnY + tpBtnH, tpBg);

                int tpBorder = MenuRenderUtils.withAlpha(isHoveringPanelTeleport ? 0xEE66CCFF : 0x884488AA, appear);
                MenuRenderUtils.renderOutline(graphics, tpBtnX, tpBtnY, tpBtnW, tpBtnH, tpBorder);

                int tpTextColor = MenuRenderUtils.withAlpha(0xFFFFFFFF, appear);
                graphics.drawCenteredString(font, tpLabel, tpBtnX + tpBtnW / 2, tpBtnY + 3, tpTextColor);
            }
        }
    }

    /**
     * Returns a grayscale variant ResourceLocation for the given icon texture.
     * Generates and registers a DynamicTexture on first call, then caches it.
     */
    private static ResourceLocation getGrayscaleIcon(ResourceLocation original) {
        ResourceLocation cached = GRAYSCALE_CACHE.get(original);
        if (cached != null) {
            return cached;
        }

        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(original);
            try (InputStream stream = resource.open()) {
                NativeImage source = NativeImage.read(stream);
                // mappedCopy applies an IntUnaryOperator to each pixel in ABGR format
                NativeImage grayscale = source.mappedCopy(pixel -> {
                    int a = (pixel >> 24) & 0xFF;
                    int b = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int r = pixel & 0xFF;

                    // Luminance-weighted grayscale, dimmed for a "disabled" look
                    int lum = (int) ((0.299f * r + 0.587f * g + 0.114f * b) * 0.6f);
                    return (a << 24) | (lum << 16) | (lum << 8) | lum;
                });
                source.close();

                String texName = original.getPath().replace('/', '_').replace('.', '_');
                ResourceLocation grayLoc = ResourceLocation.fromNamespaceAndPath(
                        AromaAffect.MOD_ID, "dynamic/gray_" + texName);
                DynamicTexture dynamicTexture = new DynamicTexture(() -> texName, grayscale);
                Minecraft.getInstance().getTextureManager().register(grayLoc, dynamicTexture);
                GRAYSCALE_CACHE.put(original, grayLoc);
                return grayLoc;
            }
        } catch (IOException e) {
            AromaAffect.LOGGER.warn("Failed to generate grayscale icon for {}: {}", original, e.getMessage());
            GRAYSCALE_CACHE.put(original, original);
            return original;
        }
    }

    /**
     * Computes which radial menu slots are locked (no unlocks for that category).
     */
    private boolean[] computeLockedSlots() {
        boolean[] locked = new boolean[entries.size()];
        var player = Minecraft.getInstance().player;
        if (player == null) {
            java.util.Arrays.fill(locked, true);
            return locked;
        }

        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);

        for (int i = 0; i < entries.size(); i++) {
            locked[i] = switch (entries.get(i).id()) {
                case "structures" -> abilities.getStructures().isEmpty();
                case "biomes" -> abilities.getBiomes().isEmpty();
                case "blocks" -> abilities.getBlocks().isEmpty();
                case "flowers" -> abilities.getFlowers().isEmpty();
                default -> false;
            };
        }

        return locked;
    }

    private void renderSelectionText(GuiGraphics graphics, int centerX, int centerY, int outerRadius, float animationProgress) {
        if (selectedIndex < 0 || selectedIndex >= entries.size() || animationProgress < 0.45f) {
            return;
        }

        float alpha = Mth.clamp((animationProgress - 0.45f) / 0.55f, 0.0f, 1.0f);
        RadialEntry entry = entries.get(selectedIndex);
        int y = centerY + outerRadius + 16;

        boolean locked = selectedIndex < cachedLockedSlots.length && cachedLockedSlots[selectedIndex];
        if (locked) {
            int lockedTitleColor = ((int) (255 * alpha) << 24) | 0x999999;
            int lockedDescColor = ((int) (180 * alpha) << 24) | 0xFF6B6B;
            graphics.drawCenteredString(font, entry.title, centerX, y, lockedTitleColor);
            graphics.drawCenteredString(font, Component.translatable("menu.aromaaffect.category.locked"), centerX, y + 12, lockedDescColor);
        } else {
            int titleColor = ((int) (255 * alpha) << 24) | 0xFFFFFF;
            int descColor = ((int) (200 * alpha) << 24) | 0xD0D0D0;
            graphics.drawCenteredString(font, entry.title, centerX, y, titleColor);
            graphics.drawCenteredString(font, entry.description, centerX, y + 12, descColor);
        }
    }

    // Source texture dimensions for the center logo (155x147 actual PNG size)
    private static final int CENTER_LOGO_TEX_W = 155;
    private static final int CENTER_LOGO_TEX_H = 147;

    private void renderCenterLogo(GuiGraphics graphics, int centerX, int centerY, float innerRadius, float animationProgress) {
        float appear = Mth.clamp(animationProgress / 0.6f, 0.0f, 1.0f);
        if (appear <= 0.0f) {
            return;
        }

        // Target size: fit inside the inner circle with padding
        float diameter = innerRadius * 2.0f * 0.65f * appear;
        float aspect = (float) CENTER_LOGO_TEX_W / CENTER_LOGO_TEX_H;
        int renderW = (int) diameter;
        int renderH = (int) (diameter / aspect);

        // Rotate both parts together toward the selected segment
        float rotation = arrowAngle - (float) (-Math.PI / 2.0);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().rotate(rotation);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ICON_CENTER_LOGO,
                -renderW / 2, -renderH / 2,
                0.0f, 0.0f,
                renderW, renderH,
                renderW, renderH
        );

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ICON_CENTER_ARROW,
                -renderW / 2, -renderH / 2,
                0.0f, 0.0f,
                renderW, renderH,
                renderW, renderH
        );

        graphics.pose().popMatrix();
    }

    private void submitRadialRenderState(GuiGraphics graphics, int centerX, int centerY, float innerRadius, int outerRadius, float animationProgress) {
        int baseColor = MenuRenderUtils.withAlpha(COLOR_RING_BASE, animationProgress);
        int selectedColor = MenuRenderUtils.withAlpha(COLOR_RING_SELECTED, animationProgress);
        int borderColor = MenuRenderUtils.withAlpha(COLOR_RING_BORDER, animationProgress);
        int separatorColor = MenuRenderUtils.withAlpha(COLOR_RING_SEPARATOR, animationProgress);
        int indicatorColor = MenuRenderUtils.withAlpha(COLOR_INDICATOR, animationProgress);

        int boundsPadding = 4;
        int boundsLeft = centerX - outerRadius - boundsPadding;
        int boundsTop = centerY - outerRadius - boundsPadding;
        int boundsSize = (outerRadius + boundsPadding) * 2;

        ScreenRectangle bounds = new ScreenRectangle(boundsLeft, boundsTop, boundsSize, boundsSize);
        GuiRenderState renderState = getGuiRenderState(graphics);
        renderState.submitGuiElement(new RadialRingRenderState(
                RenderPipelines.GUI,
                NO_TEXTURE,
                new Matrix3x2f(graphics.pose()),
                centerX,
                centerY,
                innerRadius,
                outerRadius,
                baseColor,
                selectedColor,
                borderColor,
                separatorColor,
                indicatorColor,
                START_ANGLE_RAD,
                selectedIndex,
                selectionProgress,
                bounds,
                null
        ));
    }

    private static int computeOuterRadiusPx(int width, int height) {
        int minDim = Math.min(width, height);
        int target = (int) (minDim * 0.20f);
        return Mth.clamp(target, MIN_OUTER_RADIUS, MAX_OUTER_RADIUS);
    }

    private static int computeSelectedIndex(double mouseX, double mouseY, int centerX, int centerY,
                                           float innerRadius, float outerRadius, int segmentCount) {
        if (segmentCount <= 0) {
            return -1;
        }

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distSq = dx * dx + dy * dy;

        double innerSq = innerRadius * innerRadius;
        double outerSq = outerRadius * (double) outerRadius;
        if (distSq < innerSq || distSq > outerSq) {
            return -1;
        }

        double angle = Math.atan2(dy, dx); // screen-space: +Y is down
        double step = (Math.PI * 2.0) / segmentCount;

        double relative = angle - START_ANGLE_RAD;
        relative = (relative % (Math.PI * 2.0) + (Math.PI * 2.0)) % (Math.PI * 2.0);

        int index = (int) (relative / step);
        return (index >= 0 && index < segmentCount) ? index : -1;
    }

    private static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >>> 24) & 0xFF;
        int r1 = (color1 >>> 16) & 0xFF;
        int g1 = (color1 >>> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >>> 24) & 0xFF;
        int r2 = (color2 >>> 16) & 0xFF;
        int g2 = (color2 >>> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) Mth.lerp(t, a1, a2);
        int r = (int) Mth.lerp(t, r1, r2);
        int g = (int) Mth.lerp(t, g1, g2);
        int b = (int) Mth.lerp(t, b1, b2);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static GuiRenderState getGuiRenderState(GuiGraphics graphics) {
        try {
            return (GuiRenderState) GUI_RENDER_STATE_FIELD.get(graphics);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to access GuiGraphics GuiRenderState", e);
        }
    }

    private static Field findGuiRenderStateField() {
        for (Field field : GuiGraphics.class.getDeclaredFields()) {
            if (GuiRenderState.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalStateException("Unable to locate GuiRenderState field on GuiGraphics");
    }

    private void drawDistanceLine(
            GuiGraphics graphics,
            int x,
            int y,
            TrackingDirectionIndicator.Kind directionKind,
            String distanceText,
            int color
    ) {
        int indicatorColor = TrackingDirectionIndicator.colorForKind(directionKind, color);
        TrackingDirectionIndicator.draw(graphics, x, y, directionKind, indicatorColor);
        graphics.drawString(font, distanceText, x + TrackingDirectionIndicator.getColumnWidth(), y, indicatorColor, false);
    }

    private record RadialEntry(
            String id,
            Component title,
            Component description,
            ResourceLocation icon,
            Runnable onSelect
    ) {
    }

    private record RadialRingRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            int centerX,
            int centerY,
            float innerRadius,
            float outerRadius,
            int baseColor,
            int selectedColor,
            int borderColor,
            int separatorColor,
            int indicatorColor,
            float startAngleRad,
            int selectedIndex,
            float[] selectionProgress,
            ScreenRectangle bounds,
            ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            int segments = selectionProgress.length;
            if (segments <= 0) {
                return;
            }

            float segmentAngle = TWO_PI / segments;
            int fullSteps = computeFullArcSteps(outerRadius);
            int stepsPerSegment = Math.max(8, fullSteps / segments);

            // Base + selected blend per segment.
            for (int i = 0; i < segments; i++) {
                float t = selectionProgress[i];
                int fillColor = lerpColor(baseColor, selectedColor, t);
                float start = startAngleRad + i * segmentAngle;
                addRingArc(consumer, centerX, centerY, innerRadius, outerRadius, start, segmentAngle, stepsPerSegment, fillColor);
            }

            // Borders (inner + outer).
            addRingArc(consumer, centerX, centerY, outerRadius - BORDER_THICKNESS_PX, outerRadius, startAngleRad, TWO_PI, fullSteps, borderColor);
            addRingArc(consumer, centerX, centerY, innerRadius, innerRadius + BORDER_THICKNESS_PX, startAngleRad, TWO_PI, fullSteps, borderColor);

            // Separators between segments.
            for (int i = 0; i < segments; i++) {
                float angle = startAngleRad + i * segmentAngle;
                addRadialQuad(consumer, centerX, centerY, innerRadius, outerRadius, angle, SEPARATOR_THICKNESS_PX, separatorColor);
            }

            // Center logo is rendered via blit in renderContent, not here.
        }

        private void addCenterArrow(VertexConsumer consumer, float centerX, float centerY, float innerRadius, float angleRad, int color) {
            // Unfilled arrow head (two lines) pointing at the selected segment.
            // This matches the design mock more closely and avoids relying on degenerate triangles in QUADS mode.
            float dirX = (float) Math.cos(angleRad);
            float dirY = (float) Math.sin(angleRad);
            float perpX = -dirY;
            float perpY = dirX;

            float tipR = innerRadius - 10.0f;
            // For a true 90° "L" corner, keep length == width so the two segments are perpendicular.
            float headLength = 13.0f;
            float headWidth = 13.0f;

            float tipX = centerX + dirX * tipR;
            float tipY = centerY + dirY * tipR;

            float baseCenterX = tipX - dirX * headLength;
            float baseCenterY = tipY - dirY * headLength;

            float leftX = baseCenterX + perpX * headWidth;
            float leftY = baseCenterY + perpY * headWidth;
            float rightX = baseCenterX - perpX * headWidth;
            float rightY = baseCenterY - perpY * headWidth;

            addLineQuad(consumer, tipX, tipY, leftX, leftY, CENTER_ARROW_THICKNESS_PX, color);
            addLineQuad(consumer, tipX, tipY, rightX, rightY, CENTER_ARROW_THICKNESS_PX, color);
        }

        private void addRingArc(VertexConsumer consumer, float centerX, float centerY, float innerR, float outerR,
                               float startAngle, float arcAngle, int steps, int color) {
            if (steps <= 0) {
                return;
            }

            float stepAngle = arcAngle / steps;
            double cosStep = Math.cos(stepAngle);
            double sinStep = Math.sin(stepAngle);

            double cos = Math.cos(startAngle);
            double sin = Math.sin(startAngle);

            for (int i = 0; i < steps; i++) {
                double cosNext = cos * cosStep - sin * sinStep;
                double sinNext = sin * cosStep + cos * sinStep;

                float xInner0 = centerX + (float) (innerR * cos);
                float yInner0 = centerY + (float) (innerR * sin);
                float xOuter0 = centerX + (float) (outerR * cos);
                float yOuter0 = centerY + (float) (outerR * sin);

                float xOuter1 = centerX + (float) (outerR * cosNext);
                float yOuter1 = centerY + (float) (outerR * sinNext);
                float xInner1 = centerX + (float) (innerR * cosNext);
                float yInner1 = centerY + (float) (innerR * sinNext);

                addQuad(consumer, xInner0, yInner0, xOuter0, yOuter0, xOuter1, yOuter1, xInner1, yInner1, color);

                cos = cosNext;
                sin = sinNext;
            }
        }

        private void addRadialQuad(VertexConsumer consumer, float centerX, float centerY,
                                   float innerR, float outerR, float angleRad, float thicknessPx, int color) {
            float dirX = (float) Math.cos(angleRad);
            float dirY = (float) Math.sin(angleRad);

            float perpX = -dirY;
            float perpY = dirX;
            float halfT = thicknessPx * 0.5f;

            float x0 = centerX + dirX * innerR;
            float y0 = centerY + dirY * innerR;
            float x1 = centerX + dirX * outerR;
            float y1 = centerY + dirY * outerR;

            float ax = x0 + perpX * halfT;
            float ay = y0 + perpY * halfT;
            float bx = x0 - perpX * halfT;
            float by = y0 - perpY * halfT;
            float cx = x1 - perpX * halfT;
            float cy = y1 - perpY * halfT;
            float dx = x1 + perpX * halfT;
            float dy = y1 + perpY * halfT;

            addQuad(consumer, ax, ay, bx, by, cx, cy, dx, dy, color);
        }

        private void addLineQuad(VertexConsumer consumer,
                                 float x0, float y0,
                                 float x1, float y1,
                                 float thicknessPx,
                                 int color) {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float lenSq = dx * dx + dy * dy;
            if (lenSq < 0.0001f) {
                return;
            }

            float invLen = Mth.invSqrt(lenSq);
            float nx = dx * invLen;
            float ny = dy * invLen;

            float perpX = -ny;
            float perpY = nx;
            float halfT = thicknessPx * 0.5f;

            float ax = x0 + perpX * halfT;
            float ay = y0 + perpY * halfT;
            float bx = x0 - perpX * halfT;
            float by = y0 - perpY * halfT;
            float cx = x1 - perpX * halfT;
            float cy = y1 - perpY * halfT;
            float dx2 = x1 + perpX * halfT;
            float dy2 = y1 + perpY * halfT;

            addQuad(consumer, ax, ay, bx, by, cx, cy, dx2, dy2, color);
        }

        private void addQuad(VertexConsumer consumer,
                             float x0, float y0,
                             float x1, float y1,
                             float x2, float y2,
                             float x3, float y3,
                             int color) {
            // Match vanilla GUI quad winding (culling may be enabled on GUI pipelines).
            consumer.addVertexWith2DPose(pose, x0, y0).setColor(color);
            consumer.addVertexWith2DPose(pose, x3, y3).setColor(color);
            consumer.addVertexWith2DPose(pose, x2, y2).setColor(color);
            consumer.addVertexWith2DPose(pose, x1, y1).setColor(color);
        }

        private static int computeFullArcSteps(float outerRadius) {
            return Mth.clamp((int) (outerRadius * 0.75f), 48, 96);
        }
    }
}
