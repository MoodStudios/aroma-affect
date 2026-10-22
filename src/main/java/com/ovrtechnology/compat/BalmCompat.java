package com.ovrtechnology.compat;

//? if >=26.3 {
import net.blay09.mods.balm.core.BalmHolderRegistration;
//?}
import net.minecraft.core.Holder;

public final class BalmCompat {

    private BalmCompat() {}

    //? if >=26.3 {
    public static <T> Holder<T> holder(BalmHolderRegistration<T> registration) {
        return registration.asHolder();
    }
    //?} else {
    /*public static <T> Holder<T> holder(Holder<T> holder) {
        return holder;
    }
    *///?}
}
