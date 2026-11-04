package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ModItem;
import com.ovrtechnology.compat.RegistrationCompat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * The Aroma Guide is a compass-like item that always points toward
 * the nearest village, regardless of village type.
 * <p>
 * It dynamically updates its target as the player moves, switching
 * to whichever village is closest at any given moment. While held,
 * the distance to the nearest village is displayed on the action bar.
 * <p>
 * Right-clicking opens the Aroma Affect guide UI.
 */
public class AromaGuideItem extends ModItem {

    public AromaGuideItem() {
        super(RegistrationCompat.itemProperties(Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "aroma_guide"))
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
        );
    }

    @Override
    protected InteractionResult useItem(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            GuideManager.openGuideClient();
        }
        return InteractionResult.SUCCESS;
    }
}
