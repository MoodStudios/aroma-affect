package com.ovrtechnology.entity.nosesmith.client;

import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Shows "Press Q to give the flower to the Nose Smith" when the player
 * holds the requested flower and is near the Nose Smith.
 */
public final class NoseSmithFlowerHint {

    private static boolean initialized = false;

    private NoseSmithFlowerHint() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || mc.options.hideGui) return;

        // Check if player is holding a flower in either hand
        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();

        // Find nearby NoseSmith
        List<NoseSmithEntity> nearby = mc.level.getEntitiesOfClass(
                NoseSmithEntity.class,
                mc.player.getBoundingBox().inflate(5.0)
        );

        if (nearby.isEmpty()) return;

        NoseSmithEntity smith = nearby.get(0);
        ResourceLocation requestedId = smith.getRequestedFlowerId();
        if (requestedId == null) return;

        // Check if either hand holds the requested flower
        boolean holdingFlower = isRequestedFlower(mainHand, requestedId)
                || isRequestedFlower(offHand, requestedId);

        if (!holdingFlower) return;

        // Render the hint
        String hint = "Press Q to give the flower to the Nose Smith";
        int textWidth = mc.font.width(hint);
        int x = (graphics.guiWidth() - textWidth) / 2;
        int y = graphics.guiHeight() / 2 + 30;

        // Background
        graphics.fill(x - 6, y - 4, x + textWidth + 6, y + 12, 0xAA000000);
        // Text
        graphics.drawString(mc.font, hint, x, y, 0xFFFFFF00, true);
    }

    private static boolean isRequestedFlower(ItemStack stack, ResourceLocation requestedId) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        return blockId.equals(requestedId);
    }
}
