package com.ovrtechnology.guide;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
public final class GuideIcon {

    @Nullable private final ItemStack itemStack;
    @Nullable private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    @Nullable private final String symbol;
    private final int symbolColor;

    private GuideIcon(
            @Nullable ItemStack itemStack,
            @Nullable ResourceLocation texture,
            int textureWidth,
            int textureHeight,
            @Nullable String symbol,
            int symbolColor) {
        this.itemStack = itemStack;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.symbol = symbol;
        this.symbolColor = symbolColor;
    }

    public static GuideIcon ofItem(ItemStack stack) {
        return new GuideIcon(stack, null, 0, 0, null, 0);
    }

    public static GuideIcon ofTexture(ResourceLocation texture, int width, int height) {
        return new GuideIcon(null, texture, width, height, null, 0);
    }

    public static GuideIcon ofSymbol(String symbol, int color) {
        return new GuideIcon(null, null, 0, 0, symbol, color);
    }

    public boolean isItem() {
        return itemStack != null;
    }

    public boolean isTexture() {
        return texture != null;
    }

    public boolean isSymbol() {
        return symbol != null;
    }
}
