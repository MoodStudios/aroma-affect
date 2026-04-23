package com.ovrtechnology.mixin;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.sniffer.SnifferContainer;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferTamingData;
import com.ovrtechnology.entity.sniffer.config.SnifferConfig;
import com.ovrtechnology.entity.sniffer.config.SnifferConfigLoader;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import com.ovrtechnology.sniffer.loot.SnifferLootResolver;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sniffer.class)
public abstract class SnifferTamingMixin extends Animal implements HasCustomInventoryScreen {

    @Unique private int aromaaffect$waterTicks = 0;

    @Unique private boolean aromaaffect$isSwimming = false;

    @Unique
    private static final TagKey<Block> ENHANCED_SNIFFER_DIGGABLE =
            TagKey.create(Registries.BLOCK, Ids.mod("enhanced_sniffer_diggable"));

    @Unique
    private static SnifferConfig aromaaffect$getConfig() {
        return SnifferConfigLoader.getConfig();
    }

    @Shadow
    public abstract Sniffer.State getState();

    @Shadow @Final private static EntityDataAccessor<Sniffer.State> DATA_STATE;

    @Shadow @Final private static EntityDataAccessor<Integer> DATA_DROP_SEED_AT_TICK;

    @Shadow public final AnimationState diggingAnimationState = new AnimationState();

    protected SnifferTamingMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$onMobInteract(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Sniffer self = (Sniffer) (Object) this;
        ItemStack itemStack = player.getItemInHand(hand);
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        if (data.ownerUUID != null) {
            if (player.isShiftKeyDown()) {

                if (itemStack.is(Items.SADDLE) && data.saddleItem.isEmpty()) {
                    if (!self.level().isClientSide()) {
                        data.saddleItem = itemStack.copy();
                        data.saddleItem.setCount(1);
                        if (!player.getAbilities().instabuild) {
                            itemStack.shrink(1);
                        }
                        self.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
                        SnifferContainer container = new SnifferContainer(self);
                        container.setChanged();
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                if (itemStack.getItem() instanceof SnifferNoseItem
                        && data.decorationItem.isEmpty()) {
                    if (!self.level().isClientSide()) {
                        data.decorationItem = itemStack.copy();
                        data.decorationItem.setCount(1);
                        if (!player.getAbilities().instabuild) {
                            itemStack.shrink(1);
                        }
                        self.playSound(SoundEvents.HORSE_ARMOR, 1.0F, 1.0F);
                        SnifferContainer container = new SnifferContainer(self);
                        container.setChanged();
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                if (itemStack.isEmpty()) {
                    if (!self.level().isClientSide()
                            && player instanceof ServerPlayer serverPlayer) {
                        SnifferMenuRegistry.openSnifferMenu(serverPlayer, self);
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
            }

            if (itemStack.isEmpty() && !self.isVehicle()) {
                if (!self.level().isClientSide()) {
                    player.startRiding(self);

                    self.transitionTo(Sniffer.State.IDLING);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            return;
        }

        if (itemStack.is(Items.TORCHFLOWER)) {
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            data.tamingProgress++;

            if (self.level() instanceof ServerLevel serverLevel) {
                int torchflowersNeeded = aromaaffect$getConfig().taming.torchflowersNeeded;
                if (data.tamingProgress >= torchflowersNeeded) {

                    data.ownerUUID = player.getUUID();

                    for (ServerPlayer nearbyPlayer : serverLevel.players()) {
                        if (nearbyPlayer.distanceToSqr(self) <= 128 * 128) {
                            SnifferEquipmentNetworking.sendEquipmentSync(
                                    nearbyPlayer, self.getUUID(), data);
                        }
                    }

                    serverLevel.sendParticles(
                            ParticleTypes.HEART,
                            self.getX(),
                            self.getY() + 1.0,
                            self.getZ(),
                            10,
                            0.5,
                            0.5,
                            0.5,
                            0.1);

                    self.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);

                    if (player instanceof Player) {
                        player.displayClientMessage(
                                Texts.lit("§aSuccessfully tamed the Sniffer!"), true);
                    }
                } else {

                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            self.getX(),
                            self.getY() + 1.0,
                            self.getZ(),
                            5,
                            0.4,
                            0.4,
                            0.4,
                            0.02);

                    if (player instanceof Player) {
                        player.displayClientMessage(
                                Texts.lit(
                                        "§6Taming progress: §e"
                                                + data.tamingProgress
                                                + "§6/§e"
                                                + torchflowersNeeded),
                                true);
                    }
                }
            }

            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void aromaaffect$onTick(CallbackInfo ci) {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        if (data.ownerUUID != null && self.isVehicle()) {
            Entity passenger = self.getFirstPassenger();
            SnifferContainer container = new SnifferContainer(self);

            if (passenger instanceof Player && container.hasSaddle()) {

                self.getNavigation().stop();

                if (self.isInWater()) {
                    aromaaffect$waterTicks++;

                    if (!aromaaffect$isSwimming) {
                        aromaaffect$isSwimming = true;
                    }

                    self.getEntityData().set(DATA_STATE, Sniffer.State.DIGGING);
                    self.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);

                    Entity rider = self.getFirstPassenger();
                    boolean isMoving = rider instanceof Player p && (p.zza != 0 || p.xxa != 0);

                    if (isMoving && self.level() instanceof ServerLevel serverLevel) {

                        if (self.tickCount % 3 == 0) {
                            serverLevel.sendParticles(
                                    ParticleTypes.BUBBLE,
                                    self.getX(),
                                    self.getY() + 0.3,
                                    self.getZ(),
                                    3,
                                    0.6,
                                    0.1,
                                    0.6,
                                    0.02);
                        }

                        if (self.tickCount % 5 == 0) {
                            serverLevel.sendParticles(
                                    ParticleTypes.SPLASH,
                                    self.getX(),
                                    self.getY() + 0.5,
                                    self.getZ(),
                                    2,
                                    0.5,
                                    0.0,
                                    0.5,
                                    0.1);
                        }

                        if (self.tickCount % 15 == 0) {
                            self.playSound(
                                    SoundEvents.GENERIC_SWIM,
                                    0.6F,
                                    0.8F + self.getRandom().nextFloat() * 0.4F);
                        }
                    }

                    aromaaffect$handleWaterFloating(self);
                } else {

                    if (aromaaffect$isSwimming) {
                        aromaaffect$isSwimming = false;
                        this.diggingAnimationState.stop();
                        self.getEntityData().set(DATA_STATE, Sniffer.State.IDLING);
                    }
                    aromaaffect$waterTicks = 0;
                }

                var stepAttr =
                        self.getAttribute(
                                net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
                if (stepAttr != null) {
                    var ridingConfig = aromaaffect$getConfig().riding;
                    if (self.isInWater() || aromaaffect$waterTicks > 0) {

                        stepAttr.setBaseValue(ridingConfig.waterExitStepHeight);
                    } else {
                        stepAttr.setBaseValue(ridingConfig.mountedStepHeight);
                    }
                }
            }
        } else {

            Sniffer self2 = (Sniffer) (Object) this;
            var stepAttr =
                    self2.getAttribute(
                            net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
            if (stepAttr != null) {
                stepAttr.setBaseValue(aromaaffect$getConfig().riding.normalStepHeight);
            }
            if (aromaaffect$isSwimming) {
                aromaaffect$isSwimming = false;
                this.diggingAnimationState.stop();
                self2.getEntityData().set(DATA_STATE, Sniffer.State.IDLING);
                aromaaffect$waterTicks = 0;
            }
        }
    }

    @Unique
    private void aromaaffect$handleWaterFloating(Sniffer sniffer) {
        double waterLevel = aromaaffect$getWaterLevel(sniffer);
        var waterConfig = aromaaffect$getConfig().waterFloating;

        if (waterLevel > 0) {
            Vec3 currentMotion = sniffer.getDeltaMovement();

            if (sniffer.horizontalCollision && (currentMotion.x != 0 || currentMotion.z != 0)) {
                sniffer.setDeltaMovement(currentMotion.x, 0.35, currentMotion.z);
            } else {
                double targetY = waterLevel - waterConfig.floatOffset;
                double currentY = sniffer.getY();

                double verticalMotion = (targetY - currentY) * waterConfig.buoyancyStrength;
                verticalMotion =
                        Math.max(
                                -waterConfig.maxDownwardSpeed,
                                Math.min(waterConfig.maxUpwardSpeed, verticalMotion));

                if (aromaaffect$isSwimming) {

                    float yawRad = sniffer.getYRot() * ((float) Math.PI / 180F);
                    Entity rider = sniffer.getFirstPassenger();
                    if (rider instanceof Player player) {
                        float forward = player.zza;
                        float strafe = player.xxa;
                        if (forward != 0 || strafe != 0) {
                            var swimConfig = aromaaffect$getConfig().swimming;
                            double speed = swimConfig.swimSpeed;
                            double motionX =
                                    (-Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe)
                                            * speed;
                            double motionZ =
                                    (Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe)
                                            * speed;
                            sniffer.setDeltaMovement(motionX, verticalMotion, motionZ);
                        } else {
                            sniffer.setDeltaMovement(
                                    currentMotion.x * 0.9, verticalMotion, currentMotion.z * 0.9);
                        }
                    }
                } else {
                    double drag = waterConfig.horizontalDrag;
                    sniffer.setDeltaMovement(
                            currentMotion.x * drag, verticalMotion, currentMotion.z * drag);
                }
            }

            sniffer.setAirSupply(sniffer.getMaxAirSupply());
        }
    }

    @Unique
    private double aromaaffect$getWaterLevel(Sniffer sniffer) {
        BlockPos pos = sniffer.blockPosition();

        for (int i = 0; i < 3; i++) {
            BlockPos checkPos = pos.above(i);
            BlockState state = sniffer.level().getBlockState(checkPos);
            BlockState stateAbove = sniffer.level().getBlockState(checkPos.above());

            if (state.getFluidState().isSource() && !stateAbove.getFluidState().isSource()) {
                return checkPos.getY() + 1.0;
            }
        }

        return sniffer.getY() + 1.0;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        this.setYRot(player.getYRot());
        this.yRotO = this.getYRot();
        this.setXRot(player.getXRot() * 0.5F);
        this.setRot(this.getYRot(), this.getXRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.yBodyRot;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(
            Entity passenger, EntityDimensions dimensions, float scale) {
        Vec3 base = super.getPassengerAttachmentPoint(passenger, dimensions, scale);
        if (aromaaffect$isSwimming) {
            return base.add(0.0, 0.55, 0.0);
        }
        return base;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float forward = player.zza;
        float strafe = player.xxa * 0.5F;

        if (forward <= 0.0F) {
            forward *= 0.25F;
        }

        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        var ridingConfig = aromaaffect$getConfig().riding;
        float baseSpeed =
                (float)
                                this.getAttributeValue(
                                        net.minecraft.world.entity.ai.attributes.Attributes
                                                .MOVEMENT_SPEED)
                        * ridingConfig.landSpeedMultiplier;

        if (this.isInWater()) {
            var swimConfig = aromaaffect$getConfig().swimming;

            if (!aromaaffect$isSwimming) {
                return swimConfig.slowSpeed;
            }

            return swimConfig.swimSpeed;
        }

        return baseSpeed;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());
        SnifferContainer container = new SnifferContainer(self);

        if (this.getFirstPassenger() instanceof Player player && container.hasSaddle()) {

            if (self.level().isClientSide() || data.ownerUUID != null) {
                return player;
            }
        }
        return null;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        if (data.ownerUUID != null
                && !self.level().isClientSide()
                && player instanceof ServerPlayer serverPlayer) {
            SnifferMenuRegistry.openSnifferMenu(serverPlayer, self);
        }
    }

    @Inject(method = "canDig(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$canDigEnhanced(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());
        SnifferContainer container = new SnifferContainer(self);

        if (data.ownerUUID != null && container.hasSnifferNose()) {
            BlockState blockState = self.level().getBlockState(pos.below());
            if (blockState.isSolid()) {
                cir.setReturnValue(true);
                return;
            }
        }

        BlockState blockState = self.level().getBlockState(pos.below());
        if (blockState.is(ENHANCED_SNIFFER_DIGGABLE)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "dropSeed", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$onDropSeed(CallbackInfo ci) {
        Sniffer self = (Sniffer) (Object) this;

        if (aromaaffect$isSwimming) {
            ci.cancel();
            return;
        }

        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int dropSeedAtTick = self.getEntityData().get(DATA_DROP_SEED_AT_TICK);
        if (dropSeedAtTick != self.tickCount) {
            return;
        }

        SnifferTamingData data = SnifferTamingData.get(self.getUUID());
        ResourceKey<Level> dimension = serverLevel.dimension();

        ItemStack scentToDrop = aromaaffect$getScentForDimension(dimension, data);

        if (!scentToDrop.isEmpty()) {

            aromaaffect$dropItemAtHead(self, serverLevel, scentToDrop);

            aromaaffect$markScentObtained(dimension, data);

            AromaAffect.LOGGER.debug(
                    "Sniffer {} obtained {} scent", self.getUUID(), dimension.location().getPath());

            if (data.hasAllScents()) {
                aromaaffect$grantSnifferJourneyAdvancement(self, data);
            }

            ci.cancel();
            return;
        }

        SnifferContainer container = new SnifferContainer(self);

        if (data.ownerUUID != null && container.hasSnifferNose()) {
            Optional<String> noseIdOpt = container.getSnifferNoseId();
            if (noseIdOpt.isPresent()) {
                BlockPos headPos = aromaaffect$getHeadBlock(self);
                List<ItemStack> drops =
                        SnifferLootResolver.resolve(
                                noseIdOpt.get(), serverLevel, headPos, self.getRandom());
                if (!drops.isEmpty()) {
                    for (ItemStack drop : drops) {
                        aromaaffect$dropItemStackAtHead(self, serverLevel, headPos, drop);
                    }
                    self.playSound(SoundEvents.SNIFFER_DROP_SEED, 1.0F, 1.0F);
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Unique
    private void aromaaffect$dropItemStackAtHead(
            Sniffer sniffer, ServerLevel serverLevel, BlockPos headPos, ItemStack itemStack) {

        double offsetX = (sniffer.getRandom().nextDouble() - 0.5) * 0.5;
        double offsetZ = (sniffer.getRandom().nextDouble() - 0.5) * 0.5;

        ItemEntity itemEntity =
                new ItemEntity(
                        sniffer.level(),
                        headPos.getX() + offsetX,
                        headPos.getY(),
                        headPos.getZ() + offsetZ,
                        itemStack);
        itemEntity.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(itemEntity);
    }

    @Unique
    private void aromaaffect$dropItemAtHead(
            Sniffer sniffer, ServerLevel serverLevel, ItemStack itemStack) {
        BlockPos headPos = aromaaffect$getHeadBlock(sniffer);
        ItemEntity itemEntity =
                new ItemEntity(
                        sniffer.level(), headPos.getX(), headPos.getY(), headPos.getZ(), itemStack);
        itemEntity.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(itemEntity);

        sniffer.playSound(SoundEvents.SNIFFER_DROP_SEED, 1.0F, 1.0F);
    }

    @Unique
    private BlockPos aromaaffect$getHeadBlock(Sniffer sniffer) {
        Vec3 headPos = sniffer.position().add(sniffer.getForward().scale(2.25));
        return BlockPos.containing(headPos.x(), sniffer.getY() + 0.2, headPos.z());
    }

    @Unique
    private ItemStack aromaaffect$getScentForDimension(
            ResourceKey<Level> dimension, SnifferTamingData data) {
        var scentsConfig = aromaaffect$getConfig().dimensionalScents;
        String itemId = null;
        if (dimension == Level.OVERWORLD && !data.hasOverworldScent) {
            itemId = scentsConfig.overworld;
        } else if (dimension == Level.NETHER && !data.hasNetherScent) {
            itemId = scentsConfig.nether;
        } else if (dimension == Level.END && !data.hasEndScent) {
            itemId = scentsConfig.end;
        }
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation loc = Ids.parse(itemId);
        return BuiltInRegistries.ITEM.getOptional(loc).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Unique
    private void aromaaffect$markScentObtained(
            ResourceKey<Level> dimension, SnifferTamingData data) {
        if (dimension == Level.OVERWORLD) {
            data.hasOverworldScent = true;
        } else if (dimension == Level.NETHER) {
            data.hasNetherScent = true;
        } else if (dimension == Level.END) {
            data.hasEndScent = true;
        }
    }

    @Inject(method = "onDiggingComplete", at = @At("TAIL"))
    private void aromaaffect$onFinishDigging(boolean found, CallbackInfoReturnable<Sniffer> cir) {
        Sniffer self = (Sniffer) (Object) this;

        if (aromaaffect$isSwimming) {
            return;
        }

        if (self.level().isClientSide()) {
            return;
        }

        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        if (data.ownerUUID == null) {
            return;
        }

        SnifferContainer container = new SnifferContainer(self);
        if (!container.hasSnifferNose()) {
            return;
        }

        self.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
        self.getBrain().eraseMemory(MemoryModuleType.SNIFF_COOLDOWN);
        self.getBrain()
                .setMemoryWithExpiry(
                        MemoryModuleType.SNIFF_COOLDOWN,
                        Unit.INSTANCE,
                        aromaaffect$getConfig().digging.sniffCooldownWithNose);
    }

    @Unique
    private static final ResourceLocation SNIFFER_JOURNEY_ADVANCEMENT = Ids.mod("sniffer_journey");

    @Unique
    private void aromaaffect$grantSnifferJourneyAdvancement(
            Sniffer sniffer, SnifferTamingData data) {

        if (data.ownerUUID != null && sniffer.level() instanceof ServerLevel serverLevel) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null) {

                AdvancementHolder advancementHolder =
                        serverLevel.getServer().getAdvancements().get(SNIFFER_JOURNEY_ADVANCEMENT);

                if (advancementHolder != null) {
                    AdvancementProgress progress =
                            owner.getAdvancements().getOrStartProgress(advancementHolder);
                    if (!progress.isDone()) {

                        for (String criterion : progress.getRemainingCriteria()) {
                            owner.getAdvancements().award(advancementHolder, criterion);
                        }
                    }
                }

                serverLevel.sendParticles(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        sniffer.getX(),
                        sniffer.getY() + 1.5,
                        sniffer.getZ(),
                        50,
                        1.0,
                        1.0,
                        1.0,
                        0.5);
                sniffer.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        if (!data.saddleItem.isEmpty()) {
            self.spawnAtLocation(data.saddleItem.copy());
            data.saddleItem = ItemStack.EMPTY;
        }

        if (!data.decorationItem.isEmpty()) {
            self.spawnAtLocation(data.decorationItem.copy());
            data.decorationItem = ItemStack.EMPTY;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        tag.putInt("AromaAffect.TamingProgress", data.tamingProgress);
        if (data.ownerUUID != null) {
            tag.putString("AromaAffect.OwnerUUID", data.ownerUUID.toString());
        }
        tag.putBoolean("AromaAffect.HasSaddle", !data.saddleItem.isEmpty());
        if (!data.decorationItem.isEmpty()
                && data.decorationItem.getItem() instanceof SnifferNoseItem noseItem) {
            tag.putString("AromaAffect.DecorationId", noseItem.getItemId());
        }
        tag.putBoolean("AromaAffect.HasOverworldScent", data.hasOverworldScent);
        tag.putBoolean("AromaAffect.HasNetherScent", data.hasNetherScent);
        tag.putBoolean("AromaAffect.HasEndScent", data.hasEndScent);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        data.tamingProgress = tag.getInt("AromaAffect.TamingProgress");
        if (tag.contains("AromaAffect.OwnerUUID", Tag.TAG_STRING)) {
            try {
                data.ownerUUID = UUID.fromString(tag.getString("AromaAffect.OwnerUUID"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (tag.contains("AromaAffect.HasSaddle", Tag.TAG_BYTE)
                && tag.getBoolean("AromaAffect.HasSaddle")) {
            data.saddleItem = new ItemStack(Items.SADDLE);
        }
        if (tag.contains("AromaAffect.DecorationId", Tag.TAG_STRING)) {
            String id = tag.getString("AromaAffect.DecorationId");
            SnifferNoseRegistry.getSnifferNose(id)
                    .ifPresent(nose -> data.decorationItem = new ItemStack(nose));
        }
        data.hasOverworldScent =
                tag.contains("AromaAffect.HasOverworldScent", Tag.TAG_BYTE)
                        && tag.getBoolean("AromaAffect.HasOverworldScent");
        data.hasNetherScent =
                tag.contains("AromaAffect.HasNetherScent", Tag.TAG_BYTE)
                        && tag.getBoolean("AromaAffect.HasNetherScent");
        data.hasEndScent =
                tag.contains("AromaAffect.HasEndScent", Tag.TAG_BYTE)
                        && tag.getBoolean("AromaAffect.HasEndScent");
    }
}
