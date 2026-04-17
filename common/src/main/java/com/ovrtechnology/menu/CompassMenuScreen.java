package com.ovrtechnology.menu;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Menu screen for the compass/tracking overview.
 * 
 * <p>This menu displays the current tracking status and allows players to
 * manage their active tracking targets. It shows directional guidance
 * towards tracked locations.</p>
 * 
 * <p><b>NOTE:</b> This is a placeholder implementation. Full implementation should include:</p>
 * <ul>
 *   <li>Current tracking target display</li>
 *   <li>Directional compass indicator</li>
 *   <li>Distance estimation</li>
 *   <li>Quick access to change/clear tracking target</li>
 *   <li>Recent tracking history</li>
 * </ul>
 * 
 * @see MenuManager#openCompassMenu()
 */
public class CompassMenuScreen extends BaseMenuScreen {
    
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 160;
    private static final int COLOR_PANEL_BG = 0xCC1A1A2E;
    private static final int COLOR_PANEL_BORDER = 0x88AAAACC;
    private static final int COLOR_TEXT_TITLE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_INFO = 0xFFCCCCCC;
    private static final int COLOR_TEXT_INACTIVE = 0xFF888888;
    
    public CompassMenuScreen() {
        super(Texts.tr("menu.aromaaffect.compass.title"));
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, float animationProgress) {
        // Calculate panel position (centered)
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        
        // Apply animation
        float scale = Mth.clamp(animationProgress * 1.2f, 0.0f, 1.0f);
        float alpha = animationProgress;
        
        // Render panel background
        int bgColor = MenuRenderUtils.withAlpha(COLOR_PANEL_BG, alpha);
        int borderColor = MenuRenderUtils.withAlpha(COLOR_PANEL_BORDER, alpha);
        
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, bgColor);
        MenuRenderUtils.renderOutline(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, borderColor);
        
        // Render title
        int titleColor = MenuRenderUtils.withAlpha(COLOR_TEXT_TITLE, alpha);
        Component title = Texts.tr("menu.aromaaffect.compass.title");
        graphics.drawCenteredString(font, title, width / 2, panelY + 12, titleColor);
        
        // Render separator line
        graphics.fill(panelX + 10, panelY + 28, panelX + PANEL_WIDTH - 10, panelY + 29, borderColor);
        
        // Render tracking status
        int infoColor = MenuRenderUtils.withAlpha(COLOR_TEXT_INFO, alpha);
        int inactiveColor = MenuRenderUtils.withAlpha(COLOR_TEXT_INACTIVE, alpha);
        
        // Current target (placeholder)
        Component targetLabel = Texts.tr("menu.aromaaffect.compass.target");
        graphics.drawString(font, targetLabel, panelX + 15, panelY + 40, infoColor);
        
        // Show "No active target" placeholder
        Component noTarget = Texts.tr("menu.aromaaffect.compass.no_target");
        graphics.drawString(font, noTarget, panelX + 15, panelY + 55, inactiveColor);
        
        // Render compass direction placeholder
        int compassCenterX = width / 2;
        int compassCenterY = panelY + 100;
        int compassRadius = 30;
        
        // Draw compass circle outline
        drawCircleOutline(graphics, compassCenterX, compassCenterY, compassRadius, borderColor);
        
        // Draw cardinal directions
        graphics.drawCenteredString(font, "N", compassCenterX, compassCenterY - compassRadius - 10, infoColor);
        graphics.drawCenteredString(font, "S", compassCenterX, compassCenterY + compassRadius + 4, infoColor);
        graphics.drawString(font, "W", compassCenterX - compassRadius - 10, compassCenterY - 4, infoColor);
        graphics.drawString(font, "E", compassCenterX + compassRadius + 4, compassCenterY - 4, infoColor);
        
        // Hint text at bottom
        Component hint = Texts.tr("menu.aromaaffect.compass.hint");
        graphics.drawCenteredString(font, hint, width / 2, panelY + PANEL_HEIGHT - 15, inactiveColor);
    }
    
    /**
     * Draws a simple circle outline using line segments.
     */
    private void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int segments = 32;
        double angleStep = Math.PI * 2 / segments;
        
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            
            int x1 = centerX + (int)(Math.cos(angle1) * radius);
            int y1 = centerY + (int)(Math.sin(angle1) * radius);
            int x2 = centerX + (int)(Math.cos(angle2) * radius);
            int y2 = centerY + (int)(Math.sin(angle2) * radius);
            
            // Draw a small line segment (1px thick approximation)
            graphics.fill(Math.min(x1, x2), Math.min(y1, y2), 
                         Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        // Close on any click for now (placeholder behavior)
        return false;
    }
    
    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            onClose();
            return true;
        }
        return false;
    }
}
