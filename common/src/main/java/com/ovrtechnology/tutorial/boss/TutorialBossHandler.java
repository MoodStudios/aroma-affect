package com.ovrtechnology.tutorial.boss;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.dream.TutorialDreamEndHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Handles tutorial boss events like death triggers, custom drops, and Oliver dialogues.
 */
public final class TutorialBossHandler {

    private static boolean initialized = false;
    private static final long DAMAGE_SCENT_COOLDOWN_MS = 10000;
    private static final long DAMAGE_HURT_COOLDOWN_MS = 1000;
    private static final java.util.Map<java.util.UUID, Long> lastDamageScentTime = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Long> lastDamageHurtTime = new java.util.HashMap<>();

    /** Hit counters for bosses that need multiple hits to die. */
    private static final java.util.Map<java.util.UUID, Integer> bossHitCounts = new java.util.HashMap<>();
    private static final int BLAZE_HITS_TO_KILL = 3;
    private static final int DRAGON_HITS_TO_KILL = 4;

    private TutorialBossHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // TUTORIAL MODE: Players are IMMORTAL - cancel ALL damage to players
        // Also force dragon to die on ANY hit (even bare hand)
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (!(entity.level() instanceof ServerLevel level)) {
                return EventResult.pass();
            }

            if (!TutorialModule.isActive(level)) {
                return EventResult.pass();
            }

            // Players feel damage but with cooldown to prevent spam
            if (entity instanceof Player player && player instanceof ServerPlayer sp) {
                long now = System.currentTimeMillis();
                Long lastHurt = lastDamageHurtTime.get(sp.getUUID());

                // Only apply damage effects every 1 second
                if (lastHurt != null && now - lastHurt < DAMAGE_HURT_COOLDOWN_MS) {
                    return EventResult.interruptFalse(); // silently block rapid damage
                }
                lastDamageHurtTime.put(sp.getUUID(), now);

                float reducedAmount = 1.0f;
                if (player.getHealth() <= reducedAmount + 1.0f) {
                    return EventResult.interruptFalse();
                }
                player.setHealth(player.getHealth() - reducedAmount);

                // Play hurt sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.PLAYER_HURT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

                // Trigger Machina scent on damage (10s cooldown)
                // Disabled during boss fights so Sweet victory scent arrives clean
                if (!isTutorialBossNearby(sp, level)) {
                    Long lastScent = lastDamageScentTime.get(sp.getUUID());
                    if (lastScent == null || now - lastScent >= DAMAGE_SCENT_COOLDOWN_MS) {
                        lastDamageScentTime.put(sp.getUUID(), now);
                        com.ovrtechnology.network.TutorialScentZoneNetworking.sendScentTrigger(
                                sp, "Machina", 1.0, "damage");
                    }
                }

                return EventResult.interruptFalse();
            }

            // Tutorial blaze: takes BLAZE_HITS_TO_KILL player hits to die
            if (entity instanceof Blaze blaze && blaze.getTags().contains("tutorial_boss_blaze")) {
                if (blaze.isDeadOrDying()) return EventResult.interruptFalse();
                if (!(source.getEntity() instanceof Player)) return EventResult.interruptFalse();

                int hits = bossHitCounts.getOrDefault(blaze.getUUID(), 0) + 1;
                bossHitCounts.put(blaze.getUUID(), hits);

                if (hits >= BLAZE_HITS_TO_KILL) {
                    bossHitCounts.remove(blaze.getUUID());
                    level.playSound(null, blaze.getX(), blaze.getY(), blaze.getZ(),
                            net.minecraft.sounds.SoundEvents.BLAZE_DEATH,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
                    blaze.setHealth(0.0F);
                    blaze.die(source);
                    AromaAffect.LOGGER.info("Tutorial blaze killed after {} hits", hits);
                } else {
                    // Play hit sound + hurt animation without letting vanilla damage through
                    level.playSound(null, blaze.getX(), blaze.getY(), blaze.getZ(),
                            net.minecraft.sounds.SoundEvents.BLAZE_HURT,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
                    blaze.hurtMarked = true;
                    AromaAffect.LOGGER.info("Tutorial blaze hit {}/{}", hits, BLAZE_HITS_TO_KILL);
                }
                return EventResult.interruptFalse();
            }

            // Tutorial dragon: takes DRAGON_HITS_TO_KILL player hits to die
            if (entity instanceof EnderDragon dragon && dragon.getTags().contains("tutorial_boss_dragon")) {
                if (dragon.isDeadOrDying()) return EventResult.interruptFalse();
                if (!(source.getEntity() instanceof Player)) return EventResult.interruptFalse();

                int hits = bossHitCounts.getOrDefault(dragon.getUUID(), 0) + 1;
                bossHitCounts.put(dragon.getUUID(), hits);

                if (hits >= DRAGON_HITS_TO_KILL) {
                    bossHitCounts.remove(dragon.getUUID());
                    level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                            net.minecraft.sounds.SoundEvents.ENDER_DRAGON_DEATH,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 1.0F);
                    dragon.setHealth(0.0F);
                    dragon.die(source);
                    AromaAffect.LOGGER.info("Tutorial dragon killed after {} hits", hits);
                } else {
                    level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                            net.minecraft.sounds.SoundEvents.ENDER_DRAGON_HURT,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 1.0F);
                    dragon.hurtMarked = true;
                    AromaAffect.LOGGER.info("Tutorial dragon hit {}/{}", hits, DRAGON_HITS_TO_KILL);
                }
                return EventResult.interruptFalse();
            }

            return EventResult.pass();
        });

        // TUTORIAL MODE: Players don't lose hunger - keep food level at max
        TickEvent.SERVER_POST.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;

                // Keep hunger at max (20 = full bar)
                if (player.getFoodData().getFoodLevel() < 20) {
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(5.0f);
                }
            }
        });

        // Listen for entity death events
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) {
                return EventResult.pass();
            }

            if (!TutorialModule.isActive(level)) {
                return EventResult.pass();
            }

            // Check if it's a tutorial boss (tagged blaze or dragon)
            if (entity instanceof Blaze && entity.getTags().contains("tutorial_boss_blaze")) {
                onTutorialBlazeDeath(level, entity, source);
            }
            if (entity instanceof EnderDragon && entity.getTags().contains("tutorial_boss_dragon")) {
                onTutorialDragonDeath(level, entity, source);
            }

            return EventResult.pass();
        });

        AromaAffect.LOGGER.debug("Tutorial boss handler initialized");
    }

    // Trade IDs for boss rewards - these must be created via /tutorial trade commands
    private static final String BLAZE_TRADE_ID = "boss_blaze_reward";
    private static final String DRAGON_TRADE_ID = "boss_dragon_reward";

    private static void onTutorialBlazeDeath(ServerLevel level, LivingEntity blaze, DamageSource source) {
        AromaAffect.LOGGER.info("Tutorial Blaze killed!");

        // Find the player who killed it
        Entity killer = source.getEntity();
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }

        // Give the blaze nose directly to the player (no trade needed)
        var blazeNoseItem = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse("aromaaffect:blaze_nose"));
        if (blazeNoseItem.isPresent()) {
            ItemStack noseStack = new ItemStack(blazeNoseItem.get(), 1);
            if (!player.getInventory().add(noseStack)) {
                player.drop(noseStack, false);
            }
            AromaAffect.LOGGER.info("Gave blaze_nose directly to player {}", player.getName().getString());
        }

        // Victory scent
        com.ovrtechnology.network.TutorialScentZoneNetworking.sendScentTrigger(player, "Sweet", 1.0, "blaze_victory");
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sounds.SoundSource.MASTER, 1.0f, 1.0f);

        // Find nearest Oliver and trigger dragon quest dialogue (no trade)
        TutorialOliverEntity oliver = findNearestOliver(level, blaze.getX(), blaze.getY(), blaze.getZ());
        if (oliver != null) {
            oliver.setDialogueId("boss_blaze_killed");
            oliver.setTradeId(""); // no trade
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), "boss_blaze_killed", false, "", true
            );
            AromaAffect.LOGGER.info("Triggered boss_blaze_killed dialogue (no trade) for player {}",
                    player.getName().getString());
        }
    }

    private static void onTutorialDragonDeath(ServerLevel level, LivingEntity dragon, DamageSource source) {
        AromaAffect.LOGGER.info("Tutorial Dragon killed!");

        // Restore mobGriefing if it was disabled for this dragon
        if (dragon.getTags().contains("tutorial_dragon_mobgriefing_was_on")) {
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(true, level.getServer());
            AromaAffect.LOGGER.info("Restored mobGriefing after tutorial dragon death");
        }

        // Find the player who killed it
        Entity killer = source.getEntity();
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }

        // Victory scent + sound on dragon kill
        com.ovrtechnology.network.TutorialScentZoneNetworking.sendScentTrigger(player, "Sweet", 1.0, "dragon_victory");
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sounds.SoundSource.MASTER, 1.0f, 1.0f);
        AromaAffect.LOGGER.info("Dragon victory scent + sound triggered for player {}", player.getName().getString());

        // Start the dream ending sequence (white screen → teleport → wake up)
        TutorialDreamEndHandler.startDreamSequence(player);

        AromaAffect.LOGGER.info("Triggered dream ending sequence for player {}", player.getName().getString());
    }

    private static void dropItem(ServerLevel level, LivingEntity entity, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        item.setInvulnerable(true); // protect from fire/lava in tutorial
        level.addFreshEntity(item);
    }

    /**
     * Checks if there's a tutorial boss (blaze or dragon) alive near the player.
     */
    private static boolean isTutorialBossNearby(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(50);
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity.isAlive() && (entity.getTags().contains("tutorial_boss_blaze")
                    || entity.getTags().contains("tutorial_boss_dragon"))) {
                return true;
            }
        }
        return false;
    }

    private static TutorialOliverEntity findNearestOliver(ServerLevel level, double x, double y, double z) {
        AABB searchArea = new AABB(
                x - 100, y - 50, z - 100,
                x + 100, y + 50, z + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class, searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(x, y, z);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }
}
