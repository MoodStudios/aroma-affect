package com.ovrtechnology.menu;

import com.ovrtechnology.AromaCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * Menu screen for selecting structures to track.
 * 
 * <p>This menu displays all structures that the currently equipped Nose can detect.
 * Structures are shown as cards with representative item icons.</p>
 * 
 * <p><b>NOTE:</b> This is a base implementation. Full implementation should include:</p>
 * <ul>
 *   <li>Loading structures from the equipped Nose's ability configuration</li>
 *   <li>Category filtering (villages, dungeons, monuments, etc.)</li>
 *   <li>Search functionality</li>
 *   <li>Structure-specific icons</li>
 *   <li>Displaying structure tier/rarity</li>
 *   <li>Integration with the tracking system to set the active target</li>
 * </ul>
 * 
 * @see MenuManager#openStructuresMenu()
 * @see SelectionMenuScreen
 */
public class StructuresMenuScreen extends SelectionMenuScreen {
    
    public StructuresMenuScreen() {
        super(MenuCategory.STRUCTURES);
    }
    
    @Override
    protected void loadCards() {
        cards.clear();
        
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaCraft.LOGGER.debug("No player available for structures menu");
            return;
        }
        
        // TODO: Get equipped nose and its detectable structures
        Set<ResourceLocation> detectableStructures = getDetectableStructures(player);
        
        if (detectableStructures.isEmpty()) {
            // Add placeholder structures for development/testing
            addPlaceholderStructures();
        } else {
            for (ResourceLocation structureId : detectableStructures) {
                addStructureCard(structureId, true);
            }
        }
        
        AromaCraft.LOGGER.debug("Loaded {} structure cards", cards.size());
    }
    
    /**
     * Gets the set of structures that the player's equipped nose can detect.
     */
    private Set<ResourceLocation> getDetectableStructures(Player player) {
        // TODO: Implement proper nose detection
        return Set.of();
    }
    
    /**
     * Adds placeholder structures for development and testing.
     */
    private void addPlaceholderStructures() {
        // Common structures (unlocked early)
        addStructureCard(ResourceLocation.withDefaultNamespace("village_plains"), 
                        Items.BELL.getDefaultInstance(), true, "Village (Plains)");
        addStructureCard(ResourceLocation.withDefaultNamespace("village_desert"), 
                        Items.SANDSTONE.getDefaultInstance(), true, "Village (Desert)");
        addStructureCard(ResourceLocation.withDefaultNamespace("mineshaft"), 
                        Items.RAIL.getDefaultInstance(), true, "Mineshaft");
        addStructureCard(ResourceLocation.withDefaultNamespace("ruined_portal"), 
                        Items.CRYING_OBSIDIAN.getDefaultInstance(), true, "Ruined Portal");
        addStructureCard(ResourceLocation.withDefaultNamespace("shipwreck"), 
                        Items.OAK_BOAT.getDefaultInstance(), true, "Shipwreck");
        
        // Mid-tier structures
        addStructureCard(ResourceLocation.withDefaultNamespace("desert_pyramid"), 
                        Items.TNT.getDefaultInstance(), true, "Desert Pyramid");
        addStructureCard(ResourceLocation.withDefaultNamespace("jungle_pyramid"), 
                        Items.MOSSY_COBBLESTONE.getDefaultInstance(), true, "Jungle Temple");
        addStructureCard(ResourceLocation.withDefaultNamespace("ocean_monument"), 
                        Items.PRISMARINE.getDefaultInstance(), false, "Ocean Monument");
        addStructureCard(ResourceLocation.withDefaultNamespace("pillager_outpost"), 
                        Items.CROSSBOW.getDefaultInstance(), false, "Pillager Outpost");
        addStructureCard(ResourceLocation.withDefaultNamespace("mansion"), 
                        Items.TOTEM_OF_UNDYING.getDefaultInstance(), false, "Woodland Mansion");
        
        // End-game structures (locked)
        addStructureCard(ResourceLocation.withDefaultNamespace("stronghold"), 
                        Items.END_PORTAL_FRAME.getDefaultInstance(), false, "Stronghold");
        addStructureCard(ResourceLocation.withDefaultNamespace("fortress"), 
                        Items.NETHER_BRICK.getDefaultInstance(), false, "Nether Fortress");
        addStructureCard(ResourceLocation.withDefaultNamespace("bastion_remnant"), 
                        Items.GILDED_BLACKSTONE.getDefaultInstance(), false, "Bastion Remnant");
        addStructureCard(ResourceLocation.withDefaultNamespace("end_city"), 
                        Items.PURPUR_BLOCK.getDefaultInstance(), false, "End City");
        addStructureCard(ResourceLocation.withDefaultNamespace("ancient_city"), 
                        Items.SCULK.getDefaultInstance(), false, "Ancient City");
        
        // Special structures
        addStructureCard(ResourceLocation.withDefaultNamespace("trial_chambers"), 
                        Items.TRIAL_KEY.getDefaultInstance(), false, "Trial Chambers");
    }
    
    /**
     * Adds a structure card with automatic display name.
     */
    private void addStructureCard(ResourceLocation structureId, boolean isUnlocked) {
        ItemStack icon = getStructureIcon(structureId);
        String name = structureId.getPath().replace("_", " ");
        name = capitalizeWords(name);
        addStructureCardInternal(structureId, icon, isUnlocked, name);
    }
    
    /**
     * Adds a structure card with a specific icon and display name.
     */
    private void addStructureCard(ResourceLocation structureId, ItemStack icon, 
                                  boolean isUnlocked, String displayName) {
        addStructureCardInternal(structureId, icon, isUnlocked, displayName);
    }
    
    private void addStructureCardInternal(ResourceLocation structureId, ItemStack icon, 
                                          boolean isUnlocked, String displayName) {
        Component name = Component.literal(displayName);
        Component description = Component.translatable("menu.aromacraft.structures.card.description", name);
        
        cards.add(new SelectionCard(structureId, name, icon, isUnlocked, description));
    }
    
    /**
     * Gets an appropriate icon for a structure.
     */
    private ItemStack getStructureIcon(ResourceLocation structureId) {
        // Default fallback icon
        return Items.BELL.getDefaultInstance();
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
        AromaCraft.LOGGER.info("Selected structure for tracking: {}", card.id);
        
        // TODO: Implement tracking activation
        // This should:
        // 1. Set the player's active tracking target to this structure
        // 2. Start the structure tracking/compass overlay
        // 3. Optionally trigger the StructureLookupStrategy
        // 4. Close the menu or show confirmation
        
        closeCurrentMenu();
    }
    
    private void closeCurrentMenu() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
