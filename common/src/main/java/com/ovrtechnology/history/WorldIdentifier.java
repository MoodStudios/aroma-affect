package com.ovrtechnology.history;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Resolves a unique identifier for the current world/server,
 * used to isolate per-world tracking history files.
 */
public final class WorldIdentifier {

    private static final LevelResource ROOT = new LevelResource("");

    private WorldIdentifier() {}

    /**
     * Returns a sanitized ID for the current world:
     * <ul>
     *   <li>Multiplayer: {@code mp_<server_ip>}</li>
     *   <li>Singleplayer: {@code sp_<save_folder_name>} (unique per world)</li>
     *   <li>Unknown: {@code unknown}</li>
     * </ul>
     */
    public static String getCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            return sanitize("mp_" + mc.getCurrentServer().ip);
        }
        if (mc.getSingleplayerServer() != null) {
            // Use the save folder name (unique) instead of the display name (not unique).
            // e.g. "New World" folder vs "New World (2)" folder for two worlds named "New World".
            String folderName = mc.getSingleplayerServer()
                    .getWorldPath(ROOT)
                    .getFileName()
                    .toString();
            return sanitize("sp_" + folderName);
        }
        return "unknown";
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
    }
}
