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
     * Structures category - for tracking structures like villages, strongholds, etc.
     * Icon: Village/Bell representing civilization and structures.
     */
    STRUCTURES("structures", LookupType.STRUCTURE, Items.BELL.getDefaultInstance(),
               "menu.aromacraft.category.structures", "menu.aromacraft.category.structures.description"),
    
    /**
     * Entities category - for tracking mobs and NPCs.
     * Icon: Villager spawn egg representing living entities.
     */
    ENTITIES("entities", null, Items.VILLAGER_SPAWN_EGG.getDefaultInstance(),
             "menu.aromacraft.category.entities", "menu.aromacraft.category.entities.description"),
    
    /**
     * Flora category - for tracking flowers, plants, and vegetation.
     * Icon: Poppy representing plant life.
     */
    FLORA("flora", null, Items.POPPY.getDefaultInstance(),
          "menu.aromacraft.category.flora", "menu.aromacraft.category.flora.description"),
    
    /**
     * Blocks category - for tracking specific blocks like ores.
     * Icon: Filled map representing exploration and discovery.
     */
    BLOCKS("blocks", LookupType.BLOCK, Items.FILLED_MAP.getDefaultInstance(), 
           "menu.aromacraft.category.blocks", "menu.aromacraft.category.blocks.description"),
    
    /**
     * Biomes category - for tracking biome transitions.
     * Icon: Grass block representing terrain and biomes.
     */
    BIOMES("biomes", LookupType.BIOME, Items.GRASS_BLOCK.getDefaultInstance(),
           "menu.aromacraft.category.biomes", "menu.aromacraft.category.biomes.description");
    
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
     * May return null for categories not yet implemented.
     */
    public LookupType getLookupType() {
        return lookupType;
    }
    
    /**
     * Checks if this category has a valid lookup type implementation.
     * Categories without implementations show as "coming soon" in the UI.
     */
    public boolean isImplemented() {
        return lookupType != null;
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
