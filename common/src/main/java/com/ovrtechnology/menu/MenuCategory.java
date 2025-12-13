package com.ovrtechnology.menu;

import com.ovrtechnology.lookup.LookupType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Categories available in the radial menu.
 * Each category corresponds to a type of target that can be tracked by the Nose.
 */
public enum MenuCategory {
    /**
     * Blocks category - for tracking specific blocks like ores.
     */
    BLOCKS("blocks", LookupType.BLOCK, Items.DIAMOND_ORE.getDefaultInstance(), 
           "menu.aromacraft.category.blocks", "menu.aromacraft.category.blocks.description"),
    
    /**
     * Biomes category - for tracking biome transitions.
     */
    BIOMES("biomes", LookupType.BIOME, Items.OAK_SAPLING.getDefaultInstance(),
           "menu.aromacraft.category.biomes", "menu.aromacraft.category.biomes.description"),
    
    /**
     * Structures category - for tracking structures like villages, strongholds, etc.
     */
    STRUCTURES("structures", LookupType.STRUCTURE, Items.BELL.getDefaultInstance(),
               "menu.aromacraft.category.structures", "menu.aromacraft.category.structures.description");
    
    private final String id;
    private final LookupType lookupType;
    private final ItemStack iconItem;
    private final String translationKey;
    private final String descriptionKey;
    
    MenuCategory(String id, LookupType lookupType, ItemStack iconItem, 
                 String translationKey, String descriptionKey) {
        this.id = id;
        this.lookupType = lookupType;
        this.iconItem = iconItem;
        this.translationKey = translationKey;
        this.descriptionKey = descriptionKey;
    }
    
    /**
     * Gets the unique identifier for this category.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the corresponding lookup type for this category.
     */
    public LookupType getLookupType() {
        return lookupType;
    }
    
    /**
     * Gets the item stack used as an icon for this category.
     */
    public ItemStack getIconItem() {
        return iconItem.copy();
    }
    
    /**
     * Gets the translation key for the category name.
     */
    public String getTranslationKey() {
        return translationKey;
    }
    
    /**
     * Gets the translation key for the category description.
     */
    public String getDescriptionKey() {
        return descriptionKey;
    }
    
    /**
     * Gets the translated display name component.
     */
    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }
    
    /**
     * Gets the translated description component.
     */
    public Component getDescription() {
        return Component.translatable(descriptionKey);
    }
    
    /**
     * Gets a category by its ID.
     * 
     * @param id the category ID
     * @return the category, or null if not found
     */
    public static MenuCategory fromId(String id) {
        for (MenuCategory category : values()) {
            if (category.id.equalsIgnoreCase(id)) {
                return category;
            }
        }
        return null;
    }
    
    /**
     * Gets a category from a lookup type.
     * 
     * @param type the lookup type
     * @return the category, or null if not found
     */
    public static MenuCategory fromLookupType(LookupType type) {
        for (MenuCategory category : values()) {
            if (category.lookupType == type) {
                return category;
            }
        }
        return null;
    }
}
