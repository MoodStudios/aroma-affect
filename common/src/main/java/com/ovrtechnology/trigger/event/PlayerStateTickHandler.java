package com.ovrtechnology.trigger.event;

import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

public final class PlayerStateTickHandler {

    public static final String TT_WEATHER_RAINING = "WEATHER_RAINING";
    public static final String TT_WEATHER_THUNDERING = "WEATHER_THUNDERING";
    public static final String TT_PLAYER_EYES_IN_WATER = "PLAYER_EYES_IN_WATER";
    public static final String TT_PLAYER_ON_FIRE = "PLAYER_ON_FIRE";
    public static final String TT_PLAYER_IN_LAVA = "PLAYER_IN_LAVA";
    public static final String TT_PLAYER_HEALTH_THRESHOLD = "PLAYER_HEALTH_THRESHOLD";
    public static final String TT_PLAYER_HUNGER_THRESHOLD = "PLAYER_HUNGER_THRESHOLD";
    public static final String TT_PLAYER_NEGATIVE_EFFECT = "PLAYER_NEGATIVE_EFFECT";
    public static final String TT_PLAYER_LEVEL_UP = "PLAYER_LEVEL_UP";
    public static final String TT_PLAYER_FOOD_EATEN = "PLAYER_FOOD_EATEN";
    public static final String TT_PLAYER_POTION_DRUNK = "PLAYER_POTION_DRUNK";
    public static final String TT_PLAYER_SLEPT = "PLAYER_SLEPT";
    public static final String TT_PLAYER_DIMENSION_CHANGE = "PLAYER_DIMENSION_CHANGE";

    private static final int CHECK_INTERVAL_TICKS = 20;

    private static int tickCounter = 0;

    private static final Map<String, Long> lastFiredAtMs = new HashMap<>();

    private static final Set<String> activeContinuous = new HashSet<>();

    private static int lastObservedLevel = Integer.MIN_VALUE;

    private static String lastObservedDimension = null;

    private static boolean wasSleeping = false;

    private PlayerStateTickHandler() {}

    public static void tick(LocalPlayer player) {
        if (player == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        if (mc.level == null) return;

        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        EventTriggersConfig config = EventTriggersConfig.getInstance();
        if (!config.isEventTriggersEnabled()) {
            stopAllContinuous();
            return;
        }

        evaluateContinuous(player, config, TT_WEATHER_RAINING, () -> evalRain(player));
        evaluateContinuous(player, config, TT_WEATHER_THUNDERING, () -> evalThunder(player));
        evaluateContinuous(player, config, TT_PLAYER_EYES_IN_WATER, () -> evalSubmerged(player));
        evaluateContinuous(player, config, TT_PLAYER_ON_FIRE, () -> evalOnFire(player));
        evaluateContinuous(player, config, TT_PLAYER_IN_LAVA, () -> evalInLava(player));
        evaluateContinuous(player, config, TT_PLAYER_HEALTH_THRESHOLD, () -> evalLowHealth(player));
        evaluateContinuous(player, config, TT_PLAYER_HUNGER_THRESHOLD, () -> evalLowHunger(player));
        evaluateContinuous(
                player, config, TT_PLAYER_NEGATIVE_EFFECT, () -> evalNegativeEffect(player));

        evaluateLevelUp(player, config);
        evaluateDimensionChange(player, config);
        evaluateSleep(player, config);
    }

    private static void evaluateContinuous(
            LocalPlayer player,
            EventTriggersConfig config,
            String triggerType,
            ConditionEval eval) {
        EventDefinition def = EventDefinitionLoader.getFirstByTriggerType(triggerType).orElse(null);
        if (def == null) return;

        if (!config.isCategoryEnabled(def.getCategory())) {
            stopContinuous(def);
            return;
        }

        boolean conditionMet;
        try {
            conditionMet = eval.evaluate();
        } catch (Exception e) {
            AromaAffect.LOGGER.warn(
                    "Failed to evaluate condition for event {}: {}",
                    def.getEventId(),
                    e.getMessage());
            conditionMet = false;
        }

        if (conditionMet) {
            fireContinuous(def, config);
        } else {
            stopContinuous(def);
        }
    }

    @FunctionalInterface
    private interface ConditionEval {
        boolean evaluate();
    }

    private static void fireContinuous(EventDefinition def, EventTriggersConfig config) {
        long now = System.currentTimeMillis();
        long cooldown = effectiveCooldownMs(def, config);
        Long lastTime = lastFiredAtMs.get(def.getEventId());
        boolean alreadyActive = activeContinuous.contains(def.getEventId());

        if (alreadyActive && lastTime != null && (now - lastTime) < cooldown) {
            return;
        }

        String scentName = ScentRegistry.getDisplayName(def.getScentId());
        if (scentName == null || "Unknown Scent".equals(scentName)) {
            return;
        }

        if (!EventThrottle.tryConsume()) {
            return;
        }

        ScentTrigger trigger =
                ScentTrigger.create(
                        scentName, def.resolveSource(), def.getPriority(), -1, def.getIntensity());

        boolean fired = ScentTriggerManager.getInstance().trigger(trigger);
        if (fired) {
            lastFiredAtMs.put(def.getEventId(), now);
            activeContinuous.add(def.getEventId());
            EventDebugLog.fired(def, scentName, def.getIntensity());
        }
    }

    private static void stopContinuous(EventDefinition def) {
        if (!activeContinuous.remove(def.getEventId())) return;

        String scentName = ScentRegistry.getDisplayName(def.getScentId());
        if (scentName == null) return;

        ScentTrigger active = ScentTriggerManager.getInstance().getActiveScent();
        if (active != null
                && active.scentName().equals(scentName)
                && active.source() == def.resolveSource()) {
            ScentTriggerManager.getInstance().stop(scentName);
            EventDebugLog.stopped(def, scentName);
        }
    }

    private static void stopAllContinuous() {
        if (activeContinuous.isEmpty()) return;
        for (String eventId : new HashSet<>(activeContinuous)) {
            EventDefinition def = EventDefinitionLoader.getById(eventId).orElse(null);
            if (def != null) {
                stopContinuous(def);
            } else {
                activeContinuous.remove(eventId);
            }
        }
    }

    private static long effectiveCooldownMs(EventDefinition def, EventTriggersConfig config) {
        long defCooldown = def.getCooldownMs();
        long categoryCooldown = config.getCategoryCooldownMs(def.getCategory());
        return Math.max(defCooldown, categoryCooldown);
    }

    private static boolean evalRain(LocalPlayer player) {
        Level level = player.level();
        if (!level.isRaining()) return false;
        return checkSkyAndBiome(player, level);
    }

    private static boolean evalThunder(LocalPlayer player) {
        Level level = player.level();
        if (!level.isThundering()) return false;
        return checkSkyAndBiome(player, level);
    }

    private static boolean checkSkyAndBiome(LocalPlayer player, Level level) {
        EventDefinition def =
                EventDefinitionLoader.getFirstByTriggerType(
                                level.isThundering() ? TT_WEATHER_THUNDERING : TT_WEATHER_RAINING)
                        .orElse(null);
        if (def == null) return false;
        JsonObject c = def.getConditions();

        boolean requiresSky = EventConditionUtils.getBoolean(c, "requires_sky_visible", true);
        BlockPos head = player.blockPosition().above();
        if (requiresSky && !level.canSeeSky(head)) {
            return false;
        }

        List<String> excluded = EventConditionUtils.getStringArray(c, "exclude_biome_paths");
        if (!excluded.isEmpty()) {
            String biomePath = currentBiomePath(player);
            if (EventConditionUtils.biomePathMatchesAny(biomePath, excluded)) {
                return false;
            }
        }
        return true;
    }

    private static String currentBiomePath(LocalPlayer player) {
        try {
            var holder = player.level().getBiome(player.blockPosition());
            ResourceLocation rl = holder.unwrapKey().map(k -> k.location()).orElse(null);
            return rl != null ? rl.getPath() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean evalSubmerged(LocalPlayer player) {
        return player.isEyeInFluid(FluidTags.WATER);
    }

    private static boolean evalOnFire(LocalPlayer player) {
        return player.isOnFire() && !player.isInWater();
    }

    private static boolean evalInLava(LocalPlayer player) {
        return player.isInLava();
    }

    private static boolean evalLowHealth(LocalPlayer player) {
        EventDefinition def =
                EventDefinitionLoader.getFirstByTriggerType(TT_PLAYER_HEALTH_THRESHOLD)
                        .orElse(null);
        double maxRatio =
                def == null
                        ? 0.30
                        : EventConditionUtils.getDouble(def.getConditions(), "max_hp_ratio", 0.30);
        if (player.getMaxHealth() <= 0f) return false;
        float ratio = player.getHealth() / player.getMaxHealth();
        return ratio > 0f && ratio <= maxRatio;
    }

    private static boolean evalLowHunger(LocalPlayer player) {
        EventDefinition def =
                EventDefinitionLoader.getFirstByTriggerType(TT_PLAYER_HUNGER_THRESHOLD)
                        .orElse(null);
        int maxFood =
                def == null
                        ? 4
                        : EventConditionUtils.getInt(def.getConditions(), "max_food_level", 4);
        return player.getFoodData().getFoodLevel() <= maxFood;
    }

    private static boolean evalNegativeEffect(LocalPlayer player) {
        EventDefinition def =
                EventDefinitionLoader.getFirstByTriggerType(TT_PLAYER_NEGATIVE_EFFECT).orElse(null);
        if (def == null) return false;
        List<String> effectIds =
                EventConditionUtils.getStringArray(def.getConditions(), "effect_ids");
        if (effectIds.isEmpty()) {
            for (MobEffectInstance inst : player.getActiveEffects()) {
                if (!inst.getEffect().value().isBeneficial()) return true;
            }
            return false;
        }
        Set<String> wanted = new HashSet<>(effectIds);
        for (MobEffectInstance inst : player.getActiveEffects()) {
            ResourceLocation rl =
                    net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(
                            inst.getEffect().value());
            if (rl != null && wanted.contains(rl.toString())) return true;
        }
        return false;
    }

    private static void evaluateLevelUp(LocalPlayer player, EventTriggersConfig config) {
        EventDefinition def =
                EventDefinitionLoader.getFirstByTriggerType(TT_PLAYER_LEVEL_UP).orElse(null);
        if (def == null) return;
        if (!config.isCategoryEnabled(def.getCategory())) {
            lastObservedLevel = player.experienceLevel;
            return;
        }

        int currentLevel = player.experienceLevel;
        if (lastObservedLevel == Integer.MIN_VALUE) {
            lastObservedLevel = currentLevel;
            return;
        }

        if (currentLevel > lastObservedLevel) {
            long now = System.currentTimeMillis();
            long cooldown = effectiveCooldownMs(def, config);
            Long lastTime = lastFiredAtMs.get(def.getEventId());
            if (lastTime == null || (now - lastTime) >= cooldown) {
                String scentName = ScentRegistry.getDisplayName(def.getScentId());
                if (scentName != null
                        && !"Unknown Scent".equals(scentName)
                        && EventThrottle.tryConsume()) {
                    ScentTrigger trigger =
                            ScentTrigger.create(
                                    scentName,
                                    def.resolveSource(),
                                    def.getPriority(),
                                    1,
                                    def.getIntensity());
                    boolean fired = ScentTriggerManager.getInstance().trigger(trigger);
                    if (fired) {
                        lastFiredAtMs.put(def.getEventId(), now);
                        EventDebugLog.fired(def, scentName, def.getIntensity());
                    }
                }
            }
        }

        if (currentLevel != lastObservedLevel) {
            lastObservedLevel = currentLevel;
        }
    }

    private static void evaluateDimensionChange(LocalPlayer player, EventTriggersConfig config) {
        String currentDim = player.level().dimension().location().toString();
        if (lastObservedDimension == null) {
            lastObservedDimension = currentDim;
            return;
        }
        if (!currentDim.equals(lastObservedDimension)) {
            String previous = lastObservedDimension;
            lastObservedDimension = currentDim;
            fireOneShotByTriggerType(
                    TT_PLAYER_DIMENSION_CHANGE,
                    config,
                    def -> {
                        String target =
                                def.getConditions().has("target_dimension")
                                        ? def.getConditions().get("target_dimension").getAsString()
                                        : null;
                        if (target != null && !target.equals(currentDim)) return false;
                        String from =
                                def.getConditions().has("from_dimension")
                                        ? def.getConditions().get("from_dimension").getAsString()
                                        : null;
                        if (from != null && !from.equals(previous)) return false;
                        return true;
                    },
                    -1.0);
        }
    }

    private static void evaluateSleep(LocalPlayer player, EventTriggersConfig config) {
        boolean nowSleeping = player.isSleeping();
        if (wasSleeping && !nowSleeping) {
            fireOneShotByTriggerType(TT_PLAYER_SLEPT, config, def -> true, -1.0);
        }
        wasSleeping = nowSleeping;
    }

    public static boolean fireOneShotByTriggerType(
            String triggerType,
            EventTriggersConfig config,
            java.util.function.Predicate<EventDefinition> matcher,
            double intensityOverride) {
        if (config == null) {
            config = EventTriggersConfig.getInstance();
        }
        if (!config.isEventTriggersEnabled()) return false;

        List<EventDefinition> candidates = EventDefinitionLoader.getByTriggerType(triggerType);
        if (candidates.isEmpty()) return false;

        long now = System.currentTimeMillis();
        for (EventDefinition def : candidates) {
            if (!config.isCategoryEnabled(def.getCategory())) continue;
            if (!matcher.test(def)) continue;

            long cooldown = effectiveCooldownMs(def, config);
            Long lastTime = lastFiredAtMs.get(def.getEventId());
            if (lastTime != null && (now - lastTime) < cooldown) return false;

            String scentName = ScentRegistry.getDisplayName(def.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) return false;

            if (!EventThrottle.tryConsume()) return false;

            double intensity =
                    intensityOverride > 0 && intensityOverride <= 1.0
                            ? intensityOverride
                            : def.getIntensity();

            ScentTrigger trigger =
                    ScentTrigger.create(
                            scentName, def.resolveSource(), def.getPriority(), 1, intensity);
            boolean fired = ScentTriggerManager.getInstance().trigger(trigger);
            if (fired) {
                lastFiredAtMs.put(def.getEventId(), now);
                EventDebugLog.fired(def, scentName, intensity);
            }
            return fired;
        }
        return false;
    }

    public static void resetState() {
        tickCounter = 0;
        lastFiredAtMs.clear();
        activeContinuous.clear();
        lastObservedLevel = Integer.MIN_VALUE;
        lastObservedDimension = null;
        wasSleeping = false;
    }
}
