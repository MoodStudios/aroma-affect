package com.ovrtechnology.mixin;

import com.ovrtechnology.sniffer.SnifferContainer;
import com.ovrtechnology.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.sniffer.SnifferTamingData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sniffer.class)
public abstract class SnifferTamingMixin extends Animal implements HasCustomInventoryScreen {

    @Unique
    private static final int TORCH_FLOWERS_NEEDED = 4;

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

}