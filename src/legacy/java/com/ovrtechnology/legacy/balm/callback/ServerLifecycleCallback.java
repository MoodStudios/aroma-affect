package com.ovrtechnology.legacy.balm.callback;

import com.ovrtechnology.legacy.balm.LegacyEvent;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.server.MinecraftServer;

public interface ServerLifecycleCallback {

    @FunctionalInterface
    interface Starting {
        void handle(MinecraftServer server);

        LegacyEvent<Starting> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ServerStartingEvent.class, e -> h.handle(e.getServer())));
    }

    @FunctionalInterface
    interface Started {
        void handle(MinecraftServer server);

        LegacyEvent<Started> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ServerStartedEvent.class, e -> h.handle(e.getServer())));
    }

    // Balm 21 has no stopping event; stopped is the closest hook and every handler here only clears caches
    @FunctionalInterface
    interface Stopping {
        void handle(MinecraftServer server);

        LegacyEvent<Stopping> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ServerStoppedEvent.class, e -> h.handle(e.server())));
    }

    @FunctionalInterface
    interface Stopped {
        void handle(MinecraftServer server);

        LegacyEvent<Stopped> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ServerStoppedEvent.class, e -> h.handle(e.server())));
    }
}
