package com.ovrtechnology.legacy.balm.callback;

import com.ovrtechnology.legacy.balm.LegacyEvent;
import com.ovrtechnology.legacy.balm.ServerTickHook;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ServerTickCallback {

    void handle(MinecraftServer server);

    LegacyEvent<ServerTickCallback> BEFORE = new LegacyEvent<>(h -> Balm.getEvents().onTickEvent(TickType.Server, TickPhase.Start, h::handle));
    LegacyEvent<ServerTickCallback> AFTER = new LegacyEvent<>(h -> ServerTickHook.INSTANCE.afterServerTick(h::handle));

    @FunctionalInterface
    interface ServerLevelTick {
        void handle(ServerLevel level);

        LegacyEvent<ServerLevelTick> BEFORE = new LegacyEvent<>(h -> Balm.getEvents().onTickEvent(TickType.ServerLevel, TickPhase.Start, level -> {
            if (level instanceof ServerLevel serverLevel) {
                h.handle(serverLevel);
            }
        }));
        LegacyEvent<ServerLevelTick> AFTER = new LegacyEvent<>(h -> Balm.getEvents().onTickEvent(TickType.ServerLevel, TickPhase.End, level -> {
            if (level instanceof ServerLevel serverLevel) {
                h.handle(serverLevel);
            }
        }));
    }

    @FunctionalInterface
    interface ServerPlayerTick {
        void handle(ServerPlayer player);

        LegacyEvent<ServerPlayerTick> BEFORE = new LegacyEvent<>(h -> Balm.getEvents().onTickEvent(TickType.ServerPlayer, TickPhase.Start, h::handle));
        LegacyEvent<ServerPlayerTick> AFTER = new LegacyEvent<>(h -> Balm.getEvents().onTickEvent(TickType.ServerPlayer, TickPhase.End, h::handle));
    }
}
