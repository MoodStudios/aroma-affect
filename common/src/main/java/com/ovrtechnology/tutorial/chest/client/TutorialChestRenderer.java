package com.ovrtechnology.tutorial.chest.client;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side manager for tutorial chest icon positions.
 * <p>
 * Delegates rendering to {@link TutorialChestHologram} which renders
 * a PNG exclamation icon above each unconsumed tutorial chest.
 */
public final class TutorialChestRenderer {

    // Active chest positions
    private static final Set<BlockPos> chestPositions = new HashSet<>();

    private static boolean initialized = false;

    private TutorialChestRenderer() {
    }

    /**
     * Initializes the chest renderer.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Sync positions to hologram renderer each tick
        ClientTickEvent.CLIENT_POST.register(TutorialChestRenderer::onClientTick);
        AromaAffect.LOGGER.debug("Tutorial chest renderer initialized");
    }

    /**
     * Sets the chest positions to render.
     *
     * @param positions array of chest positions
     */
    public static void setChestPositions(BlockPos[] positions) {
        chestPositions.clear();
        if (positions != null) {
            for (BlockPos pos : positions) {
                chestPositions.add(pos);
            }
        }
        TutorialChestHologram.setPositions(chestPositions);
        AromaAffect.LOGGER.debug("Chest renderer: {} positions set", chestPositions.size());
    }

    /**
     * Removes a single chest position.
     *
     * @param position the position to remove
     */
    public static void removeChestPosition(BlockPos position) {
        chestPositions.remove(position);
        TutorialChestHologram.removePosition(position);
        AromaAffect.LOGGER.debug("Chest renderer: removed position {}", position);
    }

    /**
     * Clears all chest positions.
     */
    public static void clearChestPositions() {
        chestPositions.clear();
        TutorialChestHologram.clear();
        AromaAffect.LOGGER.debug("Chest renderer: cleared all positions");
    }

    private static void onClientTick(Minecraft minecraft) {
        // Keep hologram positions in sync
        if (!chestPositions.isEmpty()) {
            TutorialChestHologram.setPositions(chestPositions);
        }
    }
}
