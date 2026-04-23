package com.ovrtechnology.guide;

import com.ovrtechnology.util.Colors;
import com.ovrtechnology.util.Texts;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
public final class GuideElement {

    public enum Type {
        HEADER,
        SUBHEADER,
        TEXT,
        ITEM_SHOWCASE,
        IMAGE,
        SEPARATOR,
        SPACER,
        TIP,
        CRAFTING_GRID,
        ABILITY_LINK,
        ICON_TEXT,
        URL_LINK
    }

    private final Type type;
    @Nullable private final Component text;
    @Nullable private final ItemStack itemStack;
    @Nullable private final ResourceLocation imageTexture;
    private final int imageWidth;
    private final int imageHeight;
    private final int spacerHeight;
    private final int color;
    @Nullable private final ItemStack[] craftingGrid;
    @Nullable private final ItemStack craftingResult;
    @Nullable private final String targetPageId;

    private GuideElement(
            Type type,
            @Nullable Component text,
            @Nullable ItemStack itemStack,
            @Nullable ResourceLocation imageTexture,
            int imageWidth,
            int imageHeight,
            int spacerHeight,
            int color,
            @Nullable ItemStack[] craftingGrid,
            @Nullable ItemStack craftingResult,
            @Nullable String targetPageId) {
        this.type = type;
        this.text = text;
        this.itemStack = itemStack;
        this.imageTexture = imageTexture;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.spacerHeight = spacerHeight;
        this.color = color;
        this.craftingGrid = craftingGrid;
        this.craftingResult = craftingResult;
        this.targetPageId = targetPageId;
    }

    private GuideElement(
            Type type,
            @Nullable Component text,
            @Nullable ItemStack itemStack,
            @Nullable ResourceLocation imageTexture,
            int imageWidth,
            int imageHeight,
            int spacerHeight,
            int color) {
        this(
                type,
                text,
                itemStack,
                imageTexture,
                imageWidth,
                imageHeight,
                spacerHeight,
                color,
                null,
                null,
                null);
    }

    public static GuideElement header(Component text) {
        return new GuideElement(Type.HEADER, text, null, null, 0, 0, 0, Colors.WHITE);
    }

    public static GuideElement subheader(Component text) {
        return new GuideElement(Type.SUBHEADER, text, null, null, 0, 0, 0, Colors.TEXT_SECONDARY);
    }

    public static GuideElement text(Component text) {
        return new GuideElement(Type.TEXT, text, null, null, 0, 0, 0, Colors.TEXT_PRIMARY);
    }

    public static GuideElement coloredText(Component text, int color) {
        return new GuideElement(Type.TEXT, text, null, null, 0, 0, 0, color);
    }

    public static GuideElement itemShowcase(ItemStack stack, Component description) {
        return new GuideElement(
                Type.ITEM_SHOWCASE, description, stack, null, 0, 0, 0, Colors.TEXT_PRIMARY);
    }

    public static GuideElement image(ResourceLocation texture, int width, int height) {
        return new GuideElement(Type.IMAGE, null, null, texture, width, height, 0, Colors.WHITE);
    }

    public static GuideElement separator() {
        return new GuideElement(Type.SEPARATOR, null, null, null, 0, 0, 0, Colors.SEPARATOR);
    }

    public static GuideElement spacer(int height) {
        return new GuideElement(Type.SPACER, null, null, null, 0, 0, height, 0);
    }

    public static GuideElement tip(Component text) {
        return new GuideElement(Type.TIP, text, null, null, 0, 0, 0, Colors.ACCENT_GREEN);
    }

    public static GuideElement abilityLink(Component abilityName, String targetPageId) {
        return new GuideElement(
                Type.ABILITY_LINK,
                abilityName,
                null,
                null,
                0,
                0,
                0,
                Colors.TEXT_PRIMARY,
                null,
                null,
                targetPageId);
    }

    public static GuideElement detectionLabel(Component text) {
        return new GuideElement(
                Type.TEXT,
                Texts.lit("\u25B8 ").withStyle(ChatFormatting.BOLD).append(text),
                null,
                null,
                0,
                0,
                0,
                Colors.TEXT_LABEL);
    }

    public static GuideElement iconText(ItemStack icon, Component text) {
        return new GuideElement(
                Type.ICON_TEXT, text, icon.copy(), null, 0, 0, 0, Colors.TEXT_PRIMARY);
    }

    public static GuideElement iconText(Component text, ItemStack... icons) {
        ItemStack[] copies = new ItemStack[icons.length];
        for (int i = 0; i < icons.length; i++) copies[i] = icons[i].copy();
        return new GuideElement(
                Type.ICON_TEXT,
                text,
                copies[0],
                null,
                0,
                0,
                0,
                Colors.TEXT_PRIMARY,
                copies,
                null,
                null);
    }

    public static GuideElement urlLink(Component label, String url) {
        return new GuideElement(
                Type.URL_LINK, label, null, null, 0, 0, 0, Colors.ACCENT_BLUE, null, null, url);
    }

    public static GuideElement ability(String text) {
        return new GuideElement(
                Type.TEXT, Texts.lit("\u2022 " + text), null, null, 0, 0, 0, Colors.TEXT_PRIMARY);
    }

    public static GuideElement ability(Component text) {
        return new GuideElement(
                Type.TEXT,
                Texts.lit("\u2022 ").append(text),
                null,
                null,
                0,
                0,
                0,
                Colors.TEXT_PRIMARY);
    }

    public static GuideElement craftingGrid(ItemStack[] grid, ItemStack result, Component label) {
        if (grid.length != 9)
            throw new IllegalArgumentException("Crafting grid must have exactly 9 slots");
        return new GuideElement(
                Type.CRAFTING_GRID,
                label,
                null,
                null,
                0,
                0,
                0,
                Colors.WHITE,
                grid.clone(),
                result.copy(),
                null);
    }
}
