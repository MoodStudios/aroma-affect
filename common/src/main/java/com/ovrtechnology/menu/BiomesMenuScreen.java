package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * Menu screen for selecting biomes to track.
 * 
 * <p>This menu displays all biomes that the currently equipped Nose can detect.
 * Biomes are shown as cards with representative item icons.</p>
 * 
 * <p><b>NOTE:</b> This is a base implementation. Full implementation should include:</p>
 * <ul>
 *   <li>Loading biomes from the equipped Nose's ability configuration</li>
 *   <li>Category filtering (overworld, nether, end, etc.)</li>
 *   <li>Search functionality</li>
 *   <li>Biome-specific icons representing the biome type</li>
 *   <li>Integration with the tracking system to set the active target</li>
 * </ul>
 * 
 * @see MenuManager#openBiomesMenu()
 * @see SelectionMenuScreen
 */
public class BiomesMenuScreen extends SelectionMenuScreen {
    
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
        
        // TODO: Get equipped nose and its detectable biomes
        Set<ResourceLocation> detectableBiomes = getDetectableBiomes(player);
        
        if (detectableBiomes.isEmpty()) {
            // Add placeholder biomes for development/testing
            addPlaceholderBiomes();
        } else {
            for (ResourceLocation biomeId : detectableBiomes) {
                addBiomeCard(biomeId, true);
            }
        }
        
        AromaAffect.LOGGER.debug("Loaded {} biome cards", cards.size());
    }
    
    /**
     * Gets the set of biomes that the player's equipped nose can detect.
     */
    private Set<ResourceLocation> getDetectableBiomes(Player player) {
        // TODO: Implement proper nose detection
        return Set.of();
    }
    
    /**
     * Adds placeholder biomes for development and testing.
     */
    private void addPlaceholderBiomes() {
        // Overworld biomes
        addBiomeCard(ResourceLocation.withDefaultNamespace("plains"), Items.GRASS_BLOCK.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("forest"), Items.OAK_LOG.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("desert"), Items.SAND.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("taiga"), Items.SPRUCE_LOG.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("jungle"), Items.JUNGLE_LOG.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("swamp"), Items.LILY_PAD.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("ocean"), Items.WATER_BUCKET.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("mushroom_fields"), Items.RED_MUSHROOM.getDefaultInstance(), true);
        
        // Snowy biomes
        addBiomeCard(ResourceLocation.withDefaultNamespace("snowy_plains"), Items.SNOWBALL.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("ice_spikes"), Items.PACKED_ICE.getDefaultInstance(), false);
        
        // Mountain biomes
        addBiomeCard(ResourceLocation.withDefaultNamespace("meadow"), Items.DANDELION.getDefaultInstance(), true);
        addBiomeCard(ResourceLocation.withDefaultNamespace("jagged_peaks"), Items.STONE.getDefaultInstance(), false);
        
        // Nether biomes (locked by default)
        addBiomeCard(ResourceLocation.withDefaultNamespace("nether_wastes"), Items.NETHERRACK.getDefaultInstance(), false);
        addBiomeCard(ResourceLocation.withDefaultNamespace("crimson_forest"), Items.CRIMSON_STEM.getDefaultInstance(), false);
        addBiomeCard(ResourceLocation.withDefaultNamespace("warped_forest"), Items.WARPED_STEM.getDefaultInstance(), false);
        addBiomeCard(ResourceLocation.withDefaultNamespace("soul_sand_valley"), Items.SOUL_SAND.getDefaultInstance(), false);
        addBiomeCard(ResourceLocation.withDefaultNamespace("basalt_deltas"), Items.BASALT.getDefaultInstance(), false);
        
        // End biomes (locked by default)
        addBiomeCard(ResourceLocation.withDefaultNamespace("the_end"), Items.END_STONE.getDefaultInstance(), false);
        addBiomeCard(ResourceLocation.withDefaultNamespace("end_highlands"), Items.CHORUS_FLOWER.getDefaultInstance(), false);
    }
    
    /**
     * Adds a biome card to the menu.
     */
    private void addBiomeCard(ResourceLocation biomeId, boolean isUnlocked) {
        // Determine an appropriate icon for the biome
        ItemStack icon = getBiomeIcon(biomeId);
        addBiomeCard(biomeId, icon, isUnlocked);
    }
    
    /**
     * Adds a biome card with a specific icon.
     */
    private void addBiomeCard(ResourceLocation biomeId, ItemStack icon, boolean isUnlocked) {
        // Create display name from biome ID
        String biomeName = biomeId.getPath().replace("_", " ");
        biomeName = capitalizeWords(biomeName);
        Component displayName = Component.literal(biomeName);
        
        // TODO: Use proper biome translation keys when available
        // Component displayName = Component.translatable("biome." + biomeId.getNamespace() + "." + biomeId.getPath());
        
        Component description = Component.translatable("menu.aromaaffect.biomes.card.description", displayName);
        
        cards.add(new SelectionCard(biomeId, displayName, icon, isUnlocked, description));
    }
    
    /**
     * Gets an appropriate icon for a biome.
     */
    private ItemStack getBiomeIcon(ResourceLocation biomeId) {
        // Default fallback icon
        return Items.GRASS_BLOCK.getDefaultInstance();
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
        
        // TODO: Implement tracking activation
        // This should:
        // 1. Set the player's active tracking target to this biome
        // 2. Start the biome tracking overlay
        // 3. Optionally close the menu or show confirmation
        
        closeCurrentMenu();
    }
    
    private void closeCurrentMenu() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
