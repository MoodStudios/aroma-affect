package com.ovrtechnology.menu;

import com.ovrtechnology.AromaCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for card-based selection menus (Blocks, Biomes, Structures).
 * 
 * <p>This provides a common framework for menus that display a grid of selectable
 * "cards" representing items that can be tracked by the Nose.</p>
 * 
 * <p>Features to implement:</p>
 * <ul>
 *   <li>Scrollable grid of cards</li>
 *   <li>Search/filter functionality</li>
 *   <li>Visual feedback for locked/unlocked items</li>
 *   <li>Selection highlighting</li>
 *   <li>Back navigation to radial menu</li>
 * </ul>
 */
public abstract class SelectionMenuScreen extends BaseMenuScreen {
    
    /**
     * The category this menu represents.
     */
    protected final MenuCategory category;
    
    /**
     * The list of selectable cards.
     */
    protected final List<SelectionCard> cards = new ArrayList<>();
    
    /**
     * Currently selected card index, or -1 if none.
     */
    protected int selectedCardIndex = -1;
    
    /**
     * Currently hovered card index, or -1 if none.
     */
    protected int hoveredCardIndex = -1;
    
    /**
     * Scroll offset for the card grid.
     */
    protected int scrollOffset = 0;
    
    /**
     * Number of cards per row in the grid.
     */
    protected static final int CARDS_PER_ROW = 6;
    
    /**
     * Size of each card in pixels.
     */
    protected static final int CARD_SIZE = 48;
    
    /**
     * Gap between cards.
     */
    protected static final int CARD_GAP = 8;
    
    /**
     * Card background color.
     */
    protected static final int CARD_COLOR = 0xB0222222;
    
    /**
     * Card hover color.
     */
    protected static final int CARD_HOVER_COLOR = 0xE0444488;
    
    /**
     * Card selected color.
     */
    protected static final int CARD_SELECTED_COLOR = 0xE0226622;
    
    /**
     * Card locked overlay color.
     */
    protected static final int CARD_LOCKED_COLOR = 0x80000000;
    
    protected SelectionMenuScreen(MenuCategory category) {
        super(Component.translatable("menu.aromacraft." + category.getId() + ".title"));
        this.category = category;
    }
    
    @Override
    protected void init() {
        super.init();
        loadCards();
    }
    
    /**
     * Loads the cards to display in this menu.
     * Override this to populate the cards list with appropriate items.
     */
    protected abstract void loadCards();
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, 
                                  float partialTick, float animationProgress) {
        int centerX = width / 2;
        
        // Calculate grid area
        int gridWidth = CARDS_PER_ROW * (CARD_SIZE + CARD_GAP) - CARD_GAP;
        int gridStartX = centerX - gridWidth / 2;
        int gridStartY = 60;
        
        // Render header
        renderHeader(graphics, centerX, animationProgress);
        
        // Render card grid
        renderCardGrid(graphics, gridStartX, gridStartY, mouseX, mouseY, animationProgress);
        
        // Render footer with back button hint
        renderFooter(graphics, centerX, animationProgress);
    }
    
    /**
     * Renders the header with category title and icon.
     */
    protected void renderHeader(GuiGraphics graphics, int centerX, float animationProgress) {
        float alpha = animationProgress;
        int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
        
        // Category title
        Component title = getTitle();
        graphics.drawCenteredString(font, title, centerX, 20, textColor);
        
        // Category icon
        if (animationProgress > 0.3f) {
            float iconAlpha = (animationProgress - 0.3f) / 0.7f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX - 40 - font.width(title) / 2, 16);
            graphics.pose().scale(iconAlpha, iconAlpha);
            graphics.renderItem(category.getIconItem(), 0, 0);
            graphics.pose().popMatrix();
        }
        
        // Description
        Component description = category.getDescription();
        int descColor = (int) (200 * alpha) << 24 | 0xAAAAAA;
        graphics.drawCenteredString(font, description, centerX, 35, descColor);
    }
    
    /**
     * Renders the grid of selection cards.
     */
    protected void renderCardGrid(GuiGraphics graphics, int startX, int startY, 
                                   int mouseX, int mouseY, float animationProgress) {
        hoveredCardIndex = -1;
        
        for (int i = 0; i < cards.size(); i++) {
            int row = i / CARDS_PER_ROW;
            int col = i % CARDS_PER_ROW;
            
            int cardX = startX + col * (CARD_SIZE + CARD_GAP);
            int cardY = startY + row * (CARD_SIZE + CARD_GAP) - scrollOffset;
            
            // Skip cards outside visible area
            if (cardY + CARD_SIZE < startY || cardY > height - 60) {
                continue;
            }
            
            // Check hover
            boolean isHovered = mouseX >= cardX && mouseX < cardX + CARD_SIZE &&
                               mouseY >= cardY && mouseY < cardY + CARD_SIZE;
            if (isHovered) {
                hoveredCardIndex = i;
            }
            
            SelectionCard card = cards.get(i);
            boolean isSelected = i == selectedCardIndex;
            
            renderCard(graphics, card, cardX, cardY, isHovered, isSelected, animationProgress);
        }
    }
    
    /**
     * Renders a single selection card.
     */
    protected void renderCard(GuiGraphics graphics, SelectionCard card, 
                              int x, int y, boolean isHovered, boolean isSelected,
                              float animationProgress) {
        // Determine card color
        int cardColor;
        if (isSelected) {
            cardColor = CARD_SELECTED_COLOR;
        } else if (isHovered) {
            cardColor = CARD_HOVER_COLOR;
        } else {
            cardColor = CARD_COLOR;
        }
        
        // Apply animation alpha
        int alpha = (int) (((cardColor >> 24) & 0xFF) * animationProgress);
        cardColor = (alpha << 24) | (cardColor & 0x00FFFFFF);
        
        // Draw card background
        graphics.fill(x, y, x + CARD_SIZE, y + CARD_SIZE, cardColor);
        
        // Draw border
        int borderColor = isSelected ? 0xFF44FF44 : (isHovered ? 0xFFAAAAFF : 0xFF666666);
        borderColor = (int) (255 * animationProgress) << 24 | (borderColor & 0x00FFFFFF);
        drawCardBorder(graphics, x, y, CARD_SIZE, CARD_SIZE, borderColor);
        
        // Render icon
        if (animationProgress > 0.2f && card.icon != null) {
            float iconScale = (animationProgress - 0.2f) / 0.8f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + CARD_SIZE / 2, y + CARD_SIZE / 2);
            graphics.pose().scale(iconScale * 1.5f, iconScale * 1.5f);
            graphics.renderItem(card.icon, -8, -8);
            graphics.pose().popMatrix();
        }
        
        // Draw locked overlay if applicable
        if (!card.isUnlocked) {
            int lockAlpha = (int) (128 * animationProgress);
            graphics.fill(x, y, x + CARD_SIZE, y + CARD_SIZE, (lockAlpha << 24));
            
            // Draw lock icon or indicator
            int lockColor = (int) (255 * animationProgress) << 24 | 0xFF5555;
            graphics.drawCenteredString(font, "🔒", x + CARD_SIZE / 2, y + CARD_SIZE / 2 - 4, lockColor);
        }
    }
    
    /**
     * Draws a border around a card.
     */
    protected void drawCardBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color); // Top
        graphics.fill(x, y + height - 1, x + width, y + height, color); // Bottom
        graphics.fill(x, y, x + 1, y + height, color); // Left
        graphics.fill(x + width - 1, y, x + width, y + height, color); // Right
    }
    
    /**
     * Renders the footer with navigation hints.
     */
    protected void renderFooter(GuiGraphics graphics, int centerX, float animationProgress) {
        float alpha = animationProgress;
        int textColor = (int) (180 * alpha) << 24 | 0xAAAAAA;
        
        Component backHint = Component.translatable("menu.aromacraft.selection.back_hint");
        graphics.drawCenteredString(font, backHint, centerX, height - 20, textColor);
    }
    
    /**
     * Handles mouse click events for card selection.
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredCardIndex >= 0) {
            SelectionCard card = cards.get(hoveredCardIndex);
            if (card.isUnlocked) {
                onCardSelected(card, hoveredCardIndex);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles mouse scroll events for scrolling the card grid.
     */
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, (cards.size() / CARDS_PER_ROW + 1) * (CARD_SIZE + CARD_GAP) - (height - 120));
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * 20));
        return true;
    }
    
    /**
     * Handles key press events.
     */
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            MenuManager.returnToRadialMenu();
            return true;
        }
        return false;
    }
    
    /**
     * Called when a card is selected.
     * 
     * @param card the selected card
     * @param index the index of the selected card
     */
    protected abstract void onCardSelected(SelectionCard card, int index);
    
    /**
     * Represents a selectable card in the menu.
     */
    public static class SelectionCard {
        /**
         * The unique identifier for this card's target.
         */
        public final ResourceLocation id;
        
        /**
         * The display name of this card.
         */
        public final Component displayName;
        
        /**
         * The icon to display.
         */
        public final ItemStack icon;
        
        /**
         * Whether this card is unlocked for selection.
         */
        public boolean isUnlocked;
        
        /**
         * Optional tooltip/description.
         */
        public final Component description;
        
        public SelectionCard(ResourceLocation id, Component displayName, ItemStack icon, 
                            boolean isUnlocked, Component description) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.isUnlocked = isUnlocked;
            this.description = description;
        }
        
        public SelectionCard(ResourceLocation id, Component displayName, ItemStack icon, boolean isUnlocked) {
            this(id, displayName, icon, isUnlocked, null);
        }
    }
}
