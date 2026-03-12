package com.ovrtechnology.tutorial.searchdiamond;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Manages SearchDiamond zones persistence using SavedData.
 */
public class SearchDiamondZoneManager extends SavedData {

    private final Map<String, SearchDiamondZone> zones = new HashMap<>();

    // Inner record for codec serialization
    private record ZoneData(String id, BlockPos corner1, BlockPos corner2,
                            BlockPos exitPoint, BlockPos triggerButton) {}

    private static final Codec<ZoneData> ZONE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(ZoneData::id),
                    BlockPos.CODEC.optionalFieldOf("corner1").forGetter(d -> Optional.ofNullable(d.corner1)),
                    BlockPos.CODEC.optionalFieldOf("corner2").forGetter(d -> Optional.ofNullable(d.corner2)),
                    BlockPos.CODEC.optionalFieldOf("exitPoint").forGetter(d -> Optional.ofNullable(d.exitPoint)),
                    BlockPos.CODEC.optionalFieldOf("triggerButton").forGetter(d -> Optional.ofNullable(d.triggerButton))
            ).apply(instance, (id, c1, c2, exit, trigger) ->
                    new ZoneData(id, c1.orElse(null), c2.orElse(null), exit.orElse(null), trigger.orElse(null)))
    );

    private static final Codec<SearchDiamondZoneManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ZONE_CODEC.listOf().fieldOf("zones").forGetter(SearchDiamondZoneManager::getZoneDataList)
            ).apply(instance, SearchDiamondZoneManager::new)
    );

    static final SavedDataType<SearchDiamondZoneManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_search_diamond_zones",
            SearchDiamondZoneManager::new,
            CODEC,
            null
    );

    public SearchDiamondZoneManager() {
    }

    private SearchDiamondZoneManager(List<ZoneData> zoneDataList) {
        for (ZoneData data : zoneDataList) {
            zones.put(data.id, new SearchDiamondZone(data.id, data.corner1, data.corner2,
                    data.exitPoint, data.triggerButton));
        }
    }

    private List<ZoneData> getZoneDataList() {
        List<ZoneData> list = new ArrayList<>();
        for (SearchDiamondZone zone : zones.values()) {
            list.add(new ZoneData(zone.getId(), zone.getCorner1(), zone.getCorner2(),
                    zone.getExitPoint(), zone.getTriggerButton()));
        }
        return list;
    }

    public static SearchDiamondZoneManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // CRUD Operations

    public static boolean createZone(ServerLevel level, String id) {
        SearchDiamondZoneManager manager = get(level);
        if (manager.zones.containsKey(id)) return false;
        manager.zones.put(id, new SearchDiamondZone(id));
        manager.setDirty();
        return true;
    }

    public static boolean deleteZone(ServerLevel level, String id) {
        SearchDiamondZoneManager manager = get(level);
        if (manager.zones.remove(id) != null) {
            manager.setDirty();
            return true;
        }
        return false;
    }

    public static boolean setCorner(ServerLevel level, String id, int corner, BlockPos pos) {
        SearchDiamondZoneManager manager = get(level);
        SearchDiamondZone zone = manager.zones.get(id);
        if (zone == null) return false;
        if (corner == 1) zone.setCorner1(pos);
        else zone.setCorner2(pos);
        manager.setDirty();
        return true;
    }

    public static boolean setExitPoint(ServerLevel level, String id, BlockPos pos) {
        SearchDiamondZoneManager manager = get(level);
        SearchDiamondZone zone = manager.zones.get(id);
        if (zone == null) return false;
        zone.setExitPoint(pos);
        manager.setDirty();
        return true;
    }

    public static boolean setTriggerButton(ServerLevel level, String id, BlockPos pos) {
        SearchDiamondZoneManager manager = get(level);
        SearchDiamondZone zone = manager.zones.get(id);
        if (zone == null) return false;
        zone.setTriggerButton(pos);
        manager.setDirty();
        return true;
    }

    public static SearchDiamondZone getZone(ServerLevel level, String id) {
        return get(level).zones.get(id);
    }

    public static Set<String> getAllZoneIds(ServerLevel level) {
        return Set.copyOf(get(level).zones.keySet());
    }

    public static Collection<SearchDiamondZone> getAllZones(ServerLevel level) {
        return get(level).zones.values();
    }

    public static List<SearchDiamondZone> getCompleteZones(ServerLevel level) {
        List<SearchDiamondZone> result = new ArrayList<>();
        for (SearchDiamondZone zone : get(level).zones.values()) {
            if (zone.isComplete()) result.add(zone);
        }
        return result;
    }

    public static Optional<SearchDiamondZone> findZoneByTriggerButton(ServerLevel level, BlockPos buttonPos) {
        for (SearchDiamondZone zone : get(level).zones.values()) {
            if (zone.getTriggerButton() != null && zone.getTriggerButton().equals(buttonPos)) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }
}
