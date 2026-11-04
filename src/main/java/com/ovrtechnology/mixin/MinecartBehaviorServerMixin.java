package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecartBehavior.class)
public abstract class MinecartBehaviorServerMixin {

    @Shadow
    protected final AbstractMinecart minecart;

    @Shadow
    public abstract Level level();

    public MinecartBehaviorServerMixin(AbstractMinecart minecart) {
        this.minecart = minecart;
    }

    public void onRideOverRail(String triggerType) {
        BlockPos pos = minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = level().getBlockState(pos);
        for (Entity entity : minecart.getPassengers()) {
            if (state.is(Blocks.POWERED_RAIL) && entity instanceof ServerPlayer player) {
                ServerEventBusHandler.fireSimpleEvent(player, triggerType);
            }
        }
    }
}
