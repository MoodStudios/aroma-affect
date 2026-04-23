package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class NoseSmithSpawnItem extends Item {

    public NoseSmithSpawnItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState blockState = level.getBlockState(clickedPos);

        BlockPos spawnPos;
        if (blockState.getCollisionShape(level, clickedPos).isEmpty()) {
            spawnPos = clickedPos;
        } else {
            spawnPos = clickedPos.relative(direction);
        }

        NoseSmithEntity noseSmith =
                new NoseSmithEntity(NoseSmithRegistry.getNOSE_SMITH().get(), serverLevel);

        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5;
        noseSmith.setPos(x, y, z);
        noseSmith.setYRot(level.getRandom().nextFloat() * 360.0F);

        serverLevel.addFreshEntity(noseSmith);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        AromaAffect.LOGGER.debug("Spawned Nose Smith at {}", spawnPos);

        return InteractionResult.SUCCESS;
    }
}
