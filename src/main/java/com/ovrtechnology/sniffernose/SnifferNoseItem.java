package com.ovrtechnology.sniffernose;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ModItem;
import com.ovrtechnology.compat.RegistrationCompat;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.function.Consumer;

/**
 * Item class for sniffer nose items in Aroma Affect.
 * 
 * <p>Sniffer nose items are designed for the Sniffer mob and are NOT equippable by players.
 * They are regular items that can be used in crafting, dropped by mobs, etc.</p>
 */
public class SnifferNoseItem extends ModItem {
    
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
        Properties properties = RegistrationCompat.itemProperties(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, itemId));
        
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
    protected void appendTooltip(ItemStack stack, Consumer<Component> tooltipAdder) {

        // Add info that this is for the Sniffer mob
        tooltipAdder.accept(Component.literal("§7For the Sniffer mob"));
        tooltipAdder.accept(Component.literal("§8Tier: " + definition.getTier()));
    }
    
    /**
     * Get the tier of this sniffer nose
     */
    public int getTier() {
        return definition.getTier();
    }
}
