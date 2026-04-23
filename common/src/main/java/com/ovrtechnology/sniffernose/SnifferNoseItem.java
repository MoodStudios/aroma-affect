package com.ovrtechnology.sniffernose;

import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import java.util.function.Consumer;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SnifferNoseItem extends Item {

    @Getter private final SnifferNoseDefinition definition;

    @Getter private final String itemId;

    public SnifferNoseItem(SnifferNoseDefinition definition, String itemId) {
        super(createProperties(definition, itemId));
        this.definition = definition;
        this.itemId = itemId;
    }

    private static Properties createProperties(SnifferNoseDefinition definition, String itemId) {
        Properties properties = new Properties();

        properties.setId(ResourceKey.create(Registries.ITEM, Ids.mod(itemId)));

        properties.stacksTo(1);

        properties.rarity(getRarityForTier(definition.getTier()));

        return properties;
    }

    private static Rarity getRarityForTier(int tier) {
        return switch (tier) {
            case 1 -> Rarity.UNCOMMON;
            case 2 -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);

        tooltipAdder.accept(Texts.lit("§7For the Sniffer mob"));
        tooltipAdder.accept(Texts.lit("§8Tier: " + definition.getTier()));
    }

    public int getTier() {
        return definition.getTier();
    }
}
