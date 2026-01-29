package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Menu screen for selecting biomes to track.
 *
 * <p>This menu displays all biomes that the currently equipped Nose can detect.
 * Biomes are shown as cards with representative item icons.</p>
 *
 * <p>When a biome is selected, the path command is executed to create a particle
 * trail leading to the nearest instance of that biome.</p>
 *
 * @see MenuManager#openBiomesMenu()
 * @see SelectionMenuScreen
 */
public class BiomesMenuScreen extends SelectionMenuScreen {

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

    public BiomesMenuScreen() {
        super(MenuCategory.BIOMES);
    }

    @Override
    protected void loadCards() {
        cards.clear();

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaAffect.LOGGER.debug("No player available for biomes menu");
            return;
        }

        // Check if player has a nose equipped
        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        // Get detectable biomes from equipped nose
        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableBiomes = abilities.getBiomes();

        if (detectableBiomes.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no biome detection abilities");
            return;
        }

        // Add cards for each detectable biome
        for (String biomeId : detectableBiomes) {
            ResourceLocation resourceLocation = ResourceLocation.parse(biomeId);
            addBiomeCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} biome cards from equipped nose", cards.size());
    }

    /**
     * Adds a biome card to the menu.
     *
     * @param biomeId    the biome's resource location
     * @param isUnlocked whether the biome is unlocked for tracking
     */
    private void addBiomeCard(ResourceLocation biomeId, boolean isUnlocked) {
        ItemStack icon = BIOME_ICONS.getOrDefault(biomeId.toString(), Items.GRASS_BLOCK.getDefaultInstance());

        // Create display name from biome ID
        String biomeName = biomeId.getPath().replace("_", " ");
        biomeName = capitalizeWords(biomeName);
        Component displayName = Component.literal(biomeName);

        Component description = Component.translatable("menu.aromaaffect.biomes.card.description", displayName);

        cards.add(new SelectionCard(biomeId, displayName, icon, isUnlocked, description));
    }

    /**
     * Capitalizes the first letter of each word.
     */
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

    @Override
    protected void onCardSelected(SelectionCard card, int index) {
        selectedCardIndex = index;
        AromaAffect.LOGGER.info("Selected biome for tracking: {}", card.id);

        // Close menu first
        closeCurrentMenu();

        // Execute path command to start tracking
        startPathToBiome(card.id);
    }

    /**
     * Starts a particle path to the selected biome by executing the path command.
     *
     * @param biomeId the biome's resource location
     */
    private void startPathToBiome(ResourceLocation biomeId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // Send the path command to the server
        String command = String.format("aromatest path biome %s", biomeId.toString());
        AromaAffect.LOGGER.debug("Executing path command: {}", command);

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand(command);
        }
    }

    /**
     * Closes this menu and returns to the game.
     */
    private void closeCurrentMenu() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
