package com.ovrtechnology.menu;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import com.ovrtechnology.structure.StructureDefinition;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.tracking.RequiredItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Menu screen for selecting structures to track.
 *
 * @see MenuManager#openStructuresMenu()
 * @see SelectionMenuScreen
 */
public class StructuresMenuScreen extends SelectionMenuScreen {

    private static final int THUMB_W = 72;
    private static final int THUMB_H = 48;
    private static final int TEX_W = 288;
    private static final int TEX_H = 192;

    private static final ResourceLocation PLACEHOLDER_IMG = Ids.mod("textures/gui/sprites/structures/placeholder.png");

    static final Map<String, StructureInfo> STRUCTURE_INFO = new HashMap<>();

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

    public StructuresMenuScreen() {
        super(MenuCategory.STRUCTURES);
    }

    @Override
    protected int getRowHeight() {
        return 56;
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
            ResourceLocation resourceLocation = Ids.parse(structureId);
            addStructureCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} structure cards from equipped nose", cards.size());
    }

    private void addStructureCard(ResourceLocation structureId, boolean isUnlocked) {
        StructureInfo info = STRUCTURE_INFO.get(structureId.toString());
        StructureDefinition structureDef = StructureDefinitionLoader.getStructureById(structureId.toString());

        ItemStack icon = null;
        String displayName;

        if (structureDef != null && structureDef.getIconBlock() != null && !structureDef.getIconBlock().isEmpty()) {
            ResourceLocation blockLoc = Ids.parse(structureDef.getIconBlock());
            if (blockLoc != null) {
                icon = BuiltInRegistries.ITEM.getOptional(blockLoc)
                        .map(ItemStack::new).orElse(null);
            }
        }

        if (info != null) {
            if (icon == null || icon.isEmpty()) icon = info.icon;
            displayName = info.displayName;
        } else {
            if (icon == null || icon.isEmpty()) icon = Items.COMPASS.getDefaultInstance();
            displayName = structureDef != null && structureDef.getFallbackName() != null
                    && !structureDef.getFallbackName().isEmpty()
                    ? structureDef.getFallbackName()
                    : MenuRenderUtils.capitalizeWords(structureId.getPath().replace("_", " "));
        }

        Component name = Texts.lit(displayName);
        Component description = Texts.tr("menu.aromaaffect.structures.card.description", name);

        ResourceLocation thumbnail = null;
        if (structureDef != null && structureDef.getRawImage() != null
                && structureDef.getRawImage().contains(":")) {
            thumbnail = ResourceLocation.tryParse(structureDef.getRawImage());
        }
        if (thumbnail == null) {
            thumbnail = StructureThumbnailResolver.resolve(structureId);
        }
        SelectionCard card = new SelectionCard(structureId, name, icon, isUnlocked, description, thumbnail);

        if (structureDef != null) {
            card.trackCost = structureDef.getTrackCost();
            RequiredItem req = structureDef.getRequiredItem();
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
    protected void renderRow(GuiGraphics graphics, SelectionCard card, int x, int y,
                              int rowWidth, boolean isHovered, boolean isTracking,
                              float animationProgress) {
        int rowHeight = getRowHeight();

        // Background
        int bgColor;
        if (isTracking) {
            bgColor = isHovered ? ROW_TRACKING_HOVER_COLOR : ROW_TRACKING_COLOR;
        } else {
            bgColor = isHovered ? ROW_HOVER_COLOR : ROW_COLOR;
        }
        int a = (int) (((bgColor >> 24) & 0xFF) * animationProgress);
        bgColor = (a << 24) | (bgColor & 0x00FFFFFF);
        graphics.fill(x, y, x + rowWidth, y + rowHeight, bgColor);

        // Border
        if (isTracking) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            MenuRenderUtils.renderOutline(graphics, x, y, rowWidth, rowHeight, borderColor);
        } else if (isHovered) {
            int borderColor = (int) (255 * animationProgress) << 24 | 0xAAAAFF;
            MenuRenderUtils.renderOutline(graphics, x, y, rowWidth, rowHeight, borderColor);
        }

        int pad = ROW_PADDING;

        // Thumbnail image (left side)
        int thumbX = x + pad;
        int thumbY = y + (rowHeight - THUMB_H) / 2;
        if (animationProgress > 0.15f) {
            float imgAlpha = (animationProgress - 0.15f) / 0.85f;
            int thumbBorder = (int) (100 * imgAlpha) << 24 | 0x666666;
            graphics.fill(thumbX - 1, thumbY - 1, thumbX + THUMB_W + 1, thumbY + THUMB_H + 1, thumbBorder);

            ResourceLocation thumbTex = card.thumbnail != null ? card.thumbnail : PLACEHOLDER_IMG;
            float thumbScale = (float) THUMB_W / TEX_W;
            graphics.pose().pushMatrix();
            graphics.pose().translate(thumbX, thumbY);
            graphics.pose().scale(thumbScale, thumbScale);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    thumbTex,
                    0, 0,
                    0.0f, 0.0f,
                    TEX_W, TEX_H,
                    TEX_W, TEX_H
            );
            graphics.pose().popMatrix();
        }

        // Item icon (after thumbnail)
        int iconX = thumbX + THUMB_W + 6;
        int iconY = y + (rowHeight - ICON_SIZE) / 2;
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
        int nameColor = isTracking
                ? (int) (255 * animationProgress) << 24 | 0x66FF66
                : (int) (255 * animationProgress) << 24 | 0xFFFFFF;
        graphics.drawString(font, card.displayName, textX, y + rowHeight / 2 - 10, nameColor);

        int idColor = (int) (180 * animationProgress) << 24 | 0x888888;
        graphics.drawString(font, card.id.toString(), textX, y + rowHeight / 2 + 2, idColor);

        // Tracking indicator or cost section
        if (isTracking) {
            int indicatorColor = (int) (255 * animationProgress) << 24 | 0x44FF44;
            Component trackingLabel = Texts.tr("menu.aromaaffect.selection.selected");
            int labelWidth = font.width(trackingLabel);
            graphics.drawString(font, trackingLabel, x + rowWidth - labelWidth - pad,
                    y + (rowHeight - 8) / 2, indicatorColor);
        } else {
            renderCostSection(graphics, card, x + rowWidth, y + rowHeight / 2, animationProgress);
        }
    }

    record StructureInfo(ItemStack icon, String displayName) {}
}
