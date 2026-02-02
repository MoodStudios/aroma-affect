package com.ovrtechnology.mixin;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.sniffer.SnifferContainer;
import com.ovrtechnology.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.sniffer.SnifferTamingData;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
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
    private static final double DIMENSION_SCENT_CHANCE = 0.85; // 85% de probabilidad

    @Unique
    private static final TagKey<Block> ENHANCED_SNIFFER_DIGGABLE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "enhanced_sniffer_diggable")
    );

    @Unique
    private static final int SCENT_DROP_DELAY_TICKS = 25; // ~1.25 segundos de delay para sincronizar con animación

    @Unique
    private static final double ENHANCED_SEARCH_CHANCE = 0.005; // 0.5% por tick = busca mucho más seguido (~10 segundos promedio)

    // Campos para el drop con delay
    @Unique
    private ItemStack aromaaffect$pendingScentDrop = null;
    @Unique
    private int aromaaffect$scentDropCountdown = 0;
    @Unique
    private double aromaaffect$pendingDropX = 0;
    @Unique
    private double aromaaffect$pendingDropY = 0;
    @Unique
    private double aromaaffect$pendingDropZ = 0;
    @Unique
    private ResourceKey<Level> aromaaffect$pendingDimension = null;

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

        // Búsqueda más frecuente cuando tiene la nariz mejorada equipada
        if (!self.level().isClientSide() && data.ownerUUID != null && !self.isVehicle()) {
            SnifferContainer noseContainer = new SnifferContainer(self);
            if (noseContainer.hasSnifferNose()) {
                // Si está en IDLING, hay chance de que empiece a buscar
                if (getState() == Sniffer.State.IDLING && self.getRandom().nextDouble() < ENHANCED_SEARCH_CHANCE) {
                    // Transicionar a SCENTING para que empiece a buscar
                    self.transitionTo(Sniffer.State.SCENTING);
                }
            }
        }

        // Manejar el drop de esencia con delay
        if (!self.level().isClientSide() && aromaaffect$pendingScentDrop != null) {
            aromaaffect$scentDropCountdown--;

            if (aromaaffect$scentDropCountdown <= 0) {
                // Ejecutar el drop
                aromaaffect$executeDelayedScentDrop(self, data);
            }
        }
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

        // Solo si está domado y tiene la nariz mejorada
        if (data.ownerUUID == null) {
            return;
        }

        SnifferContainer container = new SnifferContainer(self);
        if (!container.hasSnifferNose()) {
            return;
        }

        // Verificar si el bloque está en nuestro tag personalizado
        BlockState blockState = self.level().getBlockState(pos.below());
        if (blockState.is(ENHANCED_SNIFFER_DIGGABLE)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "dropSeed", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$onDropSeed(CallbackInfo ci) {
        Sniffer self = (Sniffer) (Object) this;

        // Solo procesar en el servidor
        if (self.level().isClientSide()) {
            return;
        }

        // Intentar reemplazar el drop con una esencia de dimensión
        if (aromaaffect$tryReplaceSeedWithScent(self)) {
            ci.cancel(); // Cancelar el drop normal, ya dropeamos la esencia
        }
    }

    @Inject(method = "finishDigging", at = @At("TAIL"))
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

        // Limpiar el cooldown de búsqueda para que pueda buscar de nuevo rápidamente
        self.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_COOLDOWN);
    }

    @Unique
    private boolean aromaaffect$tryReplaceSeedWithScent(Sniffer sniffer) {
        SnifferTamingData data = SnifferTamingData.get(sniffer.getUUID());

        // Requisitos: domado y con enhanced sniffer nose
        if (data.ownerUUID == null) {
            return false;
        }

        SnifferContainer container = new SnifferContainer(sniffer);
        if (!container.hasSnifferNose()) {
            return false;
        }

        // Determinar la dimensión actual
        ResourceKey<Level> dimension = sniffer.level().dimension();
        String scentId = null;
        boolean alreadyHasScent = false;

        if (dimension == Level.OVERWORLD) {
            alreadyHasScent = data.hasOverworldScent;
            if (!alreadyHasScent) {
                scentId = "overworld_scent";
            }
        } else if (dimension == Level.NETHER) {
            alreadyHasScent = data.hasNetherScent;
            if (!alreadyHasScent) {
                scentId = "nether_scent";
            }
        } else if (dimension == Level.END) {
            alreadyHasScent = data.hasEndScent;
            if (!alreadyHasScent) {
                scentId = "end_scent";
            }
        }

        // Si ya tiene la esencia de esta dimensión, no reemplazar
        if (scentId == null || alreadyHasScent) {
            return false;
        }

        // Probabilidad de obtener esencia (85%)
        if (sniffer.getRandom().nextDouble() > DIMENSION_SCENT_CHANCE) {
            return false;
        }

        // Obtener el item de esencia y dropearlo
        if (!(sniffer.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        final ResourceKey<Level> finalDimension = dimension;
        var scentItemOpt = ScentItemRegistry.getScentItem(scentId);

        if (scentItemOpt.isEmpty()) {
            return false;
        }

        ItemStack scentStack = new ItemStack(scentItemOpt.get());

        // Calcular posición 2 bloques adelante del Sniffer (donde mete la nariz)
        double yawRad = Math.toRadians(sniffer.getYRot());
        double offsetX = -Math.sin(yawRad) * 2.0;
        double offsetZ = Math.cos(yawRad) * 2.0;

        // Programar el drop con delay para sincronizar con la animación
        aromaaffect$pendingScentDrop = scentStack;
        aromaaffect$scentDropCountdown = SCENT_DROP_DELAY_TICKS;
        aromaaffect$pendingDropX = sniffer.getX() + offsetX;
        aromaaffect$pendingDropY = sniffer.getY() + 0.5;
        aromaaffect$pendingDropZ = sniffer.getZ() + offsetZ;
        aromaaffect$pendingDimension = finalDimension;

        return true; // Indicar que reemplazamos el drop (el item saldrá después del delay)
    }

    @Unique
    private void aromaaffect$executeDelayedScentDrop(Sniffer sniffer, SnifferTamingData data) {
        if (aromaaffect$pendingScentDrop == null || !(sniffer.level() instanceof ServerLevel serverLevel)) {
            aromaaffect$pendingScentDrop = null;
            return;
        }

        // Crear y spawnear el item en la posición guardada
        ItemEntity itemEntity = new ItemEntity(
                serverLevel,
                aromaaffect$pendingDropX,
                aromaaffect$pendingDropY,
                aromaaffect$pendingDropZ,
                aromaaffect$pendingScentDrop
        );
        itemEntity.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(itemEntity);

        // Marcar como obtenida
        if (aromaaffect$pendingDimension == Level.OVERWORLD) {
            data.hasOverworldScent = true;
        } else if (aromaaffect$pendingDimension == Level.NETHER) {
            data.hasNetherScent = true;
        } else if (aromaaffect$pendingDimension == Level.END) {
            data.hasEndScent = true;
        }

        // Efectos especiales en la posición donde salió el item
        serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                aromaaffect$pendingDropX, aromaaffect$pendingDropY + 0.5, aromaaffect$pendingDropZ,
                15, 0.3, 0.3, 0.3, 0.1
        );
        sniffer.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.2F);

        // Verificar si completó las 3 esencias para el advancement
        if (data.hasAllScents()) {
            aromaaffect$grantSnifferJourneyAdvancement(sniffer, data);
        }

        // Limpiar el drop pendiente
        aromaaffect$pendingScentDrop = null;
        aromaaffect$pendingDimension = null;
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

}