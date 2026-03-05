package com.ovrtechnology.tutorial.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Manages tutorial boss area configurations.
 * Persisted to world save data using Codec-based serialization.
 */
public class TutorialBossAreaManager extends SavedData {

    private final Map<String, TutorialBossArea> areas = new HashMap<>();

    // Track which players have already triggered each boss area (persistent)
    private final Map<String, Set<UUID>> triggeredPlayers = new HashMap<>();

    private static final Codec<AreaData> AREA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(AreaData::id),
                    Codec.STRING.fieldOf("bossType").forGetter(AreaData::bossType),
                    BlockPos.CODEC.optionalFieldOf("triggerMin").forGetter(d -> Optional.ofNullable(d.triggerMin)),
                    BlockPos.CODEC.optionalFieldOf("triggerMax").forGetter(d -> Optional.ofNullable(d.triggerMax)),
                    BlockPos.CODEC.optionalFieldOf("spawnPos").forGetter(d -> Optional.ofNullable(d.spawnPos)),
                    BlockPos.CODEC.optionalFieldOf("movementMin").forGetter(d -> Optional.ofNullable(d.movementMin)),
                    BlockPos.CODEC.optionalFieldOf("movementMax").forGetter(d -> Optional.ofNullable(d.movementMax)),
                    Codec.INT.optionalFieldOf("triggerPadding", 0).forGetter(AreaData::triggerPadding)
            ).apply(instance, (id, bossType, trigMin, trigMax, spawn, moveMin, moveMax, padding) ->
                    new AreaData(id, bossType,
                            trigMin.orElse(null), trigMax.orElse(null),
                            spawn.orElse(null),
                            moveMin.orElse(null), moveMax.orElse(null),
                            padding))
    );

    // Codec for triggered player entry (areaId -> list of UUIDs as strings)
    private static final Codec<TriggeredEntry> TRIGGERED_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("areaId").forGetter(TriggeredEntry::areaId),
                    Codec.STRING.listOf().fieldOf("playerUuids").forGetter(TriggeredEntry::playerUuids)
            ).apply(instance, TriggeredEntry::new)
    );

    private static final Codec<TutorialBossAreaManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    AREA_CODEC.listOf().fieldOf("areas").forGetter(m -> m.getAreaDataList()),
                    TRIGGERED_CODEC.listOf().optionalFieldOf("triggeredPlayers", List.of())
                            .forGetter(m -> m.getTriggeredDataList())
            ).apply(instance, TutorialBossAreaManager::new)
    );

    static final SavedDataType<TutorialBossAreaManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_boss_areas",
            TutorialBossAreaManager::new,
            CODEC,
            null
    );

    public TutorialBossAreaManager() {
    }

    private TutorialBossAreaManager(List<AreaData> areaDataList, List<TriggeredEntry> triggeredDataList) {
        for (AreaData data : areaDataList) {
            TutorialBossArea area = new TutorialBossArea(
                    data.id,
                    data.bossType,
                    data.triggerMin,
                    data.triggerMax,
                    data.spawnPos,
                    data.movementMin,
                    data.movementMax
            );
            area.setTriggerPadding(data.triggerPadding);
            areas.put(data.id, area);
        }

        // Load triggered players
        for (TriggeredEntry entry : triggeredDataList) {
            Set<UUID> uuids = new HashSet<>();
            for (String uuidStr : entry.playerUuids) {
                try {
                    uuids.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException e) {
                    AromaAffect.LOGGER.warn("Invalid UUID in triggered players: {}", uuidStr);
                }
            }
            triggeredPlayers.put(entry.areaId, uuids);
        }

        AromaAffect.LOGGER.debug("Loaded {} boss areas, {} triggered entries", areas.size(), triggeredPlayers.size());
    }

    private List<AreaData> getAreaDataList() {
        List<AreaData> list = new ArrayList<>();
        for (TutorialBossArea area : areas.values()) {
            list.add(new AreaData(
                    area.getId(),
                    area.getBossType(),
                    area.getTriggerMin(),
                    area.getTriggerMax(),
                    area.getSpawnPos(),
                    area.getMovementMin(),
                    area.getMovementMax(),
                    area.getTriggerPadding()
            ));
        }
        return list;
    }

    private List<TriggeredEntry> getTriggeredDataList() {
        List<TriggeredEntry> list = new ArrayList<>();
        for (Map.Entry<String, Set<UUID>> entry : triggeredPlayers.entrySet()) {
            List<String> uuidStrings = entry.getValue().stream()
                    .map(UUID::toString)
                    .toList();
            list.add(new TriggeredEntry(entry.getKey(), uuidStrings));
        }
        return list;
    }

    public static TutorialBossAreaManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void addArea(TutorialBossArea area) {
        areas.put(area.getId(), area);
        setDirty();
    }

    public Optional<TutorialBossArea> getArea(String id) {
        return Optional.ofNullable(areas.get(id));
    }

    public void removeArea(String id) {
        if (areas.remove(id) != null) {
            setDirty();
        }
    }

    public Collection<TutorialBossArea> getAllAreas() {
        return Collections.unmodifiableCollection(areas.values());
    }

    public Collection<TutorialBossArea> getCompleteAreas() {
        return areas.values().stream()
                .filter(TutorialBossArea::isComplete)
                .toList();
    }

    public Set<String> getAreaIds() {
        return Collections.unmodifiableSet(areas.keySet());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Triggered Players API
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Checks if a player has already triggered a boss area.
     */
    public boolean hasPlayerTriggered(String areaId, UUID playerUuid) {
        Set<UUID> triggered = triggeredPlayers.get(areaId);
        return triggered != null && triggered.contains(playerUuid);
    }

    /**
     * Marks a player as having triggered a boss area.
     */
    public void markPlayerTriggered(String areaId, UUID playerUuid) {
        triggeredPlayers.computeIfAbsent(areaId, k -> new HashSet<>()).add(playerUuid);
        setDirty();
        AromaAffect.LOGGER.debug("Marked player {} as triggered for boss area '{}'", playerUuid, areaId);
    }

    /**
     * Resets triggered status for a specific player across all areas.
     * Used during tutorial reset.
     */
    public void resetPlayerTriggers(UUID playerUuid) {
        boolean changed = false;
        for (Set<UUID> uuids : triggeredPlayers.values()) {
            if (uuids.remove(playerUuid)) {
                changed = true;
            }
        }
        if (changed) {
            setDirty();
            AromaAffect.LOGGER.debug("Reset boss area triggers for player {}", playerUuid);
        }
    }

    /**
     * Resets all triggered players for all areas.
     * Used during full tutorial reset.
     */
    public void resetAllTriggers() {
        triggeredPlayers.clear();
        setDirty();
        AromaAffect.LOGGER.debug("Reset all boss area triggers");
    }

    // Internal data records
    private record AreaData(
            String id,
            String bossType,
            BlockPos triggerMin,
            BlockPos triggerMax,
            BlockPos spawnPos,
            BlockPos movementMin,
            BlockPos movementMax,
            int triggerPadding
    ) {}

    private record TriggeredEntry(String areaId, List<String> playerUuids) {}
}
