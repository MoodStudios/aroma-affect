package com.ovrtechnology.legacy.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// Mirrors the 26.x GuiGraphicsExtractor API on top of the 1.21.1 GuiGraphics so shared GUI code compiles unchanged
public class GuiGraphicsExtractor {

    private final GuiGraphics graphics;
    private final Pose2D pose;
    private final List<Runnable> deferred = new ArrayList<>();

    public GuiGraphicsExtractor(GuiGraphics graphics) {
        this.graphics = graphics;
        this.pose = new Pose2D(graphics.pose());
    }

    public GuiGraphics raw() {
        return graphics;
    }

    public int guiWidth() {
        return graphics.guiWidth();
    }

    public int guiHeight() {
        return graphics.guiHeight();
    }

    public Pose2D pose() {
        return pose;
    }

    public void nextStratum() {
        graphics.flush();
    }

    // 26.x applies the current pose to scissor rectangles; 1.21.1 takes them in screen space
    public void enableScissor(int x0, int y0, int x1, int y1) {
        Vector2f topLeft = pose.transformPosition(x0, y0, new Vector2f());
        Vector2f bottomRight = pose.transformPosition(x1, y1, new Vector2f());
        graphics.enableScissor(Mth.floor(topLeft.x), Mth.floor(topLeft.y), Mth.floor(bottomRight.x), Mth.floor(bottomRight.y));
    }

    public void disableScissor() {
        graphics.disableScissor();
    }

    public boolean containsPointInScissor(int x, int y) {
        return graphics.containsPointInScissor(x, y);
    }

    public void horizontalLine(int x0, int x1, int y, int col) {
        graphics.hLine(x0, x1, y, col);
    }

    public void verticalLine(int x, int y0, int y1, int col) {
        graphics.vLine(x, y0, y1, col);
    }

    public void fill(int x0, int y0, int x1, int y1, int col) {
        graphics.fill(x0, y0, x1, y1, col);
    }

    public void fill(RenderPipelines.Pipeline pipeline, int x0, int y0, int x1, int y1, int col) {
        graphics.fill(x0, y0, x1, y1, col);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int col1, int col2) {
        graphics.fillGradient(x0, y0, x1, y1, col1, col2);
    }

    public void outline(int x, int y, int width, int height, int color) {
        graphics.renderOutline(x, y, width, height, color);
    }

    public void text(Font font, @Nullable String str, int x, int y, int color) {
        text(font, str, x, y, color, true);
    }

    public void text(Font font, @Nullable String str, int x, int y, int color, boolean dropShadow) {
        if (isVisible(color)) {
            graphics.drawString(font, str, x, y, color, dropShadow);
        }
    }

    public void text(Font font, FormattedCharSequence str, int x, int y, int color) {
        text(font, str, x, y, color, true);
    }

    public void text(Font font, FormattedCharSequence str, int x, int y, int color, boolean dropShadow) {
        if (isVisible(color)) {
            graphics.drawString(font, str, x, y, color, dropShadow);
        }
    }

    public void text(Font font, Component str, int x, int y, int color) {
        text(font, str, x, y, color, true);
    }

    public void text(Font font, Component str, int x, int y, int color, boolean dropShadow) {
        if (isVisible(color)) {
            graphics.drawString(font, str, x, y, color, dropShadow);
        }
    }

    public void centeredText(Font font, String str, int x, int y, int color) {
        text(font, str, x - font.width(str) / 2, y, color);
    }

    public void centeredText(Font font, Component text, int x, int y, int color) {
        FormattedCharSequence seq = text.getVisualOrderText();
        text(font, seq, x - font.width(seq) / 2, y, color);
    }

    public void centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
        text(font, text, x - font.width(text) / 2, y, color);
    }

    public int textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col) {
        return textWithWordWrap(font, string, x, y, width, col, true);
    }

    public int textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col, boolean dropShadow) {
        for (FormattedCharSequence line : font.split(string, width)) {
            text(font, line, x, y, col, dropShadow);
            y += font.lineHeight;
        }
        return y;
    }

    public void blit(RenderPipelines.Pipeline pipeline, ResourceLocation texture, int x, int y, float u, float v,
                     int width, int height, int textureWidth, int textureHeight) {
        blit(pipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight, -1);
    }

    public void blit(RenderPipelines.Pipeline pipeline, ResourceLocation texture, int x, int y, float u, float v,
                     int width, int height, int textureWidth, int textureHeight, int color) {
        blit(pipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight, color);
    }

    public void blit(RenderPipelines.Pipeline pipeline, ResourceLocation texture, int x, int y, float u, float v,
                     int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        blit(pipeline, texture, x, y, u, v, width, height, srcWidth, srcHeight, textureWidth, textureHeight, -1);
    }

    public void blit(RenderPipelines.Pipeline pipeline, ResourceLocation texture, int x, int y, float u, float v,
                     int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color) {
        if (!isVisible(color)) {
            return;
        }
        begin(color);
        graphics.blit(texture, x, y, width, height, u, v, srcWidth, srcHeight, textureWidth, textureHeight);
        end(color);
    }

    public void blitSprite(RenderPipelines.Pipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
        blitSprite(pipeline, sprite, x, y, width, height, -1);
    }

    public void blitSprite(RenderPipelines.Pipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, float alpha) {
        blitSprite(pipeline, sprite, x, y, width, height, ((int) (alpha * 255.0F) << 24) | 0xFFFFFF);
    }

    public void blitSprite(RenderPipelines.Pipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        if (!isVisible(color)) {
            return;
        }
        begin(color);
        graphics.blitSprite(sprite, x, y, width, height);
        end(color);
    }

    public void item(ItemStack itemStack, int x, int y) {
        graphics.renderItem(itemStack, x, y);
        afterDepthWrite();
    }

    public void item(ItemStack itemStack, int x, int y, int seed) {
        graphics.renderItem(itemStack, x, y, seed);
        afterDepthWrite();
    }

    public void item(LivingEntity owner, ItemStack itemStack, int x, int y, int seed) {
        graphics.renderItem(owner, itemStack, x, y, seed);
        afterDepthWrite();
    }

    public void fakeItem(ItemStack itemStack, int x, int y) {
        graphics.renderFakeItem(itemStack, x, y);
        afterDepthWrite();
    }

    public void itemDecorations(Font font, ItemStack itemStack, int x, int y) {
        graphics.renderItemDecorations(font, itemStack, x, y);
        afterDepthWrite();
    }

    public void itemDecorations(Font font, ItemStack itemStack, int x, int y, @Nullable String countText) {
        graphics.renderItemDecorations(font, itemStack, x, y, countText);
        afterDepthWrite();
    }

    // 26.x composites GUI items in painter's order; on 1.21.1 their depth would hide whatever is drawn on top later
    public void afterDepthWrite() {
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    public void setTooltipForNextFrame(Component component, int x, int y) {
        setTooltipForNextFrame(Minecraft.getInstance().font, component, x, y);
    }

    public void setTooltipForNextFrame(List<FormattedCharSequence> lines, int x, int y) {
        Font font = Minecraft.getInstance().font;
        deferred.add(() -> graphics.renderTooltip(font, lines, x, y));
    }

    public void setTooltipForNextFrame(Font font, ItemStack itemStack, int x, int y) {
        deferred.add(() -> graphics.renderTooltip(font, itemStack, x, y));
    }

    public void setTooltipForNextFrame(Font font, Component text, int x, int y) {
        deferred.add(() -> graphics.renderTooltip(font, text, x, y));
    }

    public void setTooltipForNextFrame(Font font, List<Component> texts, Optional<TooltipComponent> image, int x, int y) {
        deferred.add(() -> graphics.renderTooltip(font, texts, image, x, y));
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
        deferred.add(() -> graphics.renderComponentTooltip(font, lines, x, y));
    }

    public void defer(Runnable action) {
        deferred.add(action);
    }

    // Draws deferred elements (tooltips) on top with an untransformed pose, like 26.x does at the end of a frame
    public void flush() {
        if (deferred.isEmpty()) {
            return;
        }
        List<Runnable> pending = new ArrayList<>(deferred);
        deferred.clear();
        graphics.pose().pushPose();
        graphics.pose().last().pose().identity();
        for (Runnable r : pending) {
            r.run();
        }
        graphics.pose().popPose();
    }

    private static boolean isVisible(int color) {
        return (color >>> 24) != 0;
    }

    private static void begin(int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (color != -1) {
            RenderSystem.setShaderColor(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    ((color >>> 24) & 0xFF) / 255.0F);
        }
    }

    private static void end(int color) {
        if (color != -1) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        RenderSystem.disableBlend();
    }
}
