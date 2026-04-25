package com.ovrtechnology.trigger.event;

import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.ScentEventNetworking;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public final class ServerEventBusHandler {

    public static final String TT_BLOCK_BROKEN = "BLOCK_BROKEN";
    public static final String TT_MOB_KILLED = "MOB_KILLED";
    public static final String TT_ADVANCEMENT_OBTAINED = "ADVANCEMENT_OBTAINED";

    private static final Map<UUID, Map<String, Long>> serverCooldowns = new HashMap<>();

    private static boolean initialized = false;

    private ServerEventBusHandler() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("ServerEventBusHandler.init() called multiple times!");
            return;
        }
        initialized = true;

        BlockEvent.BREAK.register(
                (level, pos, state, player, xp) -> {
                    onBlockBroken(player, state);
                    return EventResult.pass();
                });

        EntityEvent.LIVING_DEATH.register(
                (entity, source) -> {
                    onLivingDeath(entity, source);
                    return EventResult.pass();
                });

        PlayerEvent.PLAYER_ADVANCEMENT.register(ServerEventBusHandler::onAdvancement);

        AromaAffect.LOGGER.info("ServerEventBusHandler initialized");
    }

    private static void onBlockBroken(ServerPlayer player, BlockState state) {
        if (player == null || state == null) return;
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return;
        String blockKey = blockId.toString();
        Set<String> tags = collectBlockTags(state);

        dispatch(
                player,
                TT_BLOCK_BROKEN,
                def -> matchesBlock(def.getConditions(), blockKey, tags));
    }

    private static Set<String> collectBlockTags(BlockState state) {
        Set<String> out = new HashSet<>();
        state.getTags()
                .forEach(
                        tagKey -> {
                            ResourceLocation rl = tagKey.location();
                            if (rl != null) {
                                out.add("#" + rl);
                            }
                        });
        return out;
    }

    private static boolean matchesBlock(JsonObject conditions, String blockKey, Set<String> tags) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "block_ids");
        if (!ids.isEmpty() && ids.contains(blockKey)) return true;

        List<String> tagFilters = EventConditionUtils.getStringArray(conditions, "block_tags");
        for (String t : tagFilters) {
            if (tags.contains(t)) return true;
        }
        return false;
    }

    private static void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (entity == null || source == null) return;

        ServerPlayer killer = resolvePlayerSource(source);
        if (killer == null) return;
        if (entity == killer) return;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) return;
        String entityKey = entityId.toString();
        Set<String> tags = collectEntityTags(entity);

        dispatch(
                killer,
                TT_MOB_KILLED,
                def -> matchesEntity(def.getConditions(), entityKey, tags));
    }

    private static ServerPlayer resolvePlayerSource(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer p) return p;
        if (source.getDirectEntity() instanceof ServerPlayer p) return p;
        if (source.getEntity() instanceof Player) {
            return null;
        }
        return null;
    }

    private static Set<String> collectEntityTags(LivingEntity entity) {
        Set<String> out = new HashSet<>();
        entity.getType()
                .builtInRegistryHolder()
                .tags()
                .forEach(
                        tagKey -> {
                            ResourceLocation rl = tagKey.location();
                            if (rl != null) {
                                out.add("#" + rl);
                            }
                        });
        return out;
    }

    private static boolean matchesEntity(
            JsonObject conditions, String entityKey, Set<String> tags) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "entity_ids");
        if (!ids.isEmpty() && ids.contains(entityKey)) return true;

        List<String> tagFilters = EventConditionUtils.getStringArray(conditions, "entity_tags");
        for (String t : tagFilters) {
            if (tags.contains(t)) return true;
        }
        return EventConditionUtils.getBoolean(conditions, "default", false);
    }

    private static void onAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        if (player == null || advancement == null) return;
        ResourceLocation advId = advancement.id();
        String advKey = advId != null ? advId.toString() : "";

        if (advKey.contains("/recipes/") || advKey.endsWith("/root")) {
            return;
        }

        dispatch(
                player,
                TT_ADVANCEMENT_OBTAINED,
                def -> matchesAdvancement(def.getConditions(), advKey));
    }

    private static boolean matchesAdvancement(JsonObject conditions, String advKey) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "advancement_ids");
        if (!ids.isEmpty()) return ids.contains(advKey);
        return EventConditionUtils.getBoolean(conditions, "default", false);
    }

    private static void dispatch(
            ServerPlayer player, String triggerType, Predicate<EventDefinition> matcher) {
        EventTriggersConfig config = EventTriggersConfig.getInstance();
        if (!config.isEventTriggersEnabled()) return;

        List<EventDefinition> candidates = EventDefinitionLoader.getByTriggerType(triggerType);
        if (candidates.isEmpty()) return;

        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns =
                serverCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>());

        for (EventDefinition def : candidates) {
            if (!matcher.test(def)) continue;

            long cooldown =
                    Math.max(
                            def.getCooldownMs(),
                            config.getCategoryCooldownMs(def.getCategory()));
            Long lastTime = playerCooldowns.get(def.getEventId());
            if (lastTime != null && (now - lastTime) < cooldown) {
                return;
            }

            playerCooldowns.put(def.getEventId(), now);
            ScentEventNetworking.sendEvent(player, def.getEventId());
            AromaAffect.LOGGER.debug(
                    "[Events] dispatched {} for {} via packet (trigger {})",
                    def.getEventId(),
                    player.getName().getString(),
                    triggerType);
            return;
        }
    }

    public static void clearPlayerCooldowns(UUID playerId) {
        serverCooldowns.remove(playerId);
    }
}
