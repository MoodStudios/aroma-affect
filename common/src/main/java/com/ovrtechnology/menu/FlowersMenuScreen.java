package com.ovrtechnology.menu;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.flower.FlowerDefinition;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import com.ovrtechnology.tracking.RequiredItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Menu screen for selecting flowers/flora to track.
 *
 * @see MenuManager#openFlowersMenu()
 * @see SelectionMenuScreen
 */
public class FlowersMenuScreen extends SelectionMenuScreen {

    private static final int FILTER_CHIP_HEIGHT = 18;
    private static final int FILTER_CHIP_GAP = 6;

    private static final int CHIP_COLOR = 0xB0333333;
    private static final int CHIP_ACTIVE_COLOR = 0xE0446644;
    private static final int CHIP_HOVER_COLOR = 0xE0444466;

    static final Map<String, ItemStack> FLOWER_ICONS = new HashMap<>();
    private static final Map<String, FlowerFilter> FLOWER_CATEGORIES = new HashMap<>();

    static {
        // Common flowers
        FLOWER_ICONS.put("minecraft:dandelion", Items.DANDELION.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:poppy", Items.POPPY.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:blue_orchid", Items.BLUE_ORCHID.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:allium", Items.ALLIUM.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:azure_bluet", Items.AZURE_BLUET.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:red_tulip", Items.RED_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:orange_tulip", Items.ORANGE_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:white_tulip", Items.WHITE_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:pink_tulip", Items.PINK_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:oxeye_daisy", Items.OXEYE_DAISY.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:cornflower", Items.CORNFLOWER.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:lily_of_the_valley", Items.LILY_OF_THE_VALLEY.getDefaultInstance());

        // Tall flowers
        FLOWER_ICONS.put("minecraft:sunflower", Items.SUNFLOWER.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:lilac", Items.LILAC.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:rose_bush", Items.ROSE_BUSH.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:peony", Items.PEONY.getDefaultInstance());

        // Special flowers
        FLOWER_ICONS.put("minecraft:wither_rose", Items.WITHER_ROSE.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:torchflower", Items.TORCHFLOWER.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:pitcher_plant", Items.PITCHER_PLANT.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:pink_petals", Items.PINK_PETALS.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:spore_blossom", Items.SPORE_BLOSSOM.getDefaultInstance());

        // Nether flora
        FLOWER_ICONS.put("minecraft:crimson_fungus", Items.CRIMSON_FUNGUS.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:warped_fungus", Items.WARPED_FUNGUS.getDefaultInstance());

        // Category mappings
        for (String id : List.of("minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid",
                "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip",
                "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip",
                "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley")) {
            FLOWER_CATEGORIES.put(id, FlowerFilter.COMMON);
        }
        for (String id : List.of("minecraft:sunflower", "minecraft:lilac",
                "minecraft:rose_bush", "minecraft:peony")) {
            FLOWER_CATEGORIES.put(id, FlowerFilter.TALL);
        }
        for (String id : List.of("minecraft:wither_rose", "minecraft:torchflower",
                "minecraft:pitcher_plant", "minecraft:pink_petals", "minecraft:spore_blossom")) {
            FLOWER_CATEGORIES.put(id, FlowerFilter.SPECIAL);
        }
        for (String id : List.of("minecraft:crimson_fungus", "minecraft:warped_fungus")) {
            FLOWER_CATEGORIES.put(id, FlowerFilter.NETHER);
        }
    }

    private FlowerFilter activeFilter = FlowerFilter.ALL;
    private int hoveredFilterIndex = -1;

    public FlowersMenuScreen() {
        super(MenuCategory.FLOWERS);
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
            AromaAffect.LOGGER.debug("No player available for flowers menu");
            return;
        }

        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableFlowers = abilities.getFlowers();

        if (detectableFlowers.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no flower detection abilities");
            return;
        }

        for (String flowerId : detectableFlowers) {
            ResourceLocation resourceLocation = Ids.parse(flowerId);
            addFlowerCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} flower cards from equipped nose", cards.size());
    }

    private void addFlowerCard(ResourceLocation flowerId, boolean isUnlocked) {
        ItemStack icon = FLOWER_ICONS.get(flowerId.toString());

        if (icon == null) {
            var blockOptional = BuiltInRegistries.BLOCK.get(flowerId);
            if (blockOptional.isPresent()) {
                icon = new ItemStack(blockOptional.get().value().asItem());
            }
        }

        if (icon == null || icon.isEmpty()) {
            icon = Items.POPPY.getDefaultInstance();
        }

        String flowerName = flowerId.getPath().replace("_", " ");
        flowerName = MenuRenderUtils.capitalizeWords(flowerName);
        Component displayName = Texts.lit(flowerName);
        Component description = Texts.tr("menu.aromaaffect.flowers.card.description", displayName);

        SelectionCard card = new SelectionCard(flowerId, displayName, icon, isUnlocked, description);

        // Populate cost data from FlowerDefinitionLoader
        FlowerDefinition flowerDef = FlowerDefinitionLoader.getFlowerById(flowerId.toString());
        if (flowerDef != null) {
            card.trackCost = flowerDef.getTrackCost();
            RequiredItem req = flowerDef.getRequiredItem();
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
        if (activeFilter == FlowerFilter.ALL) return true;
        FlowerFilter cardCategory = FLOWER_CATEGORIES.getOrDefault(card.id.toString(), FlowerFilter.COMMON);
        return cardCategory == activeFilter;
    }

    @Override
    protected boolean handleFilterClick(int mouseX, int mouseY) {
        if (hoveredFilterIndex >= 0) {
            FlowerFilter[] filters = FlowerFilter.values();
            activeFilter = filters[hoveredFilterIndex];
            applyFilters();
            return true;
        }
        return false;
    }

    @Override
    protected int renderBelowSearch(GuiGraphics graphics, int startX, int y, int availableWidth,
                                     int mouseX, int mouseY, float animationProgress) {
        hoveredFilterIndex = -1;
        FlowerFilter[] filters = FlowerFilter.values();

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
            FlowerFilter filter = filters[i];
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
            MenuRenderUtils.renderOutline(graphics, chipX, y, cw, FILTER_CHIP_HEIGHT, borderColor);

            int textColor = (int) (255 * alpha) << 24 | 0xFFFFFF;
            graphics.drawCenteredString(font, filter.getDisplayName(), chipX + cw / 2,
                    y + (FILTER_CHIP_HEIGHT - 8) / 2, textColor);

            chipX += cw + FILTER_CHIP_GAP;
        }

        return y + FILTER_CHIP_HEIGHT + 8;
    }

    @Override
    protected void renderRow(GuiGraphics graphics, SelectionCard card, int x, int y,
                              int rowWidth, boolean isHovered, boolean isTracking,
                              float animationProgress) {
        int rowHeight = getRowHeight();
        int bgColor;
        if (isTracking) {
            bgColor = isHovered ? ROW_TRACKING_HOVER_COLOR : ROW_TRACKING_COLOR;
        } else {
            bgColor = isHovered ? ROW_HOVER_COLOR : ROW_COLOR;
        }
        int a = (int) (((bgColor >> 24) & 0xFF) * animationProgress);
        bgColor = (a << 24) | (bgColor & 0x00FFFFFF);
        graphics.fill(x, y, x + rowWidth, y + rowHeight, bgColor);

        if (isTracking) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            MenuRenderUtils.renderOutline(graphics, x, y, rowWidth, rowHeight, borderColor);
        } else if (isHovered) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0xAAAAFF;
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
        int nameColor = isTracking
                ? (int) (255 * animationProgress) << 24 | 0x66FF66
                : (int) (255 * animationProgress) << 24 | 0xFFFFFF;
        graphics.drawString(font, card.displayName, textX, y + 6, nameColor);

        int idColor = (int) (180 * animationProgress) << 24 | 0x888888;
        graphics.drawString(font, card.id.toString(), textX, y + 18, idColor);

        if (isTracking) {
            int indicatorColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            Component trackingLabel = Texts.tr("menu.aromaaffect.selection.selected");
            int labelWidth = font.width(trackingLabel);
            graphics.drawString(font, trackingLabel, x + rowWidth - labelWidth - ROW_PADDING,
                    y + (rowHeight - 8) / 2, indicatorColor);
        } else {
            renderCostSection(graphics, card, x + rowWidth, y + rowHeight / 2, animationProgress);
        }
    }

    enum FlowerFilter {
        ALL("menu.aromaaffect.flowers.filter.all"),
        COMMON("menu.aromaaffect.flowers.filter.common"),
        TALL("menu.aromaaffect.flowers.filter.tall"),
        SPECIAL("menu.aromaaffect.flowers.filter.special"),
        NETHER("menu.aromaaffect.flowers.filter.nether");

        private final String translationKey;

        FlowerFilter(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return Texts.tr(translationKey).getString();
        }
    }
}
