package com.ovrtechnology.legacy.balm.fabric;

import com.ovrtechnology.legacy.balm.ServerTickHook;
import java.util.function.Consumer;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.minecraft.server.MinecraftServer;

public final class FabricServerTickHook implements ServerTickHook {

    @Override
    public void afterServerTick(Consumer<MinecraftServer> handler) {
        Balm.getEvents().onTickEvent(TickType.Server, TickPhase.End, handler::accept);
    }
}
