package com.ovrtechnology.guide;

import net.minecraft.ChatFormatting;
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
 *   GuideElement.craftingGrid(grid, result, "Recipe Name")
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
        TIP,
        CRAFTING_GRID,
        ABILITY_LINK,
        ICON_TEXT,
        URL_LINK
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
    @Nullable
    private final ItemStack[] craftingGrid;
    @Nullable
    private final ItemStack craftingResult;
    @Nullable
    private final String targetPageId;

    private GuideElement(Type type, @Nullable Component text, @Nullable ItemStack itemStack,
                         @Nullable ResourceLocation imageTexture, int imageWidth, int imageHeight,
                         int spacerHeight, int color,
                         @Nullable ItemStack[] craftingGrid, @Nullable ItemStack craftingResult,
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

    private GuideElement(Type type, @Nullable Component text, @Nullable ItemStack itemStack,
                         @Nullable ResourceLocation imageTexture, int imageWidth, int imageHeight,
                         int spacerHeight, int color) {
        this(type, text, itemStack, imageTexture, imageWidth, imageHeight, spacerHeight, color, null, null, null);
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

    /**
     * Creates a crafting grid element showing a 3x3 grid with a result.
     *
     * @param grid   9-element array (row-major, left-to-right top-to-bottom). Use {@link ItemStack#EMPTY} for empty slots.
     * @param result the output item
     * @param label  display label shown above the grid (e.g. "Aroma Guide")
     */
    public static GuideElement craftingGrid(ItemStack[] grid, ItemStack result, String label) {
        if (grid.length != 9) throw new IllegalArgumentException("Crafting grid must have exactly 9 slots");
        return new GuideElement(Type.CRAFTING_GRID, Component.literal(label), null, null,
                0, 0, 0, 0xFFFFFFFF, grid.clone(), result.copy(), null);
    }

    /**
     * Creates an ability link element that shows as "• AbilityName (inherited) →"
     * and navigates to the specified page when clicked.
     */
    public static GuideElement abilityLink(String abilityName, String targetPageId) {
        return new GuideElement(Type.ABILITY_LINK, Component.literal(abilityName), null, null,
                0, 0, 0, 0xFFD0D0D0, null, null, targetPageId);
    }

    /**
     * Creates a bold label for detection sub-categories (e.g. "Blocks", "Structures").
     * Renders with an accent-colored dash prefix to visually separate from items below.
     */
    public static GuideElement detectionLabel(String text) {
        return new GuideElement(Type.TEXT, Component.literal("\u25B8 " + text).withStyle(ChatFormatting.BOLD),
                null, null, 0, 0, 0, 0xFFAAAACC);
    }

    /**
     * Creates a compact element with a small item icon followed by text on the same line.
     * Used for detection lists (blocks, structures, biomes, flowers).
     */
    public static GuideElement iconText(ItemStack icon, String text) {
        return new GuideElement(Type.ICON_TEXT, Component.literal(text), icon.copy(), null,
                0, 0, 0, 0xFFD0D0D0);
    }

    /**
     * Creates a compact element with cycling item icons followed by text.
     * The icon animates through the provided items in sequence.
     */
    public static GuideElement iconText(String text, ItemStack... icons) {
        ItemStack[] copies = new ItemStack[icons.length];
        for (int i = 0; i < icons.length; i++) copies[i] = icons[i].copy();
        return new GuideElement(Type.ICON_TEXT, Component.literal(text), copies[0], null,
                0, 0, 0, 0xFFD0D0D0, copies, null, null);
    }

    /**
     * Creates a clickable URL link element that opens an external URL in the system browser.
     * Renders as underlined text with a link icon.
     *
     * @param label the display text for the link
     * @param url   the URL to open when clicked
     */
    public static GuideElement urlLink(String label, String url) {
        return new GuideElement(Type.URL_LINK, Component.literal(label), null, null,
                0, 0, 0, 0xFF6D9EF8, null, null, url);
    }

    /**
     * Creates a non-linked ability element that shows as "• AbilityName — description".
     */
    public static GuideElement ability(String text) {
        return new GuideElement(Type.TEXT, Component.literal("\u2022 " + text), null, null,
                0, 0, 0, 0xFFD0D0D0);
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

    @Nullable
    public ItemStack[] getCraftingGrid() {
        return craftingGrid;
    }

    @Nullable
    public ItemStack getCraftingResult() {
        return craftingResult;
    }

    @Nullable
    public String getTargetPageId() {
        return targetPageId;
    }
}
