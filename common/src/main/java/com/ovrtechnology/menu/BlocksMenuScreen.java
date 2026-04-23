package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.block.BlockDefinition;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.util.Colors;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BlocksMenuScreen extends SelectionMenuScreen {

    private static final int FILTER_CHIP_HEIGHT = 18;
    private static final int FILTER_CHIP_GAP = 6;

    private static final int CHIP_COLOR = Colors.HUD_OVERLAY_PANEL;
    private static final int CHIP_ACTIVE_COLOR = Colors.BORDER_PANEL_GREEN;
    private static final int CHIP_HOVER_COLOR = Colors.BORDER_PANEL;

    private static final Map<String, BlockFilter> BLOCK_CATEGORIES = new HashMap<>();

    static {
        for (String id :
                List.of(
                        "minecraft:coal_ore",
                        "minecraft:deepslate_coal_ore",
                        "minecraft:iron_ore",
                        "minecraft:deepslate_iron_ore",
                        "minecraft:copper_ore",
                        "minecraft:deepslate_copper_ore",
                        "minecraft:gold_ore",
                        "minecraft:deepslate_gold_ore",
                        "minecraft:redstone_ore",
                        "minecraft:deepslate_redstone_ore",
                        "minecraft:lapis_ore",
                        "minecraft:deepslate_lapis_ore",
                        "minecraft:diamond_ore",
                        "minecraft:deepslate_diamond_ore",
                        "minecraft:emerald_ore",
                        "minecraft:deepslate_emerald_ore",
                        "minecraft:nether_gold_ore",
                        "minecraft:nether_quartz_ore",
                        "minecraft:ancient_debris")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.ORES);
        }
        for (String id : List.of("minecraft:water", "minecraft:lava")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.FLUIDS);
        }
        for (String id : List.of("minecraft:spawner", "minecraft:chest")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.DUNGEON);
        }
        for (String id : List.of("minecraft:amethyst_cluster", "minecraft:budding_amethyst")) {
            BLOCK_CATEGORIES.put(id, BlockFilter.NATURE);
        }
    }

    private BlockFilter activeFilter = BlockFilter.ALL;
    private int hoveredFilterIndex = -1;

    public BlocksMenuScreen() {
        super(MenuCategory.BLOCKS);
    }

    @Override
    protected int getRowHeight() {
        return 32;
    }

    @Override
    protected int getListTopOffset() {
        return 52 + SEARCH_BOX_HEIGHT + 6 + FILTER_CHIP_HEIGHT + 8;
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

        NoseAbilityResolver.ResolvedAbilities abilities =
                EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableBlocks = abilities.getBlocks();

        if (detectableBlocks.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no block detection abilities");
            return;
        }

        for (String blockId : detectableBlocks) {
            ResourceLocation resourceLocation = Ids.parse(blockId);
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
            if (blockId.toString().equals("minecraft:water")) {
                icon = Items.WATER_BUCKET.getDefaultInstance();
            } else if (blockId.toString().equals("minecraft:lava")) {
                icon = Items.LAVA_BUCKET.getDefaultInstance();
            } else {
                icon = Items.BARRIER.getDefaultInstance();
            }
        }

        Component displayName = block.getName();
        Component description = Texts.tr("menu.aromaaffect.blocks.card.description", displayName);

        SelectionCard card = new SelectionCard(blockId, displayName, icon, isUnlocked, description);

        BlockDefinition blockDef = BlockDefinitionLoader.getBlockById(blockId.toString());
        if (blockDef != null) {
            card.trackCost = blockDef.getTrackCost();
            RequiredItem req = blockDef.getRequiredItem();
            if (req != null && req.getItemId() != null) {
                ResourceLocation reqId = Ids.parse(req.getItemId());
                var itemOpt = BuiltInRegistries.ITEM.get(reqId);
                if (itemOpt.isPresent()) {
                    card.requiredItem = new ItemStack(itemOpt.get().value());
                    card.requiredItemCount = req.getCount();
                }
            }
        }

        cards.add(card);
    }

    @Override
    protected boolean passesFilter(SelectionCard card) {
        if (activeFilter == BlockFilter.ALL) return true;
        BlockFilter cardCategory =
                BLOCK_CATEGORIES.getOrDefault(card.id.toString(), BlockFilter.ORES);
        return cardCategory == activeFilter;
    }

    @Override
    protected boolean handleFilterClick(int mouseX, int mouseY) {
        if (hoveredFilterIndex >= 0) {
            BlockFilter[] filters = BlockFilter.values();
            activeFilter = filters[hoveredFilterIndex];
            applyFilters();
            return true;
        }
        return false;
    }

    @Override
    protected int renderBelowSearch(
            GuiGraphics graphics,
            int startX,
            int y,
            int availableWidth,
            int mouseX,
            int mouseY,
            float animationProgress) {
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
            boolean isHovered =
                    mouseX >= chipX
                            && mouseX < chipX + cw
                            && mouseY >= y
                            && mouseY < y + FILTER_CHIP_HEIGHT;

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
            bgColor = (a << 24) | (bgColor & Colors.TRANSPARENT);

            graphics.fill(chipX, y, chipX + cw, y + FILTER_CHIP_HEIGHT, bgColor);

            int borderColor = isActive ? Colors.SUCCESS_GREEN_MUTED : Colors.BG_ROW;
            borderColor = (int) (255 * alpha) << 24 | (borderColor & Colors.TRANSPARENT);
            MenuRenderUtils.renderOutline(graphics, chipX, y, cw, FILTER_CHIP_HEIGHT, borderColor);

            int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
            graphics.drawCenteredString(
                    font,
                    filter.getDisplayName(),
                    chipX + cw / 2,
                    y + (FILTER_CHIP_HEIGHT - 8) / 2,
                    textColor);

            chipX += cw + FILTER_CHIP_GAP;
        }

        return y + FILTER_CHIP_HEIGHT + 8;
    }

    @Override
    protected void renderRow(
            GuiGraphics graphics,
            SelectionCard card,
            int x,
            int y,
            int rowWidth,
            boolean isHovered,
            boolean isTracking,
            float animationProgress) {
        int rowHeight = getRowHeight();
        int bgColor;
        if (isTracking) {
            bgColor = isHovered ? ROW_TRACKING_HOVER_COLOR : ROW_TRACKING_COLOR;
        } else {
            bgColor = isHovered ? ROW_HOVER_COLOR : ROW_COLOR;
        }
        int a = (int) (((bgColor >> 24) & 0xFF) * animationProgress);
        bgColor = (a << 24) | (bgColor & Colors.TRANSPARENT);
        graphics.fill(x, y, x + rowWidth, y + rowHeight, bgColor);

        if (isTracking) {
            int borderColor = (int) (255 * animationProgress) << 24 | Colors.SUCCESS_GREEN_RGB;
            MenuRenderUtils.renderOutline(graphics, x, y, rowWidth, rowHeight, borderColor);
        } else if (isHovered) {
            int borderColor = (int) (255 * animationProgress) << 24 | Colors.BLUE_ICON;
            MenuRenderUtils.renderOutline(graphics, x, y, rowWidth, rowHeight, borderColor);
        }

        int iconX = x + ROW_PADDING;
        int iconY = y + (rowHeight - ICON_SIZE) / 2;
        if (card.icon != null && animationProgress > 0.2f) {
            float iconAlpha = (animationProgress - 0.2f) / 0.8f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(iconX, iconY);
            graphics.pose().scale(ICON_SIZE / 16.0f * iconAlpha, ICON_SIZE / 16.0f * iconAlpha);
            graphics.renderItem(card.icon, 0, 0);
            graphics.pose().popMatrix();
        }

        int textX = iconX + ICON_SIZE + 8;
        int nameColor =
                isTracking
                        ? (int) (255 * animationProgress) << 24 | Colors.SUCCESS_GREEN_RGB_BRIGHT
                        : (int) (255 * animationProgress) << 24 | 0xFFFFFF;
        graphics.drawString(font, card.displayName, textX, y + 6, nameColor);

        int idColor = (int) (180 * animationProgress) << 24 | 0x888888;
        graphics.drawString(font, card.id.toString(), textX, y + 18, idColor);

        if (isTracking) {
            int indicatorColor = (int) (255 * animationProgress) << 24 | Colors.SUCCESS_GREEN_RGB;
            Component trackingLabel = Texts.tr("menu.aromaaffect.selection.selected");
            int labelWidth = font.width(trackingLabel);
            graphics.drawString(
                    font,
                    trackingLabel,
                    x + rowWidth - labelWidth - ROW_PADDING,
                    y + (rowHeight - 8) / 2,
                    indicatorColor);
        } else {
            renderCostSection(graphics, card, x + rowWidth, y + rowHeight / 2, animationProgress);
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
            return Texts.tr(translationKey).getString();
        }
    }
}
