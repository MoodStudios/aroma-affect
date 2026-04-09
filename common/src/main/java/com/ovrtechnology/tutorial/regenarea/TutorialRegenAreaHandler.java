package com.ovrtechnology.tutorial.regenarea;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager;
import com.ovrtechnology.tutorial.popupzone.TutorialPopupZoneHandler;
import com.ovrtechnology.tutorial.searchdiamond.SearchDiamondZoneHandler;
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
import net.minecraft.server.level.ServerPlayer;
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

    /**
     * Per-dimension protection bypass state.
     * When true for a dimension, map protection is disabled and all blocks can be broken.
     * Resets to false on tutorial reset or player join.
     */
    private static final Map<String, Boolean> protectionBypassedByDimension = new ConcurrentHashMap<>();

    private TutorialRegenAreaHandler() {
    }

    /**
     * Enables protection bypass for a specific level - allows breaking any block.
     */
    public static void enableBypass(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        protectionBypassedByDimension.put(dimKey, true);
        AromaAffect.LOGGER.info("Map protection DISABLED for {} - all blocks can be broken", dimKey);
    }

    /**
     * Enables protection bypass globally (all dimensions).
     */
    public static void enableBypass() {
        protectionBypassedByDimension.put("global", true);
        AromaAffect.LOGGER.info("Map protection DISABLED globally - all blocks can be broken");
    }

    /**
     * Disables protection bypass - map becomes protected again.
     */
    public static void disableBypass(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        protectionBypassedByDimension.remove(dimKey);
        protectionBypassedByDimension.remove("global");
        AromaAffect.LOGGER.info("Map protection ENABLED for {} - map is protected", dimKey);
    }

    /**
     * Disables protection bypass globally.
     */
    public static void disableBypass() {
        protectionBypassedByDimension.clear();
        AromaAffect.LOGGER.info("Map protection ENABLED - map is protected");
    }

    /**
     * Toggles protection bypass state for a level.
     * @return true if bypass is now enabled, false if disabled
     */
    public static boolean toggleBypass(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        boolean current = isBypassEnabled(level);
        if (current) {
            disableBypass(level);
        } else {
            enableBypass(level);
        }
        return !current;
    }

    /**
     * Checks if protection bypass is currently enabled for a level.
     */
    public static boolean isBypassEnabled(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        return protectionBypassedByDimension.getOrDefault(dimKey, false)
                || protectionBypassedByDimension.getOrDefault("global", false);
    }

    /**
     * Checks if protection bypass is currently enabled (global check).
     */
    public static boolean isBypassEnabled() {
        return !protectionBypassedByDimension.isEmpty();
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
        // TUTORIAL MAP IS INDESTRUCTIBLE except:
        // 1. Blocks inside regen areas (they regenerate)
        // 2. The Nose Smith flower position (quest item)
        // 3. When protection bypass is enabled (via /tutorial protection off)
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return EventResult.pass();
            }

            // Check if tutorial mode is active
            if (!TutorialModule.isActive(serverLevel)) {
                return EventResult.pass();
            }

            // Cobwebs: always drop string + dismiss popup
            if (state.is(net.minecraft.world.level.block.Blocks.COBWEB) && player instanceof ServerPlayer sp) {
                TutorialPopupZoneHandler.dismissStickyPopup(sp, "cowweb");
                // Force drop string regardless of tool used
                net.minecraft.world.entity.item.ItemEntity stringDrop = new net.minecraft.world.entity.item.ItemEntity(
                        serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STRING, 1));
                stringDrop.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(stringDrop);
                // Remove the cobweb and cancel vanilla drop
                serverLevel.destroyBlock(pos, false);
                return EventResult.interruptFalse();
            }

            // If protection bypass is enabled, allow all block breaking
            if (isBypassEnabled(serverLevel)) {
                return EventResult.pass();
            }

            // Check if the block is inside a regen area (ALLOWED - will regenerate)
            Optional<TutorialRegenArea> areaOpt = TutorialRegenAreaManager.findAreaContaining(serverLevel, pos);

            // Check if this is the Nose Smith flower position (ALLOWED - quest item)
            Optional<BlockPos> flowerPos = TutorialNoseSmithManager.getFlowerPos(serverLevel);
            boolean isFlowerPos = flowerPos.isPresent() && flowerPos.get().equals(pos);

            // Check if player is in SearchDiamond session and block is in their zone (ALLOWED)
            boolean isSearchDiamondZone = false;
            boolean hasSearchDiamondSession = false;
            if (player instanceof ServerPlayer serverPlayer) {
                hasSearchDiamondSession = SearchDiamondZoneHandler.hasActiveSession(serverPlayer);
                isSearchDiamondZone = SearchDiamondZoneHandler.canPlayerBreakAt(serverPlayer, pos, serverLevel);
            }

            // If NOT in regen area AND NOT the flower AND NOT in SearchDiamond zone -> BLOCK DESTRUCTION
            if (areaOpt.isEmpty() && !isFlowerPos && !isSearchDiamondZone) {
                // If player has active SearchDiamond session but is outside the zone, show helpful message
                if (hasSearchDiamondSession && player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "\u00a7e\u00a7lGet closer to the marked block! \u00a7eYou can only dig within the diamond search area."));
                }
                AromaAffect.LOGGER.debug("Blocked block break at {} - tutorial map is protected", pos);
                return EventResult.interruptFalse();
            }

            // If it's in SearchDiamond zone, allow without regen area processing
            if (isSearchDiamondZone) {
                AromaAffect.LOGGER.debug("Allowed SearchDiamond zone break at {}", pos);
                return EventResult.pass();
            }

            // If it's the flower, allow destruction without regeneration
            if (isFlowerPos) {
                AromaAffect.LOGGER.debug("Allowed flower break at {}", pos);
                return EventResult.pass();
            }

            // Block is in regen area - allow and schedule regeneration
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

        // Prevent placing flowers on the ground (causes inventory desync)
        BlockEvent.PLACE.register((level, pos, state, placer) -> {
            if (!(level instanceof ServerLevel serverLevel)) return EventResult.pass();
            if (!TutorialModule.isActive(serverLevel)) return EventResult.pass();
            if (isBypassEnabled(serverLevel)) return EventResult.pass();

            if (state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) || state.is(net.minecraft.tags.BlockTags.FLOWERS)) {
                // Re-sync inventory to fix ghost item on client
                if (placer instanceof ServerPlayer sp) {
                    sp.inventoryMenu.broadcastChanges();
                }
                return EventResult.interruptFalse();
            }
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
