package com.ovrtechnology.history;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

public final class WorldIdentifier {

    private static final LevelResource ROOT = new LevelResource("");

    private WorldIdentifier() {}

    public static String getCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            return sanitize("mp_" + mc.getCurrentServer().ip);
        }
        if (mc.getSingleplayerServer() != null) {

            String folderName =
                    mc.getSingleplayerServer().getWorldPath(ROOT).getFileName().toString();
            return sanitize("sp_" + folderName);
        }
        return "unknown";
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
    }
}
