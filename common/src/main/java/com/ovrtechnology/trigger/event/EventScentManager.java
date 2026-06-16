package com.ovrtechnology.trigger.event;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.EventScentNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side entry point for firing event/action scent hooks.
 *
 * <p>Handlers in {@link EventScentHandlers} detect a gameplay event and call
 * {@link #fire(ServerPlayer, String)} with the event id. This looks up the
 * data-driven {@link EventTriggerDefinition}, enforces a per-player per-event
 * cooldown to avoid packet spam, and sends the trigger to the client. The
 * client then gates on passive mode and applies the {@link
 * com.ovrtechnology.trigger.ScentTriggerManager} cooldown/priority.</p>
 */
public final class EventScentManager {

    /** (playerUUID, eventId) -> last fire timestamp (ms). */
    private static final Map<UUID, Map<String, Long>> lastFire = new ConcurrentHashMap<>();

    /**
     * Foods classified as "sweet"; all other edibles default to "savory".
     * Held in this regular class (not the mixin) so it initializes lazily at
     * runtime, after the item registry is populated.
     */
    private static final Set<Item> SWEET_FOODS = Set.of(
            Items.COOKIE, Items.PUMPKIN_PIE, Items.MELON_SLICE, Items.SWEET_BERRIES,
            Items.GLOW_BERRIES, Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.HONEY_BOTTLE, Items.CHORUS_FRUIT);

    private EventScentManager() {
    }

    /**
     * Classifies a just-eaten food stack and fires the matching eat hook.
     * Called from {@link com.ovrtechnology.mixin.PlayerEatMixin}.
     */
    public static void fireEat(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || !stack.has(DataComponents.FOOD)) {
            return;
        }
        fire(player, SWEET_FOODS.contains(stack.getItem()) ? "eat_sweet" : "eat_savory");
    }

    /**
     * Attempts to fire the event hook with the given id for the player.
     *
     * @param player  the player who triggered the event
     * @param eventId the event id (must match an entry in event_triggers.json)
     * @return true if a trigger packet was sent
     */
    public static boolean fire(ServerPlayer player, String eventId) {
        if (player == null) {
            return false;
        }

        EventTriggerDefinition def = EventTriggerConfigLoader.get(eventId).orElse(null);
        if (def == null || !def.isEnabled()) {
            return false;
        }

        long now = System.currentTimeMillis();
        Map<String, Long> perEvent = lastFire.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        Long last = perEvent.get(eventId);
        if (last != null && now - last < def.getCooldownMs()) {
            return false;
        }
        perEvent.put(eventId, now);

        EventScentNetworking.sendEventScent(
                player, def.getScent(), def.getIntensity(), def.getPriority(),
                def.getDurationTicks(), eventId);

        AromaAffect.LOGGER.debug("Fired event scent '{}' -> {} for {}",
                eventId, def.getScent(), player.getName().getString());
        return true;
    }

    /** Clears cooldown state for a player (e.g. on disconnect). */
    public static void reset(UUID playerId) {
        lastFire.remove(playerId);
    }
}
