package com.ovrtechnology.scentitem;

import com.ovrtechnology.AromaCraft;
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
 * Item class for scent items in AromaCraft.
 * 
 * <p>Scent items are collectible items that represent different scents.
 * They can be used in crafting recipes or as components for the nose system.</p>
 */
public class ScentItem extends Item {
    
    /**
     * The definition that was used to create this scent item
     */
    @Getter
    private final ScentItemDefinition definition;
    
    /**
     * The item ID for this scent item
     */
    @Getter
    private final String itemId;
    
    /**
     * Create a new scent item from a definition
     * 
     * @param definition The scent item definition from JSON
     * @param itemId The item ID for this scent item
     */
    public ScentItem(ScentItemDefinition definition, String itemId) {
        super(createProperties(definition, itemId));
        this.definition = definition;
        this.itemId = itemId;
    }
    
    /**
     * Create item properties from a scent item definition
     */
    private static Properties createProperties(ScentItemDefinition definition, String itemId) {
        Properties properties = new Properties();
        
        // Set the item ID - REQUIRED in Minecraft 1.21.x
        properties.setId(ResourceKey.create(
                Registries.ITEM, 
                ResourceLocation.fromNamespaceAndPath(AromaCraft.MOD_ID, itemId)
        ));
        
        // Scent items stack up to 64
        properties.stacksTo(64);
        
        // Set rarity based on priority
        properties.rarity(getRarityForPriority(definition.getPriority()));
        
        return properties;
    }
    
    /**
     * Get the Minecraft rarity based on scent priority
     */
    private static Rarity getRarityForPriority(int priority) {
        if (priority >= 7) {
            return Rarity.EPIC;
        } else if (priority >= 5) {
            return Rarity.RARE;
        } else if (priority >= 3) {
            return Rarity.UNCOMMON;
        } else {
            return Rarity.COMMON;
        }
    }
    
    /**
     * Add tooltip information to the item
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        
        // Add scent description as tooltip
        String description = definition.getDescription();
        if (description != null && !description.isEmpty()) {
            // Split long descriptions into multiple lines
            String[] words = description.split(" ");
            StringBuilder line = new StringBuilder();
            
            for (String word : words) {
                if (line.length() + word.length() + 1 > 40) {
                    tooltipAdder.accept(Component.literal("§7" + line.toString().trim()));
                    line = new StringBuilder();
                }
                line.append(word).append(" ");
            }
            
            if (line.length() > 0) {
                tooltipAdder.accept(Component.literal("§7" + line.toString().trim()));
            }
        }
    }
    
    /**
     * Get the priority of this scent item
     */
    public int getPriority() {
        return definition.getPriority();
    }
    
    /**
     * Get the fallback name of this scent item
     */
    public String getFallbackName() {
        return definition.getFallbackName();
    }
}
