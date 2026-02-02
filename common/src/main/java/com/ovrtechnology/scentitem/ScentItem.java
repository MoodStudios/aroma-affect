package com.ovrtechnology.scentitem;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.config.ItemTriggerDefinition;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.TriggerSettings;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Item class for scent items in Aroma Affect.
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
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, itemId)
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
     * Handle item use (right-click).
     * 
     * <p>This triggers the associated scent on OVR hardware if:</p>
     * <ul>
     *   <li>A trigger is configured for this item in scent_item_triggers.json</li>
     *   <li>The scent is not on cooldown</li>
     * </ul>
     * 
     * <p>If the scent is on cooldown, the item is NOT consumed and a message is shown.</p>
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Only process on client side (WebSocket is client-side)
        if (!level.isClientSide()) {
            return InteractionResult.PASS;
        }
        
        // Get the full item ID
        String fullItemId = AromaAffect.MOD_ID + ":" + itemId;
        
        // Look up trigger configuration
        Optional<ItemTriggerDefinition> triggerOpt = ScentTriggerConfigLoader.getItemTrigger(fullItemId);
        
        if (triggerOpt.isEmpty()) {
            // No trigger configured for this item
            AromaAffect.LOGGER.debug("No trigger configured for item: {}", fullItemId);
            return InteractionResult.PASS;
        }
        
        ItemTriggerDefinition triggerDef = triggerOpt.get();
        
        // Only process USE triggers
        if (!triggerDef.isUseTriggered()) {
            return InteractionResult.PASS;
        }
        
        String scentName = triggerDef.getScentName();
        long cooldownMs = triggerDef.getCooldownMsOrDefault();
        
        // Get intensity from trigger definition, falling back to global setting
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        double intensity = triggerDef.getIntensityOrDefault(settings.getItemIntensity());
        
        // Check cooldown BEFORE consuming the item
        if (!ScentTriggerManager.getInstance().canTrigger(scentName, cooldownMs)) {
            // Show cooldown message to player
            long remaining = ScentTriggerManager.getInstance().getRemainingCooldown(scentName);
            player.displayClientMessage(
                Component.translatable("message.aromaaffect.scent_cooldown", 
                    String.format("%.1f", remaining / 1000.0)),
                true
            );
            return InteractionResult.FAIL; // Don't consume item
        }
        
        // Create and execute the trigger with intensity
        ScentTrigger trigger = ScentTrigger.fromItemUse(
            scentName,
            triggerDef.getDurationTicks(),
            intensity
        );
        
        boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);
        
        if (triggered) {
            // Consume the item (if not in creative mode)
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            
            // Play a sound effect
            level.playSound(
                player, 
                player.blockPosition(), 
                SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS, 
                1.0f, 
                1.0f + (level.random.nextFloat() - 0.5f) * 0.2f
            );
            
            AromaAffect.LOGGER.debug("Item {} triggered scent '{}'", fullItemId, scentName);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
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
