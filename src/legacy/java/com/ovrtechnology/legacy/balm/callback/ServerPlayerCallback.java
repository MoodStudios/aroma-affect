package com.ovrtechnology.legacy.balm.callback;

import com.ovrtechnology.legacy.balm.LegacyEvent;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.event.PlayerLogoutEvent;
import net.minecraft.server.level.ServerPlayer;

public interface ServerPlayerCallback {

    @FunctionalInterface
    interface Join {
        void handle(ServerPlayer player);

        LegacyEvent<Join> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(PlayerLoginEvent.class, e -> h.handle(e.getPlayer())));
    }

    @FunctionalInterface
    interface Leave {
        void handle(ServerPlayer player);

        LegacyEvent<Leave> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(PlayerLogoutEvent.class, e -> h.handle(e.getPlayer())));
    }
}
