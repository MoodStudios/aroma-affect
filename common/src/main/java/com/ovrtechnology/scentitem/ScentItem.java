package com.ovrtechnology.scentitem;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.config.ItemTriggerDefinition;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.TriggerSettings;
import lombok.Getter;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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

        // Capsule items have durability (100 uses); other scent items stack normally
        if (definition.isCapsule()) {
            properties.stacksTo(1);
            properties.durability(100);
        } else {
            properties.stacksTo(64);
        }

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

        // Server-side: handle capsule durability deduction (only if not on cooldown)
        if (!level.isClientSide()) {
            if (definition.isCapsule()) {
                // Check server-side cooldown to prevent consumption while on cooldown
                if (player.getCooldowns().isOnCooldown(stack)) {
                    return InteractionResult.FAIL;
                }

                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, player, slot);

                // Apply server-side cooldown (convert ms to ticks: default 5000ms = 100 ticks)
                String fullId = AromaAffect.MOD_ID + ":" + itemId;
                int cooldownTicks = getCooldownTicks(fullId);
                player.getCooldowns().addCooldown(stack, cooldownTicks);

                level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY,
                        SoundSource.PLAYERS, 1.0f,
                        1.0f + (level.random.nextFloat() - 0.5f) * 0.2f);

                // Spawn scent particles around the player
                if (level instanceof ServerLevel serverLevel) {
                    spawnScentParticles(serverLevel, player);
                }

                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Client-side: OVR hardware scent triggering
        String fullItemId = AromaAffect.MOD_ID + ":" + itemId;

        Optional<ItemTriggerDefinition> triggerOpt = ScentTriggerConfigLoader.getItemTrigger(fullItemId);
        if (triggerOpt.isEmpty()) {
            AromaAffect.LOGGER.debug("No trigger configured for item: {}", fullItemId);
            return InteractionResult.PASS;
        }

        ItemTriggerDefinition triggerDef = triggerOpt.get();
        if (!triggerDef.isUseTriggered()) {
            return InteractionResult.PASS;
        }

        String scentName = triggerDef.getScentName();
        long cooldownMs = triggerDef.getCooldownMsOrDefault();

        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        double intensity = triggerDef.getIntensityOrDefault(settings.getItemIntensity());

        // Check cooldown BEFORE consuming — returns FAIL so server is not called
        if (!ScentTriggerManager.getInstance().canTrigger(scentName, cooldownMs)) {
            long remaining = ScentTriggerManager.getInstance().getRemainingCooldown(scentName);
            player.displayClientMessage(
                Component.translatable("message.aromaaffect.scent_cooldown",
                    String.format("%.1f", remaining / 1000.0)),
                true
            );
            return InteractionResult.FAIL;
        }

        ScentTrigger trigger = ScentTrigger.fromItemUse(
            scentName,
            triggerDef.getDurationTicks(),
            intensity
        );

        boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);

        if (triggered) {
            // For non-capsule items, consume via shrink (original behavior)
            // Capsule durability is handled server-side via hurtAndBreak
            if (!definition.isCapsule() && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            level.playSound(player, player.blockPosition(), SoundEvents.BOTTLE_EMPTY,
                    SoundSource.PLAYERS, 1.0f,
                    1.0f + (level.random.nextFloat() - 0.5f) * 0.2f);

            AromaAffect.LOGGER.debug("Item {} triggered scent '{}'", fullItemId, scentName);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Get the cooldown duration in ticks for the given item from trigger config.
     */
    private static int getCooldownTicks(String fullItemId) {
        Optional<ItemTriggerDefinition> triggerOpt = ScentTriggerConfigLoader.getItemTrigger(fullItemId);
        long cooldownMs = triggerOpt.map(ItemTriggerDefinition::getCooldownMsOrDefault)
                .orElse(ItemTriggerDefinition.DEFAULT_COOLDOWN_MS);
        return Math.max(1, (int) (cooldownMs / 50)); // ms to ticks (50ms per tick)
    }

    /**
     * Spawns colored scent particles around the player, similar to the Omara Device puff.
     */
    private void spawnScentParticles(ServerLevel serverLevel, Player player) {
        String scentName = definition.getScent();
        int[] rgb = {255, 255, 255};
        if (scentName != null) {
            Optional<ScentDefinition> scentOpt = ScentRegistry.getScentByName(scentName);
            if (scentOpt.isPresent()) {
                rgb = scentOpt.get().getColorRGB();
            }
        }

        int argb = (255 << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
        var particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, argb);

        var random = serverLevel.getRandom();
        double px = player.getX();
        double py = player.getEyeY();
        double pz = player.getZ();

        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.15 + random.nextDouble() * 0.25;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double oy = (random.nextDouble() - 0.5) * 0.5;

            serverLevel.sendParticles(particle,
                    px + ox, py + oy, pz + oz,
                    1, ox * 0.2, 0.05, oz * 0.2, 0.02);
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
