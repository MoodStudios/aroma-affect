package com.ovrtechnology.tutorial.boss;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.dream.TutorialDreamEndHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
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

    private TutorialBossHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Prevent tutorial dragon from dealing damage to players
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (!(entity.level() instanceof ServerLevel level)) {
                return EventResult.pass();
            }

            if (!TutorialModule.isActive(level)) {
                return EventResult.pass();
            }

            // If the victim is a player and the attacker is a tutorial dragon, cancel damage
            if (entity instanceof Player) {
                Entity attacker = source.getEntity();
                if (attacker instanceof EnderDragon && attacker.getTags().contains("tutorial_boss_dragon")) {
                    return EventResult.interruptFalse();
                }
            }

            return EventResult.pass();
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

        // Drop custom items (required for trade)
        dropItem(level, blaze, new ItemStack(Items.BLAZE_POWDER, 5));
        dropItem(level, blaze, new ItemStack(Items.FLINT, 2));
        dropItem(level, blaze, new ItemStack(Items.ENDER_PEARL, 1));

        // Find the player who killed it
        Entity killer = source.getEntity();
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }

        // Find nearest Oliver and trigger dialogue with trade
        TutorialOliverEntity oliver = findNearestOliver(level, blaze.getX(), blaze.getY(), blaze.getZ());
        if (oliver != null) {
            // Set up Oliver with trade for nose upgrade
            oliver.setDialogueId("boss_blaze_killed");
            oliver.setTradeId(BLAZE_TRADE_ID);

            // Open dialogue with trade button enabled
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), "boss_blaze_killed", true, BLAZE_TRADE_ID
            );

            AromaAffect.LOGGER.info("Triggered boss_blaze_killed dialogue with trade '{}' for player {}",
                    BLAZE_TRADE_ID, player.getName().getString());
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

        // Start the dream ending sequence (white screen → teleport → wake up)
        TutorialDreamEndHandler.startDreamSequence(player);

        AromaAffect.LOGGER.info("Triggered dream ending sequence for player {}", player.getName().getString());
    }

    private static void dropItem(ServerLevel level, LivingEntity entity, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
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
