package com.ovrtechnology.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A single renderable element within a guide page.
 * Elements are stacked vertically within a page and support various content types.
 *
 * <p>Use the static factory methods to create elements:</p>
 * <pre>
 *   GuideElement.header("Getting Started")
 *   GuideElement.text("Welcome to AromaCraft!")
 *   GuideElement.item(new ItemStack(Items.DIAMOND), "Diamond Nose")
 *   GuideElement.image(ResourceLocation.of("aromacraft:textures/gui/guide/banner.png"), 200, 60)
 *   GuideElement.separator()
 *   GuideElement.spacer(8)
 * </pre>
 */
public final class GuideElement {

    public enum Type {
        HEADER,
        SUBHEADER,
        TEXT,
        ITEM_SHOWCASE,
        IMAGE,
        SEPARATOR,
        SPACER,
        TIP
    }

    private final Type type;
    @Nullable
    private final Component text;
    @Nullable
    private final ItemStack itemStack;
    @Nullable
    private final ResourceLocation imageTexture;
    private final int imageWidth;
    private final int imageHeight;
    private final int spacerHeight;
    private final int color;

    private GuideElement(Type type, @Nullable Component text, @Nullable ItemStack itemStack,
                         @Nullable ResourceLocation imageTexture, int imageWidth, int imageHeight,
                         int spacerHeight, int color) {
        this.type = type;
        this.text = text;
        this.itemStack = itemStack;
        this.imageTexture = imageTexture;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.spacerHeight = spacerHeight;
        this.color = color;
    }

    public static GuideElement header(String text) {
        return header(Component.literal(text));
    }

    public static GuideElement header(Component text) {
        return new GuideElement(Type.HEADER, text, null, null, 0, 0, 0, 0xFFFFFFFF);
    }

    public static GuideElement subheader(String text) {
        return subheader(Component.literal(text));
    }

    public static GuideElement subheader(Component text) {
        return new GuideElement(Type.SUBHEADER, text, null, null, 0, 0, 0, 0xFFCCCCCC);
    }

    public static GuideElement text(String text) {
        return text(Component.literal(text));
    }

    public static GuideElement text(Component text) {
        return new GuideElement(Type.TEXT, text, null, null, 0, 0, 0, 0xFFD0D0D0);
    }

    public static GuideElement coloredText(String text, int color) {
        return new GuideElement(Type.TEXT, Component.literal(text), null, null, 0, 0, 0, color);
    }

    public static GuideElement itemShowcase(ItemStack stack, Component description) {
        return new GuideElement(Type.ITEM_SHOWCASE, description, stack, null, 0, 0, 0, 0xFFD0D0D0);
    }

    public static GuideElement image(ResourceLocation texture, int width, int height) {
        return new GuideElement(Type.IMAGE, null, null, texture, width, height, 0, 0xFFFFFFFF);
    }

    public static GuideElement separator() {
        return new GuideElement(Type.SEPARATOR, null, null, null, 0, 0, 0, 0xFF444466);
    }

    public static GuideElement spacer(int height) {
        return new GuideElement(Type.SPACER, null, null, null, 0, 0, height, 0);
    }

    public static GuideElement tip(String text) {
        return tip(Component.literal(text));
    }

    public static GuideElement tip(Component text) {
        return new GuideElement(Type.TIP, text, null, null, 0, 0, 0, 0xFF7BD48A);
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public Component getText() {
        return text;
    }

    @Nullable
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Nullable
    public ResourceLocation getImageTexture() {
        return imageTexture;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getSpacerHeight() {
        return spacerHeight;
    }

    public int getColor() {
        return color;
    }
}
