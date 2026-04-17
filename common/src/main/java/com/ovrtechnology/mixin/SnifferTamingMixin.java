package com.ovrtechnology.mixin;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.sniffer.SnifferContainer;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferTamingData;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import com.ovrtechnology.entity.sniffer.config.SnifferConfig;
import com.ovrtechnology.entity.sniffer.config.SnifferConfigLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.AnimationState;
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

    @Unique
    private int aromaaffect$waterTicks = 0;

    @Unique
    private boolean aromaaffect$isSwimming = false;

    @Unique
    private static final TagKey<Block> ENHANCED_SNIFFER_DIGGABLE = TagKey.create(
            Registries.BLOCK,
            Ids.mod("enhanced_sniffer_diggable")
    );

    /**
     * Gets the current sniffer configuration.
     */
    @Unique
    private static SnifferConfig aromaaffect$getConfig() {
        return SnifferConfigLoader.getConfig();
    }



    @Shadow
    public abstract Sniffer.State getState();

    @Shadow
    @Final
    private static EntityDataAccessor<Sniffer.State> DATA_STATE;

    @Shadow
    @Final
    private static EntityDataAccessor<Integer> DATA_DROP_SEED_AT_TICK;

    @Shadow
    public final AnimationState diggingAnimationState = new AnimationState();

    protected SnifferTamingMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Sniffer self = (Sniffer) (Object) this;
        ItemStack itemStack = player.getItemInHand(hand);
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        // If already tamed
        if (data.ownerUUID != null) {
            if (player.isShiftKeyDown()) {
                // Shift + right-click with saddle: equip saddle directly
                if (itemStack.is(Items.SADDLE) && data.saddleItem.isEmpty()) {
                    if (!self.level().isClientSide()) {
                        data.saddleItem = itemStack.copy();
                        data.saddleItem.setCount(1);
                        if (!player.getAbilities().instabuild) {
                            itemStack.shrink(1);
                        }
                        self.playSound(SoundEvents.HORSE_SADDLE.value(), 1.0F, 1.0F);
                        SnifferContainer container = new SnifferContainer(self);
                        container.setChanged();
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                // Shift + right-click with sniffer nose: equip nose directly
                if (itemStack.getItem() instanceof SnifferNoseItem && data.decorationItem.isEmpty()) {
                    if (!self.level().isClientSide()) {
                        data.decorationItem = itemStack.copy();
                        data.decorationItem.setCount(1);
                        if (!player.getAbilities().instabuild) {
                            itemStack.shrink(1);
                        }
                        self.playSound(SoundEvents.HORSE_ARMOR.value(), 1.0F, 1.0F);
                        SnifferContainer container = new SnifferContainer(self);
                        container.setChanged();
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                // Shift + right-click with empty hand: open inventory
                if (itemStack.isEmpty()) {
                    if (!self.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                        SnifferMenuRegistry.openSnifferMenu(serverPlayer, self);
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
            }

            // Right-click: mount with empty hand
            if (itemStack.isEmpty() && !self.isVehicle()) {
                if (!self.level().isClientSide()) {
                    player.startRiding(self);
                    // Reset Sniffer state to IDLING to prevent weird positions
                    self.transitionTo(Sniffer.State.IDLING);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            return;
        }

        // Taming logic with torch flowers (only if not tamed)
        if (itemStack.is(Items.TORCHFLOWER)) {
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            data.tamingProgress++;

            if (self.level() instanceof ServerLevel serverLevel) {
                int torchflowersNeeded = aromaaffect$getConfig().taming.torchflowersNeeded;
                if (data.tamingProgress >= torchflowersNeeded) {
                    // Taming successful!
                    data.ownerUUID = player.getUUID();

                    // Sync to all nearby players
                    for (ServerPlayer nearbyPlayer : serverLevel.players()) {
                        if (nearbyPlayer.distanceToSqr(self) <= 128 * 128) {
                            SnifferEquipmentNetworking.sendEquipmentSync(nearbyPlayer, self.getUUID(), data);
                        }
                    }

                    // Success particles (hearts)
                    serverLevel.sendParticles(
                            ParticleTypes.HEART,
                            self.getX(), self.getY() + 1.0, self.getZ(),
                            10, 0.5, 0.5, 0.5, 0.1
                    );

                    // Success sound
                    self.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);

                    // Message to player
                    if (player instanceof Player) {
                        player.displayClientMessage(
                                Texts.lit("§aSuccessfully tamed the Sniffer!"),
                                true
                        );
                    }
                } else {
                    // Progress particles
                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            self.getX(), self.getY() + 1.0, self.getZ(),
                            5, 0.4, 0.4, 0.4, 0.02
                    );

                    // Progress message
                    if (player instanceof Player) {
                        player.displayClientMessage(
                                Texts.lit("§6Taming progress: §e" + data.tamingProgress + "§6/§e" + torchflowersNeeded),
                                true
                        );
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

        // Only control if tamed, being ridden AND has saddle
        if (data.ownerUUID != null && self.isVehicle()) {
            Entity passenger = self.getFirstPassenger();
            SnifferContainer container = new SnifferContainer(self);

            if (passenger instanceof Player && container.hasSaddle()) {
                // Stop AI navigation
                self.getNavigation().stop();

                if (self.isInWater()) {
                    aromaaffect$waterTicks++;

                    // Enter swimming mode immediately
                    if (!aromaaffect$isSwimming) {
                        aromaaffect$isSwimming = true;
                    }

                    // Keep DIGGING visual state and block brain every tick
                    self.getEntityData().set(DATA_STATE, Sniffer.State.DIGGING);
                    self.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);

                    // Swimming effects only when the player is actually moving
                    Entity rider = self.getFirstPassenger();
                    boolean isMoving = rider instanceof Player p && (p.zza != 0 || p.xxa != 0);

                    if (isMoving && self.level() instanceof ServerLevel serverLevel) {
                        // Bubble particles every 3 ticks
                        if (self.tickCount % 3 == 0) {
                            serverLevel.sendParticles(
                                    ParticleTypes.BUBBLE,
                                    self.getX(), self.getY() + 0.3, self.getZ(),
                                    3, 0.6, 0.1, 0.6, 0.02
                            );
                        }
                        // Splash particles every 5 ticks
                        if (self.tickCount % 5 == 0) {
                            serverLevel.sendParticles(
                                    ParticleTypes.SPLASH,
                                    self.getX(), self.getY() + 0.5, self.getZ(),
                                    2, 0.5, 0.0, 0.5, 0.1
                            );
                        }
                        // Swimming sound every 15 ticks
                        if (self.tickCount % 15 == 0) {
                            self.playSound(SoundEvents.GENERIC_SWIM, 0.6F, 0.8F + self.getRandom().nextFloat() * 0.4F);
                        }
                    }

                    aromaaffect$handleWaterFloating(self);
                } else {
                    // Exiting water: restore state
                    if (aromaaffect$isSwimming) {
                        aromaaffect$isSwimming = false;
                        this.diggingAnimationState.stop();
                        self.getEntityData().set(DATA_STATE, Sniffer.State.IDLING);
                    }
                    aromaaffect$waterTicks = 0;
                }

                // Adjust step height based on context
                var stepAttr = self.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
                if (stepAttr != null) {
                    var ridingConfig = aromaaffect$getConfig().riding;
                    if (self.isInWater() || aromaaffect$waterTicks > 0) {
                        // Higher step to climb out of water onto shore
                        stepAttr.setBaseValue(ridingConfig.waterExitStepHeight);
                    } else {
                        stepAttr.setBaseValue(ridingConfig.mountedStepHeight);
                    }
                }
            }
        } else {
            // Restore normal step height and swimming state when not mounted
            Sniffer self2 = (Sniffer) (Object) this;
            var stepAttr = self2.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
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

    /**
     * Handles simple water floating for the sniffer.
     */
    @Unique
    private void aromaaffect$handleWaterFloating(Sniffer sniffer) {
        double waterLevel = aromaaffect$getWaterLevel(sniffer);
        var waterConfig = aromaaffect$getConfig().waterFloating;

        if (waterLevel > 0) {
            Vec3 currentMotion = sniffer.getDeltaMovement();

            // Hitting a shore edge while moving forward: boost up to climb out
            if (sniffer.horizontalCollision && (currentMotion.x != 0 || currentMotion.z != 0)) {
                sniffer.setDeltaMovement(currentMotion.x, 0.35, currentMotion.z);
            } else {
                double targetY = waterLevel - waterConfig.floatOffset;
                double currentY = sniffer.getY();

                // Simple buoyancy towards the surface
                double verticalMotion = (targetY - currentY) * waterConfig.buoyancyStrength;
                verticalMotion = Math.max(-waterConfig.maxDownwardSpeed, Math.min(waterConfig.maxUpwardSpeed, verticalMotion));

                if (aromaaffect$isSwimming) {
                    // Override horizontal movement directly to counteract vanilla water drag
                    float yawRad = sniffer.getYRot() * ((float) Math.PI / 180F);
                    Entity rider = sniffer.getFirstPassenger();
                    if (rider instanceof Player player) {
                        float forward = player.zza;
                        float strafe = player.xxa;
                        if (forward != 0 || strafe != 0) {
                            var swimConfig = aromaaffect$getConfig().swimming;
                            double speed = swimConfig.swimSpeed;
                            double motionX = (-Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe) * speed;
                            double motionZ = (Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe) * speed;
                            sniffer.setDeltaMovement(motionX, verticalMotion, motionZ);
                        } else {
                            sniffer.setDeltaMovement(currentMotion.x * 0.9, verticalMotion, currentMotion.z * 0.9);
                        }
                    }
                } else {
                    double drag = waterConfig.horizontalDrag;
                    sniffer.setDeltaMovement(currentMotion.x * drag, verticalMotion, currentMotion.z * drag);
                }
            }

            // Prevent drowning
            sniffer.setAirSupply(sniffer.getMaxAirSupply());
        }
    }

    /**
     * Gets the water level where the sniffer is located.
     */
    @Unique
    private double aromaaffect$getWaterLevel(Sniffer sniffer) {
        BlockPos pos = sniffer.blockPosition();

        // Search for water level from current position upwards
        for (int i = 0; i < 3; i++) {
            BlockPos checkPos = pos.above(i);
            BlockState state = sniffer.level().getBlockState(checkPos);
            BlockState stateAbove = sniffer.level().getBlockState(checkPos.above());

            // If this block is water and the one above is not, we found the surface
            if (state.getFluidState().isSource() && !stateAbove.getFluidState().isSource()) {
                return checkPos.getY() + 1.0; // Water surface
            }
        }

        // If no surface found, use current position
        return sniffer.getY() + 1.0;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        // Rotate sniffer towards where the player is looking
        this.setYRot(player.getYRot());
        this.yRotO = this.getYRot();
        this.setXRot(player.getXRot() * 0.5F);
        this.setRot(this.getYRot(), this.getXRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.yBodyRot;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
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

        // Slower reverse
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }

        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        var ridingConfig = aromaaffect$getConfig().riding;
        float baseSpeed = (float) this.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED
        ) * ridingConfig.landSpeedMultiplier;

        if (this.isInWater()) {
            var swimConfig = aromaaffect$getConfig().swimming;
            // Slow phase before swimming animation kicks in
            if (!aromaaffect$isSwimming) {
                return swimConfig.slowSpeed;
            }
            // Fast swimming once in DIGGING state
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

        // On client: if there's a mounted player and has saddle, allow control
        // (the server already validated mounting in mobInteract)
        // On server: verify that it's tamed, has saddle and is a player
        if (this.getFirstPassenger() instanceof Player player && container.hasSaddle()) {
            // On server we verify ownerUUID, on client we trust the server validated
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

        // Only open if tamed
        if (data.ownerUUID != null && !self.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            SnifferMenuRegistry.openSnifferMenu(serverPlayer, self);
        }
    }

    @Inject(method = "canDig(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$canDigEnhanced(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());
        SnifferContainer container = new SnifferContainer(self);

        // If it has a nose equipped, it can dig on any solid block
        if (data.ownerUUID != null && container.hasSnifferNose()) {
            BlockState blockState = self.level().getBlockState(pos.below());
            if (blockState.isSolid()) {
                cir.setReturnValue(true);
                return;
            }
        }

        // Any sniffer can dig on Nether/End blocks (to obtain scents)
        BlockState blockState = self.level().getBlockState(pos.below());
        if (blockState.is(ENHANCED_SNIFFER_DIGGABLE)) {
            cir.setReturnValue(true);
        }
    }

    // ==========================================
    // DIMENSIONAL SCENTS SYSTEM
    // ==========================================
    // Each scent is obtained:
    // 1. Only once per sniffer
    // 2. In its respective dimension
    // 3. Without needing to be tamed or have a nose
    // 4. With 100% probability if not yet obtained

    // ==========================================
    // BONANZA SYSTEM WITH ENHANCED NOSE
    // ==========================================
    // When it has the nose equipped, in ONE excavation it drops EVERYTHING together:
    // - 5-15 of EACH mineral (copper, iron, gold, emerald, diamond, netherite)
    // - 1-5 scent_base
    // - 1-2 seeds (torchflower/pitcher)

    /**
     * Intercepts the Sniffer drop to handle:
     * 1. Dimensional scents (100% if not obtained)
     * 2. Mineral bonanza when nose is equipped
     */
    @Inject(method = "dropSeed", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$onDropSeed(CallbackInfo ci) {
        Sniffer self = (Sniffer) (Object) this;

        // Don't drop items when DIGGING state is used for swimming animation
        if (aromaaffect$isSwimming) {
            ci.cancel();
            return;
        }

        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Verify we're on the correct tick for the drop
        int dropSeedAtTick = self.getEntityData().get(DATA_DROP_SEED_AT_TICK);
        if (dropSeedAtTick != self.tickCount) {
            return; // Not the drop tick, let vanilla handle it
        }

        SnifferTamingData data = SnifferTamingData.get(self.getUUID());
        ResourceKey<Level> dimension = serverLevel.dimension();

        // ========================================
        // STEP 1: Check dimensional scents
        // ========================================
        ItemStack scentToDrop = aromaaffect$getScentForDimension(dimension, data);

        if (!scentToDrop.isEmpty()) {
            // Drop the scent at the sniffer's head position
            aromaaffect$dropItemAtHead(self, serverLevel, scentToDrop);

            // Mark that it already has this scent
            aromaaffect$markScentObtained(dimension, data);

            AromaAffect.LOGGER.debug("Sniffer {} obtained {} scent",
                    self.getUUID(), dimension.location().getPath());

            // Check if all scents completed (for advancement)
            if (data.hasAllScents()) {
                aromaaffect$grantSnifferJourneyAdvancement(self, data);
            }

            // Cancel vanilla method
            ci.cancel();
            return;
        }

        // ========================================
        // STEP 2: Bonanza with Enhanced Nose
        // ========================================
        SnifferContainer container = new SnifferContainer(self);

        // Only if tamed AND has Enhanced Sniffer Nose equipped AND bonanza is enabled
        if (data.ownerUUID != null && container.hasSnifferNose() && aromaaffect$getConfig().bonanza.enabled) {
            // BONANZA! Drop all items together
            aromaaffect$dropBonanza(self, serverLevel);
            ci.cancel();
            return;
        }
        // If no nose or bonanza disabled, vanilla drops normal seed
    }

    /**
     * Drops the complete bonanza of items when the nose is equipped.
     * All values are configurable via sniffer_config.json
     */
    @Unique
    private void aromaaffect$dropBonanza(Sniffer sniffer, ServerLevel serverLevel) {
        BlockPos headPos = aromaaffect$getHeadBlock(sniffer);
        var random = sniffer.getRandom();
        var bonanzaConfig = aromaaffect$getConfig().bonanza;

        // ========== MINERALS (configurable) ==========
        for (SnifferConfig.MineralEntry mineral : bonanzaConfig.minerals) {
            if (!mineral.enabled) continue;

            int count = mineral.min + random.nextInt(mineral.max - mineral.min + 1);
            ResourceLocation itemId = Ids.parse(mineral.item);
            BuiltInRegistries.ITEM.getOptional(itemId).ifPresent(item -> {
                aromaaffect$dropItemStackAtHead(sniffer, serverLevel, headPos, new ItemStack(item, count));
            });
        }

        // ========== SCENT BASE (configurable) ==========
        if (bonanzaConfig.scentBase.enabled) {
            int scentBaseCount = bonanzaConfig.scentBase.min +
                    random.nextInt(bonanzaConfig.scentBase.max - bonanzaConfig.scentBase.min + 1);
            ResourceLocation scentBaseId = Ids.parse(bonanzaConfig.scentBase.item);
            BuiltInRegistries.ITEM.getOptional(scentBaseId).ifPresent(item -> {
                aromaaffect$dropItemStackAtHead(sniffer, serverLevel, headPos, new ItemStack(item, scentBaseCount));
            });
        }

        // ========== SEEDS (configurable) ==========
        if (bonanzaConfig.seeds.enabled && !bonanzaConfig.seeds.items.isEmpty()) {
            int seedCount = bonanzaConfig.seeds.min +
                    random.nextInt(bonanzaConfig.seeds.max - bonanzaConfig.seeds.min + 1);
            // Pick random seed from list
            String seedItemId = bonanzaConfig.seeds.items.get(random.nextInt(bonanzaConfig.seeds.items.size()));
            ResourceLocation seedId = Ids.parse(seedItemId);
            BuiltInRegistries.ITEM.getOptional(seedId).ifPresent(item -> {
                aromaaffect$dropItemStackAtHead(sniffer, serverLevel, headPos, new ItemStack(item, seedCount));
            });
        }

        // Normal drop sound
        sniffer.playSound(SoundEvents.SNIFFER_DROP_SEED, 1.0F, 1.0F);
    }

    /**
     * Drops an ItemStack at the sniffer's head position.
     */
    @Unique
    private void aromaaffect$dropItemStackAtHead(Sniffer sniffer, ServerLevel serverLevel, BlockPos headPos, ItemStack itemStack) {
        // Add some scatter so items don't all fall at the exact same point
        double offsetX = (sniffer.getRandom().nextDouble() - 0.5) * 0.5;
        double offsetZ = (sniffer.getRandom().nextDouble() - 0.5) * 0.5;

        ItemEntity itemEntity = new ItemEntity(
                sniffer.level(),
                headPos.getX() + offsetX,
                headPos.getY(),
                headPos.getZ() + offsetZ,
                itemStack
        );
        itemEntity.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(itemEntity);
    }

    /**
     * Drops an item at the sniffer's head position with sound.
     */
    @Unique
    private void aromaaffect$dropItemAtHead(Sniffer sniffer, ServerLevel serverLevel, ItemStack itemStack) {
        BlockPos headPos = aromaaffect$getHeadBlock(sniffer);
        ItemEntity itemEntity = new ItemEntity(
                sniffer.level(),
                headPos.getX(),
                headPos.getY(),
                headPos.getZ(),
                itemStack
        );
        itemEntity.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(itemEntity);

        // Play sound (same as vanilla)
        sniffer.playSound(SoundEvents.SNIFFER_DROP_SEED, 1.0F, 1.0F);
    }

    /**
     * Gets the sniffer's head position (replicates private getHeadBlock logic).
     */
    @Unique
    private BlockPos aromaaffect$getHeadBlock(Sniffer sniffer) {
        Vec3 headPos = sniffer.position().add(sniffer.getForward().scale(2.25));
        return BlockPos.containing(headPos.x(), sniffer.getY() + 0.2, headPos.z());
    }


    /**
     * Gets the scent corresponding to the dimension if the sniffer doesn't have it.
     */
    @Unique
    private ItemStack aromaaffect$getScentForDimension(ResourceKey<Level> dimension, SnifferTamingData data) {
        var scentsConfig = aromaaffect$getConfig().dimensionalScents;

        if (dimension == Level.OVERWORLD && !data.hasOverworldScent && scentsConfig.overworld.enabled) {
            ResourceLocation itemId = Ids.parse(scentsConfig.overworld.item);
            return BuiltInRegistries.ITEM.getOptional(itemId)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        } else if (dimension == Level.NETHER && !data.hasNetherScent && scentsConfig.nether.enabled) {
            ResourceLocation itemId = Ids.parse(scentsConfig.nether.item);
            return BuiltInRegistries.ITEM.getOptional(itemId)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        } else if (dimension == Level.END && !data.hasEndScent && scentsConfig.end.enabled) {
            ResourceLocation itemId = Ids.parse(scentsConfig.end.item);
            return BuiltInRegistries.ITEM.getOptional(itemId)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Marks the scent as obtained for the corresponding dimension.
     */
    @Unique
    private void aromaaffect$markScentObtained(ResourceKey<Level> dimension, SnifferTamingData data) {
        if (dimension == Level.OVERWORLD) {
            data.hasOverworldScent = true;
        } else if (dimension == Level.NETHER) {
            data.hasNetherScent = true;
        } else if (dimension == Level.END) {
            data.hasEndScent = true;
        }
    }

    // Sniffer drops are now handled via loot table at:
    // data/minecraft/loot_table/gameplay/sniffer_digging.json

    @Inject(method = "onDiggingComplete", at = @At("TAIL"))
    private void aromaaffect$onFinishDigging(boolean found, CallbackInfoReturnable<Sniffer> cir) {
        Sniffer self = (Sniffer) (Object) this;

        // Don't process digging logic when swimming
        if (aromaaffect$isSwimming) {
            return;
        }

        // Only process on server
        if (self.level().isClientSide()) {
            return;
        }

        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        // Only if tamed and has enhanced nose
        if (data.ownerUUID == null) {
            return;
        }

        SnifferContainer container = new SnifferContainer(self);
        if (!container.hasSnifferNose()) {
            return;
        }

        // Reset search cooldown so it searches again quickly
        self.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
        self.getBrain().eraseMemory(MemoryModuleType.SNIFF_COOLDOWN);
        self.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE,
                aromaaffect$getConfig().digging.sniffCooldownWithNose);
    }

    @Unique
    private static final ResourceLocation SNIFFER_JOURNEY_ADVANCEMENT =
            Ids.mod("sniffer_journey");

    @Unique
    private void aromaaffect$grantSnifferJourneyAdvancement(Sniffer sniffer, SnifferTamingData data) {
        // Grant advancement to owner if online
        if (data.ownerUUID != null && sniffer.level() instanceof ServerLevel serverLevel) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null) {
                // Grant the advancement
                AdvancementHolder advancementHolder = serverLevel.getServer().getAdvancements()
                        .get(SNIFFER_JOURNEY_ADVANCEMENT);

                if (advancementHolder != null) {
                    AdvancementProgress progress = owner.getAdvancements().getOrStartProgress(advancementHolder);
                    if (!progress.isDone()) {
                        // Complete all criteria
                        for (String criterion : progress.getRemainingCriteria()) {
                            owner.getAdvancements().award(advancementHolder, criterion);
                        }
                    }
                }

                // Special celebration effects
                serverLevel.sendParticles(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        sniffer.getX(), sniffer.getY() + 1.5, sniffer.getZ(),
                        50, 1.0, 1.0, 1.0, 0.5
                );
                sniffer.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        // Drop saddle if equipped
        if (!data.saddleItem.isEmpty()) {
            self.spawnAtLocation(level, data.saddleItem.copy());
            data.saddleItem = ItemStack.EMPTY;
        }

        // Drop nose if equipped
        if (!data.decorationItem.isEmpty()) {
            self.spawnAtLocation(level, data.decorationItem.copy());
            data.decorationItem = ItemStack.EMPTY;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        output.putInt("AromaAffect.TamingProgress", data.tamingProgress);
        if (data.ownerUUID != null) {
            output.putString("AromaAffect.OwnerUUID", data.ownerUUID.toString());
        }
        output.putBoolean("AromaAffect.HasSaddle", !data.saddleItem.isEmpty());
        if (!data.decorationItem.isEmpty() && data.decorationItem.getItem() instanceof SnifferNoseItem noseItem) {
            output.putString("AromaAffect.DecorationId", noseItem.getItemId());
        }
        output.putBoolean("AromaAffect.HasOverworldScent", data.hasOverworldScent);
        output.putBoolean("AromaAffect.HasNetherScent", data.hasNetherScent);
        output.putBoolean("AromaAffect.HasEndScent", data.hasEndScent);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        data.tamingProgress = input.getIntOr("AromaAffect.TamingProgress", 0);
        input.getString("AromaAffect.OwnerUUID").ifPresent(s -> data.ownerUUID = UUID.fromString(s));
        if (input.getBooleanOr("AromaAffect.HasSaddle", false)) {
            data.saddleItem = new ItemStack(Items.SADDLE);
        }
        input.getString("AromaAffect.DecorationId").ifPresent(id ->
                SnifferNoseRegistry.getSnifferNose(id).ifPresent(nose ->
                        data.decorationItem = new ItemStack(nose)
                )
        );
        data.hasOverworldScent = input.getBooleanOr("AromaAffect.HasOverworldScent", false);
        data.hasNetherScent = input.getBooleanOr("AromaAffect.HasNetherScent", false);
        data.hasEndScent = input.getBooleanOr("AromaAffect.HasEndScent", false);
    }

}