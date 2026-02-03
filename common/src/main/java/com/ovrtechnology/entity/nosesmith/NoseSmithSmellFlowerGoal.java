package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class NoseSmithSmellFlowerGoal extends Goal {

    private static final int SEARCH_RADIUS = 8;
    private static final double ARRIVE_DISTANCE_SQR = 1.5 * 1.5;
    private static final int SMELL_DURATION_MIN = 40;
    private static final int SMELL_DURATION_MAX = 60;
    private static final int COOLDOWN_MIN = 200;
    private static final int COOLDOWN_MAX = 400;

    private final NoseSmithEntity noseSmith;
    private BlockPos targetPos;
    private int smellTicks;
    private boolean hasSniffed;
    private boolean arrived;

    public NoseSmithSmellFlowerGoal(NoseSmithEntity noseSmith) {
        this.noseSmith = noseSmith;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!noseSmith.hasNose() || noseSmith.isInDialogue() || noseSmith.smellCooldownTicks > 0) {
            return false;
        }
        targetPos = findRandomFlowerBlock();
        return targetPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return noseSmith.hasNose() && !noseSmith.isInDialogue() && targetPos != null && smellTicks > 0;
    }

    @Override
    public void start() {
        noseSmith.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 0.4);
        int duration = SMELL_DURATION_MIN + noseSmith.getRandom().nextInt(SMELL_DURATION_MAX - SMELL_DURATION_MIN + 1);
        smellTicks = duration + 100; // extra ticks for walking
        hasSniffed = false;
        arrived = false;
    }

    @Override
    public void tick() {
        if (targetPos == null) return;

        double distSqr = noseSmith.distanceToSqr(Vec3.atCenterOf(targetPos));

        if (!arrived && distSqr <= ARRIVE_DISTANCE_SQR) {
            arrived = true;
            noseSmith.getNavigation().stop();
            int duration = SMELL_DURATION_MIN + noseSmith.getRandom().nextInt(SMELL_DURATION_MAX - SMELL_DURATION_MIN + 1);
            smellTicks = duration;
        }

        if (arrived) {
            noseSmith.getLookControl().setLookAt(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 30.0F, 30.0F);
            noseSmith.getNavigation().stop();

            if (!hasSniffed && smellTicks < (SMELL_DURATION_MIN + SMELL_DURATION_MAX) / 2) {
                hasSniffed = true;
                Level level = noseSmith.level();
                level.playSound(null, noseSmith.blockPosition(), ModSounds.SNIFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            noseSmith.getX(), noseSmith.getEyeY() + 0.2, noseSmith.getZ(),
                            8, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }
        }

        smellTicks--;
        if (smellTicks <= 0) {
            targetPos = null;
        }
    }

    @Override
    public void stop() {
        int cooldown = COOLDOWN_MIN + noseSmith.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN + 1);
        noseSmith.smellCooldownTicks = cooldown;
        targetPos = null;
        smellTicks = 0;
        hasSniffed = false;
        arrived = false;
    }

    private BlockPos findRandomFlowerBlock() {
        BlockPos origin = noseSmith.blockPosition();
        Level level = noseSmith.level();
        List<BlockPos> candidates = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RADIUS, -SEARCH_RADIUS / 2, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, SEARCH_RADIUS / 2, SEARCH_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (isFloralBlock(state)) {
                candidates.add(pos.immutable());
            }
        }

        if (candidates.isEmpty()) return null;
        return candidates.get(noseSmith.getRandom().nextInt(candidates.size()));
    }

    private boolean isFloralBlock(BlockState state) {
        if (state.getBlock() instanceof FlowerPotBlock pot) {
            return pot.getPotted() != net.minecraft.world.level.block.Blocks.AIR;
        }
        return state.getBlock() instanceof FlowerBlock || state.getBlock() instanceof TallFlowerBlock;
    }
}
