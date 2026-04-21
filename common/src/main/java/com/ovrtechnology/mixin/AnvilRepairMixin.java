package com.ovrtechnology.mixin;

import com.ovrtechnology.nose.NoseTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to change nose repair amount from vanilla's 1/4 (25%) to 1/3 (33%) per material.
 * This applies only to NoseItem items; all other items use vanilla behavior.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilRepairMixin extends ItemCombinerMenu {

    // Dummy constructor required by Java (never called — this is a mixin)
    protected AnvilRepairMixin(MenuType<?> menuType, int containerId, Inventory playerInventory,
                               ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slotDef) {
        super(menuType, containerId, playerInventory, access, slotDef);
    }

    @Shadow private int repairItemCountCost;
    @Shadow @Final private DataSlot cost;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void aromaaffect$adjustNoseRepair(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);
        ItemStack result = this.resultSlots.getItem(0);

        // Only adjust for NoseItem material repair
        if (result.isEmpty() || !left.is(NoseTags.NOSES)) return;
        if (!left.isDamageableItem() || !left.isValidRepairItem(right)) return;
        if (left.getDamageValue() <= 0) return;

        // Recalculate repair with 1/3 per material instead of vanilla's 1/4
        int repairPerUnit = Math.max(left.getMaxDamage() / 3, 1);
        int oldMaterialCount = this.repairItemCountCost;

        int damage = left.getDamageValue();
        int materialsUsed = 0;

        for (int m = 0; damage > 0 && m < right.getCount(); m++) {
            damage = Math.max(0, damage - repairPerUnit);
            materialsUsed++;
        }

        result.setDamageValue(damage);
        this.repairItemCountCost = materialsUsed;

        // Adjust XP cost: vanilla added 1 per material, correct for difference
        int costDiff = materialsUsed - oldMaterialCount;
        if (costDiff != 0) {
            this.cost.set(Math.max(1, this.cost.get() + costDiff));
        }
    }
}
