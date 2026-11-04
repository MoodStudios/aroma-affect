package com.ovrtechnology.legacy.balm.client.callback;

import com.ovrtechnology.legacy.balm.LegacyEvent;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface ClientTickCallback {

    void handle(Minecraft client);

    LegacyEvent<ClientTickCallback> BEFORE = new LegacyEvent<>(h -> Balm.getEvents().onTickEvent(TickType.Client, TickPhase.Start, h::handle));
    LegacyEvent<ClientTickCallback> AFTER = new LegacyEvent<>(h -> Balm.getEvents().onTickEvent(TickType.Client, TickPhase.End, h::handle));
}
