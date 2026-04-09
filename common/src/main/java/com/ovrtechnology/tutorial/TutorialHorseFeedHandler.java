package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * When a player right-clicks a horse with an apple, the horse eats it
 * and Barnyard scent is triggered. Horses cannot be mounted.
 */
public final class TutorialHorseFeedHandler {

    private static boolean initialized = false;
    private static final long COOLDOWN_MS = 5000;
    private static final Map<UUID, Long> lastTriggerTime = new HashMap<>();

    private TutorialHorseFeedHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return EventResult.pass();
            if (!(serverPlayer.level() instanceof ServerLevel level)) return EventResult.pass();
            if (!TutorialModule.isActive(level)) return EventResult.pass();
            if (!(entity instanceof Horse)) return EventResult.pass();

            var stack = player.getItemInHand(hand);

            // Apple → feed horse + Barnyard scent
            if (stack.is(Items.APPLE)) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        entity.getX(), entity.getY() + 1.0, entity.getZ(),
                        5, 0.3, 0.3, 0.3, 0.0);

                UUID playerId = serverPlayer.getUUID();
                long now = System.currentTimeMillis();
                Long lastTime = lastTriggerTime.get(playerId);
                if (lastTime == null || now - lastTime >= COOLDOWN_MS) {
                    lastTriggerTime.put(playerId, now);
                    TutorialScentZoneNetworking.sendScentTrigger(serverPlayer, "Barnyard", 1.0, "horse_feed");
                    AromaAffect.LOGGER.info("Player {} fed a horse an apple, triggering Barnyard scent",
                            serverPlayer.getName().getString());
                }

                return EventResult.interruptTrue();
            }

            // Any other interaction with horse → block mounting
            return EventResult.interruptFalse();
        });

        AromaAffect.LOGGER.debug("Tutorial horse feed handler initialized");
    }
}
