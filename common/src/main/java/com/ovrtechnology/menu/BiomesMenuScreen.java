package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Menu screen for selecting biomes to track.
 *
 * <p>Displays a searchable list of biomes that the currently equipped Nose
 * can detect. Each row shows a screenshot thumbnail, item icon, display name,
 * and Minecraft ID.</p>
 *
 * @see MenuManager#openBiomesMenu()
 * @see SelectionMenuScreen
 */
public class BiomesMenuScreen extends SelectionMenuScreen {

    private static final int ROW_HEIGHT = 56;
    private static final int ROW_PADDING = 4;
    private static final int ICON_SIZE = 24;
    private static final int MAX_LIST_WIDTH = 350;
    private static final int SEARCH_BOX_HEIGHT = 20;
    private static final int THUMB_W = 72;
    private static final int THUMB_H = 48;

    private static final int ROW_COLOR = 0xB0222222;
    private static final int ROW_HOVER_COLOR = 0xE0444488;
    private static final int ROW_TRACKING_COLOR = 0xC0224422;
    private static final int ROW_TRACKING_HOVER_COLOR = 0xE0336633;

    private static final int HEADER_ICON_SIZE = 16;
    private static final int BACK_BUTTON_SIZE = 24;
    private static final int BACK_BUTTON_PADDING = 8;

    private static final ResourceLocation ICON_BIOMES = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_biomes.png");
    private static final ResourceLocation ICON_BACK = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_back.png");
    private static final ResourceLocation PLACEHOLDER_IMG = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/biomes/placeholder.png");

    /**
     * Map of biome IDs to their representative icons.
     */
    private static final Map<String, ItemStack> BIOME_ICONS = new HashMap<>();

    static {
        // Overworld - Plains & Meadows
        BIOME_ICONS.put("minecraft:plains", Items.GRASS_BLOCK.getDefaultInstance());
        BIOME_ICONS.put("minecraft:sunflower_plains", Items.SUNFLOWER.getDefaultInstance());
        BIOME_ICONS.put("minecraft:meadow", Items.DANDELION.getDefaultInstance());

        // Overworld - Forests
        BIOME_ICONS.put("minecraft:forest", Items.OAK_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:birch_forest", Items.BIRCH_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:dark_forest", Items.DARK_OAK_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:old_growth_birch_forest", Items.BIRCH_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:flower_forest", Items.PEONY.getDefaultInstance());
        BIOME_ICONS.put("minecraft:cherry_grove", Items.CHERRY_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:windswept_forest", Items.OAK_LEAVES.getDefaultInstance());

        // Overworld - Taiga
        BIOME_ICONS.put("minecraft:taiga", Items.SPRUCE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:snowy_taiga", Items.SPRUCE_SAPLING.getDefaultInstance());
        BIOME_ICONS.put("minecraft:old_growth_pine_taiga", Items.SPRUCE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:old_growth_spruce_taiga", Items.SPRUCE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:grove", Items.SPRUCE_LEAVES.getDefaultInstance());

        // Overworld - Desert & Badlands
        BIOME_ICONS.put("minecraft:desert", Items.SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:badlands", Items.TERRACOTTA.getDefaultInstance());
        BIOME_ICONS.put("minecraft:eroded_badlands", Items.RED_SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:wooded_badlands", Items.COARSE_DIRT.getDefaultInstance());

        // Overworld - Jungle
        BIOME_ICONS.put("minecraft:jungle", Items.JUNGLE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:bamboo_jungle", Items.BAMBOO.getDefaultInstance());
        BIOME_ICONS.put("minecraft:sparse_jungle", Items.JUNGLE_SAPLING.getDefaultInstance());

        // Overworld - Swamp
        BIOME_ICONS.put("minecraft:swamp", Items.LILY_PAD.getDefaultInstance());
        BIOME_ICONS.put("minecraft:mangrove_swamp", Items.MANGROVE_LOG.getDefaultInstance());

        // Overworld - Savanna
        BIOME_ICONS.put("minecraft:savanna", Items.ACACIA_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:savanna_plateau", Items.ACACIA_SAPLING.getDefaultInstance());
        BIOME_ICONS.put("minecraft:windswept_savanna", Items.ACACIA_LEAVES.getDefaultInstance());

        // Overworld - Snowy
        BIOME_ICONS.put("minecraft:snowy_plains", Items.SNOWBALL.getDefaultInstance());
        BIOME_ICONS.put("minecraft:snowy_beach", Items.SNOW_BLOCK.getDefaultInstance());
        BIOME_ICONS.put("minecraft:snowy_slopes", Items.POWDER_SNOW_BUCKET.getDefaultInstance());
        BIOME_ICONS.put("minecraft:ice_spikes", Items.PACKED_ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:frozen_peaks", Items.ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:jagged_peaks", Items.STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:stony_peaks", Items.CALCITE.getDefaultInstance());

        // Overworld - Hills & Mountains
        BIOME_ICONS.put("minecraft:windswept_hills", Items.GRAVEL.getDefaultInstance());
        BIOME_ICONS.put("minecraft:windswept_gravelly_hills", Items.GRAVEL.getDefaultInstance());

        // Overworld - Ocean
        BIOME_ICONS.put("minecraft:ocean", Items.WATER_BUCKET.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_ocean", Items.PRISMARINE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:warm_ocean", Items.BRAIN_CORAL.getDefaultInstance());
        BIOME_ICONS.put("minecraft:lukewarm_ocean", Items.KELP.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_lukewarm_ocean", Items.SEAGRASS.getDefaultInstance());
        BIOME_ICONS.put("minecraft:cold_ocean", Items.COD.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_cold_ocean", Items.SALMON.getDefaultInstance());
        BIOME_ICONS.put("minecraft:frozen_ocean", Items.BLUE_ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_frozen_ocean", Items.BLUE_ICE.getDefaultInstance());

        // Overworld - River & Beach
        BIOME_ICONS.put("minecraft:river", Items.WATER_BUCKET.getDefaultInstance());
        BIOME_ICONS.put("minecraft:frozen_river", Items.ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:beach", Items.SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:stony_shore", Items.STONE.getDefaultInstance());

        // Overworld - Caves
        BIOME_ICONS.put("minecraft:lush_caves", Items.GLOW_BERRIES.getDefaultInstance());
        BIOME_ICONS.put("minecraft:dripstone_caves", Items.POINTED_DRIPSTONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_dark", Items.SCULK.getDefaultInstance());

        // Overworld - Mushroom
        BIOME_ICONS.put("minecraft:mushroom_fields", Items.RED_MUSHROOM.getDefaultInstance());

        // Nether
        BIOME_ICONS.put("minecraft:nether_wastes", Items.NETHERRACK.getDefaultInstance());
        BIOME_ICONS.put("minecraft:crimson_forest", Items.CRIMSON_STEM.getDefaultInstance());
        BIOME_ICONS.put("minecraft:warped_forest", Items.WARPED_STEM.getDefaultInstance());
        BIOME_ICONS.put("minecraft:soul_sand_valley", Items.SOUL_SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:basalt_deltas", Items.BASALT.getDefaultInstance());

        // End
        BIOME_ICONS.put("minecraft:the_end", Items.END_STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:end_highlands", Items.CHORUS_FLOWER.getDefaultInstance());
        BIOME_ICONS.put("minecraft:end_midlands", Items.END_STONE_BRICKS.getDefaultInstance());
        BIOME_ICONS.put("minecraft:small_end_islands", Items.END_STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:end_barrens", Items.END_STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:the_void", Items.BARRIER.getDefaultInstance());
    }

    private EditBox searchBox;
    private String searchQuery = "";
    private List<SelectionCard> filteredCards = new ArrayList<>();
    private int listScrollOffset = 0;
    private int hoveredListIndex = -1;
    private boolean isHoveringBackButton = false;

    public BiomesMenuScreen() {
        super(MenuCategory.BIOMES);
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int searchX = (width - listWidth) / 2;
        int searchY = 52;

        searchBox = new EditBox(font, searchX, searchY, listWidth, SEARCH_BOX_HEIGHT,
                Component.translatable("menu.aromaaffect.biomes.search_placeholder"));
        searchBox.setHint(Component.translatable("menu.aromaaffect.biomes.search_placeholder"));
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
                    ICON_BIOMES,
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
            AromaAffect.LOGGER.debug("No player available for biomes menu");
            return;
        }

        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableBiomes = abilities.getBiomes();

        if (detectableBiomes.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no biome detection abilities");
            return;
        }

        for (String biomeId : detectableBiomes) {
            ResourceLocation resourceLocation = ResourceLocation.parse(biomeId);
            addBiomeCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} biome cards from equipped nose", cards.size());
    }

    private void addBiomeCard(ResourceLocation biomeId, boolean isUnlocked) {
        ItemStack icon = BIOME_ICONS.getOrDefault(biomeId.toString(), Items.GRASS_BLOCK.getDefaultInstance());

        String biomeName = capitalizeWords(biomeId.getPath().replace("_", " "));
        Component displayName = Component.literal(biomeName);
        Component description = Component.translatable("menu.aromaaffect.biomes.card.description", displayName);

        cards.add(new SelectionCard(biomeId, displayName, icon, isUnlocked, description));
    }

    private void applyFilters() {
        filteredCards.clear();
        String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);

        for (SelectionCard card : cards) {
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

        int listTop = searchY + SEARCH_BOX_HEIGHT + 8;
        int listBottom = height - 10;
        renderBiomeList(graphics, listX, listTop, listWidth, listBottom, mouseX, mouseY, animationProgress);
    }

    private void renderBiomeList(GuiGraphics graphics, int listX, int listTop, int listWidth,
                                  int listBottom, int mouseX, int mouseY, float animationProgress) {
        hoveredListIndex = -1;

        if (filteredCards.isEmpty()) {
            float alpha = animationProgress;
            int textColor = (int) (180 * alpha) << 24 | 0xAAAAAA;
            graphics.drawCenteredString(font,
                    Component.translatable("menu.aromaaffect.biomes.no_results"),
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

            renderBiomeRow(graphics, card, listX, rowY, listWidth,
                    isHovered, isTracking, animationProgress);
        }

        graphics.disableScissor();
    }

    private void renderBiomeRow(GuiGraphics graphics, SelectionCard card, int x, int y,
                                 int rowWidth, boolean isHovered, boolean isTracking,
                                 float animationProgress) {
        // Background
        int bgColor;
        if (isTracking) {
            bgColor = isHovered ? ROW_TRACKING_HOVER_COLOR : ROW_TRACKING_COLOR;
        } else {
            bgColor = isHovered ? ROW_HOVER_COLOR : ROW_COLOR;
        }
        int a = (int) (((bgColor >> 24) & 0xFF) * animationProgress);
        bgColor = (a << 24) | (bgColor & 0x00FFFFFF);
        graphics.fill(x, y, x + rowWidth, y + ROW_HEIGHT, bgColor);

        // Border
        if (isTracking) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            drawCardBorder(graphics, x, y, rowWidth, ROW_HEIGHT, borderColor);
        } else if (isHovered) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0xAAAAFF;
            drawCardBorder(graphics, x, y, rowWidth, ROW_HEIGHT, borderColor);
        }

        int pad = ROW_PADDING;

        // Thumbnail image (left side)
        int thumbX = x + pad;
        int thumbY = y + (ROW_HEIGHT - THUMB_H) / 2;
        if (animationProgress > 0.15f) {
            float imgAlpha = (animationProgress - 0.15f) / 0.85f;
            int thumbBorder = (int) (100 * imgAlpha) << 24 | 0x666666;
            graphics.fill(thumbX - 1, thumbY - 1, thumbX + THUMB_W + 1, thumbY + THUMB_H + 1, thumbBorder);

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    PLACEHOLDER_IMG,
                    thumbX, thumbY,
                    0.0f, 0.0f,
                    THUMB_W, THUMB_H,
                    THUMB_W, THUMB_H
            );
        }

        // Item icon (after thumbnail)
        int iconX = thumbX + THUMB_W + 6;
        int iconY = y + (ROW_HEIGHT - ICON_SIZE) / 2;
        if (card.icon != null && animationProgress > 0.2f) {
            float iconAlpha = (animationProgress - 0.2f) / 0.8f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(iconX, iconY);
            graphics.pose().scale(ICON_SIZE / 16.0f * iconAlpha, ICON_SIZE / 16.0f * iconAlpha);
            graphics.renderItem(card.icon, 0, 0);
            graphics.pose().popMatrix();
        }

        // Text (after icon)
        int textX = iconX + ICON_SIZE + 6;
        int nameColor;
        if (isTracking) {
            nameColor = (int) (255 * animationProgress) << 24 | 0x66FF66;
        } else {
            nameColor = (int) (255 * animationProgress) << 24 | 0xFFFFFF;
        }
        graphics.drawString(font, card.displayName, textX, y + ROW_HEIGHT / 2 - 10, nameColor);

        // Minecraft ID (gray, below name)
        int idColor = (int) (180 * animationProgress) << 24 | 0x888888;
        graphics.drawString(font, card.id.toString(), textX, y + ROW_HEIGHT / 2 + 2, idColor);

        // Tracking indicator on the right side
        if (isTracking) {
            int indicatorColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            Component trackingLabel = Component.translatable("menu.aromaaffect.selection.selected");
            int labelWidth = font.width(trackingLabel);
            graphics.drawString(font, trackingLabel, x + rowWidth - labelWidth - pad,
                    y + (ROW_HEIGHT - 8) / 2, indicatorColor);
        }
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (isHoveringBackButton) {
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (hoveredListIndex >= 0 && hoveredListIndex < filteredCards.size()) {
            SelectionCard card = filteredCards.get(hoveredListIndex);
            if (card.isUnlocked) {
                if (ActiveTrackingState.isTracking(card.id)) {
                    AromaAffect.LOGGER.info("Deselecting biome: {}", card.id);
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
        int listTop = 52 + SEARCH_BOX_HEIGHT + 8;
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
        ActiveTrackingState.set(card.id, card.displayName, card.icon, MenuCategory.BIOMES);
        AromaAffect.LOGGER.info("Selected biome for tracking: {}", card.id);

        startPathToBiome(card.id);
        MenuManager.returnToRadialMenu();
    }

    private void startPathToBiome(ResourceLocation biomeId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        String command = String.format("aromatest path biome %s", biomeId.toString());
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

    private String capitalizeWords(String str) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
