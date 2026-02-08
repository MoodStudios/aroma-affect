package com.ovrtechnology.mixin;

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
import net.minecraft.network.chat.Component;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
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
    private static final int TORCH_FLOWERS_NEEDED = 4;

    @Unique
    private static final TagKey<Block> ENHANCED_SNIFFER_DIGGABLE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "enhanced_sniffer_diggable")
    );


    @Shadow
    public abstract Sniffer.State getState();

    protected SnifferTamingMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Sniffer self = (Sniffer) (Object) this;
        ItemStack itemStack = player.getItemInHand(hand);
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        // Si ya está domado
        if (data.ownerUUID != null) {
            // Montar con mano vacía
            if (itemStack.isEmpty() && !self.isVehicle()) {
                if (!self.level().isClientSide()) {
                    player.startRiding(self);
                    // Resetear el estado del Sniffer a IDLING para que no se mueva en posiciones raras
                    self.transitionTo(Sniffer.State.IDLING);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            return;
        }

        // Lógica de doma con torch flowers (solo si no está domado)
        if (itemStack.is(Items.TORCHFLOWER)) {
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            data.tamingProgress++;

            if (self.level() instanceof ServerLevel serverLevel) {
                if (data.tamingProgress >= TORCH_FLOWERS_NEEDED) {
                    // ¡Doma exitosa!
                    data.ownerUUID = player.getUUID();

                    // Partículas de éxito (corazones)
                    serverLevel.sendParticles(
                            ParticleTypes.HEART,
                            self.getX(), self.getY() + 1.0, self.getZ(),
                            10, 0.5, 0.5, 0.5, 0.1
                    );

                    // Sonido de éxito
                    self.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);

                    // Mensaje al jugador
                    if (player instanceof Player) {
                        player.displayClientMessage(
                                Component.literal("§aSuccessfully tamed the Sniffer!"),
                                true
                        );
                    }
                } else {
                    // Partículas de progreso
                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            self.getX(), self.getY() + 1.0, self.getZ(),
                            5, 0.4, 0.4, 0.4, 0.02
                    );

                    // Mensaje de progreso
                    if (player instanceof Player) {
                        player.displayClientMessage(
                                Component.literal("§6Taming progress: §e" + data.tamingProgress + "§6/§e" + TORCH_FLOWERS_NEEDED),
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

        // Solo controlar si está domado, siendo montado Y tiene silla
        if (data.ownerUUID != null && self.isVehicle()) {
            Entity passenger = self.getFirstPassenger();
            SnifferContainer container = new SnifferContainer(self);

            if (passenger instanceof Player && container.hasSaddle()) {
                // Detener navegación IA
                self.getNavigation().stop();

                // Mantener al Sniffer en estado IDLING mientras está montado
                self.transitionTo(Sniffer.State.IDLING);
            }
        }
        // YA NO NECESITAMOS MANEJAR EL DROP CON DELAY
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        // Rotar sniffer hacia donde mira el jugador
        this.setYRot(player.getYRot());
        this.yRotO = this.getYRot();
        this.setXRot(player.getXRot() * 0.5F);
        this.setRot(this.getYRot(), this.getXRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.yBodyRot;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float forward = player.zza;
        float strafe = player.xxa * 0.5F;

        // Reversa más lenta
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }

        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED
        ) * 0.8F;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());
        SnifferContainer container = new SnifferContainer(self);

        // Solo permite control si está domado, tiene silla y es jugador
        if (data.ownerUUID != null && this.getFirstPassenger() instanceof Player player && container.hasSaddle()) {
            return player;
        }
        return null;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        // Solo abrir si está domado y el jugador está montado
        if (data.ownerUUID != null && !self.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            SnifferMenuRegistry.openSnifferMenu(serverPlayer, self);
        }
    }

    @Inject(method = "canDig(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$canDigEnhanced(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Sniffer self = (Sniffer) (Object) this;
        SnifferTamingData data = SnifferTamingData.get(self.getUUID());
        SnifferContainer container = new SnifferContainer(self);

        // Si tiene nariz equipada, puede excavar en cualquier bloque sólido
        if (data.ownerUUID != null && container.hasSnifferNose()) {
            BlockState blockState = self.level().getBlockState(pos.below());
            if (blockState.isSolid()) {
                cir.setReturnValue(true);
                return;
            }
        }

        // Cualquier sniffer puede excavar en bloques del Nether/End (para obtener esencias)
        BlockState blockState = self.level().getBlockState(pos.below());
        if (blockState.is(ENHANCED_SNIFFER_DIGGABLE)) {
            cir.setReturnValue(true);
        }
    }

    // Los drops del sniffer ahora se manejan con loot table en:
    // data/minecraft/loot_table/gameplay/sniffer_digging.json

    @Inject(method = "onDiggingComplete", at = @At("TAIL"))
    private void aromaaffect$onFinishDigging(boolean found, CallbackInfoReturnable<Sniffer> cir) {
        Sniffer self = (Sniffer) (Object) this;

        // Solo procesar en servidor
        if (self.level().isClientSide()) {
            return;
        }

        SnifferTamingData data = SnifferTamingData.get(self.getUUID());

        // Solo si está domado y tiene la nariz mejorada
        if (data.ownerUUID == null) {
            return;
        }

        SnifferContainer container = new SnifferContainer(self);
        if (!container.hasSnifferNose()) {
            return;
        }

        // Resetear el cooldown de búsqueda para que vuelva a buscar rápido
        self.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
        self.getBrain().eraseMemory(MemoryModuleType.SNIFF_COOLDOWN);
        self.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 200L);
    }

    @Unique
    private static final ResourceLocation SNIFFER_JOURNEY_ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "sniffer_journey");

    @Unique
    private void aromaaffect$grantSnifferJourneyAdvancement(Sniffer sniffer, SnifferTamingData data) {
        // Otorgar advancement al dueño si está online
        if (data.ownerUUID != null && sniffer.level() instanceof ServerLevel serverLevel) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null) {
                // Otorgar el advancement
                AdvancementHolder advancementHolder = serverLevel.getServer().getAdvancements()
                        .get(SNIFFER_JOURNEY_ADVANCEMENT);

                if (advancementHolder != null) {
                    AdvancementProgress progress = owner.getAdvancements().getOrStartProgress(advancementHolder);
                    if (!progress.isDone()) {
                        // Completar todos los criterios
                        for (String criterion : progress.getRemainingCriteria()) {
                            owner.getAdvancements().award(advancementHolder, criterion);
                        }
                    }
                }

                // Efectos especiales de celebración
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

        // Soltar la saddle si tiene
        if (!data.saddleItem.isEmpty()) {
            self.spawnAtLocation(level, data.saddleItem.copy());
            data.saddleItem = ItemStack.EMPTY;
        }

        // Soltar la nariz si tiene
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