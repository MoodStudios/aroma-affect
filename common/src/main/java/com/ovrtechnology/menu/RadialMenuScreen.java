package com.ovrtechnology.menu;

import com.ovrtechnology.AromaCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Radial menu screen for selecting tracking categories.
 * 
 * <p>This is the main navigation menu of AromaCraft, activated by pressing the radial menu hotkey (default: R).
 * It displays a circular ring with category segments, matching the OVR Technology aesthetic.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Circular ring design with segment highlighting</li>
 *   <li>Smooth opening/closing animations with easing</li>
 *   <li>Hover highlighting with glow effects</li>
 *   <li>Category icons rendered on the ring</li>
 *   <li>OVR Technology branding at the top</li>
 * </ul>
 */
public class RadialMenuScreen extends BaseMenuScreen {
    
    // ==================== RING DIMENSIONS ====================
    
    /** Outer radius of the ring in pixels. */
    private static final int OUTER_RADIUS = 95;
    
    /** Inner radius of the ring (creates the hollow center). */
    private static final int INNER_RADIUS = 55;
    
    // ==================== COLORS ====================
    
    /** Base color for ring segments (semi-transparent dark blue). */
    private static final int SEGMENT_BASE_COLOR = 0xA0202840;
    
    /** Hovered segment color (purple-blue highlight). */
    private static final int SEGMENT_HOVER_COLOR = 0xD0445588;
    
    /** Inner ring edge glow color. */
    private static final int INNER_GLOW_COLOR = 0x606080B0;
    
    /** Outer ring edge glow color. */
    private static final int OUTER_GLOW_COLOR = 0x808090C0;
    
    /** Segment divider line color. */
    private static final int DIVIDER_COLOR = 0xC0607090;
    
    // ==================== STATE ====================
    
    /** The category segments to display. */
    private final List<RingSegment> segments = new ArrayList<>();
    
    /** Currently hovered segment index, or -1 if none. */
    private int hoveredSegmentIndex = -1;
    
    /** Individual segment animation progress for hover effects. */
    private final float[] segmentHoverProgress;
    
    /** Number of segments to render. */
    private final int segmentCount;
    
    public RadialMenuScreen() {
        super(Component.translatable("menu.aromacraft.radial.title"));
        
        MenuCategory[] categories = MenuCategory.values();
        this.segmentCount = categories.length;
        this.segmentHoverProgress = new float[segmentCount];
        
        initializeSegments();
    }
    
    /**
     * Initializes the ring segments with their angular positions.
     * Segments are evenly distributed around the ring starting from the top.
     */
    private void initializeSegments() {
        segments.clear();
        
        MenuCategory[] categories = MenuCategory.values();
        double anglePerSegment = 360.0 / segmentCount;
        
        // Start from top (-90 degrees) and distribute evenly
        // Offset by half a segment so icons sit at segment centers
        double startAngle = -90.0 - (anglePerSegment / 2.0);
        
        for (int i = 0; i < categories.length; i++) {
            double segmentStartAngle = startAngle + (i * anglePerSegment);
            double segmentEndAngle = segmentStartAngle + anglePerSegment;
            double iconAngle = segmentStartAngle + (anglePerSegment / 2.0);
            
            segments.add(new RingSegment(categories[i], segmentStartAngle, segmentEndAngle, iconAngle));
        }
    }
    
    @Override
    protected void init() {
        super.init();
        // Reset segment hover progress
        for (int i = 0; i < segmentHoverProgress.length; i++) {
            segmentHoverProgress[i] = 0.0f;
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Update segment hover animations
        for (int i = 0; i < segments.size(); i++) {
            if (i == hoveredSegmentIndex) {
                segmentHoverProgress[i] = Math.min(1.0f, segmentHoverProgress[i] + 0.2f);
            } else {
                segmentHoverProgress[i] = Math.max(0.0f, segmentHoverProgress[i] - 0.15f);
            }
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, 
                                  float partialTick, float animProgress) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Calculate animated radii (ring expands outward during animation)
        float animatedOuterRadius = OUTER_RADIUS * animProgress;
        float animatedInnerRadius = INNER_RADIUS * animProgress;
        
        // Update hovered segment based on mouse position
        updateHoveredSegment(mouseX, mouseY, centerX, centerY, animatedOuterRadius, animatedInnerRadius);
        
        // Render OVR logo at top
        renderOvrLogo(graphics, centerX, animProgress);
        
        // Render the ring segments
        renderRingSegments(graphics, centerX, centerY, animatedOuterRadius, animatedInnerRadius, 
                          animProgress, partialTick);
        
        // Render segment icons
        renderSegmentIcons(graphics, centerX, centerY, animProgress, partialTick);
        
        // Render ring borders/glow
        renderRingBorders(graphics, centerX, centerY, animatedOuterRadius, animatedInnerRadius, animProgress);
        
        // Render hovered segment tooltip
        if (hoveredSegmentIndex >= 0 && animProgress > 0.5f) {
            RingSegment hoveredSegment = segments.get(hoveredSegmentIndex);
            renderSegmentTooltip(graphics, hoveredSegment, centerX, centerY, animProgress);
        }
    }
    
    /**
     * Renders the OVR Technology logo at the top of the screen.
     */
    private void renderOvrLogo(GuiGraphics graphics, int centerX, float animProgress) {
        if (animProgress < 0.3f) return;
        
        float logoAlpha = (animProgress - 0.3f) / 0.7f;
        int logoY = 20;
        
        // Render text-based logo with OVR branding
        // Using text rendering as fallback - a proper texture can be added later
        int textColor = ((int)(255 * logoAlpha) << 24) | 0xFFFFFF;
        graphics.drawCenteredString(font, "OVR", centerX, logoY, textColor);
        
        int subTextColor = ((int)(180 * logoAlpha) << 24) | 0xBBBBBB;
        graphics.drawCenteredString(font, "technology", centerX, logoY + 12, subTextColor);
    }
    
    /**
     * Renders all ring segments with proper coloring and hover effects.
     */
    private void renderRingSegments(GuiGraphics graphics, int centerX, int centerY,
                                     float outerRadius, float innerRadius,
                                     float animProgress, float partialTick) {
        for (int i = 0; i < segments.size(); i++) {
            RingSegment segment = segments.get(i);
            float hoverProgress = getInterpolatedHoverProgress(i, partialTick);
            
            renderSegment(graphics, segment, i, centerX, centerY, outerRadius, innerRadius,
                         animProgress, hoverProgress);
        }
    }
    
    /**
     * Renders a single ring segment.
     */
    private void renderSegment(GuiGraphics graphics, RingSegment segment, int index,
                               int centerX, int centerY, float outerRadius, float innerRadius,
                               float animProgress, float hoverProgress) {
        // Calculate segment color with hover interpolation
        int baseColor = segment.category.isImplemented() ? SEGMENT_BASE_COLOR : 
                        (SEGMENT_BASE_COLOR & 0x00FFFFFF) | 0x60000000; // Dimmer for unimplemented
        int hoverColor = segment.category.isImplemented() ? SEGMENT_HOVER_COLOR :
                        (SEGMENT_HOVER_COLOR & 0x00FFFFFF) | 0x80000000;
        int segmentColor = lerpColor(baseColor, hoverColor, hoverProgress);
        
        // Apply animation alpha
        int alpha = (int)(((segmentColor >> 24) & 0xFF) * animProgress);
        segmentColor = (alpha << 24) | (segmentColor & 0x00FFFFFF);
        
        // Draw the segment as a filled arc
        drawFilledArc(graphics, centerX, centerY, innerRadius, outerRadius,
                     segment.startAngle, segment.endAngle, segmentColor);
        
        // Draw segment divider lines
        if (animProgress > 0.5f) {
            float dividerAlpha = (animProgress - 0.5f) / 0.5f;
            int dividerColor = (int)(((DIVIDER_COLOR >> 24) & 0xFF) * dividerAlpha) << 24 | 
                              (DIVIDER_COLOR & 0x00FFFFFF);
            
            drawRadialLine(graphics, centerX, centerY, innerRadius, outerRadius, 
                          segment.startAngle, dividerColor);
        }
    }
    
    /**
     * Renders category icons on the ring.
     */
    private void renderSegmentIcons(GuiGraphics graphics, int centerX, int centerY,
                                     float animProgress, float partialTick) {
        if (animProgress < 0.4f) return;
        
        float iconAlpha = (animProgress - 0.4f) / 0.6f;
        float iconRadius = (OUTER_RADIUS + INNER_RADIUS) / 2.0f * animProgress;
        
        for (int i = 0; i < segments.size(); i++) {
            RingSegment segment = segments.get(i);
            float hoverProgress = getInterpolatedHoverProgress(i, partialTick);
            
            // Calculate icon position on the ring
            double angleRad = Math.toRadians(segment.iconAngle);
            int iconX = centerX + (int)(iconRadius * Math.cos(angleRad));
            int iconY = centerY + (int)(iconRadius * Math.sin(angleRad));
            
            // Scale icons slightly when hovered
            float scale = iconAlpha * (1.0f + hoverProgress * 0.15f);
            
            graphics.pose().pushMatrix();
            graphics.pose().translate(iconX, iconY);
            graphics.pose().scale(scale, scale);
            
            // Render the item icon (centered at 0,0 since we translated)
            graphics.renderItem(segment.category.getIconItem(), -8, -8);
            
            graphics.pose().popMatrix();
        }
    }
    
    /**
     * Renders the inner and outer ring borders with glow effect.
     */
    private void renderRingBorders(GuiGraphics graphics, int centerX, int centerY,
                                    float outerRadius, float innerRadius, float animProgress) {
        if (animProgress < 0.2f) return;
        
        float borderAlpha = (animProgress - 0.2f) / 0.8f;
        
        // Outer ring glow
        int outerColor = (int)(((OUTER_GLOW_COLOR >> 24) & 0xFF) * borderAlpha) << 24 |
                        (OUTER_GLOW_COLOR & 0x00FFFFFF);
        drawCircle(graphics, centerX, centerY, outerRadius, outerColor);
        drawCircle(graphics, centerX, centerY, outerRadius + 1, (outerColor & 0x00FFFFFF) | 0x40000000);
        
        // Inner ring glow
        int innerColor = (int)(((INNER_GLOW_COLOR >> 24) & 0xFF) * borderAlpha) << 24 |
                        (INNER_GLOW_COLOR & 0x00FFFFFF);
        drawCircle(graphics, centerX, centerY, innerRadius, innerColor);
        drawCircle(graphics, centerX, centerY, innerRadius - 1, (innerColor & 0x00FFFFFF) | 0x40000000);
    }
    
    /**
     * Renders the tooltip for a hovered segment.
     */
    private void renderSegmentTooltip(GuiGraphics graphics, RingSegment segment,
                                       int centerX, int centerY, float animProgress) {
        float tooltipAlpha = (animProgress - 0.5f) / 0.5f;
        
        Component name = segment.category.getDisplayName();
        Component description = segment.category.getDescription();
        
        // Render category name above center
        int nameY = centerY - 8;
        int nameColor = segment.category.isImplemented() ? 
                       ((int)(255 * tooltipAlpha) << 24 | 0xFFFFFF) :
                       ((int)(255 * tooltipAlpha) << 24 | 0x888888);
        graphics.drawCenteredString(font, name, centerX, nameY, nameColor);
        
        // Render description below the ring
        int descY = centerY + OUTER_RADIUS + 20;
        int descColor = (int)(255 * tooltipAlpha) << 24 | 0xCCCCCC;
        graphics.drawCenteredString(font, description, centerX, descY, descColor);
    }
    
    /**
     * Updates which segment is currently hovered based on mouse position.
     */
    private void updateHoveredSegment(int mouseX, int mouseY, int centerX, int centerY,
                                       float outerRadius, float innerRadius) {
        hoveredSegmentIndex = -1;
        
        // Calculate distance from center
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        
        // Check if within ring
        if (distance < innerRadius || distance > outerRadius) {
            return;
        }
        
        // Calculate angle (convert to degrees, adjust for coordinate system)
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        
        // Find which segment contains this angle
        for (int i = 0; i < segments.size(); i++) {
            RingSegment segment = segments.get(i);
            if (isAngleInSegment(angle, segment.startAngle, segment.endAngle)) {
                hoveredSegmentIndex = i;
                break;
            }
        }
    }
    
    /**
     * Checks if an angle falls within a segment's angular range.
     */
    private boolean isAngleInSegment(double angle, double startAngle, double endAngle) {
        // Normalize angles to 0-360 range
        angle = normalizeAngle(angle);
        double start = normalizeAngle(startAngle);
        double end = normalizeAngle(endAngle);
        
        if (start <= end) {
            return angle >= start && angle < end;
        } else {
            // Segment wraps around 360
            return angle >= start || angle < end;
        }
    }
    
    /**
     * Normalizes an angle to the 0-360 range.
     */
    private double normalizeAngle(double angle) {
        angle = angle % 360.0;
        if (angle < 0) angle += 360.0;
        return angle;
    }
    
    /**
     * Gets the interpolated hover progress for smooth animations.
     */
    private float getInterpolatedHoverProgress(int segmentIndex, float partialTick) {
        if (segmentIndex >= segmentHoverProgress.length) return 0.0f;
        
        float current = segmentHoverProgress[segmentIndex];
        float target = (segmentIndex == hoveredSegmentIndex) ? 1.0f : 0.0f;
        float speed = (target > current) ? 0.2f : 0.15f;
        return Mth.lerp(partialTick * speed, current, target);
    }
    
    // ==================== DRAWING UTILITIES ====================
    
    /**
     * Draws a filled arc (pie slice with hollow center).
     */
    private void drawFilledArc(GuiGraphics graphics, int centerX, int centerY,
                                float innerRadius, float outerRadius,
                                double startAngle, double endAngle, int color) {
        // Draw arc by rendering many small triangular segments
        int segments = 32;
        double angleRange = endAngle - startAngle;
        double angleStep = angleRange / segments;
        
        for (int i = 0; i < segments; i++) {
            double a1 = Math.toRadians(startAngle + i * angleStep);
            double a2 = Math.toRadians(startAngle + (i + 1) * angleStep);
            
            // Calculate the four corners of this segment quad
            int ox1 = centerX + (int)(outerRadius * Math.cos(a1));
            int oy1 = centerY + (int)(outerRadius * Math.sin(a1));
            int ox2 = centerX + (int)(outerRadius * Math.cos(a2));
            int oy2 = centerY + (int)(outerRadius * Math.sin(a2));
            int ix1 = centerX + (int)(innerRadius * Math.cos(a1));
            int iy1 = centerY + (int)(innerRadius * Math.sin(a1));
            int ix2 = centerX + (int)(innerRadius * Math.cos(a2));
            int iy2 = centerY + (int)(innerRadius * Math.sin(a2));
            
            // Draw as two triangles forming a quad
            drawTriangle(graphics, ox1, oy1, ox2, oy2, ix1, iy1, color);
            drawTriangle(graphics, ix1, iy1, ox2, oy2, ix2, iy2, color);
        }
    }
    
    /**
     * Draws a radial line from inner to outer radius.
     */
    private void drawRadialLine(GuiGraphics graphics, int centerX, int centerY,
                                 float innerRadius, float outerRadius,
                                 double angle, int color) {
        double angleRad = Math.toRadians(angle);
        int x1 = centerX + (int)(innerRadius * Math.cos(angleRad));
        int y1 = centerY + (int)(innerRadius * Math.sin(angleRad));
        int x2 = centerX + (int)(outerRadius * Math.cos(angleRad));
        int y2 = centerY + (int)(outerRadius * Math.sin(angleRad));
        
        drawLine(graphics, x1, y1, x2, y2, color);
    }
    
    /**
     * Draws a circle outline.
     */
    private void drawCircle(GuiGraphics graphics, int centerX, int centerY, float radius, int color) {
        int segments = 64;
        double angleStep = 360.0 / segments;
        
        for (int i = 0; i < segments; i++) {
            double a1 = Math.toRadians(i * angleStep);
            double a2 = Math.toRadians((i + 1) * angleStep);
            
            int x1 = centerX + (int)(radius * Math.cos(a1));
            int y1 = centerY + (int)(radius * Math.sin(a1));
            int x2 = centerX + (int)(radius * Math.cos(a2));
            int y2 = centerY + (int)(radius * Math.sin(a2));
            
            drawLine(graphics, x1, y1, x2, y2, color);
        }
    }
    
    /**
     * Draws a filled triangle.
     */
    private void drawTriangle(GuiGraphics graphics, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        // Use a scanline approach for simple triangle fill
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));
        
        for (int y = minY; y <= maxY; y++) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            
            // Find intersections with triangle edges
            int[] edges = {x1, y1, x2, y2, x2, y2, x3, y3, x3, y3, x1, y1};
            for (int i = 0; i < 3; i++) {
                int ex1 = edges[i * 4];
                int ey1 = edges[i * 4 + 1];
                int ex2 = edges[i * 4 + 2];
                int ey2 = edges[i * 4 + 3];
                
                if ((ey1 <= y && ey2 > y) || (ey2 <= y && ey1 > y)) {
                    float t = (float)(y - ey1) / (ey2 - ey1);
                    int x = ex1 + (int)(t * (ex2 - ex1));
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
            
            if (minX <= maxX) {
                graphics.fill(minX, y, maxX + 1, y + 1, color);
            }
        }
    }
    
    /**
     * Draws a simple line between two points.
     */
    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        
        if (steps == 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        
        float xStep = (float)(x2 - x1) / steps;
        float yStep = (float)(y2 - y1) / steps;
        
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (int)(xStep * i);
            int py = y1 + (int)(yStep * i);
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
        
        int a = (int)Mth.lerp(t, a1, a2);
        int r = (int)Mth.lerp(t, r1, r2);
        int g = (int)Mth.lerp(t, g1, g2);
        int b = (int)Mth.lerp(t, b1, b2);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    // ==================== INPUT HANDLING ====================
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && hoveredSegmentIndex >= 0) {
            RingSegment segment = segments.get(hoveredSegmentIndex);
            onCategorySelected(segment.category);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // Escape
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
    
    /**
     * Called when a category is selected.
     * Opens the corresponding category menu or shows "coming soon" for unimplemented categories.
     */
    protected void onCategorySelected(MenuCategory category) {
        AromaCraft.LOGGER.debug("Selected category: {}", category.getId());
        
        if (!category.isImplemented()) {
            // TODO: Show "coming soon" toast or notification
            AromaCraft.LOGGER.info("Category {} is not yet implemented", category.getId());
            return;
        }
        
        // Open the appropriate menu for the selected category
        switch (category) {
            case BLOCKS -> MenuManager.openBlocksMenu();
            case BIOMES -> MenuManager.openBiomesMenu();
            case STRUCTURES -> MenuManager.openStructuresMenu();
            default -> AromaCraft.LOGGER.warn("No menu handler for category: {}", category.getId());
        }
    }
    
    // ==================== INTERNAL CLASSES ====================
    
    /**
     * Represents a segment in the radial ring menu.
     */
    private static class RingSegment {
        final MenuCategory category;
        final double startAngle;
        final double endAngle;
        final double iconAngle;
        
        RingSegment(MenuCategory category, double startAngle, double endAngle, double iconAngle) {
            this.category = category;
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.iconAngle = iconAngle;
        }
    }
}
