package com.ovrtechnology.tutorial.cinematic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages tutorial cinematics for a world.
 * <p>
 * Cinematics are stored per-world and persist across server restarts.
 */
public class TutorialCinematicManager extends SavedData {

    private final Map<String, TutorialCinematic> cinematics = new HashMap<>();

    // Codec for CinematicFrame
    private static final Codec<CinematicFrame> FRAME_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("title").forGetter(f -> Optional.ofNullable(f.title())),
                    Codec.STRING.optionalFieldOf("subtitle").forGetter(f -> Optional.ofNullable(f.subtitle())),
                    Codec.INT.optionalFieldOf("titleColor", CinematicFrame.DEFAULT_COLOR).forGetter(CinematicFrame::titleColor),
                    Codec.INT.fieldOf("duration").forGetter(CinematicFrame::duration),
                    Codec.INT.optionalFieldOf("fadeIn", CinematicFrame.DEFAULT_FADE_IN).forGetter(CinematicFrame::fadeIn),
                    Codec.INT.optionalFieldOf("fadeOut", CinematicFrame.DEFAULT_FADE_OUT).forGetter(CinematicFrame::fadeOut),
                    Codec.STRING.optionalFieldOf("sound").forGetter(f -> Optional.ofNullable(f.sound())),
                    Codec.STRING.optionalFieldOf("oliverAction").forGetter(f -> Optional.ofNullable(f.oliverAction())),
                    Codec.DOUBLE.optionalFieldOf("cameraX").forGetter(f -> Optional.ofNullable(f.cameraX())),
                    Codec.DOUBLE.optionalFieldOf("cameraY").forGetter(f -> Optional.ofNullable(f.cameraY())),
                    Codec.DOUBLE.optionalFieldOf("cameraZ").forGetter(f -> Optional.ofNullable(f.cameraZ())),
                    Codec.FLOAT.optionalFieldOf("cameraYaw").forGetter(f -> Optional.ofNullable(f.cameraYaw())),
                    Codec.FLOAT.optionalFieldOf("cameraPitch").forGetter(f -> Optional.ofNullable(f.cameraPitch()))
            ).apply(instance, (title, subtitle, color, duration, fadeIn, fadeOut, sound, oliver,
                              camX, camY, camZ, camYaw, camPitch) ->
                    new CinematicFrame(
                            title.orElse(null),
                            subtitle.orElse(null),
                            color,
                            duration,
                            fadeIn,
                            fadeOut,
                            sound.orElse(null),
                            oliver.orElse(null),
                            camX.orElse(null),
                            camY.orElse(null),
                            camZ.orElse(null),
                            camYaw.orElse(null),
                            camPitch.orElse(null)
                    ))
    );

    // Codec for CinematicData
    private static final Codec<CinematicData> CINEMATIC_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(CinematicData::id),
                    FRAME_CODEC.listOf().fieldOf("frames").forGetter(CinematicData::frames),
                    Codec.STRING.optionalFieldOf("onCompleteWaypointId").forGetter(d -> Optional.ofNullable(d.onCompleteWaypointId)),
                    Codec.STRING.optionalFieldOf("onCompleteOliverAction").forGetter(d -> Optional.ofNullable(d.onCompleteOliverAction)),
                    Codec.STRING.optionalFieldOf("onCompleteAnimationId").forGetter(d -> Optional.ofNullable(d.onCompleteAnimationId)),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("noseOverrides")
                            .forGetter(d -> d.noseOverrides.isEmpty() ? Optional.empty() : Optional.of(d.noseOverrides))
            ).apply(instance, (id, frames, waypointId, oliverAction, animationId, noseOverrides) ->
                    new CinematicData(
                            id,
                            frames,
                            waypointId.orElse(null),
                            oliverAction.orElse(null),
                            animationId.orElse(null),
                            noseOverrides.orElse(Map.of())
                    ))
    );

    // Manager codec
    private static final Codec<TutorialCinematicManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CINEMATIC_CODEC.listOf().fieldOf("cinematics").forGetter(TutorialCinematicManager::getCinematicDataList)
            ).apply(instance, TutorialCinematicManager::new)
    );

    static final SavedDataType<TutorialCinematicManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_cinematics",
            TutorialCinematicManager::new,
            CODEC,
            null
    );

    public TutorialCinematicManager() {
    }

    private TutorialCinematicManager(List<CinematicData> cinematicDataList) {
        for (CinematicData data : cinematicDataList) {
            TutorialCinematic cinematic = new TutorialCinematic(
                    data.id,
                    data.frames,
                    data.onCompleteWaypointId,
                    data.onCompleteOliverAction,
                    data.onCompleteAnimationId,
                    data.noseOverrides
            );
            cinematics.put(data.id, cinematic);
        }
    }

    private List<CinematicData> getCinematicDataList() {
        List<CinematicData> list = new ArrayList<>();
        for (TutorialCinematic cinematic : cinematics.values()) {
            list.add(new CinematicData(
                    cinematic.getId(),
                    new ArrayList<>(cinematic.getFrames()),
                    cinematic.getOnCompleteWaypointId(),
                    cinematic.getOnCompleteOliverAction(),
                    cinematic.getOnCompleteAnimationId(),
                    new HashMap<>(cinematic.getNoseOverrides())
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Cinematic CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new empty cinematic.
     *
     * @param level the server level
     * @param id    the cinematic ID
     * @return true if created, false if already exists
     */
    public static boolean createCinematic(ServerLevel level, String id) {
        TutorialCinematicManager manager = get(level);
        if (manager.cinematics.containsKey(id)) {
            return false;
        }
        manager.cinematics.put(id, new TutorialCinematic(id));
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial cinematic: {}", id);
        return true;
    }

    /**
     * Deletes a cinematic.
     *
     * @param level the server level
     * @param id    the cinematic ID
     * @return true if deleted, false if not found
     */
    public static boolean deleteCinematic(ServerLevel level, String id) {
        TutorialCinematicManager manager = get(level);
        if (manager.cinematics.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial cinematic: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Gets a cinematic by ID.
     *
     * @param level the server level
     * @param id    the cinematic ID
     * @return Optional containing the cinematic, or empty if not found
     */
    public static Optional<TutorialCinematic> getCinematic(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).cinematics.get(id));
    }

    /**
     * Gets all cinematic IDs.
     *
     * @param level the server level
     * @return set of cinematic IDs
     */
    public static Set<String> getAllCinematicIds(ServerLevel level) {
        return new HashSet<>(get(level).cinematics.keySet());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Frame Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a frame to a cinematic.
     *
     * @param level    the server level
     * @param id       the cinematic ID
     * @param duration the frame duration in ticks
     * @return true if added, false if cinematic not found
     */
    public static boolean addFrame(ServerLevel level, String id, int duration) {
        TutorialCinematicManager manager = get(level);
        TutorialCinematic cinematic = manager.cinematics.get(id);
        if (cinematic == null) {
            return false;
        }
        cinematic.addFrame(new CinematicFrame(duration));
        manager.setDirty();
        AromaAffect.LOGGER.info("Added frame to cinematic {}: duration={}", id, duration);
        return true;
    }

    /**
     * Updates a frame in a cinematic.
     *
     * @param level      the server level
     * @param id         the cinematic ID
     * @param frameIndex the frame index (0-based)
     * @param frame      the new frame
     * @return true if updated, false if cinematic or frame not found
     */
    public static boolean updateFrame(ServerLevel level, String id, int frameIndex, CinematicFrame frame) {
        TutorialCinematicManager manager = get(level);
        TutorialCinematic cinematic = manager.cinematics.get(id);
        if (cinematic == null) {
            return false;
        }
        if (!cinematic.setFrame(frameIndex, frame)) {
            return false;
        }
        manager.setDirty();
        return true;
    }

    /**
     * Removes a frame from a cinematic.
     *
     * @param level      the server level
     * @param id         the cinematic ID
     * @param frameIndex the frame index (0-based)
     * @return true if removed, false if cinematic or frame not found
     */
    public static boolean removeFrame(ServerLevel level, String id, int frameIndex) {
        TutorialCinematicManager manager = get(level);
        TutorialCinematic cinematic = manager.cinematics.get(id);
        if (cinematic == null) {
            return false;
        }
        if (!cinematic.removeFrame(frameIndex)) {
            return false;
        }
        manager.setDirty();
        AromaAffect.LOGGER.info("Removed frame {} from cinematic {}", frameIndex, id);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - On Complete Actions
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the waypoint to activate when a cinematic completes.
     *
     * @param level      the server level
     * @param id         the cinematic ID
     * @param waypointId the waypoint ID (null to clear)
     * @return true if set, false if cinematic not found
     */
    public static boolean setOnCompleteWaypoint(ServerLevel level, String id, String waypointId) {
        TutorialCinematicManager manager = get(level);
        TutorialCinematic cinematic = manager.cinematics.get(id);
        if (cinematic == null) {
            return false;
        }
        cinematic.setOnCompleteWaypointId(waypointId);
        manager.setDirty();
        if (waypointId != null) {
            AromaAffect.LOGGER.info("Set cinematic {} onComplete waypoint: {}", id, waypointId);
        } else {
            AromaAffect.LOGGER.info("Cleared cinematic {} onComplete waypoint", id);
        }
        return true;
    }

    /**
     * Sets the Oliver action to execute when a cinematic completes.
     *
     * @param level  the server level
     * @param id     the cinematic ID
     * @param action the action (null to clear)
     * @return true if set, false if cinematic not found
     */
    public static boolean setOnCompleteOliverAction(ServerLevel level, String id, String action) {
        TutorialCinematicManager manager = get(level);
        TutorialCinematic cinematic = manager.cinematics.get(id);
        if (cinematic == null) {
            return false;
        }
        cinematic.setOnCompleteOliverAction(action);
        manager.setDirty();
        if (action != null) {
            AromaAffect.LOGGER.info("Set cinematic {} onComplete oliver: {}", id, action);
        } else {
            AromaAffect.LOGGER.info("Cleared cinematic {} onComplete oliver", id);
        }
        return true;
    }

    /**
     * Sets the animation to play when a cinematic completes.
     *
     * @param level       the server level
     * @param id          the cinematic ID
     * @param animationId the animation ID (null to clear)
     * @return true if set, false if cinematic not found
     */
    public static boolean setOnCompleteAnimation(ServerLevel level, String id, String animationId) {
        TutorialCinematicManager manager = get(level);
        TutorialCinematic cinematic = manager.cinematics.get(id);
        if (cinematic == null) {
            return false;
        }
        cinematic.setOnCompleteAnimationId(animationId);
        manager.setDirty();
        if (animationId != null) {
            AromaAffect.LOGGER.info("Set cinematic {} onComplete animation: {}", id, animationId);
        } else {
            AromaAffect.LOGGER.info("Cleared cinematic {} onComplete animation", id);
        }
        return true;
    }

    private static TutorialCinematicManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data record
    // ─────────────────────────────────────────────────────────────────────────────

    private record CinematicData(
            String id,
            List<CinematicFrame> frames,
            String onCompleteWaypointId,
            String onCompleteOliverAction,
            String onCompleteAnimationId,
            Map<String, String> noseOverrides
    ) {}
}
