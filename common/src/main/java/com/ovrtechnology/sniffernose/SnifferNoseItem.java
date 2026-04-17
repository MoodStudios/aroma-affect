package com.ovrtechnology.sniffernose;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Item class for sniffer nose items in Aroma Affect.
 * 
 * <p>Sniffer nose items are designed for the Sniffer mob and are NOT equippable by players.
 * They are regular items that can be used in crafting, dropped by mobs, etc.</p>
 */
public class SnifferNoseItem extends Item {
    
    /**
     * The definition that was used to create this sniffer nose item
     */
    @Getter
    private final SnifferNoseDefinition definition;
    
    /**
     * The item ID for this sniffer nose
     */
    @Getter
    private final String itemId;
    
    /**
     * Create a new sniffer nose item from a definition
     * 
     * @param definition The sniffer nose definition from JSON
     * @param itemId The item ID for this sniffer nose
     */
    public SnifferNoseItem(SnifferNoseDefinition definition, String itemId) {
        super(createProperties(definition, itemId));
        this.definition = definition;
        this.itemId = itemId;
    }
    
    /**
     * Create item properties from a sniffer nose definition
     */
    private static Properties createProperties(SnifferNoseDefinition definition, String itemId) {
        Properties properties = new Properties();
        
        // Set the item ID - REQUIRED in Minecraft 1.21.x
        properties.setId(ResourceKey.create(
                Registries.ITEM, 
                Ids.mod(itemId)
        ));
        
        // Sniffer nose items stack to 1 (they are unique items)
        properties.stacksTo(1);
        
        // Set rarity based on tier
        properties.rarity(getRarityForTier(definition.getTier()));
        
        // NOTE: No equippable component - this is NOT wearable by players
        
        return properties;
    }
    
    /**
     * Get the Minecraft rarity based on tier
     */
    private static Rarity getRarityForTier(int tier) {
        return switch (tier) {
            case 1 -> Rarity.UNCOMMON;
            case 2 -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }
    
    /**
     * Add tooltip information to the item
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        
        // Add info that this is for the Sniffer mob
        tooltipAdder.accept(Texts.lit("§7For the Sniffer mob"));
        tooltipAdder.accept(Texts.lit("§8Tier: " + definition.getTier()));
    }
    
    /**
     * Get the tier of this sniffer nose
     */
    public int getTier() {
        return definition.getTier();
    }
}
