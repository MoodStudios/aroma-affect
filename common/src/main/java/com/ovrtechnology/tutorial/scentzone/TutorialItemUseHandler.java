package com.ovrtechnology.tutorial.scentzone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects when a player consumes/uses specific items and triggers scents.
 * - Honey Bottle → Sweet
 * - Apple (near horse) → Barnyard
 */
public final class TutorialItemUseHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;
    private static final long COOLDOWN_MS = 5000;

    private static final Map<UUID, Integer> lastHoneyCount = new HashMap<>();
    private static final Map<UUID, Integer> lastFlintDurability = new HashMap<>();
    private static final Map<UUID, Integer> lastCookedCount = new HashMap<>();
    private static final Map<UUID, Integer> lastRawMeatCount = new HashMap<>();
    private static final Map<UUID, Long> lastHoneyTrigger = new HashMap<>();
    private static final Map<UUID, Long> lastFlintTrigger = new HashMap<>();
    private static final Map<UUID, Long> lastCookedTrigger = new HashMap<>();
    private static final Map<UUID, Long> lastRawMeatTrigger = new HashMap<>();

    private TutorialItemUseHandler() {}

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
                checkPlayer(player, level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial item use handler initialized");
    }

    private static void checkPlayer(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();

        // --- Honey Bottle → Sweet ---
        int currentHoney = countItem(player, Items.HONEY_BOTTLE);
        int prevHoney = lastHoneyCount.getOrDefault(playerId, -1);
        lastHoneyCount.put(playerId, currentHoney);

        if (prevHoney > 0 && currentHoney < prevHoney) {
            triggerWithCooldown(player, playerId, lastHoneyTrigger, "Sweet", "honey_drink");
        }

        // --- Flint and Steel use → Smoky ---
        int currentFlintDmg = getFlintDamage(player);
        int prevFlintDmg = lastFlintDurability.getOrDefault(playerId, -1);
        lastFlintDurability.put(playerId, currentFlintDmg);

        if (prevFlintDmg >= 0 && currentFlintDmg > prevFlintDmg) {
            // Durability damage increased = was used
            triggerWithCooldown(player, playerId, lastFlintTrigger, "Smoky", "flint_use");
        }

        // --- Cooked food appears in inventory → Savory Spice ---
        int currentCooked = countCookedFood(player);
        int prevCooked = lastCookedCount.getOrDefault(playerId, -1);
        lastCookedCount.put(playerId, currentCooked);

        if (prevCooked >= 0 && currentCooked > prevCooked) {
            triggerWithCooldown(player, playerId, lastCookedTrigger, "Savory Spice", "cooking");
        }

        // --- Raw meat count decreases (put in furnace) → Smoky ---
        int currentRaw = countRawMeat(player);
        int prevRaw = lastRawMeatCount.getOrDefault(playerId, -1);
        lastRawMeatCount.put(playerId, currentRaw);

        if (prevRaw > 0 && currentRaw < prevRaw
                && player.containerMenu instanceof net.minecraft.world.inventory.FurnaceMenu) {
            triggerWithCooldown(player, playerId, lastRawMeatTrigger, "Smoky", "cooking_start");
        }
    }

    private static int countCookedFood(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP)
                    || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.COOKED_MUTTON)
                    || stack.is(Items.COOKED_RABBIT) || stack.is(Items.COOKED_COD)
                    || stack.is(Items.COOKED_SALMON)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countRawMeat(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.BEEF) || stack.is(Items.PORKCHOP)
                    || stack.is(Items.CHICKEN) || stack.is(Items.MUTTON)
                    || stack.is(Items.RABBIT) || stack.is(Items.COD)
                    || stack.is(Items.SALMON)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int getFlintDamage(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.FLINT_AND_STEEL)) {
                return stack.getDamageValue();
            }
        }
        return -1; // not in inventory
    }

    private static void triggerWithCooldown(ServerPlayer player, UUID playerId,
                                             Map<UUID, Long> cooldownMap,
                                             String scentName, String sourceId) {
        long now = System.currentTimeMillis();
        Long lastTime = cooldownMap.get(playerId);
        if (lastTime != null && now - lastTime < COOLDOWN_MS) return;

        cooldownMap.put(playerId, now);
        TutorialScentZoneNetworking.sendScentTrigger(player, scentName, 1.0, sourceId);
        AromaAffect.LOGGER.info("Player {} triggered {} scent ({})",
                player.getName().getString(), scentName, sourceId);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static void resetPlayer(UUID playerId) {
        lastHoneyCount.remove(playerId);
        lastFlintDurability.remove(playerId);
        lastCookedCount.remove(playerId);
        lastRawMeatCount.remove(playerId);
        lastHoneyTrigger.remove(playerId);
        lastFlintTrigger.remove(playerId);
        lastCookedTrigger.remove(playerId);
        lastRawMeatTrigger.remove(playerId);
    }

    public static void resetAll() {
        lastHoneyCount.clear();
        lastFlintDurability.clear();
        lastCookedCount.clear();
        lastRawMeatCount.clear();
        lastHoneyTrigger.clear();
        lastFlintTrigger.clear();
        lastCookedTrigger.clear();
        lastRawMeatTrigger.clear();
    }
}
