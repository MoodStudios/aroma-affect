package com.ovrtechnology.ability;

import net.minecraft.server.level.ServerPlayer;

public interface Ability {

    String getId();

    boolean canUse(ServerPlayer player);
}
