package com.ovrtechnology.tutorial.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Manages tutorial portals (teleportation zones) for a world.
 * <p>
 * Portals define areas that teleport players when entered.
 */
public class TutorialPortalManager extends SavedData {

    private final Map<String, TutorialPortal> portals = new HashMap<>();

    private static final Codec<PortalData> PORTAL_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(PortalData::id),
                    BlockPos.CODEC.optionalFieldOf("sourceCorner1").forGetter(d -> Optional.ofNullable(d.sourceCorner1)),
                    BlockPos.CODEC.optionalFieldOf("sourceCorner2").forGetter(d -> Optional.ofNullable(d.sourceCorner2)),
                    BlockPos.CODEC.optionalFieldOf("destination").forGetter(d -> Optional.ofNullable(d.destination)),
                    Codec.FLOAT.fieldOf("destYaw").forGetter(PortalData::destYaw),
                    Codec.FLOAT.fieldOf("destPitch").forGetter(PortalData::destPitch)
            ).apply(instance, (id, c1, c2, dest, yaw, pitch) ->
                    new PortalData(id, c1.orElse(null), c2.orElse(null), dest.orElse(null), yaw, pitch))
    );

    private static final Codec<TutorialPortalManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    PORTAL_CODEC.listOf().fieldOf("portals").forGetter(m -> m.getPortalDataList())
            ).apply(instance, TutorialPortalManager::new)
    );

    static final SavedDataType<TutorialPortalManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_portals",
            TutorialPortalManager::new,
            CODEC,
            null
    );

    public TutorialPortalManager() {
    }

    private TutorialPortalManager(List<PortalData> portalDataList) {
        for (PortalData data : portalDataList) {
            TutorialPortal portal = new TutorialPortal(
                    data.id,
                    data.sourceCorner1,
                    data.sourceCorner2,
                    data.destination,
                    data.destYaw,
                    data.destPitch
            );
            portals.put(data.id, portal);
        }
    }

    private List<PortalData> getPortalDataList() {
        List<PortalData> list = new ArrayList<>();
        for (TutorialPortal portal : portals.values()) {
            list.add(new PortalData(
                    portal.getId(),
                    portal.getSourceCorner1(),
                    portal.getSourceCorner2(),
                    portal.getDestination(),
                    portal.getDestYaw(),
                    portal.getDestPitch()
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Portal CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean createPortal(ServerLevel level, String id) {
        TutorialPortalManager manager = get(level);
        if (manager.portals.containsKey(id)) {
            return false;
        }
        manager.portals.put(id, new TutorialPortal(id));
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial portal: {}", id);
        return true;
    }

    public static boolean deletePortal(ServerLevel level, String id) {
        TutorialPortalManager manager = get(level);
        if (manager.portals.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial portal: {}", id);
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Source Area
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean setSourceCorner(ServerLevel level, String id, int corner, BlockPos pos) {
        TutorialPortalManager manager = get(level);
        TutorialPortal portal = manager.portals.get(id);
        if (portal == null) {
            return false;
        }
        if (corner == 1) {
            portal.setSourceCorner1(pos);
        } else if (corner == 2) {
            portal.setSourceCorner2(pos);
        } else {
            return false;
        }
        manager.setDirty();
        AromaAffect.LOGGER.info("Set portal {} source corner {} to {}", id, corner, pos);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Destination
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean setDestination(ServerLevel level, String id, BlockPos pos, float yaw, float pitch) {
        TutorialPortalManager manager = get(level);
        TutorialPortal portal = manager.portals.get(id);
        if (portal == null) {
            return false;
        }
        portal.setDestination(pos);
        portal.setDestYaw(yaw);
        portal.setDestPitch(pitch);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set portal {} destination to {} (yaw={}, pitch={})", id, pos, yaw, pitch);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Queries
    // ─────────────────────────────────────────────────────────────────────────────

    public static Optional<TutorialPortal> getPortal(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).portals.get(id));
    }

    public static Set<String> getAllPortalIds(ServerLevel level) {
        return new HashSet<>(get(level).portals.keySet());
    }

    public static List<TutorialPortal> getCompletePortals(ServerLevel level) {
        List<TutorialPortal> complete = new ArrayList<>();
        for (TutorialPortal portal : get(level).portals.values()) {
            if (portal.isComplete()) {
                complete.add(portal);
            }
        }
        return complete;
    }

    public static List<TutorialPortal> getAllPortals(ServerLevel level) {
        return new ArrayList<>(get(level).portals.values());
    }

    private static TutorialPortalManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data record
    // ─────────────────────────────────────────────────────────────────────────────

    private record PortalData(
            String id,
            BlockPos sourceCorner1,
            BlockPos sourceCorner2,
            BlockPos destination,
            float destYaw,
            float destPitch
    ) {}
}
