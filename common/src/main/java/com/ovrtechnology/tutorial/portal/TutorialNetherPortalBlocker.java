package com.ovrtechnology.tutorial.portal;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks Nether portal usage when the tutorial GameRule is active.
 * <p>
 * This prevents players from accidentally leaving the tutorial world
 * via Nether portals. Only affects worlds where {@code isOvrTutorial} is true.
 * <p>
 * Vanilla gameplay is not affected - portals work normally when the
 * GameRule is false (default).
 */
public final class TutorialNetherPortalBlocker {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 5; // Check every 5 ticks

    // Track when we last warned each player to avoid spam
    private static final Map<UUID, Long> lastWarningTime = new ConcurrentHashMap<>();
    private static final long WARNING_COOLDOWN_MS = 3000; // 3 seconds

    private TutorialNetherPortalBlocker() {
    }

    /**
     * Initializes the Nether portal blocker.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) {
                return;
            }
            tickCounter = 0;

            // Check all players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkAndBlockPortal(player);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial Nether portal blocker initialized");
    }

    /**
     * Checks if a player is in a Nether portal and blocks them if tutorial mode is active.
     */
    private static void checkAndBlockPortal(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Only block in tutorial mode
        if (!TutorialModule.isActive(level)) {
            return;
        }

        // Check if player is in a portal
        if (!isInNetherPortal(player)) {
            return;
        }

        // Reset the player's portal time to prevent teleportation
        // This field tracks how long the player has been in the portal
        player.portalProcess = null;

        // Push the player out of the portal
        pushPlayerOutOfPortal(player);

        // Send warning message (with cooldown to prevent spam)
        sendWarningMessage(player);
    }

    /**
     * Checks if a player is standing in a Nether portal block.
     */
    private static boolean isInNetherPortal(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();

        // Check the block at the player's position and one block up (for tall players)
        for (int yOffset = 0; yOffset <= 1; yOffset++) {
            BlockPos checkPos = playerPos.above(yOffset);
            BlockState state = player.level().getBlockState(checkPos);
            if (state.is(Blocks.NETHER_PORTAL)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Pushes the player out of the portal.
     */
    private static void pushPlayerOutOfPortal(ServerPlayer player) {
        // Give the player a small push away from the portal
        // We push them in the direction they came from (opposite of their facing)
        double pushStrength = 0.5;

        // Get the player's facing direction and push opposite
        double yaw = Math.toRadians(player.getYRot());
        double pushX = Math.sin(yaw) * pushStrength;
        double pushZ = -Math.cos(yaw) * pushStrength;

        // Apply velocity
        player.setDeltaMovement(pushX, 0.1, pushZ);
        player.hurtMarked = true;
    }

    /**
     * Sends a warning message to the player about blocked portals.
     */
    private static void sendWarningMessage(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long lastWarning = lastWarningTime.get(player.getUUID());

        if (lastWarning != null && (now - lastWarning) < WARNING_COOLDOWN_MS) {
            return; // Still in cooldown
        }

        lastWarningTime.put(player.getUUID(), now);

        // Send action bar message (less intrusive than chat)
        player.displayClientMessage(
                Component.literal("\u00a7c\u00a7l[Tutorial] \u00a7cNether portals are disabled in tutorial mode"),
                true // Action bar
        );
    }

    /**
     * Cleans up tracking data for a player who has left.
     */
    public static void onPlayerLeave(UUID playerId) {
        lastWarningTime.remove(playerId);
    }
}
