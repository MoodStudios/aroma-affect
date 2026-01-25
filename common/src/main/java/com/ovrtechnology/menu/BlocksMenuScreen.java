package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseAbilityResolver;
import com.ovrtechnology.nose.NoseItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Menu screen for selecting blocks to track.
 * 
 * <p>This menu displays all blocks that the currently equipped Nose can detect.
 * Blocks are shown as cards with their item icons, organized in a scrollable grid.</p>
 * 
 * <p><b>NOTE:</b> This is a base implementation. Full implementation should include:</p>
 * <ul>
 *   <li>Loading blocks from the equipped Nose's ability configuration</li>
 *   <li>Category filtering (ores, decoration, functional, etc.)</li>
 *   <li>Search functionality</li>
 *   <li>Displaying block rarity/tier requirements</li>
 *   <li>Integration with the tracking system to set the active target</li>
 * </ul>
 * 
 * @see MenuManager#openBlocksMenu()
 * @see SelectionMenuScreen
 */
public class BlocksMenuScreen extends SelectionMenuScreen {
    
    public BlocksMenuScreen() {
        super(MenuCategory.BLOCKS);
    }
    
    @Override
    protected void loadCards() {
        cards.clear();
        
        // Get the player's equipped nose to determine what blocks they can detect
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaAffect.LOGGER.debug("No player available for blocks menu");
            return;
        }
        
        // TODO: Get equipped nose from player's curios slot or inventory
        // For now, we'll add some placeholder blocks
        
        // Try to get abilities from a nose (placeholder logic)
        Set<ResourceLocation> detectableBlocks = getDetectableBlocks(player);
        
        if (detectableBlocks.isEmpty()) {
            // Add placeholder blocks for development/testing
            addPlaceholderBlocks();
        } else {
            // Add cards for each detectable block
            for (ResourceLocation blockId : detectableBlocks) {
                addBlockCard(blockId, true);
            }
        }
        
        AromaAffect.LOGGER.debug("Loaded {} block cards", cards.size());
    }
    
    /**
     * Gets the set of blocks that the player's equipped nose can detect.
     * 
     * @param player the player
     * @return set of detectable block resource locations
     */
    private Set<ResourceLocation> getDetectableBlocks(Player player) {
        // TODO: Implement proper nose detection from curios/inventory
        // This should check:
        // 1. Player's curios head slot for a NoseItem
        // 2. Fall back to hotbar/inventory if no curios
        // 3. Return the nose's detectable blocks via NoseAbilityResolver
        
        return Set.of();
    }
    
    /**
     * Adds placeholder blocks for development and testing.
     */
    private void addPlaceholderBlocks() {
        // Common ores
        addBlockCard(ResourceLocation.withDefaultNamespace("diamond_ore"), true);
        addBlockCard(ResourceLocation.withDefaultNamespace("iron_ore"), true);
        addBlockCard(ResourceLocation.withDefaultNamespace("gold_ore"), true);
        addBlockCard(ResourceLocation.withDefaultNamespace("copper_ore"), true);
        addBlockCard(ResourceLocation.withDefaultNamespace("coal_ore"), true);
        addBlockCard(ResourceLocation.withDefaultNamespace("lapis_ore"), true);
        addBlockCard(ResourceLocation.withDefaultNamespace("redstone_ore"), true);
        addBlockCard(ResourceLocation.withDefaultNamespace("emerald_ore"), true);
        
        // Deepslate ores (locked as examples)
        addBlockCard(ResourceLocation.withDefaultNamespace("deepslate_diamond_ore"), false);
        addBlockCard(ResourceLocation.withDefaultNamespace("deepslate_iron_ore"), false);
        addBlockCard(ResourceLocation.withDefaultNamespace("deepslate_gold_ore"), false);
        
        // Ancient debris (locked)
        addBlockCard(ResourceLocation.withDefaultNamespace("ancient_debris"), false);
        
        // Other interesting blocks
        addBlockCard(ResourceLocation.withDefaultNamespace("spawner"), false);
        addBlockCard(ResourceLocation.withDefaultNamespace("chest"), true);
    }
    
    /**
     * Adds a block card to the menu.
     * 
     * @param blockId the block's resource location
     * @param isUnlocked whether the block is unlocked for tracking
     */
    private void addBlockCard(ResourceLocation blockId, boolean isUnlocked) {
        // Get the block's item form for the icon
        var blockOptional = BuiltInRegistries.BLOCK.get(blockId);
        if (blockOptional.isEmpty()) {
            AromaAffect.LOGGER.warn("Block not found: {}", blockId);
            return;
        }
        
        var block = blockOptional.get().value();
        ItemStack icon = new ItemStack(block.asItem());
        if (icon.isEmpty()) {
            // Some blocks don't have item forms, skip them
            return;
        }
        
        Component displayName = block.getName();
        Component description = Component.translatable("menu.aromaaffect.blocks.card.description", displayName);
        
        cards.add(new SelectionCard(blockId, displayName, icon, isUnlocked, description));
    }
    
    @Override
    protected void onCardSelected(SelectionCard card, int index) {
        selectedCardIndex = index;
        AromaAffect.LOGGER.info("Selected block for tracking: {}", card.id);
        
        // TODO: Implement tracking activation
        // This should:
        // 1. Set the player's active tracking target to this block
        // 2. Start the tracking overlay/compass
        // 3. Optionally close the menu or show confirmation
        
        // For now, just close the menu
        closeCurrentMenu();
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
