package com.ovrtechnology.tutorial.dialogue;

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
 * Manages tutorial dialogues for a world.
 * <p>
 * Dialogues are stored per-world and persist across server restarts.
 */
public class TutorialDialogueManager extends SavedData {

    private final Map<String, TutorialDialogue> dialogues = new HashMap<>();

    private static final Codec<DialogueData> DIALOGUE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(DialogueData::id),
                    Codec.STRING.fieldOf("text").forGetter(DialogueData::text),
                    Codec.STRING.optionalFieldOf("onCompleteWaypointId").forGetter(d -> Optional.ofNullable(d.onCompleteWaypointId)),
                    Codec.STRING.optionalFieldOf("onCompleteCinematicId").forGetter(d -> Optional.ofNullable(d.onCompleteCinematicId)),
                    Codec.STRING.optionalFieldOf("onCompleteAnimationId").forGetter(d -> Optional.ofNullable(d.onCompleteAnimationId)),
                    Codec.STRING.optionalFieldOf("onCompleteOliverAction").forGetter(d -> Optional.ofNullable(d.onCompleteOliverAction))
            ).apply(instance, (id, text, waypointId, cinematicId, animationId, oliverAction) ->
                    new DialogueData(id, text, waypointId.orElse(null), cinematicId.orElse(null),
                            animationId.orElse(null), oliverAction.orElse(null)))
    );

    private static final Codec<TutorialDialogueManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    DIALOGUE_CODEC.listOf().fieldOf("dialogues").forGetter(TutorialDialogueManager::getDialogueDataList)
            ).apply(instance, TutorialDialogueManager::new)
    );

    static final SavedDataType<TutorialDialogueManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_dialogues",
            TutorialDialogueManager::new,
            CODEC,
            null
    );

    public TutorialDialogueManager() {
    }

    private TutorialDialogueManager(List<DialogueData> dataList) {
        for (DialogueData data : dataList) {
            TutorialDialogue dialogue = new TutorialDialogue(
                    data.id,
                    data.text,
                    data.onCompleteWaypointId,
                    data.onCompleteCinematicId,
                    data.onCompleteAnimationId,
                    data.onCompleteOliverAction
            );
            dialogues.put(data.id, dialogue);
        }
    }

    private List<DialogueData> getDialogueDataList() {
        List<DialogueData> list = new ArrayList<>();
        for (TutorialDialogue d : dialogues.values()) {
            list.add(new DialogueData(
                    d.getId(),
                    d.getText(),
                    d.getOnCompleteWaypointId(),
                    d.getOnCompleteCinematicId(),
                    d.getOnCompleteAnimationId(),
                    d.getOnCompleteOliverAction()
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Dialogue CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean createDialogue(ServerLevel level, String id, String text) {
        TutorialDialogueManager manager = get(level);
        if (manager.dialogues.containsKey(id)) {
            return false;
        }
        manager.dialogues.put(id, new TutorialDialogue(id, text));
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial dialogue {}", id);
        return true;
    }

    public static boolean deleteDialogue(ServerLevel level, String id) {
        TutorialDialogueManager manager = get(level);
        if (manager.dialogues.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial dialogue {}", id);
            return true;
        }
        return false;
    }

    public static Optional<TutorialDialogue> getDialogue(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).dialogues.get(id));
    }

    public static Set<String> getAllDialogueIds(ServerLevel level) {
        return new HashSet<>(get(level).dialogues.keySet());
    }

    public static boolean setText(ServerLevel level, String id, String text) {
        TutorialDialogueManager manager = get(level);
        TutorialDialogue dialogue = manager.dialogues.get(id);
        if (dialogue == null) {
            return false;
        }
        dialogue.setText(text);
        manager.setDirty();
        return true;
    }

    public static boolean setOnCompleteWaypoint(ServerLevel level, String id, String waypointId) {
        TutorialDialogueManager manager = get(level);
        TutorialDialogue dialogue = manager.dialogues.get(id);
        if (dialogue == null) {
            return false;
        }
        dialogue.setOnCompleteWaypointId(waypointId);
        manager.setDirty();
        return true;
    }

    public static boolean setOnCompleteCinematic(ServerLevel level, String id, String cinematicId) {
        TutorialDialogueManager manager = get(level);
        TutorialDialogue dialogue = manager.dialogues.get(id);
        if (dialogue == null) {
            return false;
        }
        dialogue.setOnCompleteCinematicId(cinematicId);
        manager.setDirty();
        return true;
    }

    public static boolean setOnCompleteAnimation(ServerLevel level, String id, String animationId) {
        TutorialDialogueManager manager = get(level);
        TutorialDialogue dialogue = manager.dialogues.get(id);
        if (dialogue == null) {
            return false;
        }
        dialogue.setOnCompleteAnimationId(animationId);
        manager.setDirty();
        return true;
    }

    public static boolean setOnCompleteOliverAction(ServerLevel level, String id, String action) {
        TutorialDialogueManager manager = get(level);
        TutorialDialogue dialogue = manager.dialogues.get(id);
        if (dialogue == null) {
            return false;
        }
        dialogue.setOnCompleteOliverAction(action);
        manager.setDirty();
        return true;
    }

    /**
     * Gets all dialogues as an id→text map for syncing to clients.
     */
    public static Map<String, String> getAllDialogueTexts(ServerLevel level) {
        Map<String, String> result = new HashMap<>();
        for (TutorialDialogue d : get(level).dialogues.values()) {
            result.put(d.getId(), d.getText());
        }
        return result;
    }

    private static TutorialDialogueManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data record
    // ─────────────────────────────────────────────────────────────────────────────

    private record DialogueData(
            String id,
            String text,
            String onCompleteWaypointId,
            String onCompleteCinematicId,
            String onCompleteAnimationId,
            String onCompleteOliverAction
    ) {}
}
