package com.ovrtechnology.tutorial.boss;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Handles tick-based checking for boss area triggers.
 * When a player enters a trigger area, spawns the corresponding boss.
 * Tracks spawned bosses to prevent infinite respawning.
 */
public final class TutorialBossAreaHandler {

    private static boolean initialized = false;

    // Track which areas have active bosses (key = area ID, value = boss entity UUID)
    private static final Map<String, UUID> activeBosses = new HashMap<>();

    // Track when bosses were spawned (key = area ID, value = spawn timestamp)
    private static final Map<String, Long> bossSpawnTimes = new HashMap<>();
    private static final long BOSS_GRACE_PERIOD_MS = 3000; // 3 seconds grace period after spawn

    // Track cooldowns to prevent spam checking (key = area ID + player UUID)
    private static final Map<String, Long> spawnCooldowns = new HashMap<>();
    private static final long SPAWN_COOLDOWN_MS = 5000; // 5 seconds

    private TutorialBossAreaHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register server tick handler
        TickEvent.SERVER_POST.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (!TutorialModule.isActive(level)) {
                    continue;
                }

                tickLevel(level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial boss area handler initialized");
    }

    private static void tickLevel(ServerLevel level) {
        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);

        // Clean up dead bosses
        cleanupDeadBosses(level);

        // Check each complete area
        for (TutorialBossArea area : manager.getCompleteAreas()) {
            // Skip if boss already exists for this area
            if (activeBosses.containsKey(area.getId())) {
                UUID bossUuid = activeBosses.get(area.getId());
                Entity boss = level.getEntity(bossUuid);
                if (boss != null && boss.isAlive()) {
                    // Boss is alive, enforce movement bounds
                    enforceMovementBounds(boss, area);
                    continue;
                }
                // Check if within grace period (entity might not be loaded yet)
                Long spawnTime = bossSpawnTimes.get(area.getId());
                if (spawnTime != null && System.currentTimeMillis() - spawnTime < BOSS_GRACE_PERIOD_MS) {
                    continue; // Within grace period, don't spawn another
                }
                // Boss is dead or removed, clear it
                activeBosses.remove(area.getId());
                bossSpawnTimes.remove(area.getId());
            }

            // Check if any player is in the trigger area
            for (ServerPlayer player : level.players()) {
                BlockPos playerPos = player.blockPosition();

                if (area.isInTriggerArea(playerPos)) {
                    // Check if player has already triggered this area (persistent check)
                    if (manager.hasPlayerTriggered(area.getId(), player.getUUID())) {
                        continue; // Player already triggered this area, skip
                    }

                    // Check cooldown (prevents spam within same session)
                    String cooldownKey = area.getId() + "_" + player.getUUID();
                    Long lastSpawn = spawnCooldowns.get(cooldownKey);
                    long now = System.currentTimeMillis();

                    if (lastSpawn != null && now - lastSpawn < SPAWN_COOLDOWN_MS) {
                        continue;
                    }

                    // Mark player as triggered BEFORE spawning (persistent)
                    manager.markPlayerTriggered(area.getId(), player.getUUID());

                    // Spawn the boss
                    spawnCooldowns.put(cooldownKey, now);
                    spawnBossForArea(level, area, player);
                    break; // Only spawn once per area per tick
                }
            }
        }
    }

    private static void cleanupDeadBosses(ServerLevel level) {
        long now = System.currentTimeMillis();
        activeBosses.entrySet().removeIf(entry -> {
            String areaId = entry.getKey();

            // Don't remove if within grace period (boss might still be loading)
            Long spawnTime = bossSpawnTimes.get(areaId);
            if (spawnTime != null && now - spawnTime < BOSS_GRACE_PERIOD_MS) {
                return false;
            }

            Entity boss = level.getEntity(entry.getValue());
            if (boss == null || !boss.isAlive()) {
                bossSpawnTimes.remove(areaId); // Clean up spawn time too
                return true;
            }
            return false;
        });
    }

    private static final double DEFAULT_MOVEMENT_RADIUS = 8.0; // Default radius around spawn
    private static final double DEFAULT_MOVEMENT_HEIGHT = 6.0; // Default height range

    private static void enforceMovementBounds(Entity boss, TutorialBossArea area) {
        BlockPos min;
        BlockPos max;

        if (area.hasMovementArea()) {
            min = area.getMovementMin();
            max = area.getMovementMax();
        } else if (area.hasSpawnPos()) {
            // Use default area around spawn point
            BlockPos spawn = area.getSpawnPos();
            int radius = (int) DEFAULT_MOVEMENT_RADIUS;
            int height = (int) DEFAULT_MOVEMENT_HEIGHT;
            min = new BlockPos(spawn.getX() - radius, spawn.getY() - 2, spawn.getZ() - radius);
            max = new BlockPos(spawn.getX() + radius, spawn.getY() + height, spawn.getZ() + radius);
        } else {
            return;
        }

        double x = boss.getX();
        double y = boss.getY();
        double z = boss.getZ();

        boolean needsCorrection = false;
        double newX = x;
        double newY = y;
        double newZ = z;

        // Check X bounds
        if (x < min.getX()) {
            newX = min.getX() + 0.5;
            needsCorrection = true;
        } else if (x > max.getX() + 1) {
            newX = max.getX() + 0.5;
            needsCorrection = true;
        }

        // Check Y bounds
        if (y < min.getY()) {
            newY = min.getY();
            needsCorrection = true;
        } else if (y > max.getY() + 1) {
            newY = max.getY();
            needsCorrection = true;
        }

        // Check Z bounds
        if (z < min.getZ()) {
            newZ = min.getZ() + 0.5;
            needsCorrection = true;
        } else if (z > max.getZ() + 1) {
            newZ = max.getZ() + 0.5;
            needsCorrection = true;
        }

        if (needsCorrection) {
            boss.teleportTo(newX, newY, newZ);
            boss.setDeltaMovement(0, 0, 0); // Stop momentum
            boss.hurtMarked = true; // Force position sync to client
        }
    }

    private static void spawnBossForArea(ServerLevel level, TutorialBossArea area, ServerPlayer triggerPlayer) {
        // Double-check: don't spawn if boss already exists or was recently spawned
        if (activeBosses.containsKey(area.getId())) {
            AromaAffect.LOGGER.debug("Boss already tracked for area '{}', skipping spawn", area.getId());
            return;
        }

        // Extra safety: check if there's already a tagged boss near the spawn point
        if (hasExistingBossNearSpawn(level, area)) {
            AromaAffect.LOGGER.debug("Found existing boss near spawn for area '{}', skipping", area.getId());
            return;
        }

        // Mark as spawning BEFORE actually spawning to prevent race conditions
        long now = System.currentTimeMillis();
        bossSpawnTimes.put(area.getId(), now);

        AromaAffect.LOGGER.info("Player {} triggered boss area '{}' (type: {})",
                triggerPlayer.getName().getString(), area.getId(), area.getBossType());

        // Spawn the boss (with particle/sound effects as the "animation")
        Entity boss = TutorialBossSpawner.spawnBossEntity(level, area.getBossType(), area.getSpawnPos(), area);

        if (boss != null) {
            // Track the boss
            activeBosses.put(area.getId(), boss.getUUID());
            AromaAffect.LOGGER.info("Boss spawned for area '{}', UUID: {}", area.getId(), boss.getUUID());

            // AFTER boss spawns, trigger Oliver dialogue
            TutorialOliverEntity oliver = findNearestOliver(level,
                    triggerPlayer.getX(), triggerPlayer.getY(), triggerPlayer.getZ());

            if (oliver != null) {
                String dialogueId = "boss_" + area.getBossType().toLowerCase() + "_enter";
                oliver.setDialogueId(dialogueId);
                TutorialDialogueContentNetworking.sendOpenDialogue(
                        triggerPlayer, oliver.getId(), dialogueId, false, ""
                );
                AromaAffect.LOGGER.info("Triggered Oliver dialogue '{}' after boss spawn", dialogueId);
            }
        } else {
            // Spawn failed, remove the spawn time marker
            bossSpawnTimes.remove(area.getId());
        }
    }

    /**
     * Check if there's already a tutorial boss near the spawn point.
     */
    private static boolean hasExistingBossNearSpawn(ServerLevel level, TutorialBossArea area) {
        BlockPos spawn = area.getSpawnPos();
        if (spawn == null) return false;

        AABB searchArea = new AABB(
                spawn.getX() - 30, spawn.getY() - 20, spawn.getZ() - 30,
                spawn.getX() + 30, spawn.getY() + 20, spawn.getZ() + 30
        );

        String bossTag = "tutorial_boss_" + area.getBossType().toLowerCase();

        // Check for any entity with the tutorial boss tag
        List<Blaze> blazes = level.getEntitiesOfClass(Blaze.class, searchArea,
                entity -> entity.getTags().contains(bossTag));

        return !blazes.isEmpty();
    }

    /**
     * Find the nearest Oliver entity within search range.
     */
    private static TutorialOliverEntity findNearestOliver(ServerLevel level, double x, double y, double z) {
        AABB searchArea = new AABB(
                x - 100, y - 50, z - 100,
                x + 100, y + 50, z + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class, searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(x, y, z);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    /**
     * Check if a boss is currently active for the given area.
     */
    public static boolean isBossActive(String areaId) {
        return activeBosses.containsKey(areaId);
    }

    /**
     * Clear all tracked bosses (e.g., for reset).
     */
    public static void clearAllBosses() {
        activeBosses.clear();
        bossSpawnTimes.clear();
        spawnCooldowns.clear();
    }

    /**
     * Force remove a boss from tracking (e.g., when manually killed).
     */
    public static void removeBoss(String areaId) {
        activeBosses.remove(areaId);
        bossSpawnTimes.remove(areaId);
    }
}
