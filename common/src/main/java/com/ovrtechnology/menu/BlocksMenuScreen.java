package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Menu screen for selecting blocks to track.
 *
 * <p>Displays a searchable, filterable list of blocks that the currently
 * equipped Nose can detect. Each row shows the block icon, display name, and
 * Minecraft ID.</p>
 *
 * @see MenuManager#openBlocksMenu()
 * @see SelectionMenuScreen
 */
public class BlocksMenuScreen extends SelectionMenuScreen {

    private static final int ROW_HEIGHT = 32;
    private static final int ROW_PADDING = 4;
    private static final int ICON_SIZE = 24;
    private static final int MAX_LIST_WIDTH = 350;
    private static final int FILTER_CHIP_HEIGHT = 18;
    private static final int FILTER_CHIP_GAP = 6;
    private static final int SEARCH_BOX_HEIGHT = 20;

    private static final int ROW_COLOR = 0xB0222222;
    private static final int ROW_HOVER_COLOR = 0xE0444488;
    private static final int ROW_TRACKING_COLOR = 0xC0224422;
    private static final int ROW_TRACKING_HOVER_COLOR = 0xE0336633;
    private static final int CHIP_COLOR = 0xB0333333;
    private static final int CHIP_ACTIVE_COLOR = 0xE0446644;
    private static final int CHIP_HOVER_COLOR = 0xE0444466;

    private static final int HEADER_ICON_SIZE = 16;
    private static final int BACK_BUTTON_SIZE = 24;
    private static final int BACK_BUTTON_PADDING = 8;

    private static final ResourceLocation ICON_BLOCKS = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_blocks.png");
    private static final ResourceLocation ICON_BACK = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_back.png");

    private static final Map<String, BlockFilter> BLOCK_CATEGORIES = new HashMap<>();

    static {
        // Ores
        for (String id : List.of(
                "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
                "minecraft:ancient_debris")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.ORES);
        }
        // Fluids
        for (String id : List.of("minecraft:water", "minecraft:lava")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.FLUIDS);
        }
        // Dungeon
        for (String id : List.of("minecraft:spawner", "minecraft:chest")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.DUNGEON);
        }
        // Nature
        for (String id : List.of(
                "minecraft:amethyst_cluster", "minecraft:budding_amethyst")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.NATURE);
        }
    }

    private EditBox searchBox;
    private String searchQuery = "";
    private BlockFilter activeFilter = BlockFilter.ALL;
    private List<SelectionCard> filteredCards = new ArrayList<>();
    private int listScrollOffset = 0;
    private int hoveredListIndex = -1;
    private int hoveredFilterIndex = -1;
    private boolean isHoveringBackButton = false;

    public BlocksMenuScreen() {
        super(MenuCategory.BLOCKS);
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int searchX = (width - listWidth) / 2;
        int searchY = 52;

        searchBox = new EditBox(font, searchX, searchY, listWidth, SEARCH_BOX_HEIGHT,
                Component.translatable("menu.aromaaffect.blocks.search_placeholder"));
        searchBox.setHint(Component.translatable("menu.aromaaffect.blocks.search_placeholder"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(query -> {
            searchQuery = query;
            applyFilters();
        });
        addWidget(searchBox);

        applyFilters();
    }

    @Override
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
                    ICON_BLOCKS,
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

    private void renderBackButton(GuiGraphics graphics, int mouseX, int mouseY, float animationProgress) {
        float appear = Math.max(0.0f, (animationProgress - 0.2f) / 0.8f);
        if (appear <= 0.0f) {
            return;
        }

        int bx = BACK_BUTTON_PADDING;
        int by = BACK_BUTTON_PADDING;
        int bSize = BACK_BUTTON_SIZE + 8;

        isHoveringBackButton = mouseX >= bx && mouseX < bx + bSize
                && mouseY >= by && mouseY < by + bSize;

        if (isHoveringBackButton) {
            int bgColor = (int) (0x80 * appear) << 24 | 0x9A7CFF;
            graphics.fill(bx, by, bx + bSize, by + bSize, bgColor);
            int borderColor = (int) (0x88 * appear) << 24 | 0xFFFFFF;
            drawCardBorder(graphics, bx, by, bSize, bSize, borderColor);
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

    @Override
    protected void loadCards() {
        cards.clear();

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaAffect.LOGGER.debug("No player available for blocks menu");
            return;
        }

        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableBlocks = abilities.getBlocks();

        if (detectableBlocks.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no block detection abilities");
            return;
        }

        for (String blockId : detectableBlocks) {
            ResourceLocation resourceLocation = ResourceLocation.parse(blockId);
            addBlockCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} block cards from equipped nose", cards.size());
    }

    private void addBlockCard(ResourceLocation blockId, boolean isUnlocked) {
        var blockOptional = BuiltInRegistries.BLOCK.get(blockId);
        if (blockOptional.isEmpty()) {
            AromaAffect.LOGGER.warn("Block not found: {}", blockId);
            return;
        }

        var block = blockOptional.get().value();
        ItemStack icon = new ItemStack(block.asItem());

        if (icon.isEmpty()) {
            // Blocks without item form get a placeholder
            if (blockId.toString().equals("minecraft:water")) {
                icon = Items.WATER_BUCKET.getDefaultInstance();
            } else if (blockId.toString().equals("minecraft:lava")) {
                icon = Items.LAVA_BUCKET.getDefaultInstance();
            } else {
                icon = Items.BARRIER.getDefaultInstance();
            }
        }

        Component displayName = block.getName();
        Component description = Component.translatable("menu.aromaaffect.blocks.card.description", displayName);

        cards.add(new SelectionCard(blockId, displayName, icon, isUnlocked, description));
    }

    private void applyFilters() {
        filteredCards.clear();
        String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);

        for (SelectionCard card : cards) {
            if (activeFilter != BlockFilter.ALL) {
                BlockFilter cardCategory = BLOCK_CATEGORIES.getOrDefault(card.id.toString(), BlockFilter.ORES);
                if (cardCategory != activeFilter) {
                    continue;
                }
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

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                  float partialTick, float animationProgress) {
        int centerX = width / 2;
        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int listX = (width - listWidth) / 2;

        renderHeader(graphics, centerX, animationProgress);
        renderBackButton(graphics, mouseX, mouseY, animationProgress);

        int searchY = 52;
        searchBox.setX(listX);
        searchBox.setY(searchY);
        searchBox.setWidth(listWidth);
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        int chipY = searchY + SEARCH_BOX_HEIGHT + 6;
        renderFilterChips(graphics, listX, chipY, listWidth, mouseX, mouseY, animationProgress);

        int listTop = chipY + FILTER_CHIP_HEIGHT + 8;
        int listBottom = height - 10;
        renderBlockList(graphics, listX, listTop, listWidth, listBottom, mouseX, mouseY, animationProgress);
    }

    private void renderFilterChips(GuiGraphics graphics, int startX, int y, int availableWidth,
                                    int mouseX, int mouseY, float animationProgress) {
        hoveredFilterIndex = -1;
        BlockFilter[] filters = BlockFilter.values();

        int totalWidth = 0;
        int[] chipWidths = new int[filters.length];
        for (int i = 0; i < filters.length; i++) {
            String label = filters[i].getDisplayName();
            chipWidths[i] = font.width(label) + 12;
            totalWidth += chipWidths[i];
        }
        totalWidth += FILTER_CHIP_GAP * (filters.length - 1);

        int chipX = startX + (availableWidth - totalWidth) / 2;
        float alpha = animationProgress;

        for (int i = 0; i < filters.length; i++) {
            BlockFilter filter = filters[i];
            int cw = chipWidths[i];
            boolean isActive = filter == activeFilter;
            boolean isHovered = mouseX >= chipX && mouseX < chipX + cw
                    && mouseY >= y && mouseY < y + FILTER_CHIP_HEIGHT;

            if (isHovered) {
                hoveredFilterIndex = i;
            }

            int bgColor;
            if (isActive) {
                bgColor = CHIP_ACTIVE_COLOR;
            } else if (isHovered) {
                bgColor = CHIP_HOVER_COLOR;
            } else {
                bgColor = CHIP_COLOR;
            }
            int a = (int) (((bgColor >> 24) & 0xFF) * alpha);
            bgColor = (a << 24) | (bgColor & 0x00FFFFFF);

            graphics.fill(chipX, y, chipX + cw, y + FILTER_CHIP_HEIGHT, bgColor);

            int borderColor = isActive ? 0xFF66AA66 : 0xFF555555;
            borderColor = (int) (255 * alpha) << 24 | (borderColor & 0x00FFFFFF);
            drawCardBorder(graphics, chipX, y, cw, FILTER_CHIP_HEIGHT, borderColor);

            int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
            graphics.drawCenteredString(font, filter.getDisplayName(), chipX + cw / 2,
                    y + (FILTER_CHIP_HEIGHT - 8) / 2, textColor);

            chipX += cw + FILTER_CHIP_GAP;
        }
    }

    private void renderBlockList(GuiGraphics graphics, int listX, int listTop, int listWidth,
                                  int listBottom, int mouseX, int mouseY, float animationProgress) {
        hoveredListIndex = -1;

        if (filteredCards.isEmpty()) {
            float alpha = animationProgress;
            int textColor = (int) (180 * alpha) << 24 | 0xAAAAAA;
            graphics.drawCenteredString(font,
                    Component.translatable("menu.aromaaffect.blocks.no_results"),
                    width / 2, listTop + 20, textColor);
            return;
        }

        graphics.enableScissor(listX, listTop, listX + listWidth, listBottom);

        for (int i = 0; i < filteredCards.size(); i++) {
            int rowY = listTop + i * (ROW_HEIGHT + ROW_PADDING) - listScrollOffset;

            if (rowY + ROW_HEIGHT < listTop || rowY > listBottom) {
                continue;
            }

            boolean isHovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= Math.max(rowY, listTop) && mouseY < Math.min(rowY + ROW_HEIGHT, listBottom)
                    && mouseY >= listTop && mouseY < listBottom;

            if (isHovered) {
                hoveredListIndex = i;
            }

            SelectionCard card = filteredCards.get(i);
            boolean isTracking = ActiveTrackingState.isTracking(card.id);

            renderBlockRow(graphics, card, listX, rowY, listWidth,
                    isHovered, isTracking, animationProgress);
        }

        graphics.disableScissor();
    }

    private void renderBlockRow(GuiGraphics graphics, SelectionCard card, int x, int y,
                                 int rowWidth, boolean isHovered, boolean isTracking,
                                 float animationProgress) {
        int bgColor;
        if (isTracking) {
            bgColor = isHovered ? ROW_TRACKING_HOVER_COLOR : ROW_TRACKING_COLOR;
        } else {
            bgColor = isHovered ? ROW_HOVER_COLOR : ROW_COLOR;
        }
        int a = (int) (((bgColor >> 24) & 0xFF) * animationProgress);
        bgColor = (a << 24) | (bgColor & 0x00FFFFFF);
        graphics.fill(x, y, x + rowWidth, y + ROW_HEIGHT, bgColor);

        if (isTracking) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            drawCardBorder(graphics, x, y, rowWidth, ROW_HEIGHT, borderColor);
        } else if (isHovered) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0xAAAAFF;
            drawCardBorder(graphics, x, y, rowWidth, ROW_HEIGHT, borderColor);
        }

        int iconX = x + ROW_PADDING;
        int iconY = y + (ROW_HEIGHT - ICON_SIZE) / 2;
        if (card.icon != null && animationProgress > 0.2f) {
            float iconAlpha = (animationProgress - 0.2f) / 0.8f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(iconX, iconY);
            graphics.pose().scale(ICON_SIZE / 16.0f * iconAlpha, ICON_SIZE / 16.0f * iconAlpha);
            graphics.renderItem(card.icon, 0, 0);
            graphics.pose().popMatrix();
        }

        int textX = iconX + ICON_SIZE + 8;
        int nameColor;
        if (isTracking) {
            nameColor = (int) (255 * animationProgress) << 24 | 0x66FF66;
        } else {
            nameColor = (int) (255 * animationProgress) << 24 | 0xFFFFFF;
        }
        graphics.drawString(font, card.displayName, textX, y + 6, nameColor);

        int idColor = (int) (180 * animationProgress) << 24 | 0x888888;
        graphics.drawString(font, card.id.toString(), textX, y + 18, idColor);

        if (isTracking) {
            int indicatorColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            Component trackingLabel = Component.translatable("menu.aromaaffect.selection.selected");
            int labelWidth = font.width(trackingLabel);
            graphics.drawString(font, trackingLabel, x + rowWidth - labelWidth - ROW_PADDING,
                    y + (ROW_HEIGHT - 8) / 2, indicatorColor);
        }
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        if (isHoveringBackButton) {
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (hoveredFilterIndex >= 0) {
            BlockFilter[] filters = BlockFilter.values();
            activeFilter = filters[hoveredFilterIndex];
            applyFilters();
            return true;
        }

        if (hoveredListIndex >= 0 && hoveredListIndex < filteredCards.size()) {
            SelectionCard card = filteredCards.get(hoveredListIndex);
            if (card.isUnlocked) {
                if (ActiveTrackingState.isTracking(card.id)) {
                    AromaAffect.LOGGER.info("Deselecting block: {}", card.id);
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
        int listTop = 52 + SEARCH_BOX_HEIGHT + 6 + FILTER_CHIP_HEIGHT + 8;
        int listBottom = height - 10;
        int visibleHeight = listBottom - listTop;
        int totalHeight = filteredCards.size() * (ROW_HEIGHT + ROW_PADDING);
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

    @Override
    protected void onCardSelected(SelectionCard card, int index) {
        selectedCardIndex = index;
        ActiveTrackingState.set(card.id, card.displayName, card.icon, MenuCategory.BLOCKS);
        AromaAffect.LOGGER.info("Selected block for tracking: {}", card.id);

        startPathToBlock(card.id);
        MenuManager.returnToRadialMenu();
    }

    private void startPathToBlock(ResourceLocation blockId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String command = String.format("aromatest path block %s", blockId.toString());
        AromaAffect.LOGGER.debug("Executing path command: {}", command);

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand(command);
        }
    }

    private void stopPath() {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand("aromatest path stop");
        }
    }

    enum BlockFilter {
        ALL("menu.aromaaffect.blocks.filter.all"),
        ORES("menu.aromaaffect.blocks.filter.ores"),
        FLUIDS("menu.aromaaffect.blocks.filter.fluids"),
        DUNGEON("menu.aromaaffect.blocks.filter.dungeon"),
        NATURE("menu.aromaaffect.blocks.filter.nature");

        private final String translationKey;

        BlockFilter(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return Component.translatable(translationKey).getString();
        }
    }
}
