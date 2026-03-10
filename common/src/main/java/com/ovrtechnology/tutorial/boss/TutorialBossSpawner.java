package com.ovrtechnology.tutorial.boss;

import com.ovrtechnology.AromaAffect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

/**
 * Handles spawning tutorial bosses.
 * Uses vanilla entities with modified stats for simplicity.
 */
public final class TutorialBossSpawner {

    private TutorialBossSpawner() {
    }

    /**
     * Spawns a tutorial boss at the given position.
     *
     * @param level    the server level
     * @param bossType "blaze" or "dragon"
     * @param pos      spawn position
     * @return true if successful
     */
    public static boolean spawnBoss(ServerLevel level, String bossType, BlockPos pos) {
        return spawnBossEntity(level, bossType, pos, null) != null;
    }

    /**
     * Spawns a tutorial boss at the given position, returning the entity.
     *
     * @param level    the server level
     * @param bossType "blaze" or "dragon"
     * @param pos      spawn position
     * @param area     optional area for movement bounds (can be null)
     * @return the spawned entity, or null if failed
     */
    @Nullable
    public static Entity spawnBossEntity(ServerLevel level, String bossType, BlockPos pos, @Nullable TutorialBossArea area) {
        switch (bossType.toLowerCase()) {
            case "blaze":
                return spawnTutorialBlaze(level, pos, area);
            case "dragon":
                return spawnTutorialDragon(level, pos, area);
            default:
                return null;
        }
    }

    @Nullable
    private static Blaze spawnTutorialBlaze(ServerLevel level, BlockPos pos, @Nullable TutorialBossArea area) {
        Blaze blaze = EntityType.BLAZE.create(level, EntitySpawnReason.COMMAND);
        if (blaze == null) {
            return null;
        }

        // Set position
        blaze.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // Modify stats - easy to kill
        if (blaze.getAttribute(Attributes.MAX_HEALTH) != null) {
            blaze.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0D);  // 5 hearts
            blaze.setHealth(10.0F);
        }

        // Set red custom name
        blaze.setCustomName(Component.literal("Blaze").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        blaze.setCustomNameVisible(true);

        // Tag for custom drops and identification
        blaze.addTag("tutorial_boss_blaze");
        if (area != null) {
            blaze.addTag("tutorial_boss_area:" + area.getId());
        }

        // Spawn effects
        spawnBlazeEffects(level, pos);

        // Add to world
        level.addFreshEntity(blaze);

        AromaAffect.LOGGER.info("Spawned tutorial blaze at {}", pos);
        return blaze;
    }

    @Nullable
    private static EnderDragon spawnTutorialDragon(ServerLevel level, BlockPos pos, @Nullable TutorialBossArea area) {
        EnderDragon dragon = EntityType.ENDER_DRAGON.create(level, EntitySpawnReason.COMMAND);
        if (dragon == null) {
            return null;
        }

        // Set position
        dragon.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // Modify stats - dies in 1 hit (1 HP = half heart)
        if (dragon.getAttribute(Attributes.MAX_HEALTH) != null) {
            dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0D);
            dragon.setHealth(1.0F);
        }

        // Make dragon SILENT - prevents long growl/death sounds
        // We'll play our own short sounds instead
        dragon.setSilent(true);

        // AI stays ON for natural flying animation - position controlled by TutorialBossAreaHandler

        // Set purple custom name
        dragon.setCustomName(Component.literal("Dragon").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        dragon.setCustomNameVisible(true);

        // Tag for identification
        dragon.addTag("tutorial_boss_dragon");
        if (area != null) {
            dragon.addTag("tutorial_boss_area:" + area.getId());
        }

        // Disable mobGriefing while tutorial dragon is alive to prevent block destruction
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, level.getServer());
            dragon.addTag("tutorial_dragon_mobgriefing_was_on");
            AromaAffect.LOGGER.info("Temporarily disabled mobGriefing for tutorial dragon");
        }

        // Add to world
        level.addFreshEntity(dragon);

        // Spawn effects
        spawnDragonEffects(level, pos);

        AromaAffect.LOGGER.info("Spawned tutorial dragon at {}", pos);
        return dragon;
    }

    private static void spawnDragonEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        // Dragon particles
        level.sendParticles(
                ParticleTypes.PORTAL,
                x, y + 2, z,
                80,
                2.0, 2.0, 2.0,
                0.1
        );
        level.sendParticles(
                ParticleTypes.END_ROD,
                x, y + 3, z,
                40,
                1.5, 2.0, 1.5,
                0.05
        );

        // Sound - short flap instead of long growl (dragon dies too fast for growl)
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private static void spawnBlazeEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        // Fire particles
        level.sendParticles(
                ParticleTypes.FLAME,
                x, y + 1, z,
                50,
                0.5, 1.0, 0.5,
                0.1
        );
        level.sendParticles(
                ParticleTypes.SMOKE,
                x, y + 1, z,
                30,
                0.5, 1.0, 0.5,
                0.05
        );

        // Sound
        level.playSound(null, pos, SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.8F);
    }
}
