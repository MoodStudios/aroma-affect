package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ovrtechnology.trigger.PassiveModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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

    private static final int COLOR_RING_BASE = 0x66D7D7D7;
    private static final int COLOR_RING_SELECTED = 0x809A7CFF;
    private static final int COLOR_RING_BORDER = 0x88FFFFFF;
    private static final int COLOR_RING_SEPARATOR = 0x80A88CFF;
    private static final int COLOR_INDICATOR = 0xEEFFFFFF;

    private static final TextureSetup NO_TEXTURE = TextureSetup.noTexture();

    private static final Field GUI_RENDER_STATE_FIELD = findGuiRenderStateField();

    private final List<RadialEntry> entries = new ArrayList<>();
    private float[] selectionProgress = new float[0];

    /**
     * Selected slice index (computed during render based on mouse position).
     */
    private int selectedIndex = -1;

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
    private static final ResourceLocation ICON_COMPASS = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_compass.png");
    private static final ResourceLocation ICON_PASSIVE = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_passive.png");

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
        selectedIndex = -1;
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
        }
    }

    // Track hover states for corner buttons
    private boolean isHoveringConfig = false;
    private boolean isHoveringCompass = false;
    private boolean isHoveringPassive = false;
    private boolean isHoveringStopPath = false;

    // Color for stop button (reddish)
    private static final int COLOR_STOP_BUTTON = 0x80FF6B6B;

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
        renderSelectionText(graphics, centerX, centerY, outerRadius, animationProgress);

        // Render corner buttons (config top-right, compass bottom-right)
        renderCornerButtons(graphics, mouseX, mouseY, animationProgress);
    }

    /**
     * Renders the corner buttons for config (top-right), compass (bottom-right), and stop path (bottom-left).
     */
    private void renderCornerButtons(GuiGraphics graphics, int mouseX, int mouseY, float animationProgress) {
        float appear = Mth.clamp((animationProgress - 0.3f) / 0.7f, 0.0f, 1.0f);
        if (appear <= 0.0f) {
            return;
        }

        int buttonSize = CORNER_ICON_SIZE + 8;  // Icon size + padding for hover area
        int iconOffset = 4;  // Center icon in button area

        // Config button position (top-right corner)
        int configX = width - CORNER_BUTTON_PADDING - buttonSize;
        int configY = CORNER_BUTTON_PADDING;

        // Compass button position (bottom-right corner)
        int compassX = width - CORNER_BUTTON_PADDING - buttonSize;
        int compassY = height - CORNER_BUTTON_PADDING - buttonSize;

        // Stop Path button position (bottom-left corner)
        int stopX = CORNER_BUTTON_PADDING;
        int stopY = height - CORNER_BUTTON_PADDING - buttonSize;

        // Check hover states
        isHoveringConfig = isInBounds(mouseX, mouseY, configX, configY, buttonSize, buttonSize);
        isHoveringCompass = isInBounds(mouseX, mouseY, compassX, compassY, buttonSize, buttonSize);
        isHoveringStopPath = isInBounds(mouseX, mouseY, stopX, stopY, buttonSize, buttonSize);

        // Render config button background when hovered
        if (isHoveringConfig) {
            int bgColor = withAlpha(COLOR_RING_SELECTED, appear);
            graphics.fill(configX, configY, configX + buttonSize, configY + buttonSize, bgColor);
            int borderColor = withAlpha(COLOR_RING_BORDER, appear);
            renderOutline(graphics, configX, configY, buttonSize, buttonSize, borderColor);
        }

        // Render compass button background when hovered
        if (isHoveringCompass) {
            int bgColor = withAlpha(COLOR_RING_SELECTED, appear);
            graphics.fill(compassX, compassY, compassX + buttonSize, compassY + buttonSize, bgColor);
            int borderColor = withAlpha(COLOR_RING_BORDER, appear);
            renderOutline(graphics, compassX, compassY, buttonSize, buttonSize, borderColor);
        }

        // Render stop path button background (always visible with slight tint, brighter when hovered)
        int stopBgColor = isHoveringStopPath ? withAlpha(COLOR_STOP_BUTTON, appear) : withAlpha(0x40FF6B6B, appear);
        graphics.fill(stopX, stopY, stopX + buttonSize, stopY + buttonSize, stopBgColor);
        int stopBorderColor = withAlpha(isHoveringStopPath ? 0xFFFF6B6B : COLOR_RING_BORDER, appear);
        renderOutline(graphics, stopX, stopY, buttonSize, buttonSize, stopBorderColor);

        // Render config icon
        float configScale = isHoveringConfig ? 1.1f : 1.0f;
        int configIconSize = (int) (CORNER_ICON_SIZE * configScale);
        int configIconX = configX + iconOffset + (CORNER_ICON_SIZE - configIconSize) / 2;
        int configIconY = configY + iconOffset + (CORNER_ICON_SIZE - configIconSize) / 2;

        graphics.blit(ICON_CONFIG, configIconX, configIconY, 0, 0, configIconSize, configIconSize, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);

        // Render compass icon
        float compassScale = isHoveringCompass ? 1.1f : 1.0f;
        int compassIconSize = (int) (CORNER_ICON_SIZE * compassScale);
        int compassIconX = compassX + iconOffset + (CORNER_ICON_SIZE - compassIconSize) / 2;
        int compassIconY = compassY + iconOffset + (CORNER_ICON_SIZE - compassIconSize) / 2;

        graphics.blit(ICON_COMPASS, compassIconX, compassIconY, 0, 0, compassIconSize, compassIconSize, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);

        // Render stop path "X" icon (drawn as text since we don't have a texture)
        int stopTextColor = withAlpha(isHoveringStopPath ? 0xFFFFFFFF : 0xFFDDDDDD, appear);
        Component stopText = Component.literal("X");
        int textX = stopX + buttonSize / 2 - font.width(stopText) / 2;
        int textY = stopY + buttonSize / 2 - 4;
        graphics.drawString(font, stopText, textX, textY, stopTextColor);

        // Render tooltips for corner buttons when hovered
        if (isHoveringConfig) {
            graphics.drawString(font, Component.translatable("menu.aromaaffect.button.config"),
                    configX - font.width(Component.translatable("menu.aromaaffect.button.config")) - 8,
                    configY + buttonSize / 2 - 4,
                    withAlpha(0xFFFFFFFF, appear));
        }
        if (isHoveringCompass) {
            graphics.drawString(font, Component.translatable("menu.aromaaffect.button.compass"),
                    compassX - font.width(Component.translatable("menu.aromaaffect.button.compass")) - 8,
                    compassY + buttonSize / 2 - 4,
                    withAlpha(0xFFFFFFFF, appear));
        }
        if (isHoveringStopPath) {
            Component stopLabel = Component.literal("Stop Path");
            graphics.drawString(font, stopLabel,
                    stopX + buttonSize + 8,
                    stopY + buttonSize / 2 - 4,
                    withAlpha(0xFFFF6B6B, appear));
        }

        // Passive mode button position (top-left corner)
        int passiveX = CORNER_BUTTON_PADDING;
        int passiveY = CORNER_BUTTON_PADDING;

        // Check hover state
        isHoveringPassive = isInBounds(mouseX, mouseY, passiveX, passiveY, buttonSize, buttonSize);

        // Render passive button background (green if enabled, gray if disabled)
        boolean isPassiveEnabled = PassiveModeManager.isPassiveModeEnabled();
        int passiveBgColor;
        if (isHoveringPassive) {
            passiveBgColor = isPassiveEnabled ? 0xA066CC66 : 0xA0666666;
        } else {
            passiveBgColor = isPassiveEnabled ? 0x8044AA44 : 0x80444444;
        }
        graphics.fill(passiveX, passiveY, passiveX + buttonSize, passiveY + buttonSize,
                withAlpha(passiveBgColor, appear));

        int passiveBorderColor = withAlpha(COLOR_RING_BORDER, appear);
        renderOutline(graphics, passiveX, passiveY, buttonSize, buttonSize, passiveBorderColor);

        // Render passive icon
        float passiveScale = isHoveringPassive ? 1.1f : 1.0f;
        int passiveIconSize = (int) (CORNER_ICON_SIZE * passiveScale);
        int passiveIconX = passiveX + iconOffset + (CORNER_ICON_SIZE - passiveIconSize) / 2;
        int passiveIconY = passiveY + iconOffset + (CORNER_ICON_SIZE - passiveIconSize) / 2;

        graphics.blit(ICON_PASSIVE, passiveIconX, passiveIconY, 0, 0,
                passiveIconSize, passiveIconSize, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);

        // Render tooltip for passive button
        if (isHoveringPassive) {
            Component passiveLabel = isPassiveEnabled
                    ? Component.translatable("menu.aromaaffect.button.passive.on")
                    : Component.translatable("menu.aromaaffect.button.passive.off");
            graphics.drawString(font, passiveLabel,
                    passiveX + buttonSize + 8,
                    passiveY + buttonSize / 2 - 4,
                    withAlpha(0xFFFFFFFF, appear));
        }
    }

    private static boolean isInBounds(double x, double y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    /**
     * Renders a rectangular outline using fill commands.
     */
    private static void renderOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // Top edge
        graphics.fill(x, y, x + width, y + 1, color);
        // Bottom edge
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        // Left edge
        graphics.fill(x, y, x + 1, y + height, color);
        // Right edge
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        // Check corner buttons first
        if (isHoveringConfig) {
            AromaAffect.LOGGER.debug("Config button clicked");
            MenuManager.openConfigMenu();
            return true;
        }
        if (isHoveringCompass) {
            AromaAffect.LOGGER.debug("Compass button clicked");
            MenuManager.openCompassMenu();
            return true;
        }
        if (isHoveringStopPath) {
            AromaAffect.LOGGER.debug("Stop Path button clicked");
            executeStopPath();
            return true;
        }
        if (isHoveringPassive) {
            AromaAffect.LOGGER.debug("Passive mode button clicked");
            PassiveModeManager.togglePassiveMode();
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

        onEntrySelected(entries.get(index));
        return true;
    }

    /**
     * Executes the stop path command and closes the menu.
     */
    private void executeStopPath() {
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

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
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

        float iconRadius = (innerRadius + outerRadius) * 0.5f;
        float segmentAngle = TWO_PI / entries.size();

        for (int i = 0; i < entries.size(); i++) {
            RadialEntry entry = entries.get(i);
            float angle = START_ANGLE_RAD + (i + 0.5f) * segmentAngle;

            float x = centerX + (float) Math.cos(angle) * iconRadius;
            float y = centerY + (float) Math.sin(angle) * iconRadius;

            // Scale factor for selection feedback and appear animation
            float scale = (1.0f + 0.2f * selectionProgress[i]) * appear;
            int iconSize = (int) (ICON_DISPLAY_SIZE * scale);
            int halfSize = iconSize / 2;

            // Render high-resolution texture
            // For high-res icons, we sample the full texture (0,0 to ICON_TEXTURE_SIZE)
            // and render it at the display size for proper downscaling
            graphics.blit(
                    entry.icon,
                    (int)(x - halfSize), (int)(y - halfSize),  // render position
                    0, 0,  // texture UV start (top-left of texture)
                    iconSize, iconSize,  // render size (display dimensions)
                    ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE   // source texture dimensions for UV mapping
            );
        }
    }

    private void renderSelectionText(GuiGraphics graphics, int centerX, int centerY, int outerRadius, float animationProgress) {
        if (selectedIndex < 0 || selectedIndex >= entries.size() || animationProgress < 0.45f) {
            return;
        }

        float alpha = Mth.clamp((animationProgress - 0.45f) / 0.55f, 0.0f, 1.0f);
        int titleColor = ((int) (255 * alpha) << 24) | 0xFFFFFF;
        int descColor = ((int) (200 * alpha) << 24) | 0xD0D0D0;

        RadialEntry entry = entries.get(selectedIndex);
        int y = centerY + outerRadius + 16;
        graphics.drawCenteredString(font, entry.title, centerX, y, titleColor);
        graphics.drawCenteredString(font, entry.description, centerX, y + 12, descColor);
    }

    private void submitRadialRenderState(GuiGraphics graphics, int centerX, int centerY, float innerRadius, int outerRadius, float animationProgress) {
        int baseColor = withAlpha(COLOR_RING_BASE, animationProgress);
        int selectedColor = withAlpha(COLOR_RING_SELECTED, animationProgress);
        int borderColor = withAlpha(COLOR_RING_BORDER, animationProgress);
        int separatorColor = withAlpha(COLOR_RING_SEPARATOR, animationProgress);
        int indicatorColor = withAlpha(COLOR_INDICATOR, animationProgress);

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

    private static int withAlpha(int argb, float alphaMul) {
        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        int na = Mth.clamp((int) (a * alphaMul), 0, 255);
        return (na << 24) | rgb;
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

            // Indicator: centered arrow pointing to the selected segment (inside the inner circle).
            if (selectedIndex >= 0 && selectedIndex < segments) {
                float indicatorAlpha = selectionProgress[selectedIndex];
                if (indicatorAlpha > 0.01f) {
                    float angle = startAngleRad + (selectedIndex + 0.5f) * segmentAngle;
                    int color = withAlpha(indicatorColor, indicatorAlpha);
                    addCenterArrow(consumer, centerX, centerY, innerRadius, angle, color);
                }
            }
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
