package com.ovrtechnology.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class PlayerCompat {

    private PlayerCompat() {}

    public static void overlay(Player player, Component message) {
        //? if >=26 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }
}
