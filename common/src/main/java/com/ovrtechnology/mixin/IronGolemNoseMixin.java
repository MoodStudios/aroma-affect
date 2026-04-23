package com.ovrtechnology.mixin;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.irongolem.IronGolemNoseAccessor;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.entity.nosesmith.SpecialRoseItem;
import com.ovrtechnology.network.IronGolemNoseNetworking;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolem.class)
public abstract class IronGolemNoseMixin extends AbstractGolem implements IronGolemNoseAccessor {

    @Unique private static final double aromaaffect$ROSE_PICKUP_RADIUS = 1.75D;

    @Unique private boolean aromaaffect$hasNose = true;

    @Unique private boolean aromaaffect$noseSynced = false;

    protected IronGolemNoseMixin(EntityType<? extends AbstractGolem> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean aromaaffect$hasNose() {
        return aromaaffect$hasNose;
    }

    @Override
    public void aromaaffect$setHasNose(boolean hasNose) {
        aromaaffect$hasNose = hasNose;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aromaaffect$saveNoseData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("aromaaffect:HasNose", aromaaffect$hasNose);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aromaaffect$loadNoseData(CompoundTag tag, CallbackInfo ci) {
        aromaaffect$hasNose =
                !tag.contains("aromaaffect:HasNose", Tag.TAG_BYTE)
                        || tag.getBoolean("aromaaffect:HasNose");
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void aromaaffect$onAiStep(CallbackInfo ci) {

        if (!aromaaffect$noseSynced
                && !aromaaffect$hasNose
                && this.level() instanceof ServerLevel serverLevel) {
            IronGolemNoseNetworking.broadcastNoseSync(
                    this.getUUID(), false, serverLevel.getServer().getPlayerList().getPlayers());
            aromaaffect$noseSynced = true;
        } else if (!aromaaffect$noseSynced && aromaaffect$hasNose) {
            aromaaffect$noseSynced = true;
        }

        if (aromaaffect$hasNose && this.level() instanceof ServerLevel serverLevel) {
            aromaaffect$tryConsumeSpecialRose(serverLevel);
        }
    }

    @Unique
    private void aromaaffect$tryConsumeSpecialRose(ServerLevel serverLevel) {
        List<ItemEntity> nearbyItems =
                serverLevel.getEntitiesOfClass(
                        ItemEntity.class,
                        this.getBoundingBox()
                                .inflate(
                                        aromaaffect$ROSE_PICKUP_RADIUS,
                                        0.75D,
                                        aromaaffect$ROSE_PICKUP_RADIUS));

        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack stack = itemEntity.getItem();
            if (!(stack.getItem() instanceof SpecialRoseItem)) {
                continue;
            }

            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }

            Player thrower =
                    itemEntity.getOwner() instanceof Player p
                            ? p
                            : serverLevel.getNearestPlayer(
                                    this, aromaaffect$ROSE_PICKUP_RADIUS + 2.0D);

            aromaaffect$giveNoseReward(serverLevel, thrower);
            return;
        }
    }

    @Unique
    private void aromaaffect$giveNoseReward(
            ServerLevel serverLevel, @org.jetbrains.annotations.Nullable Player receiver) {
        aromaaffect$hasNose = false;

        IronGolemNoseNetworking.broadcastNoseSync(
                this.getUUID(), false, serverLevel.getServer().getPlayerList().getPlayers());

        if (NoseSmithRegistry.getIRON_NOSE().isPresent()) {
            ItemStack noseItem = new ItemStack(NoseSmithRegistry.getIRON_NOSE().get());
            double spawnX = this.getX();
            double spawnY = this.getEyeY() - 0.3D;
            double spawnZ = this.getZ();
            ItemEntity drop = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, noseItem);
            drop.setPickUpDelay(10);

            if (receiver != null) {
                double dx = receiver.getX() - spawnX;
                double dy = receiver.getEyeY() - spawnY;
                double dz = receiver.getZ() - spawnZ;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0.01D) {
                    double speed = 0.3D;
                    drop.setDeltaMovement(
                            dx / dist * speed, dy / dist * speed + 0.15D, dz / dist * speed);
                }
            }

            serverLevel.addFreshEntity(drop);
        } else {
            AromaAffect.LOGGER.warn("Failed to drop iron_nose: item not registered");
        }

        serverLevel.playSound(
                null,
                this.blockPosition(),
                SoundEvents.IRON_GOLEM_REPAIR,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F);
        serverLevel.playSound(
                null,
                this.blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                0.75F,
                1.2F);
    }
}
