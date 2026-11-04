package com.ovrtechnology.legacy.balm;

import java.util.function.Consumer;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.server.MinecraftServer;

// Balm 21.0.66 registers its NeoForge end-of-server-tick listener on the abstract ServerTickEvent, which throws
public interface ServerTickHook {

    ServerTickHook INSTANCE = Balm.<ServerTickHook>platformProxy()
            .withFabric("com.ovrtechnology.legacy.balm.fabric.FabricServerTickHook")
            .withNeoForge("com.ovrtechnology.legacy.balm.neoforge.NeoForgeServerTickHook")
            .build();

    void afterServerTick(Consumer<MinecraftServer> handler);
}
