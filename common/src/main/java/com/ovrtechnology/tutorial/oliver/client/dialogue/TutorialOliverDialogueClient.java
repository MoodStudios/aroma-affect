package com.ovrtechnology.tutorial.oliver.client.dialogue;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side entry point for opening the Tutorial Oliver dialogue.
 * <p>
 * This class is called via reflection from {@link TutorialOliverEntity#openDialogueUiClient()}
 * to maintain client/common code separation.
 */
public final class TutorialOliverDialogueClient {

    /**
     * Tracks how many forced dialogues have been opened this session.
     * Used to alternate Kindred scent triggers (first always, then every other).
     */
    private static int forcedDialogueCount = 0;

    private TutorialOliverDialogueClient() {
        // Utility class
    }

    /**
     * Resets the forced dialogue counter (e.g., when starting a new tutorial session).
     */
    public static void resetForcedDialogueCount() {
        forcedDialogueCount = 0;
    }

    /**
     * Opens the dialogue screen for the given Oliver entity.
     *
     * @param oliver the Tutorial Oliver entity to display dialogue for
     */
    public static void open(TutorialOliverEntity oliver) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new TutorialOliverDialogueScreen(oliver));
    }

    /**
     * Opens the dialogue screen with specific parameters from a network packet.
     *
     * @param entityId   the Oliver entity ID
     * @param dialogueId the dialogue ID to display
     * @param hasTrade   whether to show a trade button
     * @param tradeId    the trade ID for the trade button
     * @param forced     true if triggered by stage progression (not manual click)
     */
    public static void openWithParams(int entityId, String dialogueId,
                                       boolean hasTrade, String tradeId, boolean forced) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            AromaAffect.LOGGER.warn("[DialogueClient] Cannot open dialogue: level is null");
            return;
        }

        net.minecraft.world.entity.Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof TutorialOliverEntity oliver)) {
            AromaAffect.LOGGER.warn("[DialogueClient] Cannot open dialogue '{}': entity {} not found or not Oliver (found: {})",
                    dialogueId, entityId, entity != null ? entity.getClass().getSimpleName() : "null");
            return;
        }

        // Trigger Kindred scent only on the FIRST forced dialogue
        if (forced && forcedDialogueCount == 0) {
            triggerKindredScent();
        }
        if (forced) {
            forcedDialogueCount++;
        }
        // Oliver asks about the smoke after the mine explosion; the explosion's own puff ends as he speaks
        if (forced && "oliver_path_opened".equals(dialogueId)) {
            triggerScent("Smoky", 100);
        }
        // Oliver asks about the metal in the air as the dragon fight starts; the room's own Machina zone
        // is off to the side and many players never walk into it. Boss dialogues are not sent as forced
        if ("boss_dragon_enter".equals(dialogueId)) {
            triggerScent("Machina", 100);
        }

        AromaAffect.LOGGER.info("[DialogueClient] Opening dialogue '{}' for Oliver entityId={}", dialogueId, entityId);
        minecraft.setScreen(new TutorialOliverDialogueScreen(oliver, dialogueId, hasTrade, tradeId));
    }

    /**
     * Opens a self-dialogue screen where the player's own skin is rendered.
     * Used for the dream ending sequence.
     *
     * @param dialogueId the dialogue ID to display
     */
    public static void openSelfDialogue(String dialogueId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        String playerName = minecraft.player.getName().getString();
        minecraft.setScreen(new TutorialOliverDialogueScreen(
                minecraft.player, playerName, dialogueId));
    }

    /**
     * Triggers the Kindred scent on the OVR hardware with visual feedback.
     */
    private static void triggerKindredScent() {
        triggerScent("Kindred", 200); // 10 seconds
    }

    private static void triggerScent(String scentName, int durationTicks) {
        ScentTrigger trigger = ScentTrigger.create(
                scentName,
                ScentTriggerSource.CUSTOM_EVENT,
                ScentPriority.HIGH,
                durationTicks,
                1.0
        );
        boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);

        if (triggered) {
            // Show border overlay (respects user config)
            if (ClientConfig.getInstance().isPassivePuffOverlay()) {
                ScentPuffOverlay.onScentPuff(scentName, 1.0);
            }

            // Chat message disabled for PAX demo
        }

        AromaAffect.LOGGER.info("[DialogueClient] Triggered {} scent (sent: {})", scentName, triggered);
    }
}
