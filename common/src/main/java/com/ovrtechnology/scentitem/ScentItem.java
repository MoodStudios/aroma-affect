package com.ovrtechnology.scentitem;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.ItemTriggerDefinition;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.TriggerSettings;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Getter;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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

public class ScentItem extends Item {

    @Getter private final ScentItemDefinition definition;

    @Getter private final String itemId;

    public ScentItem(ScentItemDefinition definition, String itemId) {
        super(createProperties(definition, itemId));
        this.definition = definition;
        this.itemId = itemId;
    }

    private static Properties createProperties(ScentItemDefinition definition, String itemId) {
        Properties properties = new Properties();

        properties.setId(ResourceKey.create(Registries.ITEM, Ids.mod(itemId)));

        if (definition.isCapsule()) {
            properties.stacksTo(1);
            properties.durability(100);
        } else {
            properties.stacksTo(64);
        }

        properties.rarity(getRarityForPriority(definition.getPriority()));

        return properties;
    }

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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (definition.isCapsule()) {

                if (player.getCooldowns().isOnCooldown(stack)) {
                    return InteractionResult.FAIL;
                }

                EquipmentSlot slot =
                        hand == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND
                                : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, player, slot);

                String fullId = AromaAffect.MOD_ID + ":" + itemId;
                int cooldownTicks = getCooldownTicks(fullId);
                player.getCooldowns().addCooldown(stack, cooldownTicks);

                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.BOTTLE_EMPTY,
                        SoundSource.PLAYERS,
                        1.0f,
                        1.0f + (level.random.nextFloat() - 0.5f) * 0.2f);

                if (level instanceof ServerLevel serverLevel) {
                    spawnScentParticles(serverLevel, player);
                }

                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        String fullItemId = AromaAffect.MOD_ID + ":" + itemId;

        Optional<ItemTriggerDefinition> triggerOpt =
                ScentTriggerConfigLoader.getItemTrigger(fullItemId);
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

        if (!ScentTriggerManager.getInstance().canTrigger(scentName, cooldownMs)) {
            long remaining = ScentTriggerManager.getInstance().getRemainingCooldown(scentName);
            player.displayClientMessage(
                    Texts.tr(
                            "message.aromaaffect.scent_cooldown",
                            String.format("%.1f", remaining / 1000.0)),
                    true);
            return InteractionResult.FAIL;
        }

        ScentTrigger trigger =
                ScentTrigger.fromItemUse(scentName, triggerDef.getDurationTicks(), intensity);

        boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);

        if (triggered) {

            if (!definition.isCapsule() && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            level.playSound(
                    player,
                    player.blockPosition(),
                    SoundEvents.BOTTLE_EMPTY,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f + (level.random.nextFloat() - 0.5f) * 0.2f);

            if (ClientConfig.getInstance().isDebugScentMessages()) {
                int intensityPercent = (int) Math.round(intensity * 100);
                String message =
                        String.format(
                                "§d[Aroma Affect] §7Scent: §e%s §7(§ditem use§7) §8[%d%%]",
                                scentName, intensityPercent);
                player.displayClientMessage(Texts.lit(message), false);
            }

            AromaAffect.LOGGER.debug("Item {} triggered scent '{}'", fullItemId, scentName);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static int getCooldownTicks(String fullItemId) {
        Optional<ItemTriggerDefinition> triggerOpt =
                ScentTriggerConfigLoader.getItemTrigger(fullItemId);
        long cooldownMs =
                triggerOpt
                        .map(ItemTriggerDefinition::getCooldownMsOrDefault)
                        .orElse(ItemTriggerDefinition.DEFAULT_COOLDOWN_MS);
        return Math.max(1, (int) (cooldownMs / 50));
    }

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

            serverLevel.sendParticles(
                    particle, px + ox, py + oy, pz + oz, 1, ox * 0.2, 0.05, oz * 0.2, 0.02);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);

        String description = definition.getDescription();
        if (description != null && !description.isEmpty()) {

            String[] words = description.split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                if (line.length() + word.length() + 1 > 40) {
                    tooltipAdder.accept(Texts.lit("§7" + line.toString().trim()));
                    line = new StringBuilder();
                }
                line.append(word).append(" ");
            }

            if (!line.isEmpty()) {
                tooltipAdder.accept(Texts.lit("§7" + line.toString().trim()));
            }
        }
    }

    public int getPriority() {
        return definition.getPriority();
    }

    public String getFallbackName() {
        return definition.getFallbackName();
    }
}
