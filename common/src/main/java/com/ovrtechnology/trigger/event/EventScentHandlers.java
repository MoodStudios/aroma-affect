package com.ovrtechnology.trigger.event;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers all server-side detection for event/action scent hooks and routes
 * each detected event to {@link EventScentManager#fire}.
 *
 * <p>Ported from the PAX demo handlers (rain, water, mining, flower pickup,
 * item use, horse feed), minus the tutorial gating and minus any gameplay-
 * altering side effects (drops, mounting blocks, item consumption) — these are
 * pure scent hooks that leave vanilla behavior untouched. The scent / cooldown
 * / priority for each id is data-driven via {@code event_triggers.json}.</p>
 */
public final class EventScentHandlers {

    /** How often (in ticks) the polling checks run. */
    private static final int CHECK_INTERVAL = 10;

    /** Block id -> event id mapping for the mining hook. */
    private static final Map<String, String> MINE_EVENTS = Map.ofEntries(
            Map.entry("minecraft:iron_ore", "ore_mined"),
            Map.entry("minecraft:deepslate_iron_ore", "ore_mined"),
            Map.entry("minecraft:gold_ore", "ore_mined"),
            Map.entry("minecraft:deepslate_gold_ore", "ore_mined"),
            Map.entry("minecraft:coal_ore", "ore_mined"),
            Map.entry("minecraft:deepslate_coal_ore", "ore_mined"),
            Map.entry("minecraft:diamond_ore", "ore_mined"),
            Map.entry("minecraft:deepslate_diamond_ore", "ore_mined"),
            Map.entry("minecraft:emerald_ore", "ore_mined"),
            Map.entry("minecraft:deepslate_emerald_ore", "ore_mined"),
            Map.entry("minecraft:nether_gold_ore", "nether_gold_mined"));

    // Per-player polling state.
    private static final Map<UUID, Boolean> wasExposedToRain = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> rainFired = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> wasInWater = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> waterFired = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastFlowerCount = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastFlintDamage = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastCookedCount = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastRawMeatCount = new ConcurrentHashMap<>();

    private static int tickCounter = 0;
    private static boolean initialized = false;

    private EventScentHandlers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // ── Tick-based polling: rain, water, flower pickup, item use ──────────
        TickEvent.SERVER_POST.register(server -> {
            if (++tickCounter < CHECK_INTERVAL) {
                return;
            }
            tickCounter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) {
                    continue;
                }
                checkRain(player, level);
                checkWater(player);
                checkFlowerPickup(player);
                checkItemUse(player);
            }
        });

        // ── Block break: mining ores ──────────────────────────────────────────
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel) {
                String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                String eventId = MINE_EVENTS.get(blockId);
                if (eventId != null) {
                    EventScentManager.fire(serverPlayer, eventId);
                }
            }
            return EventResult.pass();
        });

        // ── Block place: planting seeds/crops ────────────────────────────────
        BlockEvent.PLACE.register((level, pos, state, placer) -> {
            if (placer instanceof ServerPlayer serverPlayer && level instanceof ServerLevel) {
                Block block = state.getBlock();
                if (block instanceof CropBlock || block instanceof StemBlock
                        || block instanceof NetherWartBlock) {
                    EventScentManager.fire(serverPlayer, "plant_seeds");
                }
            }
            return EventResult.pass();
        });

        // ── Interact with horse holding an apple: feed ───────────────────────
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.level() instanceof ServerLevel
                    && entity instanceof Horse
                    && player.getItemInHand(hand).is(Items.APPLE)) {
                EventScentManager.fire(serverPlayer, "horse_feed");
            }
            return EventResult.pass();
        });

        AromaAffect.LOGGER.info("EventScentHandlers initialized");
    }

    // ── Rain: fire once per exposure, reset on entering/leaving rain ──────────
    private static void checkRain(ServerPlayer player, ServerLevel level) {
        UUID id = player.getUUID();
        boolean exposed = level.isRaining() && level.canSeeSky(player.blockPosition());
        boolean prev = wasExposedToRain.getOrDefault(id, false);
        wasExposedToRain.put(id, exposed);

        if (exposed != prev) {
            rainFired.put(id, false);
        }
        if (exposed && !rainFired.getOrDefault(id, false)) {
            rainFired.put(id, true);
            EventScentManager.fire(player, "rain_exposure");
        }
    }

    // ── Water: fire once per entry, reset on leaving ─────────────────────────
    private static void checkWater(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean inWater = player.isInWater();
        boolean prev = wasInWater.getOrDefault(id, false);
        wasInWater.put(id, inWater);

        if (inWater != prev) {
            waterFired.put(id, false);
        }
        if (inWater && !waterFired.getOrDefault(id, false)) {
            waterFired.put(id, true);
            EventScentManager.fire(player, "water_touch");
        }
    }

    // ── Flower pickup: fire when flower count increases ──────────────────────
    private static void checkFlowerPickup(ServerPlayer player) {
        UUID id = player.getUUID();
        int current = countFlowers(player);
        int prev = lastFlowerCount.getOrDefault(id, current);
        lastFlowerCount.put(id, current);
        if (current > prev) {
            EventScentManager.fire(player, "flower_pickup");
        }
    }

    // ── Item use: flint use, cooking (eating is handled by PlayerEatMixin) ────
    private static void checkItemUse(ServerPlayer player) {
        UUID id = player.getUUID();

        // Flint and steel used (durability damage increased) -> Smoky
        int flintDmg = getFlintDamage(player);
        int prevFlintDmg = lastFlintDamage.getOrDefault(id, flintDmg);
        lastFlintDamage.put(id, flintDmg);
        if (flintDmg >= 0 && prevFlintDmg >= 0 && flintDmg > prevFlintDmg) {
            EventScentManager.fire(player, "flint_use");
        }

        // Cooked food appeared -> Savory Spice
        int cooked = countCookedFood(player);
        int prevCooked = lastCookedCount.getOrDefault(id, cooked);
        lastCookedCount.put(id, cooked);
        if (cooked > prevCooked) {
            EventScentManager.fire(player, "cooking");
        }

        // Raw meat put into a furnace -> Smoky
        int raw = countRawMeat(player);
        int prevRaw = lastRawMeatCount.getOrDefault(id, raw);
        lastRawMeatCount.put(id, raw);
        if (raw < prevRaw && player.containerMenu instanceof FurnaceMenu) {
            EventScentManager.fire(player, "cooking_start");
        }
    }

    // ── Inventory helpers ────────────────────────────────────────────────────

    private static int countFlowers(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof BlockItem blockItem) {
                BlockState state = blockItem.getBlock().defaultBlockState();
                if (state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.FLOWERS)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
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
        return -1;
    }
}
