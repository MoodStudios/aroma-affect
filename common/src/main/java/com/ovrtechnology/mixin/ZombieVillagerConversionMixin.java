package com.ovrtechnology.mixin;

import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.entity.nosesmith.NoseSmithZombieMarker;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When a Nose Smith is zombified, vanilla creates a regular {@link ZombieVillager}.
 * Curing that zombie villager normally produces a plain {@link net.minecraft.world.entity.npc.Villager},
 * losing all Nose Smith data. This mixin:
 * <ol>
 *   <li>Persists a "was Nose Smith" flag and the Nose Smith's quest state in NBT.</li>
 *   <li>Redirects the cure conversion to create a {@link NoseSmithEntity} instead
 *       of a generic Villager when the flag is set.</li>
 * </ol>
 */
@Mixin(ZombieVillager.class)
public class ZombieVillagerConversionMixin implements NoseSmithZombieMarker {

    @Unique
    private boolean aromaaffect$wasNoseSmith = false;
    @Unique
    private boolean aromaaffect$noseSmithHasNose = true;
    @Unique
    private String aromaaffect$noseSmithRequestedFlower = "";
    @Unique
    private boolean aromaaffect$noseSmithHouseDecorated = false;
    @Unique
    private long aromaaffect$noseSmithNoseRemovedGameTime = -1L;

    @Override
    public void aromaaffect$markAsNoseSmith(boolean hasNose, String requestedFlower, boolean houseDecorated, long noseRemovedGameTime) {
        this.aromaaffect$wasNoseSmith = true;
        this.aromaaffect$noseSmithHasNose = hasNose;
        this.aromaaffect$noseSmithRequestedFlower = requestedFlower != null ? requestedFlower : "";
        this.aromaaffect$noseSmithHouseDecorated = houseDecorated;
        this.aromaaffect$noseSmithNoseRemovedGameTime = noseRemovedGameTime;
    }

    // ── NBT persistence ──────────────────────────────────────────────────

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aromaaffect$saveNoseSmithData(ValueOutput output, CallbackInfo ci) {
        if (aromaaffect$wasNoseSmith) {
            output.putBoolean("aromaaffect:WasNoseSmith", true);
            output.putBoolean("aromaaffect:NoseSmithHasNose", aromaaffect$noseSmithHasNose);
            output.putString("aromaaffect:NoseSmithRequestedFlower", aromaaffect$noseSmithRequestedFlower);
            output.putBoolean("aromaaffect:NoseSmithHouseDecorated", aromaaffect$noseSmithHouseDecorated);
            if (aromaaffect$noseSmithNoseRemovedGameTime >= 0) {
                output.putString("aromaaffect:NoseSmithNoseRemovedGameTime", Long.toString(aromaaffect$noseSmithNoseRemovedGameTime));
            }
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aromaaffect$loadNoseSmithData(ValueInput input, CallbackInfo ci) {
        aromaaffect$wasNoseSmith = input.getBooleanOr("aromaaffect:WasNoseSmith", false);
        if (aromaaffect$wasNoseSmith) {
            aromaaffect$noseSmithHasNose = input.getBooleanOr("aromaaffect:NoseSmithHasNose", true);
            aromaaffect$noseSmithRequestedFlower = input.getString("aromaaffect:NoseSmithRequestedFlower").orElse("");
            aromaaffect$noseSmithHouseDecorated = input.getBooleanOr("aromaaffect:NoseSmithHouseDecorated", false);
            aromaaffect$noseSmithNoseRemovedGameTime = input.getString("aromaaffect:NoseSmithNoseRemovedGameTime")
                    .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return -1L; } })
                    .orElse(-1L);
        }
    }

    // ── Cure redirect ────────────────────────────────────────────────────

    @Redirect(
            method = "finishConversion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/ZombieVillager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"
            )
    )
    @Nullable
    private <T extends Mob> T aromaaffect$redirectConversion(
            ZombieVillager self,
            EntityType<T> entityType,
            ConversionParams params,
            ConversionParams.AfterConversion<T> afterConversion
    ) {
        if (!aromaaffect$wasNoseSmith) {
            return self.convertTo(entityType, params, afterConversion);
        }

        @SuppressWarnings("unchecked")
        EntityType<T> noseSmithType = (EntityType<T>) (EntityType<?>) NoseSmithRegistry.getNOSE_SMITH().get();

        return self.convertTo(noseSmithType, params, entity -> {
            afterConversion.finalizeConversion(entity);
            if (entity instanceof NoseSmithEntity noseSmith) {
                noseSmith.restoreNoseSmithData(
                        aromaaffect$noseSmithHasNose,
                        aromaaffect$noseSmithRequestedFlower,
                        aromaaffect$noseSmithHouseDecorated,
                        aromaaffect$noseSmithNoseRemovedGameTime
                );
            }
        });
    }
}
