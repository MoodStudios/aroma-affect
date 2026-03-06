package com.ovrtechnology.tutorial.popupzone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Manages tutorial popup zones for a world.
 * Persisted to world save data using Codec-based serialization.
 */
public class TutorialPopupZoneManager extends SavedData {

    private final Map<String, TutorialPopupZone> zones = new HashMap<>();

    private static final Codec<ZoneData> ZONE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(ZoneData::id),
                    Codec.STRING.fieldOf("text").forGetter(ZoneData::text),
                    BlockPos.CODEC.optionalFieldOf("corner1").forGetter(d -> Optional.ofNullable(d.corner1)),
                    BlockPos.CODEC.optionalFieldOf("corner2").forGetter(d -> Optional.ofNullable(d.corner2))
            ).apply(instance, (id, text, c1, c2) ->
                    new ZoneData(id, text, c1.orElse(null), c2.orElse(null)))
    );

    private static final Codec<TutorialPopupZoneManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ZONE_CODEC.listOf().fieldOf("zones").forGetter(m -> m.getZoneDataList())
            ).apply(instance, TutorialPopupZoneManager::new)
    );

    static final SavedDataType<TutorialPopupZoneManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_popup_zones",
            TutorialPopupZoneManager::new,
            CODEC,
            null
    );

    public TutorialPopupZoneManager() {
    }

    private TutorialPopupZoneManager(List<ZoneData> zoneDataList) {
        for (ZoneData data : zoneDataList) {
            zones.put(data.id, new TutorialPopupZone(data.id, data.text, data.corner1, data.corner2));
        }
    }

    private List<ZoneData> getZoneDataList() {
        List<ZoneData> list = new ArrayList<>();
        for (TutorialPopupZone zone : zones.values()) {
            list.add(new ZoneData(zone.getId(), zone.getText(), zone.getCorner1(), zone.getCorner2()));
        }
        return list;
    }

    public static TutorialPopupZoneManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static boolean createZone(ServerLevel level, String id, String text) {
        TutorialPopupZoneManager manager = get(level);
        if (manager.zones.containsKey(id)) return false;
        manager.zones.put(id, new TutorialPopupZone(id, text));
        manager.setDirty();
        return true;
    }

    public static boolean deleteZone(ServerLevel level, String id) {
        TutorialPopupZoneManager manager = get(level);
        if (manager.zones.remove(id) != null) {
            manager.setDirty();
            return true;
        }
        return false;
    }

    public static boolean setCorner(ServerLevel level, String id, int corner, BlockPos pos) {
        TutorialPopupZoneManager manager = get(level);
        TutorialPopupZone zone = manager.zones.get(id);
        if (zone == null) return false;
        if (corner == 1) zone.setCorner1(pos);
        else zone.setCorner2(pos);
        manager.setDirty();
        return true;
    }

    public static boolean setText(ServerLevel level, String id, String text) {
        TutorialPopupZoneManager manager = get(level);
        TutorialPopupZone zone = manager.zones.get(id);
        if (zone == null) return false;
        zone.setText(text);
        manager.setDirty();
        return true;
    }

    public static Optional<TutorialPopupZone> getZone(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).zones.get(id));
    }

    public static Set<String> getAllZoneIds(ServerLevel level) {
        return new HashSet<>(get(level).zones.keySet());
    }

    public static List<TutorialPopupZone> getCompleteZones(ServerLevel level) {
        return get(level).zones.values().stream()
                .filter(TutorialPopupZone::isComplete)
                .toList();
    }

    private record ZoneData(String id, String text, BlockPos corner1, BlockPos corner2) {}
}
