package com.ovrtechnology.tutorial.scentzone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects item pickups during the tutorial and triggers scents.
 * Currently: flowers trigger the "Floral" scent.
 */
public final class TutorialItemPickupHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // every 1 second
    private static final long COOLDOWN_MS = 10000; // 10 second cooldown between triggers

    /** Tracks the last flower count per player to detect new pickups. */
    private static final Map<UUID, Integer> lastFlowerCount = new HashMap<>();

    /** Tracks last trigger time per player to enforce cooldown. */
    private static final Map<UUID, Long> lastTriggerTime = new HashMap<>();

    private TutorialItemPickupHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;
                checkPlayer(player);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial item pickup handler initialized");
    }

    private static void checkPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        int currentFlowers = countFlowers(player);
        int previous = lastFlowerCount.getOrDefault(playerId, 0);

        lastFlowerCount.put(playerId, currentFlowers);

        // New flower picked up
        if (currentFlowers > previous && previous >= 0) {
            long now = System.currentTimeMillis();
            Long lastTime = lastTriggerTime.get(playerId);
            if (lastTime == null || now - lastTime >= COOLDOWN_MS) {
                lastTriggerTime.put(playerId, now);
                TutorialScentZoneNetworking.sendScentTrigger(player, "Floral", 1.0, "flower_pickup");
                AromaAffect.LOGGER.info("Player {} picked up a flower, triggering Floral scent", player.getName().getString());
            }
        }
    }

    private static int countFlowers(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BlockItem blockItem) {
                BlockState state = blockItem.getBlock().defaultBlockState();
                if (state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.FLOWERS)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    /**
     * Resets tracking for a player (on tutorial restart).
     */
    public static void resetPlayer(UUID playerId) {
        lastFlowerCount.remove(playerId);
        lastTriggerTime.remove(playerId);
    }

    /**
     * Resets all tracking.
     */
    public static void resetAll() {
        lastFlowerCount.clear();
        lastTriggerTime.clear();
    }
}
