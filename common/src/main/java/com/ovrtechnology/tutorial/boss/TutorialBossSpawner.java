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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.EntitySpawnReason;
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
                // Dragon is complex, use blaze for now
                return spawnTutorialBlaze(level, pos, area);
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
