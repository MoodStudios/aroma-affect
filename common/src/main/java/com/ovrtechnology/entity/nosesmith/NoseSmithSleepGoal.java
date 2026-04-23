package com.ovrtechnology.entity.nosesmith;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class NoseSmithSleepGoal extends Goal {

    private static final int SEARCH_RADIUS = 16;
    private static final double ARRIVE_DISTANCE_SQR = 1.5 * 1.5;

    private final NoseSmithEntity noseSmith;
    private BlockPos bedPos;
    private boolean isSleeping;

    @SuppressWarnings("this-escape")
    public NoseSmithSleepGoal(NoseSmithEntity noseSmith) {
        this.noseSmith = noseSmith;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Level level = noseSmith.level();
        if (!isNight(level)) {
            return false;
        }
        if (noseSmith.isInDialogue()) {
            return false;
        }
        bedPos = findNearbyBed(level);
        return bedPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        Level level = noseSmith.level();
        if (!isNight(level)) {
            return false;
        }
        if (noseSmith.isInDialogue()) {
            return false;
        }
        if (noseSmith.getLastHurtByMob() != null
                && noseSmith.getLastHurtByMobTimestamp() > noseSmith.tickCount - 40) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        isSleeping = false;
        if (bedPos != null) {
            noseSmith
                    .getNavigation()
                    .moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.5D);
        }
    }

    @Override
    public void tick() {
        if (isSleeping) {
            return;
        }
        if (bedPos == null) {
            return;
        }
        double distSqr = noseSmith.blockPosition().distSqr(bedPos);
        if (distSqr <= ARRIVE_DISTANCE_SQR) {
            noseSmith.getNavigation().stop();
            noseSmith.startSleeping(bedPos);
            isSleeping = true;
        } else if (noseSmith.getNavigation().isDone()) {
            noseSmith
                    .getNavigation()
                    .moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.5D);
        }
    }

    @Override
    public void stop() {
        if (isSleeping) {
            noseSmith.stopSleeping();
            isSleeping = false;
        }
        bedPos = null;
    }

    private static boolean isNight(Level level) {
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= 13000L && dayTime < 23000L;
    }

    private BlockPos findNearbyBed(Level level) {
        BlockPos origin = noseSmith.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.getBlock() instanceof BedBlock) {
                        if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
                            return mutable.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }
}
