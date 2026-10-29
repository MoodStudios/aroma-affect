package com.ovrtechnology.compat;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//? if >=26 {
import net.minecraft.world.item.component.TooltipDisplay;
//?} else {
/*import java.util.List;
import net.minecraft.world.InteractionResultHolder;
*///?}

// Bridges the item methods whose signatures changed between 1.21.1 and 26.x so subclasses stay version-free
public abstract class ModItem extends Item {

    protected ModItem(Properties properties) {
        super(properties);
    }

    // Return null to keep the vanilla behavior
    @Nullable
    protected InteractionResult useItem(Level level, Player player, InteractionHand hand) {
        return null;
    }

    protected void appendTooltip(ItemStack stack, Consumer<Component> tooltip) {
    }

    // Repair ingredient id for anvils; 26.x items set it as a component through ItemCompat#applyRepairItem
    @Nullable
    protected String repairItemId(ItemStack stack) {
        return null;
    }

    //? if >=26 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = useItem(level, player, hand);
        return result != null ? result : super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        appendTooltip(stack, tooltipAdder);
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = useItem(level, player, hand);
        if (result == null) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            case CONSUME, CONSUME_PARTIAL -> InteractionResultHolder.consume(stack);
            case FAIL -> InteractionResultHolder.fail(stack);
            default -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        appendTooltip(stack, tooltip::add);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        String repairId = repairItemId(stack);
        return repairId != null ? ItemCompat.isRepairIngredient(repairId, ingredient) : super.isValidRepairItem(stack, ingredient);
    }
    *///?}
}
