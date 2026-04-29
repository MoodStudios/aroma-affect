package com.ovrtechnology.neoforge.accessory;

import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.accessory.NoseAccessory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * Server-side guard that prevents a player from wearing a nose in both the vanilla HEAD slot and
 * the Curios "face" slot at the same time.
 *
 * <p>Policy: when a nose is equipped to HEAD and Curios is loaded, it is auto-routed to the face
 * slot. If the face slot already holds another nose, the new one is bounced to the inventory (or
 * dropped if the inventory is full).
 *
 * <p>Only registered when Curios is on the modlist. The reverse direction (drag into face while
 * HEAD already has one) is intentionally not covered here to avoid coupling to per-version Curios
 * event APIs.
 */
public final class NoseSlotEnforcer {

    private NoseSlotEnforcer() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(
                LivingEquipmentChangeEvent.class, NoseSlotEnforcer::onEquipChange);
    }

    private static void onEquipChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.HEAD) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack newHead = event.getTo();
        if (!(newHead.getItem() instanceof NoseItem)) return;

        // Pull it off HEAD before re-equipping so the next setItemSlot doesn't fight us.
        ItemStack moved = newHead.copy();
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        ItemStack previous = NoseAccessory.equip(player, moved);

        if (previous.isEmpty() || !(previous.getItem() instanceof NoseItem)) {
            // Face was empty (or had a non-nose, which the curios:tag validator
            // should have prevented). Promotion succeeded; return any displaced
            // non-nose item to the player.
            if (!previous.isEmpty()) {
                if (!player.getInventory().add(previous)) {
                    player.drop(previous, false);
                }
            }
            return;
        }

        // Face already held another nose. Restore it and bounce the new one.
        NoseAccessory.equip(player, previous);
        if (!player.getInventory().add(moved)) {
            player.drop(moved, false);
        }
    }
}
