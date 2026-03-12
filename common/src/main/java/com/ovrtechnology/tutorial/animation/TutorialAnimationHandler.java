package com.ovrtechnology.tutorial.animation;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.network.TutorialAnimationNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Server-side handler that manages active tutorial animations.
 * <p>
 * When an animation is played, this handler progressively removes blocks
 * in layers with particles and sounds, creating a dramatic visual effect.
 * <p>
 * Layer patterns per type:
 * <ul>
 *   <li>WALL_BREAK: layers from center outward (explosion pattern)</li>
 *   <li>DOOR_OPEN: layers from center splitting left/right</li>
 *   <li>DEBRIS_CLEAR: layers from top to bottom</li>
 * </ul>
 */
public final class TutorialAnimationHandler {

    private static final int TICKS_BETWEEN_LAYERS = 7;

    // OVR Purple dust particle
    private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(
            0xFFA890F0,  // OVR Purple
            1.2f
    );

    private static final Map<String, ActiveAnimation> activeAnimations = new HashMap<>();
    private static boolean initialized = false;

    private TutorialAnimationHandler() {
    }

    /**
     * Initializes the animation handler.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (!TutorialModule.isActive(level)) {
                    continue;
                }
                tickAnimations(level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial animation handler initialized");
    }

    /**
     * Checks if any animation is currently active in any level.
     *
     * @return true if at least one animation is playing
     */
    public static boolean isAnyAnimationActive() {
        return !activeAnimations.isEmpty();
    }

    /**
     * Stops all active animations for a level without completing them.
     * Called during tutorial reset to prevent in-progress animations from
     * continuing to destroy blocks after reset restores them.
     *
     * @param level the server level
     * @return number of animations stopped
     */
    public static int stopAllActiveAnimations(ServerLevel level) {
        int count = 0;
        Iterator<Map.Entry<String, ActiveAnimation>> it = activeAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActiveAnimation> entry = it.next();
            if (entry.getValue().level == level) {
                AromaAffect.LOGGER.info("Force-stopped active animation {}", entry.getKey());
                it.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * Starts playing an animation.
     *
     * @param level       the server level
     * @param animationId the animation ID
     * @return true if started, false if animation not found or not ready
     */
    public static boolean play(ServerLevel level, String animationId) {
        var animOpt = TutorialAnimationManager.getAnimation(level, animationId);
        if (animOpt.isEmpty()) {
            return false;
        }

        TutorialAnimation animation = animOpt.get();
        if (!animation.isComplete()) {
            return false;
        }

        // Already active
        if (activeAnimations.containsKey(animationId)) {
            return false;
        }

        BlockPos corner1 = animation.getCorner1();
        BlockPos corner2 = animation.getCorner2();

        // Compute layers based on type (validate before snapshotting)
        List<List<BlockPos>> layers = computeLayers(animation.getType(), corner1, corner2);
        if (layers.isEmpty()) {
            AromaAffect.LOGGER.warn("Animation {} region is empty, skipping", animationId);
            return false;
        }

        // Snapshot all non-air blocks before removal so they can be restored on reset
        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        Map<BlockPos, String> snapshot = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        snapshot.put(pos, TutorialAnimationManager.serializeBlockState(state));
                    }
                }
            }
        }
        TutorialAnimationManager.setSavedBlocks(level, animationId, snapshot);

        ActiveAnimation active = new ActiveAnimation(
                animationId,
                animation.getType(),
                corner1,
                corner2,
                layers,
                level
        );
        activeAnimations.put(animationId, active);

        // Send S2C packet for client-side particles
        for (ServerPlayer player : level.players()) {
            TutorialAnimationNetworking.sendAnimationPlay(
                    player, animation.getType(), corner1, corner2
            );
        }

        // Play dramatic start sound
        double cx = (corner1.getX() + corner2.getX()) / 2.0 + 0.5;
        double cy = (corner1.getY() + corner2.getY()) / 2.0 + 0.5;
        double cz = (corner1.getZ() + corner2.getZ()) / 2.0 + 0.5;
        playStartSound(level, animation.getType(), cx, cy, cz);

        AromaAffect.LOGGER.info("Started animation {}", animationId);
        return true;
    }

    private static void tickAnimations(ServerLevel level) {
        if (activeAnimations.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<String, ActiveAnimation>> it = activeAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActiveAnimation> entry = it.next();
            ActiveAnimation active = entry.getValue();

            if (active.level != level) {
                continue;
            }

            active.tickCounter++;
            if (active.tickCounter < TICKS_BETWEEN_LAYERS) {
                continue;
            }
            active.tickCounter = 0;

            if (active.currentLayer >= active.layers.size()) {
                // Animation complete
                TutorialAnimationManager.markPlayed(level, active.animationId);
                it.remove();

                // Play completion sound
                double cx = (active.corner1.getX() + active.corner2.getX()) / 2.0 + 0.5;
                double cy = (active.corner1.getY() + active.corner2.getY()) / 2.0 + 0.5;
                double cz = (active.corner1.getZ() + active.corner2.getZ()) / 2.0 + 0.5;
                playCompleteSound(level, active.type, cx, cy, cz);

                AromaAffect.LOGGER.info("Animation {} completed", active.animationId);
                onAnimationComplete(level, active.animationId);
                continue;
            }

            // Process current layer
            List<BlockPos> layer = active.layers.get(active.currentLayer);
            processLayer(level, active, layer);
            active.currentLayer++;
        }
    }

    /**
     * Called when an animation completes. Executes on-complete hooks for all players in the level.
     */
    private static void onAnimationComplete(ServerLevel level, String animationId) {
        var animOpt = TutorialAnimationManager.getAnimation(level, animationId);
        if (animOpt.isEmpty()) {
            return;
        }

        TutorialAnimation animation = animOpt.get();

        // Execute Oliver action for all players in level
        if (animation.hasOnCompleteOliverAction()) {
            for (ServerPlayer player : level.players()) {
                executeOliverAction(player, level, animation.getOnCompleteOliverAction());
            }
        }

        // Start cinematic for all players in level
        if (animation.hasOnCompleteCinematic()) {
            for (ServerPlayer player : level.players()) {
                TutorialCinematicHandler.startCinematic(player, animation.getOnCompleteCinematicId());
            }
        }

        // Activate waypoint for all players in level
        if (animation.hasOnCompleteWaypoint()) {
            String waypointId = animation.getOnCompleteWaypointId();
            java.util.Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                for (ServerPlayer player : level.players()) {
                    TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                    TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
                }
                AromaAffect.LOGGER.debug("Activated waypoint {} after animation {}", waypointId, animationId);
            } else {
                AromaAffect.LOGGER.warn("On-complete waypoint {} not found or incomplete", waypointId);
            }
        }
    }

    /**
     * Executes Oliver action(s) for a player.
     * Multiple actions can be separated by semicolon (;).
     * Two-pass: player-only actions first (clearinventory, teleportplayer),
     * then Oliver-dependent actions.
     */
    private static void executeOliverAction(ServerPlayer player, ServerLevel level, String action) {
        String[] actions = action.split(";");

        // First pass: player-only actions (don't need Oliver)
        for (String singleAction : actions) {
            singleAction = singleAction.trim();
            if (singleAction.isEmpty()) continue;
            String actionLower = singleAction.toLowerCase();

            if (actionLower.startsWith("clearinventory:")) {
                String keepStr = singleAction.substring(15);
                java.util.Set<String> keepItems = new java.util.HashSet<>(java.util.Arrays.asList(keepStr.split(",")));
                com.ovrtechnology.tutorial.trade.TutorialTradeHandler.clearInventoryKeeping(player, keepItems);
            } else if (actionLower.startsWith("teleportplayer:")) {
                String coordsStr = singleAction.substring(15);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        com.ovrtechnology.tutorial.noseequip.TutorialNoseEquipHandler.performEpicTeleport(player, level, x, y, z);
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid teleportplayer coordinates: {}", coordsStr);
                    }
                }
            }
        }

        // Second pass: Oliver-dependent actions
        TutorialOliverEntity oliver = findNearestOliver(player, level);
        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found nearby", action);
            return;
        }

        String pendingDialogueId = null;

        for (String singleAction : actions) {
            singleAction = singleAction.trim();
            if (singleAction.isEmpty()) continue;

            String actionLower = singleAction.toLowerCase();

            if (actionLower.equals("follow")) {
                oliver.setFollowing(player);
            } else if (actionLower.equals("stop")) {
                oliver.setStationary();
            } else if (actionLower.startsWith("walkto:")) {
                String coordsStr = singleAction.substring(7);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        oliver.setWalkingTo(new BlockPos(x, y, z));
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid walkto coordinates: {}", coordsStr);
                    }
                }
            } else if (actionLower.startsWith("dialogue:")) {
                pendingDialogueId = singleAction.substring(9).trim();
                oliver.setDialogueId(pendingDialogueId);
            } else if (actionLower.startsWith("trade:")) {
                oliver.setTradeId(singleAction.substring(6).trim());
            } else if (actionLower.startsWith("lookup:")) {
                String blockId = singleAction.substring(7).trim();
                triggerLookup(player, level, blockId);
            }
            // clearinventory and teleportplayer already handled in first pass
        }

        // Open dialogue AFTER processing all actions (so trade is set first)
        if (pendingDialogueId != null) {
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), pendingDialogueId,
                    oliver.hasTrade(), oliver.getTradeId()
            );
        }
    }

    private static void triggerLookup(ServerPlayer player, ServerLevel level, String blockId) {
        LookupTarget target = LookupTarget.block(ResourceLocation.parse(blockId));
        BlockPos origin = player.blockPosition();

        LookupManager.getInstance().lookupAsync(level, origin, target, result -> {
            if (result.isSuccess()) {
                BlockPos destination = result.getPosition();
                ActivePathManager.getInstance().createPath(
                        player, level, destination,
                        ActivePathManager.TargetType.BLOCK, blockId
                );
                int distance = (int) Math.sqrt(origin.distSqr(destination));
                PathScentNetworking.sendPathFound(player, distance, destination);
                AromaAffect.LOGGER.debug("Tutorial lookup found {} at {} for player {}",
                        blockId, destination, player.getName().getString());
            } else {
                AromaAffect.LOGGER.warn("Tutorial lookup failed for {}: {}",
                        blockId, result.failureReason());
            }
        });
    }

    /**
     * Finds the nearest Oliver entity to a player.
     */
    private static TutorialOliverEntity findNearestOliver(ServerPlayer player, ServerLevel level) {
        AABB searchArea = new AABB(
                player.getX() - 100, player.getY() - 50, player.getZ() - 100,
                player.getX() + 100, player.getY() + 50, player.getZ() + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class,
                searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    private static void processLayer(ServerLevel level, ActiveAnimation active, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            // Spawn block break particles before removing
            level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8,    // count
                    0.3,  // xSpread
                    0.3,  // ySpread
                    0.3,  // zSpread
                    0.1   // speed
            );

            // Remove block
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        // Spawn type-specific particles
        spawnTypeParticles(level, active.type, positions);

        // Play type-specific sound
        playTypeSound(level, active, positions);
    }

    private static void spawnTypeParticles(ServerLevel level, TutorialAnimationType type, List<BlockPos> positions) {
        if (positions.isEmpty()) {
            return;
        }

        // Calculate center of layer
        double avgX = 0, avgY = 0, avgZ = 0;
        for (BlockPos pos : positions) {
            avgX += pos.getX() + 0.5;
            avgY += pos.getY() + 0.5;
            avgZ += pos.getZ() + 0.5;
        }
        avgX /= positions.size();
        avgY /= positions.size();
        avgZ /= positions.size();

        switch (type) {
            case WALL_BREAK -> {
                // Explosion + campfire smoke + purple dust
                level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        avgX, avgY, avgZ,
                        2, 0.5, 0.5, 0.5, 0.0
                );
                level.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        avgX, avgY, avgZ,
                        6, 0.8, 0.5, 0.8, 0.03
                );
                level.sendParticles(
                        PURPLE_DUST,
                        avgX, avgY, avgZ,
                        10, 0.6, 0.6, 0.6, 0.0
                );
            }
            case DOOR_OPEN -> {
                // Purple dust swoosh + end rod sparkles
                level.sendParticles(
                        PURPLE_DUST,
                        avgX, avgY, avgZ,
                        15, 1.0, 0.5, 0.3, 0.0
                );
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        avgX, avgY, avgZ,
                        8, 0.5, 0.5, 0.5, 0.05
                );
            }
            case DEBRIS_CLEAR -> {
                // Cherry leaves + end rod + enchant rising upward
                level.sendParticles(
                        ParticleTypes.CHERRY_LEAVES,
                        avgX, avgY + 0.5, avgZ,
                        12, 0.6, 0.3, 0.6, 0.04
                );
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        avgX, avgY + 0.5, avgZ,
                        6, 0.4, 0.2, 0.4, 0.06
                );
                level.sendParticles(
                        ParticleTypes.ENCHANT,
                        avgX, avgY, avgZ,
                        10, 0.5, 0.5, 0.5, 0.8
                );
            }
        }
    }

    private static void playTypeSound(ServerLevel level, ActiveAnimation active, List<BlockPos> positions) {
        if (positions.isEmpty()) {
            return;
        }

        // Use center of the animation region for sound
        double cx = (active.corner1.getX() + active.corner2.getX()) / 2.0 + 0.5;
        double cy = (active.corner1.getY() + active.corner2.getY()) / 2.0 + 0.5;
        double cz = (active.corner1.getZ() + active.corner2.getZ()) / 2.0 + 0.5;

        switch (active.type) {
            case WALL_BREAK -> level.playSound(
                    null, cx, cy, cz,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f
            );
            case DOOR_OPEN -> level.playSound(
                    null, cx, cy, cz,
                    SoundEvents.PISTON_EXTEND,
                    SoundSource.BLOCKS,
                    1.0f, 0.8f + level.getRandom().nextFloat() * 0.4f
            );
            case DEBRIS_CLEAR -> level.playSound(
                    null, cx, cy, cz,
                    SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.BLOCKS,
                    1.0f, 1.0f + level.getRandom().nextFloat() * 0.3f
            );
        }
    }

    /**
     * Plays a dramatic sound when an animation starts.
     */
    private static void playStartSound(ServerLevel level, TutorialAnimationType type,
                                        double cx, double cy, double cz) {
        switch (type) {
            case WALL_BREAK -> {
                // TNT fuse hiss + big explosion
                level.playSound(null, cx, cy, cz,
                        SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.5f, 1.0f);
                level.playSound(null, cx, cy, cz,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.2f, 0.7f);
            }
            case DOOR_OPEN -> {
                // Iron door creak + piston
                level.playSound(null, cx, cy, cz,
                        SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5f, 0.6f);
                level.playSound(null, cx, cy, cz,
                        SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.2f, 0.5f);
            }
            case DEBRIS_CLEAR -> {
                // Enchant + amethyst shimmer
                level.playSound(null, cx, cy, cz,
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 0.8f);
                level.playSound(null, cx, cy, cz,
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Plays a satisfying sound when an animation completes.
     */
    private static void playCompleteSound(ServerLevel level, TutorialAnimationType type,
                                           double cx, double cy, double cz) {
        switch (type) {
            case WALL_BREAK -> {
                // Settling debris + anvil land
                level.playSound(null, cx, cy, cz,
                        SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.8f);
                level.playSound(null, cx, cy, cz,
                        SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0f, 0.6f);
            }
            case DOOR_OPEN -> {
                // Heavy thud
                level.playSound(null, cx, cy, cz,
                        SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.2f, 0.5f);
                level.playSound(null, cx, cy, cz,
                        SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0f, 0.7f);
            }
            case DEBRIS_CLEAR -> {
                // Magical completion
                level.playSound(null, cx, cy, cz,
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                level.playSound(null, cx, cy, cz,
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2f, 1.5f);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Layer computation
    // ─────────────────────────────────────────────────────────────────────────────

    private static List<List<BlockPos>> computeLayers(TutorialAnimationType type,
                                                       BlockPos corner1, BlockPos corner2) {
        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return switch (type) {
            case WALL_BREAK -> computeWallBreakLayers(minX, maxX, minY, maxY, minZ, maxZ);
            case DOOR_OPEN -> computeDoorOpenLayers(minX, maxX, minY, maxY, minZ, maxZ);
            case DEBRIS_CLEAR -> computeDebrisClearLayers(minX, maxX, minY, maxY, minZ, maxZ);
        };
    }

    /**
     * WALL_BREAK: layers from center outward (explosion pattern).
     * Blocks closest to the center are removed first.
     */
    private static List<List<BlockPos>> computeWallBreakLayers(
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        // Collect all blocks with their distance to center
        List<BlockDistEntry> entries = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dist = Math.sqrt(
                            (x + 0.5 - centerX) * (x + 0.5 - centerX) +
                            (y + 0.5 - centerY) * (y + 0.5 - centerY) +
                            (z + 0.5 - centerZ) * (z + 0.5 - centerZ)
                    );
                    entries.add(new BlockDistEntry(new BlockPos(x, y, z), dist));
                }
            }
        }

        entries.sort((a, b) -> Double.compare(a.distance, b.distance));
        return distributeIntoLayers(entries);
    }

    /**
     * DOOR_OPEN: layers from center splitting left/right.
     * Uses the longest horizontal axis for splitting.
     */
    private static List<List<BlockPos>> computeDoorOpenLayers(
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        // Determine the main horizontal axis (the wider one)
        int xSpan = maxX - minX;
        int zSpan = maxZ - minZ;
        boolean splitOnX = xSpan >= zSpan;

        double center = splitOnX
                ? (minX + maxX) / 2.0
                : (minZ + maxZ) / 2.0;

        // Sort by distance from center axis
        List<BlockDistEntry> entries = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dist = splitOnX
                            ? Math.abs(x + 0.5 - center)
                            : Math.abs(z + 0.5 - center);
                    entries.add(new BlockDistEntry(new BlockPos(x, y, z), dist));
                }
            }
        }

        entries.sort((a, b) -> Double.compare(a.distance, b.distance));
        return distributeIntoLayers(entries);
    }

    /**
     * DEBRIS_CLEAR: layers from top to bottom.
     */
    private static List<List<BlockPos>> computeDebrisClearLayers(
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        List<List<BlockPos>> layers = new ArrayList<>();

        // Each Y-level is a layer, from top to bottom
        for (int y = maxY; y >= minY; y--) {
            List<BlockPos> layer = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    layer.add(new BlockPos(x, y, z));
                }
            }
            layers.add(layer);
        }

        return layers;
    }

    /**
     * Distributes sorted block entries into ~6 layers.
     */
    private static List<List<BlockPos>> distributeIntoLayers(List<BlockDistEntry> entries) {
        int targetLayers = Math.min(6, Math.max(2, entries.size() / 4));
        int blocksPerLayer = Math.max(1, entries.size() / targetLayers);

        List<List<BlockPos>> layers = new ArrayList<>();
        List<BlockPos> currentLayer = new ArrayList<>();

        for (BlockDistEntry entry : entries) {
            currentLayer.add(entry.pos);
            if (currentLayer.size() >= blocksPerLayer && layers.size() < targetLayers - 1) {
                layers.add(currentLayer);
                currentLayer = new ArrayList<>();
            }
        }

        if (!currentLayer.isEmpty()) {
            layers.add(currentLayer);
        }

        return layers;
    }

    private record BlockDistEntry(BlockPos pos, double distance) {}

    // ─────────────────────────────────────────────────────────────────────────────
    // Active animation state
    // ─────────────────────────────────────────────────────────────────────────────

    private static class ActiveAnimation {
        final String animationId;
        final TutorialAnimationType type;
        final BlockPos corner1;
        final BlockPos corner2;
        final List<List<BlockPos>> layers;
        final ServerLevel level;
        int currentLayer;
        int tickCounter;

        ActiveAnimation(String animationId, TutorialAnimationType type,
                        BlockPos corner1, BlockPos corner2,
                        List<List<BlockPos>> layers, ServerLevel level) {
            this.animationId = animationId;
            this.type = type;
            this.corner1 = corner1;
            this.corner2 = corner2;
            this.layers = layers;
            this.level = level;
            this.currentLayer = 0;
            this.tickCounter = 0;
        }
    }
}
