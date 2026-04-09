package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * During the tutorial, players take visual damage (hit animation, sound, knockback)
 * but their health is restored after a short delay so they never die.
 * The delay allows the damage animation to play visually.
 */
public final class TutorialDamageHandler {

    private static boolean initialized = false;

    /** Delay in ticks before healing after taking damage (10 ticks = 0.5s). */
    private static final int HEAL_DELAY_TICKS = 10;

    /** Tracks when each player last took damage. */
    private static final Map<UUID, Float> lastKnownHealth = new HashMap<>();
    private static final Map<UUID, Integer> healCooldown = new HashMap<>();

    private TutorialDamageHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Protect dropped items from fire/lava — mark invulnerable on spawn
        EntityEvent.ADD.register((entity, level) -> {
            if (entity instanceof ItemEntity && level instanceof ServerLevel sl && TutorialModule.isActive(sl)) {
                entity.setInvulnerable(true);
            }
            return EventResult.pass();
        });

        TickEvent.SERVER_POST.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;

                // Remove resistance effect if present (let damage visuals through)
                if (player.hasEffect(MobEffects.RESISTANCE)) {
                    player.removeEffect(MobEffects.RESISTANCE);
                }

                // Disable invulnerable flags (may be inherited from level.dat)
                if (player.isInvulnerable()) {
                    player.setInvulnerable(false);
                }
                if (player.getAbilities().invulnerable) {
                    player.getAbilities().invulnerable = false;
                    player.onUpdateAbilities();
                }

                UUID playerId = player.getUUID();
                float currentHealth = player.getHealth();
                float maxHealth = player.getMaxHealth();
                float previousHealth = lastKnownHealth.getOrDefault(playerId, maxHealth);

                // Detect damage taken
                if (currentHealth < previousHealth) {
                    // Player just took damage — start heal cooldown
                    healCooldown.put(playerId, HEAL_DELAY_TICKS);
                    AromaAffect.LOGGER.info("[DamageDebug] DAMAGE DETECTED! health went from {} to {}", previousHealth, currentHealth);
                }

                lastKnownHealth.put(playerId, currentHealth);

                // Prevent death — if critically low, heal immediately
                if (currentHealth <= 2.0f) {
                    player.setHealth(maxHealth);
                    lastKnownHealth.put(playerId, maxHealth);
                    healCooldown.remove(playerId);
                } else {
                    // Heal after delay
                    Integer cooldown = healCooldown.get(playerId);
                    if (cooldown != null) {
                        if (cooldown <= 0) {
                            player.setHealth(maxHealth);
                            lastKnownHealth.put(playerId, maxHealth);
                            healCooldown.remove(playerId);
                        } else {
                            healCooldown.put(playerId, cooldown - 1);
                        }
                    } else if (currentHealth < maxHealth) {
                        // No recent damage but health is low — heal (e.g. wither effect)
                        healCooldown.put(playerId, HEAL_DELAY_TICKS);
                    }
                }

                // Keep food full
                if (player.getFoodData().getFoodLevel() < 20) {
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(5.0f);
                }
            }
        });

        AromaAffect.LOGGER.debug("Tutorial damage handler initialized");
    }
}
