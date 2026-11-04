package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.compat.ModItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class SpecialRoseItem extends ModItem {

    public SpecialRoseItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    protected void appendTooltip(ItemStack stack, Consumer<Component> tooltipAdder) {
        tooltipAdder.accept(Component.translatable("item.aromaaffect.special_rose.lore")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GOLD));
    }
}
