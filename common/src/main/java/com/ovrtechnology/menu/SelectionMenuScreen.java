package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.PathScentNetworking;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Abstract base for the four searchable selection menus
 * (Blocks, Biomes, Structures, Flowers).
 *
 * <p>Subclasses only need to provide:</p>
 * <ul>
 *   <li>{@link #loadCards()} — populate the {@code cards} list</li>
 *   <li>{@link #getRowHeight()} — 32 for compact, 56 for thumbnail rows</li>
 *   <li>{@link #renderRow} — draw one list row</li>
 * </ul>
 *
 * <p>Optionally override the filter hooks for menus with filter chips.</p>
 */
public abstract class SelectionMenuScreen extends BaseMenuScreen {

    // ── Shared layout constants ──────────────────────────────────────────

    protected static final int ROW_PADDING = 4;
    protected static final int ICON_SIZE = 24;
    protected static final int MAX_LIST_WIDTH = 350;
    protected static final int SEARCH_BOX_HEIGHT = 20;
    protected static final int HEADER_ICON_SIZE = 16;
    protected static final int BACK_BUTTON_SIZE = 24;
    protected static final int BACK_BUTTON_PADDING = 8;

    protected static final ResourceLocation ICON_BACK = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_back.png");

    // ── Shared row colors ────────────────────────────────────────────────

    protected static final int ROW_COLOR = 0xB0222222;
    protected static final int ROW_HOVER_COLOR = 0xE0444488;
    protected static final int ROW_TRACKING_COLOR = 0xC0224422;
    protected static final int ROW_TRACKING_HOVER_COLOR = 0xE0336633;

    // ── Shared state ─────────────────────────────────────────────────────

    protected final MenuCategory category;
    protected final List<SelectionCard> cards = new ArrayList<>();

    protected EditBox searchBox;
    protected String searchQuery = "";
    protected List<SelectionCard> filteredCards = new ArrayList<>();
    protected int listScrollOffset = 0;
    protected int hoveredListIndex = -1;
    protected boolean isHoveringBackButton = false;

    protected int selectedCardIndex = -1;

    // ── Constructor ──────────────────────────────────────────────────────

    protected SelectionMenuScreen(MenuCategory category) {
        super(Component.translatable("menu.aromaaffect." + category.getId() + ".title"));
        this.category = category;
    }

    // ── Abstract / overridable hooks ─────────────────────────────────────

    /** Populate {@link #cards} from the equipped nose's abilities. */
    protected abstract void loadCards();

    /** Pixel height of a single list row (32 for compact, 56 for thumbnail). */
    protected abstract int getRowHeight();

    /** Render one row at the given position. */
    protected abstract void renderRow(GuiGraphics graphics, SelectionCard card,
                                      int x, int y, int rowWidth,
                                      boolean isHovered, boolean isTracking,
                                      float animationProgress);

    /**
     * Extra per-card filter logic beyond the search query.
     * Return {@code true} to include the card, {@code false} to exclude it.
     * Default implementation accepts all cards.
     */
    protected boolean passesFilter(SelectionCard card) {
        return true;
    }

    /**
     * Handle a click that lands on the filter chip area.
     * Return {@code true} if the click was consumed.
     */
    protected boolean handleFilterClick(int mouseX, int mouseY) {
        return false;
    }

    /**
     * Render any filter chips (or other UI) between the search box and the list.
     * Return the Y coordinate where the item list should start.
     */
    protected int renderBelowSearch(GuiGraphics graphics, int listX, int chipY,
                                    int listWidth, int mouseX, int mouseY,
                                    float animationProgress) {
        return chipY;
    }

    /**
     * Y offset where the scrollable list begins, used for scroll calculations.
     * Override in subclasses that render filter chips to account for chip height.
     */
    protected int getListTopOffset() {
        return 52 + SEARCH_BOX_HEIGHT + 8;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int searchX = (width - listWidth) / 2;
        int searchY = 52;

        searchBox = new EditBox(font, searchX, searchY, listWidth, SEARCH_BOX_HEIGHT,
                Component.translatable("menu.aromaaffect." + category.getId() + ".search_placeholder"));
        searchBox.setHint(Component.translatable("menu.aromaaffect." + category.getId() + ".search_placeholder"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(query -> {
            searchQuery = query;
            applyFilters();
        });
        addWidget(searchBox);

        loadCards();
        applyFilters();
    }

    // ── Filtering ────────────────────────────────────────────────────────

    protected void applyFilters() {
        filteredCards.clear();
        String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);

        for (SelectionCard card : cards) {
            if (!passesFilter(card)) {
                continue;
            }
            if (!lowerQuery.isEmpty()) {
                String name = card.displayName.getString().toLowerCase(Locale.ROOT);
                String id = card.id.toString().toLowerCase(Locale.ROOT);
                if (!name.contains(lowerQuery) && !id.contains(lowerQuery)) {
                    continue;
                }
            }
            filteredCards.add(card);
        }

        listScrollOffset = 0;
    }

    // ── Rendering ────────────────────────────────────────────────────────

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                  float partialTick, float animationProgress) {
        int centerX = width / 2;
        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int listX = (width - listWidth) / 2;

        renderHeader(graphics, centerX, animationProgress);
        renderBackButton(graphics, mouseX, mouseY, animationProgress);

        // Search box
        int searchY = 52;
        searchBox.setX(listX);
        searchBox.setY(searchY);
        searchBox.setWidth(listWidth);
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        // Optional filter chips (subclass hook)
        int chipY = searchY + SEARCH_BOX_HEIGHT + 6;
        int listTop = renderBelowSearch(graphics, listX, chipY, listWidth, mouseX, mouseY, animationProgress);
        if (listTop == chipY) {
            // No filter chips rendered — use standard spacing
            listTop = searchY + SEARCH_BOX_HEIGHT + 8;
        }

        int listBottom = height - 10;
        renderItemList(graphics, listX, listTop, listWidth, listBottom, mouseX, mouseY, animationProgress);
    }

    protected void renderHeader(GuiGraphics graphics, int centerX, float animationProgress) {
        float alpha = animationProgress;
        int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;

        Component title = getTitle();
        graphics.drawCenteredString(font, title, centerX, 20, textColor);

        if (animationProgress > 0.3f) {
            float iconAlpha = (animationProgress - 0.3f) / 0.7f;
            int iconX = centerX - font.width(title) / 2 - HEADER_ICON_SIZE - 4;
            int iconY = 16;
            int iconSize = (int) (HEADER_ICON_SIZE * iconAlpha);
            int offset = (HEADER_ICON_SIZE - iconSize) / 2;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    category.getHeaderIcon(),
                    iconX + offset, iconY + offset,
                    0.0f, 0.0f,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        }

        Component description = category.getDescription();
        int descColor = (int) (200 * alpha) << 24 | 0xAAAAAA;
        graphics.drawCenteredString(font, description, centerX, 35, descColor);
    }

    protected void renderBackButton(GuiGraphics graphics, int mouseX, int mouseY, float animationProgress) {
        float appear = Math.max(0.0f, (animationProgress - 0.2f) / 0.8f);
        if (appear <= 0.0f) return;

        int bx = BACK_BUTTON_PADDING;
        int by = BACK_BUTTON_PADDING;
        int bSize = BACK_BUTTON_SIZE + 8;

        isHoveringBackButton = mouseX >= bx && mouseX < bx + bSize
                && mouseY >= by && mouseY < by + bSize;

        if (isHoveringBackButton) {
            int bgColor = (int) (0x80 * appear) << 24 | 0x9A7CFF;
            graphics.fill(bx, by, bx + bSize, by + bSize, bgColor);
            int borderColor = (int) (0x88 * appear) << 24 | 0xFFFFFF;
            MenuRenderUtils.renderOutline(graphics, bx, by, bSize, bSize, borderColor);
        }

        float scale = isHoveringBackButton ? 1.1f : 1.0f;
        int iconSize = (int) (BACK_BUTTON_SIZE * scale * appear);
        int iconOffset = (bSize - iconSize) / 2;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ICON_BACK,
                bx + iconOffset, by + iconOffset,
                0.0f, 0.0f,
                iconSize, iconSize,
                iconSize, iconSize
        );
    }

    protected void renderItemList(GuiGraphics graphics, int listX, int listTop, int listWidth,
                                   int listBottom, int mouseX, int mouseY, float animationProgress) {
        hoveredListIndex = -1;

        if (filteredCards.isEmpty()) {
            float alpha = animationProgress;
            int textColor = (int) (180 * alpha) << 24 | 0xAAAAAA;
            graphics.drawCenteredString(font,
                    Component.translatable("menu.aromaaffect." + category.getId() + ".no_results"),
                    width / 2, listTop + 20, textColor);
            return;
        }

        int rowHeight = getRowHeight();
        graphics.enableScissor(listX, listTop, listX + listWidth, listBottom);

        for (int i = 0; i < filteredCards.size(); i++) {
            int rowY = listTop + i * (rowHeight + ROW_PADDING) - listScrollOffset;

            if (rowY + rowHeight < listTop || rowY > listBottom) {
                continue;
            }

            boolean isHovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= Math.max(rowY, listTop) && mouseY < Math.min(rowY + rowHeight, listBottom)
                    && mouseY >= listTop && mouseY < listBottom;

            if (isHovered) {
                hoveredListIndex = i;
            }

            SelectionCard card = filteredCards.get(i);
            boolean isTracking = ActiveTrackingState.isTracking(card.id);

            renderRow(graphics, card, listX, rowY, listWidth, isHovered, isTracking, animationProgress);
        }

        graphics.disableScissor();
    }

    // ── Input handling ───────────────────────────────────────────────────

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (isHoveringBackButton) {
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (handleFilterClick((int) mouseX, (int) mouseY)) {
            return true;
        }

        if (hoveredListIndex >= 0 && hoveredListIndex < filteredCards.size()) {
            SelectionCard card = filteredCards.get(hoveredListIndex);
            if (card.isUnlocked) {
                if (ActiveTrackingState.isTracking(card.id)) {
                    AromaAffect.LOGGER.info("Deselecting {}: {}", category.getId(), card.id);
                    ActiveTrackingState.clear();
                    stopPath();
                    return true;
                }
                onCardSelected(card, cards.indexOf(card));
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean handleMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listTop = getListTopOffset();
        int listBottom = height - 10;
        int visibleHeight = listBottom - listTop;
        int totalHeight = filteredCards.size() * (getRowHeight() + ROW_PADDING);
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        listScrollOffset = (int) Math.max(0, Math.min(maxScroll, listScrollOffset - scrollY * 20));
        return true;
    }

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == 256) {
                searchBox.setFocused(false);
                return true;
            }
            return false;
        }

        if (keyCode == 256 || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            MenuManager.returnToRadialMenu();
            return true;
        }
        return false;
    }

    // ── Cost rendering helper ─────────────────────────────────────────────

    protected void renderCostSection(GuiGraphics graphics, SelectionCard card,
                                      int rowRight, int rowCenterY, float animationProgress) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int alpha = (int) (255 * animationProgress);
        int costX = rowRight - ROW_PADDING;

        // Nose durability cost
        String costText = String.valueOf(card.trackCost);
        int costTextWidth = font.width(costText);

        // Check if player can afford
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        boolean canAffordDurability = true;
        if (!headStack.isEmpty() && headStack.isDamageableItem()) {
            int remaining = headStack.getMaxDamage() - headStack.getDamageValue();
            canAffordDurability = remaining >= card.trackCost;
        }

        int costColor = canAffordDurability
                ? (alpha << 24 | 0xFFAA00)
                : (alpha << 24 | 0xFF4444);

        costX -= costTextWidth;
        graphics.drawString(font, costText, costX, rowCenterY - 4, costColor);

        // Nose icon (14x14, rendered from head item)
        int noseIconSize = 14;
        costX -= noseIconSize + 2;
        if (!headStack.isEmpty() && animationProgress > 0.2f) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(costX, rowCenterY - noseIconSize / 2);
            graphics.pose().scale(noseIconSize / 16.0f, noseIconSize / 16.0f);
            graphics.renderItem(headStack, 0, 0);
            graphics.pose().popMatrix();
        }

        // Required item (if any)
        if (card.requiredItem != null && !card.requiredItem.isEmpty()) {
            costX -= 6; // gap

            String reqText = "x" + card.requiredItemCount;
            int reqTextWidth = font.width(reqText);

            boolean hasRequiredItem = playerHasItem(player, card.requiredItem, card.requiredItemCount);
            int reqColor = hasRequiredItem
                    ? (alpha << 24 | 0xFFAA00)
                    : (alpha << 24 | 0xFF4444);

            costX -= reqTextWidth;
            graphics.drawString(font, reqText, costX, rowCenterY - 4, reqColor);

            int reqIconSize = 14;
            costX -= reqIconSize + 2;
            if (animationProgress > 0.2f) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(costX, rowCenterY - reqIconSize / 2);
                graphics.pose().scale(reqIconSize / 16.0f, reqIconSize / 16.0f);
                graphics.renderItem(card.requiredItem, 0, 0);
                graphics.pose().popMatrix();
            }
        }
    }

    private boolean playerHasItem(Player player, ItemStack required, int count) {
        if (required == null || required.isEmpty() || count <= 0) return true;
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItem(stack, required)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }

    // ── Card selection / path commands ────────────────────────────────────

    protected void onCardSelected(SelectionCard card, int index) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Pre-validate durability
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headStack.isEmpty() && headStack.isDamageableItem()) {
            int remaining = headStack.getMaxDamage() - headStack.getDamageValue();
            if (remaining < card.trackCost) {
                player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
                AromaAffect.LOGGER.info("Not enough nose durability for {}: need {}, have {}",
                        card.id, card.trackCost, remaining);
                return;
            }
        }

        // Check if passive mode is active - cannot use active tracking while passive mode is enabled
        if (PassiveModeManager.isPassiveModeEnabled()) {
            showErrorNotification(Component.translatable("message.aromaaffect.tracking.passive_mode_active"));
            player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
            AromaAffect.LOGGER.info("Cannot start tracking while passive mode is active");
            return;
        }

        // Pre-validate required item
        if (card.requiredItem != null && !card.requiredItem.isEmpty() && card.requiredItemCount > 0) {
            if (!playerHasItem(player, card.requiredItem, card.requiredItemCount)) {
                player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
                AromaAffect.LOGGER.info("Missing required item for {}: need {}x {}",
                        card.id, card.requiredItemCount, card.requiredItem.getDisplayName().getString());
                return;
            }
        }

        selectedCardIndex = index;
        ActiveTrackingState.set(card.id, card.displayName, card.icon, category);
        AromaAffect.LOGGER.info("Selected {} for tracking: {}", category.getId(), card.id);

        startPath(card.id);
        MenuManager.returnToRadialMenu();
    }

    protected void startPath(ResourceLocation targetId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Sync blacklist to server before path command
        if (Minecraft.getInstance().getConnection() != null) {
            PathScentNetworking.sendBlacklistSync(
                    Minecraft.getInstance().getConnection().registryAccess());
        }

        String command = String.format("aromatest path %s %s",
                category.getPathCommandType(), targetId.toString());
        AromaAffect.LOGGER.debug("Executing path command: {}", command);

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand(command);
        }
    }

    protected void stopPath() {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand("aromatest path stop");
        }
    }

    // ── Inner class ──────────────────────────────────────────────────────

    public static class SelectionCard {
        public final ResourceLocation id;
        public final Component displayName;
        public final ItemStack icon;
        public boolean isUnlocked;
        public final Component description;
        public final ResourceLocation thumbnail;
        public int trackCost = 10;
        public ItemStack requiredItem;
        public int requiredItemCount = 0;

        public SelectionCard(ResourceLocation id, Component displayName, ItemStack icon,
                            boolean isUnlocked, Component description, ResourceLocation thumbnail) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.isUnlocked = isUnlocked;
            this.description = description;
            this.thumbnail = thumbnail;
        }

        public SelectionCard(ResourceLocation id, Component displayName, ItemStack icon,
                            boolean isUnlocked, Component description) {
            this(id, displayName, icon, isUnlocked, description, null);
        }

        public SelectionCard(ResourceLocation id, Component displayName, ItemStack icon, boolean isUnlocked) {
            this(id, displayName, icon, isUnlocked, null, null);
        }
    }
}
