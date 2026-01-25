package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * Menu screen for selecting flowers/flora to track.
 * 
 * <p>This menu displays all flower-type blocks that the currently equipped Nose can detect.
 * Flowers are shown as cards with their item icons.</p>
 * 
 * <p><b>NOTE:</b> This is a base implementation. Full implementation should include:</p>
 * <ul>
 *   <li>Loading flowers from the equipped Nose's ability configuration</li>
 *   <li>Category filtering (common, rare, biome-specific, etc.)</li>
 *   <li>Search functionality</li>
 *   <li>Integration with the tracking system to set the active target</li>
 * </ul>
 * 
 * @see MenuManager#openFlowersMenu()
 * @see SelectionMenuScreen
 */
public class FlowersMenuScreen extends SelectionMenuScreen {
    
    public FlowersMenuScreen() {
        super(MenuCategory.FLOWERS);
    }
    
    @Override
    protected void loadCards() {
        cards.clear();
        
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaAffect.LOGGER.debug("No player available for flowers menu");
            return;
        }
        
        // TODO: Get equipped nose and its detectable flowers
        Set<ResourceLocation> detectableFlowers = getDetectableFlowers(player);
        
        if (detectableFlowers.isEmpty()) {
            // Add placeholder flowers for development/testing
            addPlaceholderFlowers();
        } else {
            for (ResourceLocation flowerId : detectableFlowers) {
                addFlowerCard(flowerId, true);
            }
        }
        
        AromaAffect.LOGGER.debug("Loaded {} flower cards", cards.size());
    }
    
    /**
     * Gets the set of flowers that the player's equipped nose can detect.
     */
    private Set<ResourceLocation> getDetectableFlowers(Player player) {
        // TODO: Implement proper nose detection
        return Set.of();
    }
    
    /**
     * Adds placeholder flowers for development and testing.
     */
    private void addPlaceholderFlowers() {
        // Common flowers (unlocked by default)
        addFlowerCard(ResourceLocation.withDefaultNamespace("poppy"), Items.POPPY.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("dandelion"), Items.DANDELION.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("blue_orchid"), Items.BLUE_ORCHID.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("allium"), Items.ALLIUM.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("azure_bluet"), Items.AZURE_BLUET.getDefaultInstance(), true);
        
        // Tulips
        addFlowerCard(ResourceLocation.withDefaultNamespace("red_tulip"), Items.RED_TULIP.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("orange_tulip"), Items.ORANGE_TULIP.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("white_tulip"), Items.WHITE_TULIP.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("pink_tulip"), Items.PINK_TULIP.getDefaultInstance(), true);
        
        // Other flowers
        addFlowerCard(ResourceLocation.withDefaultNamespace("oxeye_daisy"), Items.OXEYE_DAISY.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("cornflower"), Items.CORNFLOWER.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("lily_of_the_valley"), Items.LILY_OF_THE_VALLEY.getDefaultInstance(), true);
        
        // Tall flowers
        addFlowerCard(ResourceLocation.withDefaultNamespace("sunflower"), Items.SUNFLOWER.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("lilac"), Items.LILAC.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("rose_bush"), Items.ROSE_BUSH.getDefaultInstance(), true);
        addFlowerCard(ResourceLocation.withDefaultNamespace("peony"), Items.PEONY.getDefaultInstance(), true);
        
        // Rare/special flowers (locked by default)
        addFlowerCard(ResourceLocation.withDefaultNamespace("wither_rose"), Items.WITHER_ROSE.getDefaultInstance(), false);
        addFlowerCard(ResourceLocation.withDefaultNamespace("torchflower"), Items.TORCHFLOWER.getDefaultInstance(), false);
        addFlowerCard(ResourceLocation.withDefaultNamespace("pitcher_plant"), Items.PITCHER_PLANT.getDefaultInstance(), false);
        
        // Nether flora (locked by default)
        addFlowerCard(ResourceLocation.withDefaultNamespace("crimson_fungus"), Items.CRIMSON_FUNGUS.getDefaultInstance(), false);
        addFlowerCard(ResourceLocation.withDefaultNamespace("warped_fungus"), Items.WARPED_FUNGUS.getDefaultInstance(), false);
    }
    
    /**
     * Adds a flower card to the menu.
     */
    private void addFlowerCard(ResourceLocation flowerId, boolean isUnlocked) {
        // Determine an appropriate icon for the flower
        ItemStack icon = getFlowerIcon(flowerId);
        addFlowerCard(flowerId, icon, isUnlocked);
    }
    
    /**
     * Adds a flower card with a specific icon.
     */
    private void addFlowerCard(ResourceLocation flowerId, ItemStack icon, boolean isUnlocked) {
        // Create display name from flower ID
        String flowerName = flowerId.getPath().replace("_", " ");
        flowerName = capitalizeWords(flowerName);
        Component displayName = Component.literal(flowerName);
        
        // TODO: Use proper item translation keys when available
        // Component displayName = Component.translatable("block." + flowerId.getNamespace() + "." + flowerId.getPath());
        
        Component description = Component.translatable("menu.aromaaffect.flowers.card.description", displayName);
        
        cards.add(new SelectionCard(flowerId, displayName, icon, isUnlocked, description));
    }
    
    /**
     * Gets an appropriate icon for a flower.
     */
    private ItemStack getFlowerIcon(ResourceLocation flowerId) {
        // Default fallback icon
        return Items.POPPY.getDefaultInstance();
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
        AromaAffect.LOGGER.info("Selected flower for tracking: {}", card.id);
        
        // TODO: Implement tracking activation
        // This should:
        // 1. Set the player's active tracking target to this flower
        // 2. Start the flower tracking overlay
        // 3. Optionally close the menu or show confirmation
        
        closeCurrentMenu();
    }
    
    private void closeCurrentMenu() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
