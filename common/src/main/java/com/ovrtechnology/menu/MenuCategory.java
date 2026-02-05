package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.lookup.LookupType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Categories available in the radial menu.
 * Each category corresponds to a type of target that can be tracked by the Nose.
 */
public enum MenuCategory {
    BLOCKS("blocks", LookupType.BLOCK, Items.DIAMOND_ORE.getDefaultInstance(),
           "menu.aromaaffect.category.blocks", "menu.aromaaffect.category.blocks.description",
           ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_blocks.png"),
           "block"),

    BIOMES("biomes", LookupType.BIOME, Items.OAK_SAPLING.getDefaultInstance(),
           "menu.aromaaffect.category.biomes", "menu.aromaaffect.category.biomes.description",
           ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_biomes.png"),
           "biome"),

    STRUCTURES("structures", LookupType.STRUCTURE, Items.BELL.getDefaultInstance(),
               "menu.aromaaffect.category.structures", "menu.aromaaffect.category.structures.description",
               ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_structures.png"),
               "structure"),

    FLOWERS("flowers", LookupType.FLOWER, Items.POPPY.getDefaultInstance(),
            "menu.aromaaffect.category.flowers", "menu.aromaaffect.category.flowers.description",
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_flowers.png"),
            "block");

    private final String id;
    private final LookupType lookupType;
    private final ItemStack iconItem;
    private final String translationKey;
    private final String descriptionKey;
    private final ResourceLocation headerIcon;
    private final String pathCommandType;

    MenuCategory(String id, LookupType lookupType, ItemStack iconItem,
                 String translationKey, String descriptionKey,
                 ResourceLocation headerIcon, String pathCommandType) {
        this.id = id;
        this.lookupType = lookupType;
        this.iconItem = iconItem;
        this.translationKey = translationKey;
        this.descriptionKey = descriptionKey;
        this.headerIcon = headerIcon;
        this.pathCommandType = pathCommandType;
    }

    public String getId() {
        return id;
    }

    public LookupType getLookupType() {
        return lookupType;
    }

    public ItemStack getIconItem() {
        return iconItem.copy();
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    /**
     * Gets the texture used as the header icon in selection menus.
     */
    public ResourceLocation getHeaderIcon() {
        return headerIcon;
    }

    /**
     * Gets the path command type string (e.g. "block", "biome", "structure").
     */
    public String getPathCommandType() {
        return pathCommandType;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    public Component getDescription() {
        return Component.translatable(descriptionKey);
    }

    public static MenuCategory fromId(String id) {
        for (MenuCategory category : values()) {
            if (category.id.equalsIgnoreCase(id)) {
                return category;
            }
        }
        return null;
    }

    public static MenuCategory fromLookupType(LookupType type) {
        for (MenuCategory category : values()) {
            if (category.lookupType == type) {
                return category;
            }
        }
        return null;
    }
}
