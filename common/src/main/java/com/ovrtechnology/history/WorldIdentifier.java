package com.ovrtechnology.history;

import net.minecraft.client.Minecraft;

/**
 * Resolves a unique identifier for the current world/server,
 * used to isolate per-world tracking history files.
 */
public final class WorldIdentifier {

    private WorldIdentifier() {}

    /**
     * Returns a sanitized ID for the current world:
     * <ul>
     *   <li>Multiplayer: {@code mp_<server_ip>}</li>
     *   <li>Singleplayer: {@code sp_<level_name>}</li>
     *   <li>Unknown: {@code unknown}</li>
     * </ul>
     */
    public static String getCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            return sanitize("mp_" + mc.getCurrentServer().ip);
        }
        if (mc.getSingleplayerServer() != null) {
            return sanitize("sp_" + mc.getSingleplayerServer().getWorldData().getLevelName());
        }
        return "unknown";
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
    }
}
