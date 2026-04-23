package com.ovrtechnology.mixin;

import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.entity.nosesmith.NoseSmithZombieMarker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombieVillager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieVillager.class)
public class ZombieVillagerConversionMixin implements NoseSmithZombieMarker {

    @Unique private boolean aromaaffect$wasNoseSmith = false;
    @Unique private boolean aromaaffect$noseSmithHasNose = true;
    @Unique private String aromaaffect$noseSmithRequestedFlower = "";
    @Unique private boolean aromaaffect$noseSmithHouseDecorated = false;
    @Unique private long aromaaffect$noseSmithNoseRemovedGameTime = -1L;

    @Override
    public void aromaaffect$markAsNoseSmith(
            boolean hasNose,
            String requestedFlower,
            boolean houseDecorated,
            long noseRemovedGameTime) {
        this.aromaaffect$wasNoseSmith = true;
        this.aromaaffect$noseSmithHasNose = hasNose;
        this.aromaaffect$noseSmithRequestedFlower = requestedFlower != null ? requestedFlower : "";
        this.aromaaffect$noseSmithHouseDecorated = houseDecorated;
        this.aromaaffect$noseSmithNoseRemovedGameTime = noseRemovedGameTime;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aromaaffect$saveNoseSmithData(CompoundTag tag, CallbackInfo ci) {
        if (aromaaffect$wasNoseSmith) {
            tag.putBoolean("aromaaffect:WasNoseSmith", true);
            tag.putBoolean("aromaaffect:NoseSmithHasNose", aromaaffect$noseSmithHasNose);
            tag.putString(
                    "aromaaffect:NoseSmithRequestedFlower", aromaaffect$noseSmithRequestedFlower);
            tag.putBoolean(
                    "aromaaffect:NoseSmithHouseDecorated", aromaaffect$noseSmithHouseDecorated);
            if (aromaaffect$noseSmithNoseRemovedGameTime >= 0) {
                tag.putString(
                        "aromaaffect:NoseSmithNoseRemovedGameTime",
                        Long.toString(aromaaffect$noseSmithNoseRemovedGameTime));
            }
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aromaaffect$loadNoseSmithData(CompoundTag tag, CallbackInfo ci) {
        aromaaffect$wasNoseSmith =
                tag.contains("aromaaffect:WasNoseSmith", Tag.TAG_BYTE)
                        && tag.getBoolean("aromaaffect:WasNoseSmith");
        if (aromaaffect$wasNoseSmith) {
            aromaaffect$noseSmithHasNose =
                    !tag.contains("aromaaffect:NoseSmithHasNose", Tag.TAG_BYTE)
                            || tag.getBoolean("aromaaffect:NoseSmithHasNose");
            aromaaffect$noseSmithRequestedFlower =
                    tag.contains("aromaaffect:NoseSmithRequestedFlower", Tag.TAG_STRING)
                            ? tag.getString("aromaaffect:NoseSmithRequestedFlower")
                            : "";
            aromaaffect$noseSmithHouseDecorated =
                    tag.contains("aromaaffect:NoseSmithHouseDecorated", Tag.TAG_BYTE)
                            && tag.getBoolean("aromaaffect:NoseSmithHouseDecorated");
            if (tag.contains("aromaaffect:NoseSmithNoseRemovedGameTime", Tag.TAG_STRING)) {
                try {
                    aromaaffect$noseSmithNoseRemovedGameTime =
                            Long.parseLong(
                                    tag.getString("aromaaffect:NoseSmithNoseRemovedGameTime"));
                } catch (NumberFormatException e) {
                    aromaaffect$noseSmithNoseRemovedGameTime = -1L;
                }
            } else {
                aromaaffect$noseSmithNoseRemovedGameTime = -1L;
            }
        }
    }

    @Redirect(
            method = "finishConversion",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/monster/ZombieVillager;convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;"))
    @Nullable
    private <T extends Mob> T aromaaffect$redirectConversion(
            ZombieVillager self, EntityType<T> entityType, boolean preserveCanPickUpLoot) {
        if (!aromaaffect$wasNoseSmith) {
            return self.convertTo(entityType, preserveCanPickUpLoot);
        }

        @SuppressWarnings("unchecked")
        EntityType<T> noseSmithType =
                (EntityType<T>) (EntityType<?>) NoseSmithRegistry.getNOSE_SMITH().get();

        T result = self.convertTo(noseSmithType, preserveCanPickUpLoot);
        if (result instanceof NoseSmithEntity noseSmith) {
            noseSmith.restoreNoseSmithData(
                    aromaaffect$noseSmithHasNose,
                    aromaaffect$noseSmithRequestedFlower,
                    aromaaffect$noseSmithHouseDecorated,
                    aromaaffect$noseSmithNoseRemovedGameTime);
        }
        return result;
    }
}
