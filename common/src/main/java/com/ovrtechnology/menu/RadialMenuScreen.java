package com.ovrtechnology.menu;

import com.ovrtechnology.AromaCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Radial menu screen for selecting tracking categories.
 * 
 * <p>This is the main navigation menu of AromaCraft, activated by pressing the radial menu hotkey (default: R).
 * It displays three category options (Blocks, Biomes, Structures) arranged in a radial pattern,
 * with two options at the top and one at the bottom.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Smooth opening/closing animations with easing</li>
 *   <li>Hover highlighting with scaling effects</li>
 *   <li>Category icons using ItemStack rendering</li>
 *   <li>Visual feedback on selection</li>
 * </ul>
 */
public class RadialMenuScreen extends BaseMenuScreen {
    
    /**
     * Radius of the radial menu from center to category icons.
     */
    private static final int MENU_RADIUS = 80;
    
    /**
     * Size of each category button/slot.
     */
    private static final int SLOT_SIZE = 48;
    
    /**
     * Size of the item icon within each slot.
     */
    private static final int ICON_SIZE = 32;
    
    /**
     * Size of the center indicator circle.
     */
    private static final int CENTER_SIZE = 24;
    
    /**
     * Color for unselected slots (semi-transparent dark).
     */
    private static final int SLOT_COLOR = 0xB0222222;
    
    /**
     * Color for hovered slots.
     */
    private static final int SLOT_HOVER_COLOR = 0xE0444488;
    
    /**
     * Color for the slot border.
     */
    private static final int SLOT_BORDER_COLOR = 0xFF666666;
    
    /**
     * Color for the hovered slot border.
     */
    private static final int SLOT_HOVER_BORDER_COLOR = 0xFFAAAAFF;
    
    /**
     * Color for the center indicator.
     */
    private static final int CENTER_COLOR = 0xC0333333;
    
    /**
     * The category slots to display.
     */
    private final List<RadialSlot> slots = new ArrayList<>();
    
    /**
     * Currently hovered slot index, or -1 if none.
     */
    private int hoveredSlotIndex = -1;
    
    /**
     * The previously hovered slot for animation purposes.
     */
    private int previousHoveredSlot = -1;
    
    /**
     * Individual slot animation progress for hover effects.
     */
    private final float[] slotHoverProgress = new float[MenuCategory.values().length];
    
    public RadialMenuScreen() {
        super(Component.translatable("menu.aromacraft.radial.title"));
        initializeSlots();
    }
    
    /**
     * Initializes the radial slots with their positions.
     * Layout: Blocks (top-left), Biomes (top-right), Structures (bottom-center)
     */
    private void initializeSlots() {
        slots.clear();
        
        // Calculate angles for 3 slots: top-left, top-right, bottom-center
        // Angles in degrees: -135 (top-left), -45 (top-right), 90 (bottom)
        double[] angles = { -135, -45, 90 };
        MenuCategory[] categories = { MenuCategory.BLOCKS, MenuCategory.BIOMES, MenuCategory.STRUCTURES };
        
        for (int i = 0; i < categories.length; i++) {
            slots.add(new RadialSlot(categories[i], angles[i]));
        }
    }
    
    @Override
    protected void init() {
        super.init();
        // Reset slot hover progress
        for (int i = 0; i < slotHoverProgress.length; i++) {
            slotHoverProgress[i] = 0.0f;
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Update slot hover animations
        for (int i = 0; i < slots.size(); i++) {
            if (i == hoveredSlotIndex) {
                slotHoverProgress[i] = Math.min(1.0f, slotHoverProgress[i] + 0.2f);
            } else {
                slotHoverProgress[i] = Math.max(0.0f, slotHoverProgress[i] - 0.15f);
            }
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, 
                                  float partialTick, float animProgress) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Calculate animated radius (expands outward during animation)
        float animatedRadius = MENU_RADIUS * animProgress;
        
        // Calculate scale for the entire menu
        float menuScale = 0.5f + 0.5f * animProgress;
        
        // Update hovered slot based on mouse position
        updateHoveredSlot(mouseX, mouseY, centerX, centerY, animatedRadius);
        
        // Render center indicator
        renderCenterIndicator(graphics, centerX, centerY, animProgress);
        
        // Render each slot
        for (int i = 0; i < slots.size(); i++) {
            RadialSlot slot = slots.get(i);
            float hoverProgress = getInterpolatedHoverProgress(i, partialTick);
            renderSlot(graphics, slot, i, centerX, centerY, animatedRadius, 
                      animProgress, hoverProgress, partialTick);
        }
        
        // Render connecting lines from center to slots
        renderConnectingLines(graphics, centerX, centerY, animatedRadius, animProgress);
        
        // Render hovered slot tooltip
        if (hoveredSlotIndex >= 0 && animProgress > 0.5f) {
            RadialSlot hoveredSlot = slots.get(hoveredSlotIndex);
            renderSlotTooltip(graphics, hoveredSlot, mouseX, mouseY, animProgress);
        }
    }
    
    /**
     * Renders the center indicator of the radial menu.
     */
    private void renderCenterIndicator(GuiGraphics graphics, int centerX, int centerY, float animProgress) {
        int size = (int) (CENTER_SIZE * animProgress);
        int halfSize = size / 2;
        
        // Draw center circle background
        int alpha = (int) (192 * animProgress);
        int centerColor = (alpha << 24) | 0x333333;
        
        graphics.fill(centerX - halfSize, centerY - halfSize, 
                     centerX + halfSize, centerY + halfSize, centerColor);
        
        // Draw center border
        int borderAlpha = (int) (255 * animProgress);
        int borderColor = (borderAlpha << 24) | 0x888888;
        drawRectBorder(graphics, centerX - halfSize, centerY - halfSize, 
                      centerX + halfSize, centerY + halfSize, borderColor);
    }
    
    /**
     * Renders a single radial slot.
     */
    private void renderSlot(GuiGraphics graphics, RadialSlot slot, int index,
                           int centerX, int centerY, float radius,
                           float animProgress, float hoverProgress, float partialTick) {
        // Calculate slot position
        double angleRad = Math.toRadians(slot.angle);
        int slotX = centerX + (int) (radius * Math.cos(angleRad));
        int slotY = centerY + (int) (radius * Math.sin(angleRad));
        
        // Calculate animated slot size (slightly larger when hovered)
        float scaleBonus = hoverProgress * 0.15f;
        float scale = animProgress * (1.0f + scaleBonus);
        int currentSlotSize = (int) (SLOT_SIZE * scale);
        int halfSlot = currentSlotSize / 2;
        
        // Determine colors based on hover state
        int slotColor = lerpColor(SLOT_COLOR, SLOT_HOVER_COLOR, hoverProgress);
        int borderColor = lerpColor(SLOT_BORDER_COLOR, SLOT_HOVER_BORDER_COLOR, hoverProgress);
        
        // Apply animation alpha
        int slotAlpha = (int) (((slotColor >> 24) & 0xFF) * animProgress);
        slotColor = (slotAlpha << 24) | (slotColor & 0x00FFFFFF);
        int borderAlpha = (int) (((borderColor >> 24) & 0xFF) * animProgress);
        borderColor = (borderAlpha << 24) | (borderColor & 0x00FFFFFF);
        
        // Draw slot background
        graphics.fill(slotX - halfSlot, slotY - halfSlot, 
                     slotX + halfSlot, slotY + halfSlot, slotColor);
        
        // Draw slot border
        drawRectBorder(graphics, slotX - halfSlot, slotY - halfSlot, 
                      slotX + halfSlot, slotY + halfSlot, borderColor);
        
        // Render item icon
        if (animProgress > 0.3f) {
            float iconAlpha = (animProgress - 0.3f) / 0.7f;
            int iconSize = (int) (ICON_SIZE * scale * iconAlpha);
            int iconOffset = iconSize / 2;
            
            // Use pose stack for scaling the item
            graphics.pose().pushMatrix();
            float itemScale = scale * iconAlpha;
            graphics.pose().translate(slotX, slotY);
            graphics.pose().scale(itemScale, itemScale);
            
            // Render the item at the center (offset to center the 16x16 item)
            graphics.renderItem(slot.category.getIconItem(), -8, -8);
            
            graphics.pose().popMatrix();
        }
        
        // Render category label below the slot
        if (animProgress > 0.6f) {
            float labelAlpha = (animProgress - 0.6f) / 0.4f;
            Component label = slot.category.getDisplayName();
            int labelWidth = font.width(label);
            int labelY = slotY + halfSlot + 4;
            int labelColor = (int) (255 * labelAlpha) << 24 | 0xFFFFFF;
            
            graphics.drawCenteredString(font, label, slotX, labelY, labelColor);
        }
    }
    
    /**
     * Renders connecting lines from the center to each slot.
     */
    private void renderConnectingLines(GuiGraphics graphics, int centerX, int centerY, 
                                        float radius, float animProgress) {
        if (animProgress < 0.2f) return;
        
        float lineProgress = (animProgress - 0.2f) / 0.8f;
        int lineAlpha = (int) (100 * lineProgress);
        int lineColor = (lineAlpha << 24) | 0x888888;
        
        for (RadialSlot slot : slots) {
            double angleRad = Math.toRadians(slot.angle);
            int endX = centerX + (int) (radius * 0.6f * Math.cos(angleRad));
            int endY = centerY + (int) (radius * 0.6f * Math.sin(angleRad));
            
            // Draw a simple line (1 pixel wide)
            drawLine(graphics, centerX, centerY, endX, endY, lineColor);
        }
    }
    
    /**
     * Renders the tooltip for a hovered slot.
     */
    private void renderSlotTooltip(GuiGraphics graphics, RadialSlot slot, 
                                    int mouseX, int mouseY, float animProgress) {
        Component description = slot.category.getDescription();
        float tooltipAlpha = (animProgress - 0.5f) / 0.5f;
        
        // Render description below the radial menu
        int descY = height / 2 + MENU_RADIUS + SLOT_SIZE + 20;
        int descColor = (int) (255 * tooltipAlpha) << 24 | 0xCCCCCC;
        
        graphics.drawCenteredString(font, description, width / 2, descY, descColor);
    }
    
    /**
     * Updates which slot is currently hovered based on mouse position.
     */
    private void updateHoveredSlot(int mouseX, int mouseY, int centerX, int centerY, float radius) {
        previousHoveredSlot = hoveredSlotIndex;
        hoveredSlotIndex = -1;
        
        for (int i = 0; i < slots.size(); i++) {
            RadialSlot slot = slots.get(i);
            double angleRad = Math.toRadians(slot.angle);
            int slotX = centerX + (int) (radius * Math.cos(angleRad));
            int slotY = centerY + (int) (radius * Math.sin(angleRad));
            
            int halfSlot = SLOT_SIZE / 2;
            if (mouseX >= slotX - halfSlot && mouseX <= slotX + halfSlot &&
                mouseY >= slotY - halfSlot && mouseY <= slotY + halfSlot) {
                hoveredSlotIndex = i;
                break;
            }
        }
    }
    
    /**
     * Gets the interpolated hover progress for smooth animations.
     */
    private float getInterpolatedHoverProgress(int slotIndex, float partialTick) {
        float current = slotHoverProgress[slotIndex];
        float target = (slotIndex == hoveredSlotIndex) ? 1.0f : 0.0f;
        float speed = (target > current) ? 0.2f : 0.15f;
        return Mth.lerp(partialTick * speed, current, target);
    }
    
    /**
     * Handles mouse click events.
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredSlotIndex >= 0) { // Left click
            RadialSlot slot = slots.get(hoveredSlotIndex);
            onCategorySelected(slot.category);
            return true;
        }
        return false;
    }
    
    /**
     * Handles key press events.
     */
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        // Close on escape or the radial menu key
        if (keyCode == 256) { // Escape
            onClose();
            return true;
        }
        return false;
    }
    
    /**
     * Called when a category is selected.
     * Opens the corresponding category menu.
     */
    protected void onCategorySelected(MenuCategory category) {
        AromaCraft.LOGGER.debug("Selected category: {}", category.getId());
        
        // Open the appropriate menu for the selected category
        switch (category) {
            case BLOCKS:
                MenuManager.openBlocksMenu();
                break;
            case BIOMES:
                MenuManager.openBiomesMenu();
                break;
            case STRUCTURES:
                MenuManager.openStructuresMenu();
                break;
        }
    }
    
    /**
     * Draws a rectangular border.
     */
    private void drawRectBorder(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color); // Top
        graphics.fill(x1, y2 - 1, x2, y2, color); // Bottom
        graphics.fill(x1, y1, x1 + 1, y2, color); // Left
        graphics.fill(x2 - 1, y1, x2, y2, color); // Right
    }
    
    /**
     * Draws a simple line between two points.
     */
    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // Simple line drawing using fill for horizontal/vertical or diagonal approximation
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        
        if (steps == 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        
        float xStep = (float) (x2 - x1) / steps;
        float yStep = (float) (y2 - y1) / steps;
        
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (int) (xStep * i);
            int py = y1 + (int) (yStep * i);
            graphics.fill(px, py, px + 1, py + 1, color);
        }
    }
    
    /**
     * Linearly interpolates between two colors.
     */
    private static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) Mth.lerp(t, a1, a2);
        int r = (int) Mth.lerp(t, r1, r2);
        int g = (int) Mth.lerp(t, g1, g2);
        int b = (int) Mth.lerp(t, b1, b2);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Internal class representing a slot in the radial menu.
     */
    private static class RadialSlot {
        final MenuCategory category;
        final double angle;
        
        RadialSlot(MenuCategory category, double angle) {
            this.category = category;
            this.angle = angle;
        }
    }
}
