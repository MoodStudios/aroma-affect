package com.ovrtechnology.tutorial.scentzone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Manages scent zones for a world. Persisted via SavedData.
 */
public class TutorialScentZoneManager extends SavedData {

    private final Map<String, TutorialScentZone> zones = new HashMap<>();

    private static final Codec<ZoneData> ZONE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(ZoneData::id),
                    Codec.INT.fieldOf("x").forGetter(ZoneData::x),
                    Codec.INT.fieldOf("y").forGetter(ZoneData::y),
                    Codec.INT.fieldOf("z").forGetter(ZoneData::z),
                    // Backwards-compatible: reads "radiusX" if present, falls back to "radius", then default 5
                    Codec.INT.optionalFieldOf("radiusX").forGetter(d -> java.util.Optional.of(d.radiusX)),
                    Codec.INT.optionalFieldOf("radiusY").forGetter(d -> java.util.Optional.of(d.radiusY)),
                    Codec.INT.optionalFieldOf("radiusZ").forGetter(d -> java.util.Optional.of(d.radiusZ)),
                    Codec.INT.optionalFieldOf("radius").forGetter(d -> java.util.Optional.empty()),
                    Codec.STRING.fieldOf("scentName").forGetter(ZoneData::scentName),
                    Codec.DOUBLE.fieldOf("intensity").forGetter(ZoneData::intensity),
                    Codec.INT.fieldOf("cooldownSeconds").forGetter(ZoneData::cooldownSeconds),
                    Codec.BOOL.fieldOf("oneShot").forGetter(ZoneData::oneShot),
                    Codec.BOOL.fieldOf("enabled").forGetter(ZoneData::enabled)
            ).apply(instance, (id, x, y, z, rxOpt, ryOpt, rzOpt, radiusOpt, scentName, intensity, cooldown, oneShot, enabled) -> {
                // If old "radius" field exists, use it for all 3 axes
                int fallback = radiusOpt.orElse(5);
                return new ZoneData(id, x, y, z,
                        rxOpt.orElse(fallback), ryOpt.orElse(fallback), rzOpt.orElse(fallback),
                        scentName, intensity, cooldown, oneShot, enabled);
            })
    );

    private static final Codec<TutorialScentZoneManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ZONE_CODEC.listOf().fieldOf("zones").forGetter(TutorialScentZoneManager::getZoneDataList)
            ).apply(instance, TutorialScentZoneManager::new)
    );

    static final SavedDataType<TutorialScentZoneManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_scent_zones",
            TutorialScentZoneManager::new,
            CODEC,
            null
    );

    public TutorialScentZoneManager() {
    }

    private TutorialScentZoneManager(List<ZoneData> dataList) {
        for (ZoneData data : dataList) {
            zones.put(data.id, new TutorialScentZone(
                    data.id, new BlockPos(data.x, data.y, data.z),
                    data.radiusX, data.radiusY, data.radiusZ,
                    data.scentName, data.intensity,
                    data.cooldownSeconds, data.oneShot, data.enabled
            ));
        }
    }

    private List<ZoneData> getZoneDataList() {
        List<ZoneData> list = new ArrayList<>();
        for (TutorialScentZone z : zones.values()) {
            list.add(new ZoneData(
                    z.getId(), z.getPosition().getX(), z.getPosition().getY(), z.getPosition().getZ(),
                    z.getRadiusX(), z.getRadiusY(), z.getRadiusZ(),
                    z.getScentName(), z.getIntensity(),
                    z.getCooldownSeconds(), z.isOneShot(), z.isEnabled()
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static CRUD API
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean createZone(ServerLevel level, String id, BlockPos pos, int radiusX, int radiusY, int radiusZ, String scentName) {
        TutorialScentZoneManager mgr = get(level);
        if (mgr.zones.containsKey(id)) return false;
        mgr.zones.put(id, new TutorialScentZone(id, pos, radiusX, radiusY, radiusZ, scentName));
        mgr.setDirty();
        AromaAffect.LOGGER.info("Created scent zone '{}'", id);
        return true;
    }

    public static boolean deleteZone(ServerLevel level, String id) {
        TutorialScentZoneManager mgr = get(level);
        if (mgr.zones.remove(id) != null) {
            mgr.setDirty();
            AromaAffect.LOGGER.info("Deleted scent zone '{}'", id);
            return true;
        }
        return false;
    }

    public static Optional<TutorialScentZone> getZone(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).zones.get(id));
    }

    public static Collection<TutorialScentZone> getAllZones(ServerLevel level) {
        return Collections.unmodifiableCollection(get(level).zones.values());
    }

    public static Set<String> getAllZoneIds(ServerLevel level) {
        return new HashSet<>(get(level).zones.keySet());
    }

    public static void updateZone(ServerLevel level, TutorialScentZone zone) {
        TutorialScentZoneManager mgr = get(level);
        mgr.zones.put(zone.getId(), zone);
        mgr.setDirty();
    }

    private static TutorialScentZoneManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────

    private record ZoneData(String id, int x, int y, int z, int radiusX, int radiusY, int radiusZ,
                            String scentName, double intensity,
                            int cooldownSeconds, boolean oneShot, boolean enabled) {}
}
