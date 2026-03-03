package com.ovrtechnology.tutorial.regenarea;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for block regeneration in tutorial areas.
 * <p>
 * When a player breaks a block inside an enabled regen area, this handler
 * schedules the block to regenerate after the configured delay.
 * <p>
 * Features:
 * <ul>
 *   <li>Detects block breaks via Architectury events</li>
 *   <li>Schedules regeneration with configurable delay</li>
 *   <li>Shows particles and plays sound on regeneration</li>
 *   <li>Only active when tutorial mode is enabled</li>
 * </ul>
 */
public final class TutorialRegenAreaHandler {

    /**
     * Tracks pending block regenerations.
     * Key: BlockPos, Value: PendingRegen data
     */
    private static final Map<PendingRegenKey, PendingRegen> pendingRegens = new ConcurrentHashMap<>();

    private static boolean initialized = false;

    private TutorialRegenAreaHandler() {
    }

    /**
     * Initializes the regen area handler.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Listen for block break events
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return EventResult.pass();
            }

            // Check if tutorial mode is active
            if (!TutorialModule.isActive(serverLevel)) {
                return EventResult.pass();
            }

            // Check if the block is inside a regen area
            Optional<TutorialRegenArea> areaOpt = TutorialRegenAreaManager.findAreaContaining(serverLevel, pos);
            if (areaOpt.isEmpty()) {
                return EventResult.pass();
            }

            TutorialRegenArea area = areaOpt.get();

            // Check if we have a saved block state for this position
            String savedState = area.getSavedBlock(pos);
            if (savedState == null) {
                // No saved state - use current state before it's broken
                savedState = TutorialRegenAreaManager.serializeBlockState(state);
            }

            // Schedule regeneration
            int delayTicks = area.getRegenDelayTicks();
            PendingRegenKey key = new PendingRegenKey(serverLevel.dimension().location().toString(), pos);
            pendingRegens.put(key, new PendingRegen(
                    serverLevel,
                    pos,
                    savedState,
                    delayTicks
            ));

            AromaAffect.LOGGER.debug("Scheduled block regen at {} in {} ticks (area: {})",
                    pos, delayTicks, area.getId());

            return EventResult.pass();
        });

        // Tick pending regenerations
        TickEvent.SERVER_POST.register(server -> {
            if (pendingRegens.isEmpty()) {
                return;
            }

            Iterator<Map.Entry<PendingRegenKey, PendingRegen>> it = pendingRegens.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<PendingRegenKey, PendingRegen> entry = it.next();
                PendingRegen regen = entry.getValue();

                regen.ticksRemaining--;

                if (regen.ticksRemaining <= 0) {
                    // Time to regenerate!
                    regenerateBlock(regen);
                    it.remove();
                }
            }
        });

        AromaAffect.LOGGER.debug("Tutorial regen area handler initialized");
    }

    /**
     * Regenerates a block at the specified position.
     */
    private static void regenerateBlock(PendingRegen regen) {
        ServerLevel level = regen.level;
        BlockPos pos = regen.pos;
        String stateString = regen.blockState;

        // Check if the level is still valid
        if (level == null || level.isClientSide()) {
            return;
        }

        // Check if tutorial mode is still active
        if (!TutorialModule.isActive(level)) {
            return;
        }

        // Only regenerate if the block is still air (player didn't place something)
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.isAir()) {
            AromaAffect.LOGGER.debug("Skipped regen at {} - block already present", pos);
            return;
        }

        try {
            BlockState blockState = BlockStateParser.parseForBlock(
                    level.holderLookup(Registries.BLOCK),
                    stateString,
                    false
            ).blockState();

            // Place the block
            level.setBlock(pos, blockState, Block.UPDATE_ALL);

            // Spawn particles
            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8,    // count
                    0.3,  // xSpread
                    0.3,  // ySpread
                    0.3,  // zSpread
                    0.0   // speed
            );

            // Play sound
            level.playSound(
                    null,
                    pos,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    0.5f,
                    1.2f + level.getRandom().nextFloat() * 0.3f
            );

            AromaAffect.LOGGER.debug("Regenerated block at {}: {}", pos, stateString);

        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Failed to regenerate block at {} with state '{}': {}",
                    pos, stateString, e.getMessage());
        }
    }

    /**
     * Cancels all pending regenerations for a specific level.
     *
     * @param level the server level
     * @return number of regenerations cancelled
     */
    public static int cancelAllForLevel(ServerLevel level) {
        String dimensionKey = level.dimension().location().toString();
        int count = 0;

        Iterator<Map.Entry<PendingRegenKey, PendingRegen>> it = pendingRegens.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<PendingRegenKey, PendingRegen> entry = it.next();
            if (entry.getKey().dimension.equals(dimensionKey)) {
                it.remove();
                count++;
            }
        }

        if (count > 0) {
            AromaAffect.LOGGER.info("Cancelled {} pending block regenerations", count);
        }
        return count;
    }

    /**
     * Cancels all pending regenerations.
     *
     * @return number of regenerations cancelled
     */
    public static int cancelAll() {
        int count = pendingRegens.size();
        pendingRegens.clear();
        if (count > 0) {
            AromaAffect.LOGGER.info("Cancelled all {} pending block regenerations", count);
        }
        return count;
    }

    /**
     * Gets the number of pending regenerations.
     */
    public static int getPendingCount() {
        return pendingRegens.size();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data classes
    // ─────────────────────────────────────────────────────────────────────────────

    private record PendingRegenKey(String dimension, BlockPos pos) {}

    private static class PendingRegen {
        final ServerLevel level;
        final BlockPos pos;
        final String blockState;
        int ticksRemaining;

        PendingRegen(ServerLevel level, BlockPos pos, String blockState, int ticksRemaining) {
            this.level = level;
            this.pos = pos;
            this.blockState = blockState;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
