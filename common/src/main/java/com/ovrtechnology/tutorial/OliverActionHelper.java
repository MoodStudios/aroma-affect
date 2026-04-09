package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialIntroNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared helper for player-only oliver actions that don't require an Oliver entity.
 * Called from the "first pass" of executeOliverAction() in all handlers.
 */
public final class OliverActionHelper {

    private OliverActionHelper() {}

    /**
     * Processes a player-only action if recognized.
     *
     * @return true if the action was handled, false if not recognized
     */
    public static boolean processPlayerAction(ServerPlayer player, String action) {
        String actionLower = action.toLowerCase().trim();

        if (actionLower.equals("unlockpassive")) {
            TutorialIntroNetworking.sendPassiveLock(player, false);
            AromaAffect.LOGGER.info("Oliver action: passive mode unlocked for player {}", player.getName().getString());
            return true;
        }

        if (actionLower.equals("lockpassive")) {
            TutorialIntroNetworking.sendPassiveLock(player, true);
            AromaAffect.LOGGER.info("Oliver action: passive mode locked for player {}", player.getName().getString());
            return true;
        }

        return false;
    }
}
