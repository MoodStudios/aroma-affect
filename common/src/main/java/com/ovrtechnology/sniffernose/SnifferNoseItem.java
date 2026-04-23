package com.ovrtechnology.sniffernose;

import com.ovrtechnology.util.Texts;
import java.util.List;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

public class SnifferNoseItem extends Item {

    @Getter private final SnifferNoseDefinition definition;

    @Getter private final String itemId;

    public SnifferNoseItem(SnifferNoseDefinition definition, String itemId) {
        super(createProperties(definition));
        this.definition = definition;
        this.itemId = itemId;
    }

    private static Properties createProperties(SnifferNoseDefinition definition) {
        Properties properties = new Properties();
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
            List<Component> tooltip,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        tooltip.add(Texts.lit("§7For the Sniffer mob"));
        tooltip.add(Texts.lit("§8Tier: " + definition.getTier()));
    }

    public int getTier() {
        return definition.getTier();
    }
}
