package com.ovrtechnology.tutorial.finishscreen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages finish screen trigger zones (SavedData persistence).
 */
public class TutorialFinishZoneManager extends SavedData {

    private final Map<String, TutorialFinishZone> zones = new HashMap<>();

    private record ZoneData(String id, BlockPos corner1, BlockPos corner2) {}

    private static final Codec<ZoneData> ZONE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(ZoneData::id),
                    BlockPos.CODEC.optionalFieldOf("corner1", BlockPos.ZERO).forGetter(ZoneData::corner1),
                    BlockPos.CODEC.optionalFieldOf("corner2", BlockPos.ZERO).forGetter(ZoneData::corner2)
            ).apply(instance, ZoneData::new)
    );

    private static final Codec<TutorialFinishZoneManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ZONE_CODEC.listOf().fieldOf("zones").forGetter(TutorialFinishZoneManager::getZoneDataList)
            ).apply(instance, TutorialFinishZoneManager::new)
    );

    static final SavedDataType<TutorialFinishZoneManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_finish_zones",
            TutorialFinishZoneManager::new,
            CODEC,
            null
    );

    public TutorialFinishZoneManager() {}

    private TutorialFinishZoneManager(List<ZoneData> dataList) {
        for (ZoneData data : dataList) {
            TutorialFinishZone zone = new TutorialFinishZone(data.id);
            if (!data.corner1.equals(BlockPos.ZERO)) zone.setCorner1(data.corner1);
            if (!data.corner2.equals(BlockPos.ZERO)) zone.setCorner2(data.corner2);
            zones.put(data.id, zone);
        }
    }

    private List<ZoneData> getZoneDataList() {
        List<ZoneData> list = new ArrayList<>();
        for (TutorialFinishZone zone : zones.values()) {
            list.add(new ZoneData(
                    zone.getId(),
                    zone.getCorner1() != null ? zone.getCorner1() : BlockPos.ZERO,
                    zone.getCorner2() != null ? zone.getCorner2() : BlockPos.ZERO
            ));
        }
        return list;
    }

    private static TutorialFinishZoneManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─── Static API ──────────────────────────────────────────────────────────

    public static boolean createZone(ServerLevel level, String id) {
        TutorialFinishZoneManager mgr = get(level);
        if (mgr.zones.containsKey(id)) return false;
        mgr.zones.put(id, new TutorialFinishZone(id));
        mgr.setDirty();
        return true;
    }

    public static boolean deleteZone(ServerLevel level, String id) {
        TutorialFinishZoneManager mgr = get(level);
        if (mgr.zones.remove(id) != null) {
            mgr.setDirty();
            return true;
        }
        return false;
    }

    public static boolean setCorner(ServerLevel level, String id, int corner, BlockPos pos) {
        TutorialFinishZoneManager mgr = get(level);
        TutorialFinishZone zone = mgr.zones.get(id);
        if (zone == null) return false;
        if (corner == 1) zone.setCorner1(pos);
        else zone.setCorner2(pos);
        mgr.setDirty();
        return true;
    }

    public static TutorialFinishZone getZone(ServerLevel level, String id) {
        return get(level).zones.get(id);
    }

    public static Set<String> getAllZoneIds(ServerLevel level) {
        return Set.copyOf(get(level).zones.keySet());
    }

    public static List<TutorialFinishZone> getCompleteZones(ServerLevel level) {
        List<TutorialFinishZone> result = new ArrayList<>();
        for (TutorialFinishZone zone : get(level).zones.values()) {
            if (zone.isComplete()) result.add(zone);
        }
        return result;
    }
}
