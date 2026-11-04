package com.ovrtechnology.mixin.legacy;

import com.ovrtechnology.nose.NoseItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.x noses carry an Equippable component with damageOnHurt; on 1.21.1 only ArmorItem is damaged by hits
@Mixin(LivingEntity.class)
public abstract class NoseHurtEquipmentMixin {

    @Inject(method = "doHurtEquipment", at = @At("TAIL"))
    private void aromaaffect$hurtNose(DamageSource source, float amount, EquipmentSlot[] slots, CallbackInfo ci) {
        if (amount <= 0.0F) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        int damage = (int) Math.max(1.0F, amount / 4.0F);
        for (EquipmentSlot slot : slots) {
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.getItem() instanceof NoseItem && stack.canBeHurtBy(source)) {
                stack.hurtAndBreak(damage, self, slot);
            }
        }
    }
}
