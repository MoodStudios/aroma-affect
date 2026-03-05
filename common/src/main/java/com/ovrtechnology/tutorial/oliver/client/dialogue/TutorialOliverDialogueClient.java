package com.ovrtechnology.tutorial.oliver.client.dialogue;

import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import net.minecraft.client.Minecraft;

/**
 * Client-side entry point for opening the Tutorial Oliver dialogue.
 * <p>
 * This class is called via reflection from {@link TutorialOliverEntity#openDialogueUiClient()}
 * to maintain client/common code separation.
 */
public final class TutorialOliverDialogueClient {

    private TutorialOliverDialogueClient() {
        // Utility class
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
     */
    public static void openWithParams(int entityId, String dialogueId,
                                       boolean hasTrade, String tradeId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        net.minecraft.world.entity.Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof TutorialOliverEntity oliver)) {
            return;
        }

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
}
