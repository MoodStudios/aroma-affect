package com.ovrtechnology.tutorial.searchdiamond;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.SearchDiamondNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for SearchDiamond minigame.
 */
public final class SearchDiamondZoneHandler {

    private static boolean initialized = false;

    // Track active game sessions per player
    private static final Map<UUID, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    // Track saved blocks for each zone (for regeneration)
    private static final Map<String, Map<BlockPos, BlockState>> savedZoneBlocks = new ConcurrentHashMap<>();

    private SearchDiamondZoneHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Handle block breaking
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (!(level instanceof ServerLevel serverLevel)) return EventResult.pass();
            if (!(player instanceof ServerPlayer serverPlayer)) return EventResult.pass();
            if (!TutorialModule.isActive(serverLevel)) return EventResult.pass();

            UUID playerId = serverPlayer.getUUID();
            ActiveSession session = activeSessions.get(playerId);
            if (session == null) return EventResult.pass();

            SearchDiamondZone zone = SearchDiamondZoneManager.getZone(serverLevel, session.zoneId);
            if (zone == null) return EventResult.pass();

            // Check if block is inside the zone
            if (!zone.isInsideArea(pos)) return EventResult.pass();

            AromaAffect.LOGGER.info("[SearchDiamond] Block broken at {} in zone {}, block: {}",
                    pos.toShortString(), zone.getId(), state.getBlock().getName().getString());

            // Track broken block for regeneration
            session.brokenBlocks.put(pos, state);

            // Check if player found ANY diamond ore in the zone
            boolean isDiamondOre = state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE);
            AromaAffect.LOGGER.info("[SearchDiamond] Block is diamond ore: {}", isDiamondOre);

            if (isDiamondOre) {
                AromaAffect.LOGGER.info("[SearchDiamond] DIAMOND FOUND! Calling onDiamondFound...");
                onDiamondFound(serverPlayer, zone, session);
                return EventResult.interruptFalse(); // Cancel the break, we handle it
            }

            return EventResult.pass();
        });

        // Handle button clicks to trigger the minigame
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(serverPlayer.level() instanceof ServerLevel level)) return InteractionResult.PASS;
            if (!TutorialModule.isActive(level)) return InteractionResult.PASS;

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ButtonBlock)) return InteractionResult.PASS;

            // Check if this button is a trigger for any zone
            Optional<SearchDiamondZone> zoneOpt = SearchDiamondZoneManager.findZoneByTriggerButton(level, pos);
            if (zoneOpt.isEmpty()) return InteractionResult.PASS;

            SearchDiamondZone zone = zoneOpt.get();
            if (!zone.isComplete()) return InteractionResult.PASS;

            // Check if player already has an active session
            if (activeSessions.containsKey(serverPlayer.getUUID())) {
                // Send reminder message in red
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7c\u00a7lFind the diamond! \u00a7cRemember to press R to open the menu, go to Blocks, and search for Diamond!"));
                return InteractionResult.PASS;
            }

            // Start the minigame for this player
            startGame(serverPlayer, zone);
            return InteractionResult.PASS;
        });

        AromaAffect.LOGGER.debug("SearchDiamond zone handler initialized");
    }

    /**
     * Starts a new diamond search game for a player.
     */
    public static void startGame(ServerPlayer player, SearchDiamondZone zone) {
        UUID playerId = player.getUUID();
        ServerLevel level = (ServerLevel) player.level();

        // End any previous session
        endSession(player, false);

        // Save current blocks in the zone if not already saved
        saveZoneBlocks(level, zone);

        // Place a random diamond ore in the zone
        BlockPos diamondPos = placeRandomDiamond(level, zone);
        if (diamondPos == null) {
            AromaAffect.LOGGER.warn("Could not place diamond in zone {}", zone.getId());
            return;
        }
        zone.setDiamondLocation(diamondPos);

        // Create new session
        ActiveSession session = new ActiveSession(zone.getId());
        activeSessions.put(playerId, session);

        // Send GUI to player
        SearchDiamondNetworking.sendStartScreen(player);

        // Send hologram position to player (at zone center)
        BlockPos center = zone.getCenter();
        if (center != null) {
            SearchDiamondNetworking.sendHologramPosition(player, center);
            AromaAffect.LOGGER.info("Sent hologram position to player at zone center: {}", center.toShortString());
        }

        AromaAffect.LOGGER.info("Player {} started SearchDiamond game in zone {}, diamond at {}",
                player.getName().getString(), zone.getId(), diamondPos.toShortString());
    }

    /**
     * Called when player finds the diamond.
     */
    private static void onDiamondFound(ServerPlayer player, SearchDiamondZone zone, ActiveSession session) {
        ServerLevel level = (ServerLevel) player.level();

        AromaAffect.LOGGER.info("[SearchDiamond] Player {} found the diamond in zone {}!",
                player.getName().getString(), zone.getId());

        // Clear entire inventory (removes tools AND any blocks picked up)
        AromaAffect.LOGGER.info("[SearchDiamond] Clearing player inventory...");
        player.getInventory().clearContent();

        // Teleport to exit point
        BlockPos exit = zone.getExitPoint();
        if (exit != null) {
            AromaAffect.LOGGER.info("[SearchDiamond] Teleporting to exit point: {}", exit.toShortString());
            player.teleportTo(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5);
        } else {
            AromaAffect.LOGGER.warn("[SearchDiamond] No exit point defined for zone!");
        }

        // Regenerate the zone
        AromaAffect.LOGGER.info("[SearchDiamond] Regenerating zone with {} broken blocks", session.brokenBlocks.size());
        regenerateZone(level, zone, session);

        // Clear diamond location
        zone.setDiamondLocation(null);

        // End session
        activeSessions.remove(player.getUUID());

        // Clear hologram
        SearchDiamondNetworking.sendClearHologram(player);
        AromaAffect.LOGGER.info("[SearchDiamond] Cleared hologram for player");

        // Victory scent + sound
        com.ovrtechnology.network.TutorialScentZoneNetworking.sendScentTrigger(player, "Sweet", 1.0, "diamond_victory");
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sounds.SoundSource.MASTER, 1.0f, 1.0f);

        // Send success title
        AromaAffect.LOGGER.info("[SearchDiamond] Sending success title to player");
        SearchDiamondNetworking.sendSuccessScreen(player);
    }

    /**
     * Ends a player's session, optionally regenerating the zone.
     */
    public static void endSession(ServerPlayer player, boolean regenerate) {
        UUID playerId = player.getUUID();
        ActiveSession session = activeSessions.remove(playerId);
        if (session == null) return;

        // Clear hologram for player
        SearchDiamondNetworking.sendClearHologram(player);

        if (regenerate && player.level() instanceof ServerLevel level) {
            SearchDiamondZone zone = SearchDiamondZoneManager.getZone(level, session.zoneId);
            if (zone != null) {
                regenerateZone(level, zone, session);
                zone.setDiamondLocation(null);
            }
        }
    }

    /**
     * Saves all blocks in a zone for later regeneration.
     */
    private static void saveZoneBlocks(ServerLevel level, SearchDiamondZone zone) {
        if (savedZoneBlocks.containsKey(zone.getId())) return;

        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int x = zone.getMinX(); x <= zone.getMaxX(); x++) {
            for (int y = zone.getMinY(); y <= zone.getMaxY(); y++) {
                for (int z = zone.getMinZ(); z <= zone.getMaxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        blocks.put(pos, state);
                    }
                }
            }
        }
        savedZoneBlocks.put(zone.getId(), blocks);
        AromaAffect.LOGGER.debug("Saved {} blocks for zone {}", blocks.size(), zone.getId());
    }

    /**
     * Places a diamond ore at a random solid block position in the zone.
     */
    private static BlockPos placeRandomDiamond(ServerLevel level, SearchDiamondZone zone) {
        List<BlockPos> validPositions = new ArrayList<>();

        for (int x = zone.getMinX(); x <= zone.getMaxX(); x++) {
            for (int y = zone.getMinY(); y <= zone.getMaxY(); y++) {
                for (int z = zone.getMinZ(); z <= zone.getMaxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    // Only replace solid, non-air blocks (like stone)
                    if (state.isSolid() && !state.isAir()) {
                        validPositions.add(pos);
                    }
                }
            }
        }

        if (validPositions.isEmpty()) return null;

        // Pick random position
        Random random = new Random();
        BlockPos diamondPos = validPositions.get(random.nextInt(validPositions.size()));

        // Place diamond ore
        level.setBlock(diamondPos, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);

        return diamondPos;
    }

    /**
     * Regenerates all broken blocks in a zone.
     */
    private static void regenerateZone(ServerLevel level, SearchDiamondZone zone, ActiveSession session) {
        // Restore broken blocks from this session
        for (Map.Entry<BlockPos, BlockState> entry : session.brokenBlocks.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
        }

        // Also restore from saved blocks if diamond was placed
        Map<BlockPos, BlockState> saved = savedZoneBlocks.get(zone.getId());
        if (saved != null && zone.getDiamondLocation() != null) {
            BlockState original = saved.get(zone.getDiamondLocation());
            if (original != null) {
                level.setBlock(zone.getDiamondLocation(), original, Block.UPDATE_ALL);
            }
        }

        AromaAffect.LOGGER.debug("Regenerated {} blocks in zone {}", session.brokenBlocks.size(), zone.getId());
    }

    /**
     * Regenerates a zone completely from saved state.
     */
    public static void fullRegenerateZone(ServerLevel level, String zoneId) {
        Map<BlockPos, BlockState> saved = savedZoneBlocks.get(zoneId);
        if (saved == null) return;

        for (Map.Entry<BlockPos, BlockState> entry : saved.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
        }

        SearchDiamondZone zone = SearchDiamondZoneManager.getZone(level, zoneId);
        if (zone != null) {
            zone.setDiamondLocation(null);
        }

        AromaAffect.LOGGER.info("Fully regenerated zone {} with {} blocks", zoneId, saved.size());
    }

    /**
     * Resets all zone data (call on tutorial reset).
     */
    public static void resetAll() {
        activeSessions.clear();
        savedZoneBlocks.clear();
    }

    /**
     * Resets all zone data with full regeneration (call on tutorial reset).
     * This regenerates all zones from saved state before clearing.
     */
    public static void resetAll(ServerLevel level) {
        // First, regenerate all zones that have saved state
        for (String zoneId : savedZoneBlocks.keySet()) {
            fullRegenerateZone(level, zoneId);
        }

        // Clear all diamond locations in zones
        for (SearchDiamondZone zone : SearchDiamondZoneManager.getAllZones(level)) {
            zone.setDiamondLocation(null);
        }

        // Now clear session state
        activeSessions.clear();
        savedZoneBlocks.clear();

        AromaAffect.LOGGER.info("SearchDiamond reset complete - all zones regenerated and state cleared");
    }

    /**
     * Checks if a player can break a block (is in active session and inside zone).
     */
    public static boolean canPlayerBreakAt(ServerPlayer player, BlockPos pos, ServerLevel level) {
        UUID playerId = player.getUUID();
        ActiveSession session = activeSessions.get(playerId);
        if (session == null) return false;

        SearchDiamondZone zone = SearchDiamondZoneManager.getZone(level, session.zoneId);
        return zone != null && zone.isInsideArea(pos);
    }

    /**
     * Checks if a player has an active SearchDiamond session (regardless of position).
     */
    public static boolean hasActiveSession(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID());
    }

    /**
     * Active game session for a player.
     */
    private static class ActiveSession {
        final String zoneId;
        final Map<BlockPos, BlockState> brokenBlocks = new HashMap<>();

        ActiveSession(String zoneId) {
            this.zoneId = zoneId;
        }
    }
}
