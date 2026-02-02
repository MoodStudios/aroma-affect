package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class NoseSmithSniffEntityGoal extends Goal {

    private static final double SEARCH_RADIUS = 8.0;
    private static final double ARRIVE_DISTANCE_SQR = 2.0 * 2.0;
    private static final int SNIFF_DURATION = 50;
    private static final int COOLDOWN_MIN = 300;
    private static final int COOLDOWN_MAX = 600;
    private static final int SNIFF_TICK = 20;
    private static final int REACTION_TICK = 35;

    private final NoseSmithEntity noseSmith;
    private LivingEntity target;
    private int ticksRemaining;
    private boolean hasSniffed;
    private boolean hasReacted;

    public NoseSmithSniffEntityGoal(NoseSmithEntity noseSmith) {
        this.noseSmith = noseSmith;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!noseSmith.hasNose() || noseSmith.isInDialogue() || noseSmith.sniffEntityCooldownTicks > 0) {
            return false;
        }
        target = findRandomTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return noseSmith.hasNose()
                && !noseSmith.isInDialogue()
                && target != null
                && target.isAlive()
                && ticksRemaining > 0;
    }

    @Override
    public void start() {
        noseSmith.getNavigation().moveTo(target, 0.4);
        ticksRemaining = SNIFF_DURATION + 100;
        hasSniffed = false;
        hasReacted = false;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            ticksRemaining = 0;
            return;
        }

        double distSqr = noseSmith.distanceToSqr(target);

        if (distSqr > ARRIVE_DISTANCE_SQR) {
            noseSmith.getNavigation().moveTo(target, 0.4);
            ticksRemaining--;
            return;
        }

        // Arrived — stop and look at target
        noseSmith.getNavigation().stop();
        noseSmith.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (!hasSniffed) {
            hasSniffed = true;
            ticksRemaining = SNIFF_DURATION;
        }

        if (ticksRemaining == SNIFF_DURATION - SNIFF_TICK) {
            Level level = noseSmith.level();
            level.playSound(null, noseSmith.blockPosition(), ModSounds.SNIFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        if (!hasReacted && ticksRemaining == SNIFF_DURATION - REACTION_TICK) {
            hasReacted = true;
            Level level = noseSmith.level();
            boolean approve = noseSmith.getRandom().nextBoolean();
            if (approve) {
                level.playSound(null, noseSmith.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            noseSmith.getX(), noseSmith.getEyeY() + 0.2, noseSmith.getZ(),
                            8, 0.3, 0.3, 0.3, 0.05
                    );
                }
            } else {
                level.playSound(null, noseSmith.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.ANGRY_VILLAGER,
                            noseSmith.getX(), noseSmith.getEyeY() + 0.2, noseSmith.getZ(),
                            6, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }
        }

        ticksRemaining--;
        if (ticksRemaining <= 0) {
            target = null;
        }
    }

    @Override
    public void stop() {
        int cooldown = COOLDOWN_MIN + noseSmith.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN + 1);
        noseSmith.sniffEntityCooldownTicks = cooldown;
        target = null;
        ticksRemaining = 0;
        hasSniffed = false;
        hasReacted = false;
    }

    private LivingEntity findRandomTarget() {
        AABB box = noseSmith.getBoundingBox().inflate(SEARCH_RADIUS);
        Level level = noseSmith.level();
        List<LivingEntity> candidates = new ArrayList<>();

        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (!player.isSpectator() && player.isAlive()) {
                candidates.add(player);
            }
        }

        for (Villager villager : level.getEntitiesOfClass(Villager.class, box)) {
            if (villager != noseSmith && villager.isAlive()) {
                candidates.add(villager);
            }
        }

        if (candidates.isEmpty()) return null;
        return candidates.get(noseSmith.getRandom().nextInt(candidates.size()));
    }
}
