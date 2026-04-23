package com.ovrtechnology.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SinglePoolElement.class)
public class JigsawPriorityMixin {

    @Inject(method = "getShuffledJigsawBlocks", at = @At("RETURN"))
    private void aromaaffect$prioritizeNoseSmithHouse(
            StructureTemplateManager manager,
            BlockPos pos,
            Rotation rotation,
            RandomSource random,
            CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> cir) {

        List<StructureTemplate.StructureBlockInfo> jigsaws = cir.getReturnValue();
        for (int i = 1; i < jigsaws.size(); i++) {
            CompoundTag nbt = jigsaws.get(i).nbt();
            if (nbt == null) continue;
            String poolStr = nbt.getString("pool");
            if (poolStr.startsWith("aromaaffect:") && poolStr.contains("nose_smith_house")) {
                jigsaws.add(0, jigsaws.remove(i));
                break;
            }
        }
    }
}
