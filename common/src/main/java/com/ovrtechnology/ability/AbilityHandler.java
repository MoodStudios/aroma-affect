package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class AbilityHandler {

    private static final Map<UUID, ActiveInteraction> ACTIVE_INTERACTIONS = new HashMap<>();

    private static boolean initialized = false;

    private AbilityHandler() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("AbilityHandler.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing AbilityHandler...");

        InteractionEvent.RIGHT_CLICK_BLOCK.register(AbilityHandler::onRightClickBlock);

        TickEvent.SERVER_LEVEL_POST.register(AbilityHandler::onServerTick);

        initialized = true;
        AromaAffect.LOGGER.info("AbilityHandler initialized");
    }

    private static InteractionResult onRightClickBlock(
            Player player, InteractionHand hand, BlockPos pos, Direction direction) {

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        Block block = player.level().getBlockState(pos).getBlock();

        for (BlockInteractionAbility ability : AbilityRegistry.getBlockInteractionAbilities()) {
            if (!ability.isValidTarget(block)) {
                continue;
            }

            if (!ability.canUse(serverPlayer)) {

                long cooldown = ability.getRemainingCooldown(serverPlayer);
                if (cooldown > 0) {
                    AromaAffect.LOGGER.debug(
                            "Player {} on cooldown for {} ({} more ticks)",
                            player.getName().getString(),
                            ability.getId(),
                            cooldown);
                }
                continue;
            }

            ACTIVE_INTERACTIONS.put(player.getUUID(), new ActiveInteraction(pos, ability));

            boolean completed = ability.onInteract(serverPlayer, pos);

            if (completed) {
                ACTIVE_INTERACTIONS.remove(player.getUUID());
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void onServerTick(Level level) {
        if (level.isClientSide()) {
            return;
        }

        Iterator<Map.Entry<UUID, ActiveInteraction>> iterator =
                ACTIVE_INTERACTIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveInteraction> entry = iterator.next();
            UUID playerId = entry.getKey();
            ActiveInteraction interaction = entry.getValue();
            BlockPos targetPos = interaction.pos();
            BlockInteractionAbility ability = interaction.ability();

            Player player = level.getPlayerByUUID(playerId);
            if (player == null || !(player instanceof ServerPlayer serverPlayer)) {
                iterator.remove();
                continue;
            }

            if (player.level() != level) {
                continue;
            }

            if (!isStillInteracting(serverPlayer, targetPos, ability)) {
                ability.onCancel(serverPlayer);
                iterator.remove();
                continue;
            }

            boolean completed = ability.onTick(serverPlayer, targetPos);

            if (completed) {
                iterator.remove();
            }
        }
    }

    private static boolean isStillInteracting(
            ServerPlayer player, BlockPos targetPos, BlockInteractionAbility ability) {

        double distance = player.position().distanceTo(targetPos.getCenter());
        if (distance > 5.0) {
            return false;
        }

        Block block = player.level().getBlockState(targetPos).getBlock();
        if (!ability.isValidTarget(block)) {
            return false;
        }

        if (!ability.canUse(player)) {
            return false;
        }

        return true;
    }

    private record ActiveInteraction(BlockPos pos, BlockInteractionAbility ability) {}
}
