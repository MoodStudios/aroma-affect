package com.ovrtechnology.menu;

import com.ovrtechnology.lookup.LookupType;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Getter
public enum MenuCategory {
    BLOCKS(
            "blocks",
            LookupType.BLOCK,
            Items.DIAMOND_ORE.getDefaultInstance(),
            "menu.aromaaffect.category.blocks",
            "menu.aromaaffect.category.blocks.description",
            Ids.mod("textures/gui/sprites/radial/icon_blocks.png"),
            "block"),

    BIOMES(
            "biomes",
            LookupType.BIOME,
            Items.OAK_SAPLING.getDefaultInstance(),
            "menu.aromaaffect.category.biomes",
            "menu.aromaaffect.category.biomes.description",
            Ids.mod("textures/gui/sprites/radial/icon_biomes.png"),
            "biome"),

    STRUCTURES(
            "structures",
            LookupType.STRUCTURE,
            Items.BELL.getDefaultInstance(),
            "menu.aromaaffect.category.structures",
            "menu.aromaaffect.category.structures.description",
            Ids.mod("textures/gui/sprites/radial/icon_structures.png"),
            "structure"),

    FLOWERS(
            "flowers",
            LookupType.FLOWER,
            Items.POPPY.getDefaultInstance(),
            "menu.aromaaffect.category.flowers",
            "menu.aromaaffect.category.flowers.description",
            Ids.mod("textures/gui/sprites/radial/icon_flowers.png"),
            "block");

    private final String id;
    private final LookupType lookupType;

    @Getter(AccessLevel.NONE)
    private final ItemStack iconItem;

    private final String translationKey;
    private final String descriptionKey;
    private final ResourceLocation headerIcon;
    private final String pathCommandType;

    MenuCategory(
            String id,
            LookupType lookupType,
            ItemStack iconItem,
            String translationKey,
            String descriptionKey,
            ResourceLocation headerIcon,
            String pathCommandType) {
        this.id = id;
        this.lookupType = lookupType;
        this.iconItem = iconItem;
        this.translationKey = translationKey;
        this.descriptionKey = descriptionKey;
        this.headerIcon = headerIcon;
        this.pathCommandType = pathCommandType;
    }

    public ItemStack getIconItem() {
        return iconItem.copy();
    }

    public Component getDisplayName() {
        return Texts.tr(translationKey);
    }

    public Component getDescription() {
        return Texts.tr(descriptionKey);
    }

    public static MenuCategory fromId(String id) {
        for (MenuCategory category : values()) {
            if (category.id.equalsIgnoreCase(id)) {
                return category;
            }
        }
        return null;
    }
}
