package com.ovrtechnology.guide;

import com.ovrtechnology.util.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class AromaGuideItem extends Item {

    public AromaGuideItem() {
        super(
                new Properties()
                        .setId(ResourceKey.create(Registries.ITEM, Ids.mod("aroma_guide")))
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            GuideManager.openGuideClient();
        }
        return InteractionResult.SUCCESS;
    }
}
