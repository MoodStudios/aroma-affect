package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.MobTriggerDefinition;
import com.ovrtechnology.trigger.config.PassiveModeConfig;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PassiveModeManager {

    private static final boolean DEV_MODE = true;

    private static final int CHECK_INTERVAL_TICKS = 20;

    private static double getMobActivationRange() {
        return ClientConfig.getInstance().getPassiveMobRange();
    }

    private static double getBlockActivationRange() {
        return ClientConfig.getInstance().getPassiveBlockRange();
    }

    private static final double LOOK_AT_ANGLE_DEGREES = 30.0;

    private static final double LOOK_AT_COS_THRESHOLD =
            Math.cos(Math.toRadians(LOOK_AT_ANGLE_DEGREES));

    private static final int STARTUP_DELAY_TICKS = 100;

    private static final Set<String> HOSTILE_MOBS =
            Set.of(
                    "minecraft:zombie",
                    "minecraft:skeleton",
                    "minecraft:creeper",
                    "minecraft:spider",
                    "minecraft:cave_spider",
                    "minecraft:enderman",
                    "minecraft:blaze",
                    "minecraft:ghast",
                    "minecraft:magma_cube",
                    "minecraft:piglin",
                    "minecraft:hoglin",
                    "minecraft:strider",
                    "minecraft:wither_skeleton",
                    "minecraft:ender_dragon",
                    "minecraft:wither",
                    "minecraft:warden",
                    "minecraft:elder_guardian");

    private static ScentTrigger currentPassiveTrigger = null;

    private static String currentTriggerSource = null;

    private static TriggerType currentTriggerType = null;

    private static String lastBiomeId = null;

    private static final Map<TriggerType, Long> typeTriggerTimes = new HashMap<>();

    private static String lastStructureId = null;

    private static volatile String serverStructureId = null;

    private static int tickCounter = 0;
    private static int startupTicksElapsed = 0;
    private static boolean startupComplete = false;

    private PassiveModeManager() {}

    public static void tick(Player player) {
        if (player == null) {
            AromaAffect.LOGGER.warn("Passive-mode tick called with null player!");
            return;
        }

        if (!isPassiveModeEnabled()) {
            clearPassiveScent(player);
            return;
        }

        if (ActiveTrackingState.isTracking()) {
            clearPassiveScent(player);
            return;
        }

        if (!startupComplete) {
            if (++startupTicksElapsed < STARTUP_DELAY_TICKS) {
                return;
            }
            startupComplete = true;
            AromaAffect.LOGGER.info(
                    "[PassiveMode] Startup delay complete, beginning passive scans");
        }

        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        AromaAffect.LOGGER.debug(
                "[PassiveModeManager] Processing tick (every {} ticks)", CHECK_INTERVAL_TICKS);

        if (!DEV_MODE && !OvrWebSocketClient.getInstance().isConnected()) {
            AromaAffect.LOGGER.debug("Passive-mode disabled: OVR hardware not connected");
            clearPassiveScent(player);
            return;
        }

        if (currentPassiveTrigger != null) {
            ScentTrigger activeScent = ScentTriggerManager.getInstance().getActiveScent();
            if (activeScent == null || activeScent.source() != ScentTriggerSource.PASSIVE_MODE) {

                AromaAffect.LOGGER.debug(
                        "Passive trigger was replaced or expired, clearing state to re-evaluate");
                currentPassiveTrigger = null;
                currentTriggerSource = null;
                currentTriggerType = null;
            }
        }

        Level level = player.level();
        if (!level.isClientSide()) {
            return;
        }

        BlockPos playerPos = player.blockPosition();

        TriggerCandidate mobCandidate = evaluateMobTriggers(level, player);
        TriggerCandidate blockCandidate = evaluateBlockTriggers(level, playerPos, player);
        TriggerCandidate structureCandidate = evaluateStructureTriggers(level, playerPos);
        TriggerCandidate biomeCandidate = evaluateBiomeTriggers(level, playerPos);

        TriggerCandidate bestCandidate = mobCandidate;
        if (bestCandidate == null) bestCandidate = blockCandidate;
        if (bestCandidate == null) bestCandidate = structureCandidate;
        if (bestCandidate == null) bestCandidate = biomeCandidate;

        if (bestCandidate != null) {

            if (canActivateScent(bestCandidate)) {
                activateTrigger(player, bestCandidate);
            }

            return;
        }

        if (currentPassiveTrigger != null) {
            AromaAffect.LOGGER.debug("No trigger candidates nearby, clearing active scent");
            clearPassiveScent(player);
        }
    }

    private enum TriggerType {
        BLOCK,
        MOB,
        STRUCTURE,
        BIOME
    }

    private record TriggerCandidate(
            ScentTrigger trigger,
            String source,
            String displayName,
            TriggerType type,
            int range,
            double distance) {}

    private static boolean canActivateScent(TriggerCandidate candidate) {
        String scentName = candidate.trigger.scentName();
        long cooldownMs = getEffectiveCooldown(candidate);
        long now = System.currentTimeMillis();

        if (candidate.type == TriggerType.MOB && isHostileMob(candidate.source)) {
            ScentTrigger activeScent = ScentTriggerManager.getInstance().getActiveScent();
            if (activeScent != null
                    && activeScent.source() == ScentTriggerSource.PASSIVE_MODE
                    && currentTriggerType != TriggerType.MOB) {
                if (DEV_MODE) {
                    AromaAffect.LOGGER.info(
                            "[PassiveMode] HOSTILE MOB INTERRUPT: {} at distance {}",
                            scentName,
                            candidate.distance);
                }
                return true;
            }
        }

        Long lastTriggerTime = typeTriggerTimes.get(candidate.type);
        if (lastTriggerTime != null && (now - lastTriggerTime) < cooldownMs) {
            AromaAffect.LOGGER.debug(
                    "[PassiveMode] Blocked by type cooldown: {} ({}ms remaining)",
                    candidate.type,
                    cooldownMs - (now - lastTriggerTime));
            return false;
        }

        return true;
    }

    private static long getCooldownForType(TriggerType type) {
        ClientConfig config = ClientConfig.getInstance();
        return switch (type) {
            case BLOCK -> config.getPassiveBlockCooldownMs();
            case MOB -> config.getPassiveMobCooldownMs();
            case STRUCTURE -> ScentTriggerConfigLoader.getSettings().getStructureCooldownMs();
            case BIOME -> ScentTriggerConfigLoader.getSettings().getBiomeCooldownMs();
        };
    }

    private static long getEffectiveCooldown(TriggerCandidate candidate) {

        if (candidate.type == TriggerType.MOB && !isHostileMob(candidate.source)) {
            return ClientConfig.getInstance().getPassivePassiveMobCooldownMs();
        }
        return getCooldownForType(candidate.type);
    }

    private static final double MIN_INTENSITY = 0.3;

    private static final double FULL_INTENSITY_DISTANCE = 1.5;

    private static double calculateIntensityByDistance(double distance, double maxRange) {

        if (distance <= FULL_INTENSITY_DISTANCE) {
            return 1.0;
        }

        if (maxRange <= FULL_INTENSITY_DISTANCE) {
            return 1.0;
        }

        double effectiveRange = maxRange - FULL_INTENSITY_DISTANCE;
        double effectiveDistance = distance - FULL_INTENSITY_DISTANCE;

        double ratio = effectiveDistance / effectiveRange;
        double intensity = 1.0 - (ratio * (1.0 - MIN_INTENSITY));

        return Math.max(MIN_INTENSITY, Math.min(1.0, intensity));
    }

    private static TriggerCandidate evaluateBlockTriggers(
            Level level, BlockPos playerPos, Player player) {
        int searchRange = (int) Math.ceil(getBlockActivationRange());
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 viewDir = player.getViewVector(1.0f).normalize();
        double activationRange = getBlockActivationRange();

        Map<Block, BlockTriggerDefinition> triggersByBlock = new HashMap<>();
        for (BlockTriggerDefinition trigger : ScentTriggerConfigLoader.getAllBlockTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) continue;
            try {
                ResourceLocation loc = Ids.parse(trigger.getBlockId());
                level.registryAccess()
                        .lookupOrThrow(Registries.BLOCK)
                        .getOptional(loc)
                        .ifPresent(block -> triggersByBlock.put(block, trigger));
            } catch (Exception e) {

            }
        }
        if (triggersByBlock.isEmpty()) return null;

        TriggerCandidate bestCandidate = null;
        double bestDot = -1;

        for (int x = -searchRange; x <= searchRange; x++) {
            for (int y = -searchRange; y <= searchRange; y++) {
                for (int z = -searchRange; z <= searchRange; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    Block block = level.getBlockState(checkPos).getBlock();

                    BlockTriggerDefinition trigger = triggersByBlock.get(block);
                    if (trigger == null) continue;

                    double distance = Math.sqrt(playerPos.distSqr(checkPos));
                    if (distance > activationRange) continue;

                    Vec3 toBlock = Vec3.atCenterOf(checkPos).subtract(eyePos).normalize();
                    double dot = viewDir.dot(toBlock);

                    if (dot >= LOOK_AT_COS_THRESHOLD && dot > bestDot) {
                        bestDot = dot;
                        double intensity = calculateIntensityByDistance(distance, activationRange);

                        ScentTrigger scentTrigger =
                                ScentTrigger.fromPassiveMode(
                                        trigger.getScentName(),
                                        ScentPriority.MEDIUM,
                                        -1,
                                        intensity);

                        String source = "block:" + trigger.getBlockId();
                        String displayName = getBlockDisplayName(level, trigger.getBlockId());

                        bestCandidate =
                                new TriggerCandidate(
                                        scentTrigger,
                                        source,
                                        displayName,
                                        TriggerType.BLOCK,
                                        (int) activationRange,
                                        distance);
                    }
                }
            }
        }
        return bestCandidate;
    }

    private static TriggerCandidate evaluateMobTriggers(Level level, Player player) {
        int searchRange = (int) Math.ceil(getMobActivationRange());

        for (MobTriggerDefinition trigger : ScentTriggerConfigLoader.getAllMobTriggers()) {
            if (!trigger.isValid()) {
                continue;
            }

            String entityTypeId = trigger.getEntityType();

            Optional<Entity> foundEntity = findNearbyMob(level, player, entityTypeId, searchRange);

            if (foundEntity.isPresent()) {
                double distance = player.distanceTo(foundEntity.get());

                if (distance <= getMobActivationRange()
                        && (HOSTILE_MOBS.contains(entityTypeId)
                                || isPlayerLookingAt(
                                        player, foundEntity.get().getEyePosition(1.0f)))) {
                    double intensity =
                            calculateIntensityByDistance(distance, getMobActivationRange());

                    ScentTrigger scentTrigger =
                            ScentTrigger.fromPassiveMode(
                                    trigger.getScentName(), ScentPriority.HIGH, -1, intensity);

                    String source = "mob:" + entityTypeId;
                    String displayName = getMobDisplayName(foundEntity.get());

                    return new TriggerCandidate(
                            scentTrigger,
                            source,
                            displayName,
                            TriggerType.MOB,
                            (int) getMobActivationRange(),
                            distance);
                }
            }
        }
        return null;
    }

    private static Optional<Entity> findNearbyMob(
            Level level, Player player, String entityTypeId, int range) {
        try {
            ResourceLocation entityLocation = Ids.parse(entityTypeId);
            Optional<EntityType<?>> entityTypeOpt =
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENTITY_TYPE)
                            .getOptional(entityLocation);

            if (entityTypeOpt.isEmpty()) {
                return Optional.empty();
            }

            EntityType<?> targetType = entityTypeOpt.get();

            AABB searchBox = player.getBoundingBox().inflate(range);

            var entities =
                    level.getEntities(
                            player,
                            searchBox,
                            entity -> entity.getType() == targetType && entity.isAlive());

            if (!entities.isEmpty()) {

                Entity closest = null;
                double closestDist = Double.MAX_VALUE;
                for (Entity entity : entities) {
                    double dist = player.distanceToSqr(entity);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = entity;
                    }
                }
                return Optional.ofNullable(closest);
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Error searching for mob {}: {}", entityTypeId, e.getMessage());
        }

        return Optional.empty();
    }

    private static String getMobDisplayName(Entity entity) {
        return entity.getType().getDescription().getString();
    }

    private static TriggerCandidate evaluateStructureTriggers(Level level, BlockPos playerPos) {
        String currentStructureId = serverStructureId;

        if (currentStructureId == null) {

            if (lastStructureId != null) {
                if (DEV_MODE) {
                    AromaAffect.LOGGER.info("[PassiveMode] Left structure: {}", lastStructureId);
                }
                lastStructureId = null;
            }
            return null;
        }

        if (currentStructureId.equals(lastStructureId)) {
            return null;
        }

        String previousStructure = lastStructureId;
        lastStructureId = currentStructureId;

        if (DEV_MODE) {
            AromaAffect.LOGGER.info(
                    "[PassiveMode] Entered structure: {} (previous: {})",
                    currentStructureId,
                    previousStructure);
        }

        for (StructureTriggerDefinition trigger :
                ScentTriggerConfigLoader.getAllStructureTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) continue;
            if (!trigger.getStructureId().equals(currentStructureId)) continue;

            ScentTrigger scentTrigger =
                    ScentTrigger.fromPassiveMode(
                            trigger.getScentName(), ScentPriority.MEDLOW, -1, 1.0);

            String source = "structure:" + currentStructureId;
            String displayName = formatResourceId(currentStructureId);

            return new TriggerCandidate(
                    scentTrigger, source, displayName, TriggerType.STRUCTURE, 0, 0);
        }

        return null;
    }

    private static TriggerCandidate evaluateBiomeTriggers(Level level, BlockPos playerPos) {
        var biomeHolder = level.getBiome(playerPos);
        String currentBiomeId =
                Objects.requireNonNull(
                                level.registryAccess()
                                        .lookupOrThrow(Registries.BIOME)
                                        .getKey(biomeHolder.value()))
                        .toString();

        if (currentBiomeId.equals(lastBiomeId)) {
            return null;
        }

        String previousBiome = lastBiomeId;
        lastBiomeId = currentBiomeId;

        if (DEV_MODE) {
            AromaAffect.LOGGER.info(
                    "[PassiveMode] Biome changed: {} -> {}", previousBiome, currentBiomeId);
        }

        Optional<BiomeTriggerDefinition> triggerOpt =
                ScentTriggerConfigLoader.getBiomeTrigger(currentBiomeId);

        if (triggerOpt.isPresent()) {
            BiomeTriggerDefinition trigger = triggerOpt.get();

            double intensity = 1.0;

            ScentTrigger scentTrigger =
                    ScentTrigger.fromPassiveMode(
                            trigger.getScentName(), trigger.getPriority(), -1, intensity);

            String source = "biome:" + currentBiomeId;
            String displayName = getBiomeDisplayName(currentBiomeId);

            return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.BIOME, 0, 0);
        }

        return null;
    }

    private static void activateTrigger(Player player, TriggerCandidate candidate) {
        ScentTriggerManager manager = ScentTriggerManager.getInstance();

        ScentTrigger currentActive = manager.getActiveScent();
        if (currentActive != null && currentActive.source() == ScentTriggerSource.PASSIVE_MODE) {

            manager.stop();
        }

        currentPassiveTrigger = candidate.trigger;
        currentTriggerSource = candidate.source;
        currentTriggerType = candidate.type;

        typeTriggerTimes.put(candidate.type, System.currentTimeMillis());

        boolean triggered = manager.trigger(candidate.trigger);

        if (triggered && ClientConfig.getInstance().isPassivePuffOverlay()) {
            ScentPuffOverlay.onScentPuff(
                    candidate.trigger.scentName(), candidate.trigger.intensity());
        }

        if (DEV_MODE) {
            AromaAffect.LOGGER.info(
                    "[PassiveMode] activateTrigger: {} -> triggered={}",
                    candidate.source,
                    triggered);
        }

        if (ClientConfig.getInstance().isDebugScentMessages()) {

            String triggerTypeName = candidate.type.name().toLowerCase();

            int intensityPercent = (int) Math.round(candidate.trigger.intensity() * 100);

            String message;
            if (candidate.type == TriggerType.BIOME) {

                message =
                        String.format(
                                "§6[Aroma Affect] §7Scent: §e%s §7(%s: §b%s§7) §8[%d%%]",
                                candidate.trigger.scentName(),
                                triggerTypeName,
                                candidate.displayName,
                                intensityPercent);
            } else {

                message =
                        String.format(
                                "§6[Aroma Affect] §7Scent: §e%s §7(%s: §b%s§7) §8[%d%% | %.1fm / %dm]",
                                candidate.trigger.scentName(),
                                triggerTypeName,
                                candidate.displayName,
                                intensityPercent,
                                candidate.distance,
                                candidate.range);
            }

            player.displayClientMessage(Texts.lit(message), false);
        }

        AromaAffect.LOGGER.debug(
                "Passive-mode activated: {} from {} (intensity: {}%, distance: {}, range: {})",
                candidate.trigger.scentName(),
                candidate.source,
                (int) Math.round(candidate.trigger.intensity() * 100),
                candidate.distance,
                candidate.range);
    }

    public static void setServerStructureId(String structureId) {
        serverStructureId = structureId;
    }

    private static String getBlockDisplayName(Level level, String blockId) {
        try {
            ResourceLocation location = Ids.parse(blockId);
            Optional<Block> blockOpt =
                    level.registryAccess().lookupOrThrow(Registries.BLOCK).getOptional(location);

            if (blockOpt.isPresent()) {
                Block block = blockOpt.get();
                String displayName = block.getDescriptionId();
                if (displayName.startsWith("block.")) {
                    displayName = displayName.substring(6);
                }
                displayName = formatResourceName(displayName);
                return displayName;
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.debug(
                    "Error getting block display name for {}: {}", blockId, e.getMessage());
        }

        return formatResourceId(blockId);
    }

    private static String getBiomeDisplayName(String biomeId) {
        return formatResourceId(biomeId);
    }

    private static String formatResourceId(String resourceId) {
        try {
            ResourceLocation location = Ids.parse(resourceId);
            String path = location.getPath();
            return formatResourceName(path);
        } catch (Exception e) {
            return resourceId;
        }
    }

    private static String formatResourceName(String name) {
        String formatted = name.replace('_', ' ');
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    private static boolean isHostileMob(String source) {
        if (source == null || !source.startsWith("mob:")) return false;
        String entityType = source.substring(4);
        return HOSTILE_MOBS.contains(entityType);
    }

    private static boolean isPlayerLookingAt(Player player, Vec3 targetPos) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 viewDir = player.getViewVector(1.0f).normalize();
        Vec3 toTarget = targetPos.subtract(eyePos).normalize();
        double dot = viewDir.dot(toTarget);
        return dot >= LOOK_AT_COS_THRESHOLD;
    }

    private static void clearPassiveScent(Player player) {
        if (currentPassiveTrigger != null) {
            ScentTriggerManager manager = ScentTriggerManager.getInstance();
            manager.getActiveScentOptional()
                    .filter(trigger -> trigger.source() == ScentTriggerSource.PASSIVE_MODE)
                    .ifPresent(
                            trigger -> {
                                manager.stop();
                                AromaAffect.LOGGER.debug(
                                        "Passive-mode scent stopped: {}", trigger.scentName());
                            });

            currentPassiveTrigger = null;
            currentTriggerSource = null;
            currentTriggerType = null;
            lastStructureId = null;
        }
    }

    public static void stopPassiveMode() {
        if (currentPassiveTrigger != null) {
            ScentTriggerManager.getInstance().stop();
            currentPassiveTrigger = null;
            currentTriggerSource = null;
            currentTriggerType = null;
            lastStructureId = null;
        }
    }

    public static boolean isPassiveModeEnabled() {
        return PassiveModeConfig.getInstance().isPassiveModeEnabled();
    }

    public static void setPassiveModeEnabled(boolean enabled) {
        PassiveModeConfig config = PassiveModeConfig.getInstance();
        config.setPassiveModeEnabled(enabled);
        config.save();

        if (!enabled) {
            stopPassiveMode();
        }
        AromaAffect.LOGGER.info("Passive mode {}", enabled ? "enabled" : "disabled");
    }

    public static void togglePassiveMode() {
        setPassiveModeEnabled(!isPassiveModeEnabled());
    }

    public static long getCurrentCooldownMs() {
        if (currentTriggerType == null) {
            return 0;
        }
        return getCooldownForType(currentTriggerType);
    }
}
