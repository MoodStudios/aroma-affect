package com.ovrtechnology.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SinglePoolElement.class)
public class JigsawPriorityMixin {

    @Inject(method = "getShuffledJigsawBlocks", at = @At("RETURN"))
    private void aromaaffect$prioritizeNoseSmithHouse(
            StructureTemplateManager manager, BlockPos pos,
            Rotation rotation, RandomSource random,
            CallbackInfoReturnable<List<StructureTemplate.JigsawBlockInfo>> cir) {

        List<StructureTemplate.JigsawBlockInfo> jigsaws = cir.getReturnValue();
        for (int i = 1; i < jigsaws.size(); i++) {
            ResourceLocation pool = jigsaws.get(i).pool().location();
            if (pool.getNamespace().equals("aromaaffect")
                    && pool.getPath().contains("nose_smith_house")) {
                jigsaws.add(0, jigsaws.remove(i));
                break;
            }
        }
    }
}
