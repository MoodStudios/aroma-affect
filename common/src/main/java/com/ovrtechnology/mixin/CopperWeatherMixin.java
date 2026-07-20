package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ChangeOverTimeBlock.class)
public interface CopperWeatherMixin {

    @Inject(method = "getNextState", at = @At("HEAD"))
    private void aromaaffect$onGetNextState(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfoReturnable<Optional<BlockState>> cir) {
        if (this instanceof WeatheringCopper) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                player.sendOverlayMessage(Component.literal("Distance: " + player.blockPosition().distSqr(pos)));
                if (player.blockPosition().distSqr(pos) <= 15.0) {
                    ServerEventBusHandler.onCopperOxidize(player);
                }
            }
        }
    }
}
