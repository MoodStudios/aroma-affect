package com.ovrtechnology.tutorial.scentzone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Triggers the associated scent when a player mines a block.
 * Server-side — uses a static block→scent map (not BlockRegistry which is client-only).
 * 10 second cooldown per player to avoid spam.
 */
public final class TutorialBlockMineHandler {

    private static boolean initialized = false;
    private static final long COOLDOWN_MS = 10000;
    private static final Map<UUID, Long> lastTriggerTime = new HashMap<>();

    /** Server-side block ID → OVR scent name mapping. */
    private static final Map<String, String> BLOCK_SCENT_MAP = Map.ofEntries(
            Map.entry("minecraft:iron_ore", "Terra Silva"),
            Map.entry("minecraft:deepslate_iron_ore", "Terra Silva"),
            Map.entry("minecraft:gold_ore", "Terra Silva"),
            Map.entry("minecraft:deepslate_gold_ore", "Terra Silva"),
            Map.entry("minecraft:coal_ore", "Terra Silva"),
            Map.entry("minecraft:deepslate_coal_ore", "Terra Silva"),
            Map.entry("minecraft:diamond_ore", "Terra Silva"),
            Map.entry("minecraft:deepslate_diamond_ore", "Terra Silva"),
            Map.entry("minecraft:emerald_ore", "Terra Silva"),
            Map.entry("minecraft:deepslate_emerald_ore", "Terra Silva"),
            Map.entry("minecraft:nether_gold_ore", "Smoky")
    );

    private TutorialBlockMineHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return EventResult.pass();
            if (!(level instanceof ServerLevel serverLevel)) return EventResult.pass();
            if (!TutorialModule.isActive(serverLevel)) return EventResult.pass();

            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            String scentName = BLOCK_SCENT_MAP.get(blockId);
            if (scentName == null) return EventResult.pass();

            UUID playerId = serverPlayer.getUUID();
            long now = System.currentTimeMillis();
            Long lastTime = lastTriggerTime.get(playerId);
            if (lastTime != null && now - lastTime < COOLDOWN_MS) return EventResult.pass();

            lastTriggerTime.put(playerId, now);
            TutorialScentZoneNetworking.sendScentTrigger(serverPlayer, scentName, 1.0, "block_mined");
            AromaAffect.LOGGER.info("Player {} mined {} -> scent '{}'",
                    serverPlayer.getName().getString(), blockId, scentName);

            return EventResult.pass();
        });

        AromaAffect.LOGGER.debug("Tutorial block mine handler initialized");
    }

    public static void resetPlayer(UUID playerId) {
        lastTriggerTime.remove(playerId);
    }

    public static void resetAll() {
        lastTriggerTime.clear();
    }
}
