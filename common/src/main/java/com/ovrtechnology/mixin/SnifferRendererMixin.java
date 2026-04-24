package com.ovrtechnology.mixin;

import com.ovrtechnology.entity.sniffer.SnifferTamingData;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import com.ovrtechnology.util.Ids;
import net.minecraft.client.renderer.entity.SnifferRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnifferRenderer.class)
public class SnifferRendererMixin {

    @Unique
    private static final ResourceLocation aromaaffect$SNIFFER_WITH_SADDLE =
            Ids.mod("textures/entity/sniffer_with_saddle.png");

    @Unique
    private static final ResourceLocation aromaaffect$SNIFFER_WITH_NOSE =
            Ids.mod("textures/entity/sniffer_with_nose.png");

    @Unique
    private static final ResourceLocation aromaaffect$SNIFFER_WITH_SADDLE_AND_NOSE =
            Ids.mod("textures/entity/sniffer_with_saddle_and_nose.png");

    @Inject(
            method =
                    "getTextureLocation(Lnet/minecraft/world/entity/animal/sniffer/Sniffer;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true)
    private void aromaaffect$getCustomTexture(
            Sniffer sniffer, CallbackInfoReturnable<ResourceLocation> cir) {
        SnifferTamingData data = SnifferTamingData.get(sniffer.getUUID());
        boolean hasSaddle = !data.saddleItem.isEmpty() && data.saddleItem.is(Items.SADDLE);
        boolean hasNose =
                !data.decorationItem.isEmpty()
                        && data.decorationItem.getItem() instanceof SnifferNoseItem;

        if (hasSaddle && hasNose) {
            cir.setReturnValue(aromaaffect$SNIFFER_WITH_SADDLE_AND_NOSE);
        } else if (hasSaddle) {
            cir.setReturnValue(aromaaffect$SNIFFER_WITH_SADDLE);
        } else if (hasNose) {
            cir.setReturnValue(aromaaffect$SNIFFER_WITH_NOSE);
        }
    }
}
