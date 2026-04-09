package com.ovrtechnology.nose.client;

import com.ovrtechnology.nose.NoseItem;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Shows "Right Click to equip your Nose" when the player has a nose
 * in their inventory but not equipped on their head.
 */
public final class NoseEquipHint {

    private static boolean initialized = false;

    private NoseEquipHint() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || mc.options.hideGui) return;
        if (mc.screen != null) return;

        ItemStack head = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        int equippedTier = (head.getItem() instanceof NoseItem equippedNose) ? equippedNose.getDefinition().getTier() : 0;

        // Check if holding a nose in either hand
        NoseItem heldNose = null;
        if (mc.player.getMainHandItem().getItem() instanceof NoseItem n) heldNose = n;
        else if (mc.player.getOffhandItem().getItem() instanceof NoseItem n) heldNose = n;

        if (heldNose == null) return;

        // Only show if no nose equipped OR held nose is better tier
        if (equippedTier > 0 && heldNose.getDefinition().getTier() <= equippedTier) return;

        String hint = equippedTier == 0
                ? "Right Click to equip your Nose"
                : "Right Click to upgrade your Nose";
        int textWidth = mc.font.width(hint);
        int x = (graphics.guiWidth() - textWidth) / 2;
        int y = graphics.guiHeight() / 2 + 30;
        graphics.fill(x - 6, y - 4, x + textWidth + 6, y + 12, 0xAA000000);
        graphics.drawString(mc.font, hint, x, y, 0xFFFFFF00, true);
    }
}
