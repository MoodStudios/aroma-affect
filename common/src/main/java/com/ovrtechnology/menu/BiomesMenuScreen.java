package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.biome.BiomeDefinition;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
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

public class BiomesMenuScreen extends SelectionMenuScreen {

    private static final int THUMB_W = 72;
    private static final int THUMB_H = 48;
    private static final int TEX_W = 288;
    private static final int TEX_H = 192;

    private static final ResourceLocation PLACEHOLDER_IMG =
            Ids.mod("textures/gui/sprites/biomes/placeholder.png");

    static final Map<String, ItemStack> BIOME_ICONS = new HashMap<>();

    static {
        BIOME_ICONS.put("minecraft:plains", Items.GRASS_BLOCK.getDefaultInstance());
        BIOME_ICONS.put("minecraft:sunflower_plains", Items.SUNFLOWER.getDefaultInstance());
        BIOME_ICONS.put("minecraft:meadow", Items.DANDELION.getDefaultInstance());

        BIOME_ICONS.put("minecraft:forest", Items.OAK_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:birch_forest", Items.BIRCH_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:dark_forest", Items.DARK_OAK_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:old_growth_birch_forest", Items.BIRCH_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:flower_forest", Items.PEONY.getDefaultInstance());
        BIOME_ICONS.put("minecraft:cherry_grove", Items.CHERRY_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:windswept_forest", Items.OAK_LEAVES.getDefaultInstance());
        BIOME_ICONS.put("minecraft:pale_garden", Items.MOSS_BLOCK.getDefaultInstance());

        BIOME_ICONS.put("minecraft:taiga", Items.SPRUCE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:snowy_taiga", Items.SPRUCE_SAPLING.getDefaultInstance());
        BIOME_ICONS.put("minecraft:old_growth_pine_taiga", Items.SPRUCE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:old_growth_spruce_taiga", Items.SPRUCE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:grove", Items.SPRUCE_LEAVES.getDefaultInstance());

        BIOME_ICONS.put("minecraft:desert", Items.SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:badlands", Items.TERRACOTTA.getDefaultInstance());
        BIOME_ICONS.put("minecraft:eroded_badlands", Items.RED_SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:wooded_badlands", Items.COARSE_DIRT.getDefaultInstance());

        BIOME_ICONS.put("minecraft:jungle", Items.JUNGLE_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:bamboo_jungle", Items.BAMBOO.getDefaultInstance());
        BIOME_ICONS.put("minecraft:sparse_jungle", Items.JUNGLE_SAPLING.getDefaultInstance());

        BIOME_ICONS.put("minecraft:swamp", Items.LILY_PAD.getDefaultInstance());
        BIOME_ICONS.put("minecraft:mangrove_swamp", Items.MANGROVE_LOG.getDefaultInstance());

        BIOME_ICONS.put("minecraft:savanna", Items.ACACIA_LOG.getDefaultInstance());
        BIOME_ICONS.put("minecraft:savanna_plateau", Items.ACACIA_SAPLING.getDefaultInstance());
        BIOME_ICONS.put("minecraft:windswept_savanna", Items.ACACIA_LEAVES.getDefaultInstance());

        BIOME_ICONS.put("minecraft:snowy_plains", Items.SNOWBALL.getDefaultInstance());
        BIOME_ICONS.put("minecraft:snowy_beach", Items.SNOW_BLOCK.getDefaultInstance());
        BIOME_ICONS.put("minecraft:snowy_slopes", Items.POWDER_SNOW_BUCKET.getDefaultInstance());
        BIOME_ICONS.put("minecraft:ice_spikes", Items.PACKED_ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:frozen_peaks", Items.ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:jagged_peaks", Items.STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:stony_peaks", Items.CALCITE.getDefaultInstance());

        BIOME_ICONS.put("minecraft:windswept_hills", Items.GRAVEL.getDefaultInstance());
        BIOME_ICONS.put("minecraft:windswept_gravelly_hills", Items.GRAVEL.getDefaultInstance());

        BIOME_ICONS.put("minecraft:ocean", Items.WATER_BUCKET.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_ocean", Items.PRISMARINE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:warm_ocean", Items.BRAIN_CORAL.getDefaultInstance());
        BIOME_ICONS.put("minecraft:lukewarm_ocean", Items.KELP.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_lukewarm_ocean", Items.SEAGRASS.getDefaultInstance());
        BIOME_ICONS.put("minecraft:cold_ocean", Items.COD.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_cold_ocean", Items.SALMON.getDefaultInstance());
        BIOME_ICONS.put("minecraft:frozen_ocean", Items.BLUE_ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_frozen_ocean", Items.BLUE_ICE.getDefaultInstance());

        BIOME_ICONS.put("minecraft:river", Items.WATER_BUCKET.getDefaultInstance());
        BIOME_ICONS.put("minecraft:frozen_river", Items.ICE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:beach", Items.SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:stony_shore", Items.STONE.getDefaultInstance());

        BIOME_ICONS.put("minecraft:lush_caves", Items.GLOW_BERRIES.getDefaultInstance());
        BIOME_ICONS.put("minecraft:dripstone_caves", Items.POINTED_DRIPSTONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:deep_dark", Items.SCULK.getDefaultInstance());

        BIOME_ICONS.put("minecraft:mushroom_fields", Items.RED_MUSHROOM.getDefaultInstance());

        BIOME_ICONS.put("minecraft:nether_wastes", Items.NETHERRACK.getDefaultInstance());
        BIOME_ICONS.put("minecraft:crimson_forest", Items.CRIMSON_STEM.getDefaultInstance());
        BIOME_ICONS.put("minecraft:warped_forest", Items.WARPED_STEM.getDefaultInstance());
        BIOME_ICONS.put("minecraft:soul_sand_valley", Items.SOUL_SAND.getDefaultInstance());
        BIOME_ICONS.put("minecraft:basalt_deltas", Items.BASALT.getDefaultInstance());

        BIOME_ICONS.put("minecraft:the_end", Items.END_STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:end_highlands", Items.CHORUS_FLOWER.getDefaultInstance());
        BIOME_ICONS.put("minecraft:end_midlands", Items.END_STONE_BRICKS.getDefaultInstance());
        BIOME_ICONS.put("minecraft:small_end_islands", Items.END_STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:end_barrens", Items.END_STONE.getDefaultInstance());
        BIOME_ICONS.put("minecraft:the_void", Items.BARRIER.getDefaultInstance());
    }

    public BiomesMenuScreen() {
        super(MenuCategory.BIOMES);
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
            AromaAffect.LOGGER.debug("No player available for biomes menu");
            return;
        }

        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        NoseAbilityResolver.ResolvedAbilities abilities =
                EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableBiomes = abilities.getBiomes();

        if (detectableBiomes.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no biome detection abilities");
            return;
        }

        for (String biomeId : detectableBiomes) {
            ResourceLocation resourceLocation = Ids.parse(biomeId);
            addBiomeCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} biome cards from equipped nose", cards.size());
    }

    private void addBiomeCard(ResourceLocation biomeId, boolean isUnlocked) {
        BiomeDefinition biomeDef = BiomeDefinitionLoader.getBiomeById(biomeId.toString());

        ItemStack icon = null;
        if (biomeDef != null && biomeDef.getBlock() != null && !biomeDef.getBlock().isEmpty()) {
            ResourceLocation blockLoc = Ids.parse(biomeDef.getBlock());
            if (blockLoc != null) {
                icon =
                        BuiltInRegistries.ITEM
                                .getOptional(blockLoc)
                                .map(ItemStack::new)
                                .orElse(null);
            }
        }
        if (icon == null || icon.isEmpty()) {
            icon =
                    BIOME_ICONS.getOrDefault(
                            biomeId.toString(), Items.GRASS_BLOCK.getDefaultInstance());
        }

        String biomeName = MenuRenderUtils.capitalizeWords(biomeId.getPath().replace("_", " "));
        Component displayName = Texts.lit(biomeName);
        Component description = Texts.tr("menu.aromaaffect.biomes.card.description", displayName);

        ResourceLocation thumbnail = null;
        if (biomeDef != null
                && biomeDef.getRawImage() != null
                && biomeDef.getRawImage().contains(":")) {
            thumbnail = ResourceLocation.tryParse(biomeDef.getRawImage());
        }
        if (thumbnail == null) {
            thumbnail = BiomeThumbnailResolver.resolve(biomeId);
        }
        SelectionCard card =
                new SelectionCard(biomeId, displayName, icon, isUnlocked, description, thumbnail);

        if (biomeDef != null) {
            card.trackCost = biomeDef.getTrackCost();
            RequiredItem req = biomeDef.getRequiredItem();
            if (req != null && req.getItemId() != null) {
                ResourceLocation reqId = Ids.parse(req.getItemId());
                var itemOpt = BuiltInRegistries.ITEM.getOptional(reqId);
                if (itemOpt.isPresent()) {
                    card.requiredItem = new ItemStack(itemOpt.get());
                    card.requiredItemCount = req.getCount();
                }
            }
        }

        cards.add(card);
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

        int pad = ROW_PADDING;

        int thumbX = x + pad;
        int thumbY = y + (rowHeight - THUMB_H) / 2;
        if (animationProgress > 0.15f) {
            float imgAlpha = (animationProgress - 0.15f) / 0.85f;
            int thumbBorder = (int) (100 * imgAlpha) << 24 | 0x666666;
            graphics.fill(
                    thumbX - 1,
                    thumbY - 1,
                    thumbX + THUMB_W + 1,
                    thumbY + THUMB_H + 1,
                    thumbBorder);

            ResourceLocation thumbTex = card.thumbnail != null ? card.thumbnail : PLACEHOLDER_IMG;
            MenuRenderUtils.blitScaledNearest(
                    graphics, thumbTex, thumbX, thumbY, THUMB_W, THUMB_H, TEX_W, TEX_H);
        }

        int iconX = thumbX + THUMB_W + 6;
        int iconY = y + (rowHeight - ICON_SIZE) / 2;
        if (card.icon != null && animationProgress > 0.2f) {
            float iconAlpha = (animationProgress - 0.2f) / 0.8f;
            graphics.pose().pushPose();
            graphics.pose().translate(iconX, iconY, 0);
            graphics.pose().scale(ICON_SIZE / 16.0f * iconAlpha, ICON_SIZE / 16.0f * iconAlpha, 1);
            graphics.renderItem(card.icon, 0, 0);
            graphics.pose().popPose();
        }

        int textX = iconX + ICON_SIZE + 6;
        int nameColor =
                isTracking
                        ? (int) (255 * animationProgress) << 24 | Colors.SUCCESS_GREEN_RGB_BRIGHT
                        : (int) (255 * animationProgress) << 24 | 0xFFFFFF;
        graphics.drawString(font, card.displayName, textX, y + rowHeight / 2 - 10, nameColor);

        int idColor = (int) (180 * animationProgress) << 24 | 0x888888;
        graphics.drawString(font, card.id.toString(), textX, y + rowHeight / 2 + 2, idColor);

        if (isTracking) {
            int indicatorColor = (int) (255 * animationProgress) << 24 | Colors.SUCCESS_GREEN_RGB;
            Component trackingLabel = Texts.tr("menu.aromaaffect.selection.selected");
            int labelWidth = font.width(trackingLabel);
            graphics.drawString(
                    font,
                    trackingLabel,
                    x + rowWidth - labelWidth - pad,
                    y + (rowHeight - 8) / 2,
                    indicatorColor);
        } else {
            renderCostSection(graphics, card, x + rowWidth, y + rowHeight / 2, animationProgress);
        }
    }
}
