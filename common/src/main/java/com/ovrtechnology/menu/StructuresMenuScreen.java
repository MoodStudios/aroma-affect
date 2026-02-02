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
 * Menu screen for selecting structures to track.
 *
 * <p>Displays a searchable list of structures that the currently equipped Nose
 * can detect. Each row shows a screenshot thumbnail, item icon, display name,
 * and Minecraft ID.</p>
 *
 * @see MenuManager#openStructuresMenu()
 * @see SelectionMenuScreen
 */
public class StructuresMenuScreen extends SelectionMenuScreen {

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

    private static final ResourceLocation ICON_STRUCTURES = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_structures.png");
    private static final ResourceLocation ICON_BACK = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_back.png");
    private static final ResourceLocation PLACEHOLDER_IMG = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/structures/placeholder.png");

    /**
     * Map of structure IDs to their representative icons and display names.
     */
    private static final Map<String, StructureInfo> STRUCTURE_INFO = new HashMap<>();

    static {
        // Villages
        STRUCTURE_INFO.put("minecraft:village_plains", new StructureInfo(Items.BELL.getDefaultInstance(), "Village (Plains)"));
        STRUCTURE_INFO.put("minecraft:village_desert", new StructureInfo(Items.SANDSTONE.getDefaultInstance(), "Village (Desert)"));
        STRUCTURE_INFO.put("minecraft:village_savanna", new StructureInfo(Items.ACACIA_LOG.getDefaultInstance(), "Village (Savanna)"));
        STRUCTURE_INFO.put("minecraft:village_snowy", new StructureInfo(Items.SNOWBALL.getDefaultInstance(), "Village (Snowy)"));
        STRUCTURE_INFO.put("minecraft:village_taiga", new StructureInfo(Items.SPRUCE_LOG.getDefaultInstance(), "Village (Taiga)"));

        // Common overworld structures
        STRUCTURE_INFO.put("minecraft:mineshaft", new StructureInfo(Items.RAIL.getDefaultInstance(), "Mineshaft"));
        STRUCTURE_INFO.put("minecraft:mineshaft_mesa", new StructureInfo(Items.POWERED_RAIL.getDefaultInstance(), "Mineshaft (Mesa)"));
        STRUCTURE_INFO.put("minecraft:ruined_portal", new StructureInfo(Items.CRYING_OBSIDIAN.getDefaultInstance(), "Ruined Portal"));
        STRUCTURE_INFO.put("minecraft:ruined_portal_nether", new StructureInfo(Items.OBSIDIAN.getDefaultInstance(), "Ruined Portal (Nether)"));
        STRUCTURE_INFO.put("minecraft:shipwreck", new StructureInfo(Items.OAK_BOAT.getDefaultInstance(), "Shipwreck"));
        STRUCTURE_INFO.put("minecraft:ocean_ruin_cold", new StructureInfo(Items.PRISMARINE_BRICKS.getDefaultInstance(), "Ocean Ruins (Cold)"));
        STRUCTURE_INFO.put("minecraft:ocean_ruin_warm", new StructureInfo(Items.PRISMARINE.getDefaultInstance(), "Ocean Ruins (Warm)"));
        STRUCTURE_INFO.put("minecraft:buried_treasure", new StructureInfo(Items.HEART_OF_THE_SEA.getDefaultInstance(), "Buried Treasure"));

        // Pyramids and temples
        STRUCTURE_INFO.put("minecraft:desert_pyramid", new StructureInfo(Items.TNT.getDefaultInstance(), "Desert Pyramid"));
        STRUCTURE_INFO.put("minecraft:jungle_pyramid", new StructureInfo(Items.MOSSY_COBBLESTONE.getDefaultInstance(), "Jungle Temple"));
        STRUCTURE_INFO.put("minecraft:igloo", new StructureInfo(Items.SNOW_BLOCK.getDefaultInstance(), "Igloo"));
        STRUCTURE_INFO.put("minecraft:swamp_hut", new StructureInfo(Items.CAULDRON.getDefaultInstance(), "Witch Hut"));

        // Pillager structures
        STRUCTURE_INFO.put("minecraft:pillager_outpost", new StructureInfo(Items.CROSSBOW.getDefaultInstance(), "Pillager Outpost"));
        STRUCTURE_INFO.put("minecraft:mansion", new StructureInfo(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Woodland Mansion"));

        // Ocean
        STRUCTURE_INFO.put("minecraft:monument", new StructureInfo(Items.PRISMARINE_SHARD.getDefaultInstance(), "Ocean Monument"));

        // End-game overworld
        STRUCTURE_INFO.put("minecraft:stronghold", new StructureInfo(Items.END_PORTAL_FRAME.getDefaultInstance(), "Stronghold"));
        STRUCTURE_INFO.put("minecraft:ancient_city", new StructureInfo(Items.SCULK.getDefaultInstance(), "Ancient City"));
        STRUCTURE_INFO.put("minecraft:trail_ruins", new StructureInfo(Items.BRUSH.getDefaultInstance(), "Trail Ruins"));
        STRUCTURE_INFO.put("minecraft:trial_chambers", new StructureInfo(Items.TRIAL_KEY.getDefaultInstance(), "Trial Chambers"));

        // Nether structures
        STRUCTURE_INFO.put("minecraft:fortress", new StructureInfo(Items.NETHER_BRICK.getDefaultInstance(), "Nether Fortress"));
        STRUCTURE_INFO.put("minecraft:bastion_remnant", new StructureInfo(Items.GILDED_BLACKSTONE.getDefaultInstance(), "Bastion Remnant"));

        // End structures
        STRUCTURE_INFO.put("minecraft:end_city", new StructureInfo(Items.PURPUR_BLOCK.getDefaultInstance(), "End City"));
    }

    private EditBox searchBox;
    private String searchQuery = "";
    private List<SelectionCard> filteredCards = new ArrayList<>();
    private int listScrollOffset = 0;
    private int hoveredListIndex = -1;
    private boolean isHoveringBackButton = false;

    public StructuresMenuScreen() {
        super(MenuCategory.STRUCTURES);
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int searchX = (width - listWidth) / 2;
        int searchY = 52;

        searchBox = new EditBox(font, searchX, searchY, listWidth, SEARCH_BOX_HEIGHT,
                Component.translatable("menu.aromaaffect.structures.search_placeholder"));
        searchBox.setHint(Component.translatable("menu.aromaaffect.structures.search_placeholder"));
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
                    ICON_STRUCTURES,
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
            AromaAffect.LOGGER.debug("No player available for structures menu");
            return;
        }

        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableStructures = abilities.getStructures();

        if (detectableStructures.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no structure detection abilities");
            return;
        }

        for (String structureId : detectableStructures) {
            ResourceLocation resourceLocation = ResourceLocation.parse(structureId);
            addStructureCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} structure cards from equipped nose", cards.size());
    }

    private void addStructureCard(ResourceLocation structureId, boolean isUnlocked) {
        StructureInfo info = STRUCTURE_INFO.get(structureId.toString());

        ItemStack icon;
        String displayName;

        if (info != null) {
            icon = info.icon;
            displayName = info.displayName;
        } else {
            icon = Items.COMPASS.getDefaultInstance();
            displayName = capitalizeWords(structureId.getPath().replace("_", " "));
        }

        Component name = Component.literal(displayName);
        Component description = Component.translatable("menu.aromaaffect.structures.card.description", name);

        cards.add(new SelectionCard(structureId, name, icon, isUnlocked, description));
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
        renderStructureList(graphics, listX, listTop, listWidth, listBottom, mouseX, mouseY, animationProgress);
    }

    private void renderStructureList(GuiGraphics graphics, int listX, int listTop, int listWidth,
                                      int listBottom, int mouseX, int mouseY, float animationProgress) {
        hoveredListIndex = -1;

        if (filteredCards.isEmpty()) {
            float alpha = animationProgress;
            int textColor = (int) (180 * alpha) << 24 | 0xAAAAAA;
            graphics.drawCenteredString(font,
                    Component.translatable("menu.aromaaffect.structures.no_results"),
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

            renderStructureRow(graphics, card, listX, rowY, listWidth,
                    isHovered, isTracking, animationProgress);
        }

        graphics.disableScissor();
    }

    private void renderStructureRow(GuiGraphics graphics, SelectionCard card, int x, int y,
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
            // Dim border around thumbnail
            int thumbBorder = (int) (100 * imgAlpha) << 24 | 0x666666;
            graphics.fill(thumbX - 1, thumbY - 1, thumbX + THUMB_W + 1, thumbY + THUMB_H + 1, thumbBorder);

            // Render placeholder (pass render size as texture size to map full image)
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
                    AromaAffect.LOGGER.info("Deselecting structure: {}", card.id);
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
        ActiveTrackingState.set(card.id, card.displayName, card.icon, MenuCategory.STRUCTURES);
        AromaAffect.LOGGER.info("Selected structure for tracking: {}", card.id);

        startPathToStructure(card.id);
        MenuManager.returnToRadialMenu();
    }

    private void startPathToStructure(ResourceLocation structureId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        String command = String.format("aromatest path structure %s", structureId.toString());
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

    private record StructureInfo(ItemStack icon, String displayName) {}
}
