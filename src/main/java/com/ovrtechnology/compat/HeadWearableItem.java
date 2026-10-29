package com.ovrtechnology.compat;

//? if <26 {
/*import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
*///?}

// Head-slot equipment: 26.x uses the Equippable component (ItemCompat#headEquippable), 1.21.1 the Equipable interface
public abstract class HeadWearableItem extends ModItem
        //? if <26 {
        /*implements net.minecraft.world.item.Equipable
        *///?}
{

    protected HeadWearableItem(Properties properties) {
        super(properties);
    }

    //? if <26 {
    /*@Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }
    *///?}
}
