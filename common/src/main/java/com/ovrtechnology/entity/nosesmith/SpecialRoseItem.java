package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.util.Texts;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class SpecialRoseItem extends Item {

    public SpecialRoseItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag tooltipFlag) {
        tooltip.add(
                Texts.tr("item.aromaaffect.special_rose.lore")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GOLD));
    }
}
