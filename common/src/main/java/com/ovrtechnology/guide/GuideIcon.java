package com.ovrtechnology.guide;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an icon that can be displayed in the guide.
 * Can be either an item stack or a texture resource location.
 */
public final class GuideIcon {

    @Nullable
    private final ItemStack itemStack;
    @Nullable
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;

    private GuideIcon(@Nullable ItemStack itemStack, @Nullable ResourceLocation texture,
                      int textureWidth, int textureHeight) {
        this.itemStack = itemStack;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public static GuideIcon ofItem(ItemStack stack) {
        return new GuideIcon(stack, null, 0, 0);
    }

    public static GuideIcon ofTexture(ResourceLocation texture, int width, int height) {
        return new GuideIcon(null, texture, width, height);
    }

    public boolean isItem() {
        return itemStack != null;
    }

    public boolean isTexture() {
        return texture != null;
    }

    @Nullable
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Nullable
    public ResourceLocation getTexture() {
        return texture;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }
}
