package com.ovrtechnology.legacy.balm.neoforge;

import com.ovrtechnology.legacy.balm.ServerTickHook;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class NeoForgeServerTickHook implements ServerTickHook {

    @Override
    public void afterServerTick(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> handler.accept(event.getServer()));
    }
}
