package com.ovrtechnology.tutorial.musiczone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Manages tutorial music zones for a world.
 * Persisted to world save data using Codec-based serialization.
 */
public class TutorialMusicZoneManager extends SavedData {

    private final Map<String, TutorialMusicZone> zones = new HashMap<>();

    private static final Codec<ZoneData> ZONE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(ZoneData::id),
                    Codec.STRING.fieldOf("soundId").forGetter(ZoneData::soundId),
                    BlockPos.CODEC.optionalFieldOf("corner1").forGetter(d -> Optional.ofNullable(d.corner1)),
                    BlockPos.CODEC.optionalFieldOf("corner2").forGetter(d -> Optional.ofNullable(d.corner2)),
                    Codec.FLOAT.optionalFieldOf("volume", 1.0f).forGetter(ZoneData::volume),
                    Codec.FLOAT.optionalFieldOf("pitch", 1.0f).forGetter(ZoneData::pitch)
            ).apply(instance, (id, soundId, c1, c2, vol, p) ->
                    new ZoneData(id, soundId, c1.orElse(null), c2.orElse(null), vol, p))
    );

    private static final Codec<TutorialMusicZoneManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ZONE_CODEC.listOf().fieldOf("zones").forGetter(m -> m.getZoneDataList())
            ).apply(instance, TutorialMusicZoneManager::new)
    );

    static final SavedDataType<TutorialMusicZoneManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_music_zones",
            TutorialMusicZoneManager::new,
            CODEC,
            null
    );

    public TutorialMusicZoneManager() {
    }

    private TutorialMusicZoneManager(List<ZoneData> zoneDataList) {
        for (ZoneData data : zoneDataList) {
            TutorialMusicZone zone = new TutorialMusicZone(
                    data.id, data.soundId,
                    data.corner1, data.corner2,
                    data.volume, data.pitch
            );
            zones.put(data.id, zone);
        }
    }

    private List<ZoneData> getZoneDataList() {
        List<ZoneData> list = new ArrayList<>();
        for (TutorialMusicZone zone : zones.values()) {
            list.add(new ZoneData(
                    zone.getId(), zone.getSoundId(),
                    zone.getCorner1(), zone.getCorner2(),
                    zone.getVolume(), zone.getPitch()
            ));
        }
        return list;
    }

    public static TutorialMusicZoneManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean createZone(ServerLevel level, String id, String soundId) {
        TutorialMusicZoneManager manager = get(level);
        if (manager.zones.containsKey(id)) {
            return false;
        }
        manager.zones.put(id, new TutorialMusicZone(id, soundId));
        manager.setDirty();
        return true;
    }

    public static boolean deleteZone(ServerLevel level, String id) {
        TutorialMusicZoneManager manager = get(level);
        if (manager.zones.remove(id) != null) {
            manager.setDirty();
            return true;
        }
        return false;
    }

    public static boolean setCorner(ServerLevel level, String id, int corner, BlockPos pos) {
        TutorialMusicZoneManager manager = get(level);
        TutorialMusicZone zone = manager.zones.get(id);
        if (zone == null) return false;
        if (corner == 1) {
            zone.setCorner1(pos);
        } else {
            zone.setCorner2(pos);
        }
        manager.setDirty();
        return true;
    }

    public static boolean setVolume(ServerLevel level, String id, float volume) {
        TutorialMusicZoneManager manager = get(level);
        TutorialMusicZone zone = manager.zones.get(id);
        if (zone == null) return false;
        zone.setVolume(volume);
        manager.setDirty();
        return true;
    }

    public static boolean setSoundId(ServerLevel level, String id, String soundId) {
        TutorialMusicZoneManager manager = get(level);
        TutorialMusicZone zone = manager.zones.get(id);
        if (zone == null) return false;
        zone.setSoundId(soundId);
        manager.setDirty();
        return true;
    }

    public static Optional<TutorialMusicZone> getZone(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).zones.get(id));
    }

    public static Set<String> getAllZoneIds(ServerLevel level) {
        return new HashSet<>(get(level).zones.keySet());
    }

    public static List<TutorialMusicZone> getCompleteZones(ServerLevel level) {
        return get(level).zones.values().stream()
                .filter(TutorialMusicZone::isComplete)
                .toList();
    }

    public static List<TutorialMusicZone> getAllZones(ServerLevel level) {
        return new ArrayList<>(get(level).zones.values());
    }

    private record ZoneData(
            String id,
            String soundId,
            BlockPos corner1,
            BlockPos corner2,
            float volume,
            float pitch
    ) {}
}
