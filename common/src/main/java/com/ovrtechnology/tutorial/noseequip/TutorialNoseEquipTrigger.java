package com.ovrtechnology.tutorial.noseequip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages nose equip triggers for the tutorial system.
 * <p>
 * When a player equips a specific nose, configured actions fire:
 * waypoint, cinematic, animation, oliver action.
 * Each trigger fires only once per player (tracked by played set).
 */
public class TutorialNoseEquipTrigger extends SavedData {

    private final Map<String, NoseEquipAction> triggers = new HashMap<>();

    private static final Codec<NoseEquipAction> ACTION_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("noseId").forGetter(NoseEquipAction::noseId),
                    Codec.STRING.optionalFieldOf("onCompleteWaypointId", "").forGetter(a -> a.onCompleteWaypointId != null ? a.onCompleteWaypointId : ""),
                    Codec.STRING.optionalFieldOf("onCompleteCinematicId", "").forGetter(a -> a.onCompleteCinematicId != null ? a.onCompleteCinematicId : ""),
                    Codec.STRING.optionalFieldOf("onCompleteAnimationId", "").forGetter(a -> a.onCompleteAnimationId != null ? a.onCompleteAnimationId : ""),
                    Codec.STRING.optionalFieldOf("onCompleteOliverAction", "").forGetter(a -> a.onCompleteOliverAction != null ? a.onCompleteOliverAction : "")
            ).apply(instance, NoseEquipAction::new)
    );

    private static final Codec<TutorialNoseEquipTrigger> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ACTION_CODEC.listOf().fieldOf("triggers").forGetter(TutorialNoseEquipTrigger::getTriggerList)
            ).apply(instance, TutorialNoseEquipTrigger::new)
    );

    static final SavedDataType<TutorialNoseEquipTrigger> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_nose_equip",
            TutorialNoseEquipTrigger::new,
            CODEC,
            null
    );

    public TutorialNoseEquipTrigger() {
    }

    private TutorialNoseEquipTrigger(List<NoseEquipAction> triggerList) {
        for (NoseEquipAction action : triggerList) {
            triggers.put(action.noseId, action);
        }
    }

    private List<NoseEquipAction> getTriggerList() {
        return new ArrayList<>(triggers.values());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean addTrigger(ServerLevel level, String noseId) {
        TutorialNoseEquipTrigger manager = get(level);
        if (manager.triggers.containsKey(noseId)) return false;
        manager.triggers.put(noseId, new NoseEquipAction(noseId, "", "", "", ""));
        manager.setDirty();
        return true;
    }

    public static boolean removeTrigger(ServerLevel level, String noseId) {
        TutorialNoseEquipTrigger manager = get(level);
        if (manager.triggers.remove(noseId) != null) {
            manager.setDirty();
            return true;
        }
        return false;
    }

    public static Optional<NoseEquipAction> getTrigger(ServerLevel level, String noseId) {
        return Optional.ofNullable(get(level).triggers.get(noseId));
    }

    public static Set<String> getAllTriggerNoseIds(ServerLevel level) {
        return Set.copyOf(get(level).triggers.keySet());
    }

    public static boolean setWaypoint(ServerLevel level, String noseId, String waypointId) {
        TutorialNoseEquipTrigger manager = get(level);
        NoseEquipAction action = manager.triggers.get(noseId);
        if (action == null) return false;
        manager.triggers.put(noseId, action.withWaypoint(waypointId));
        manager.setDirty();
        return true;
    }

    public static boolean setCinematic(ServerLevel level, String noseId, String cinematicId) {
        TutorialNoseEquipTrigger manager = get(level);
        NoseEquipAction action = manager.triggers.get(noseId);
        if (action == null) return false;
        manager.triggers.put(noseId, action.withCinematic(cinematicId));
        manager.setDirty();
        return true;
    }

    public static boolean setAnimation(ServerLevel level, String noseId, String animationId) {
        TutorialNoseEquipTrigger manager = get(level);
        NoseEquipAction action = manager.triggers.get(noseId);
        if (action == null) return false;
        manager.triggers.put(noseId, action.withAnimation(animationId));
        manager.setDirty();
        return true;
    }

    public static boolean setOliverAction(ServerLevel level, String noseId, String oliverAction) {
        TutorialNoseEquipTrigger manager = get(level);
        NoseEquipAction action = manager.triggers.get(noseId);
        if (action == null) return false;
        manager.triggers.put(noseId, action.withOliverAction(oliverAction));
        manager.setDirty();
        return true;
    }

    private static TutorialNoseEquipTrigger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Immutable action data for a nose equip trigger.
     */
    public record NoseEquipAction(
            String noseId,
            String onCompleteWaypointId,
            String onCompleteCinematicId,
            String onCompleteAnimationId,
            String onCompleteOliverAction
    ) {
        public boolean hasWaypoint() { return onCompleteWaypointId != null && !onCompleteWaypointId.isEmpty(); }
        public boolean hasCinematic() { return onCompleteCinematicId != null && !onCompleteCinematicId.isEmpty(); }
        public boolean hasAnimation() { return onCompleteAnimationId != null && !onCompleteAnimationId.isEmpty(); }
        public boolean hasOliverAction() { return onCompleteOliverAction != null && !onCompleteOliverAction.isEmpty(); }

        public NoseEquipAction withWaypoint(String id) { return new NoseEquipAction(noseId, id, onCompleteCinematicId, onCompleteAnimationId, onCompleteOliverAction); }
        public NoseEquipAction withCinematic(String id) { return new NoseEquipAction(noseId, onCompleteWaypointId, id, onCompleteAnimationId, onCompleteOliverAction); }
        public NoseEquipAction withAnimation(String id) { return new NoseEquipAction(noseId, onCompleteWaypointId, onCompleteCinematicId, id, onCompleteOliverAction); }
        public NoseEquipAction withOliverAction(String action) { return new NoseEquipAction(noseId, onCompleteWaypointId, onCompleteCinematicId, onCompleteAnimationId, action); }
    }
}
