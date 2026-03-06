package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import dev.architectury.event.events.common.PlayerEvent;
import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.phys.AABB;

/**
 * Handles syncing sniffer taming data to clients when they join the server
 * or when sniffers come into view.
 */
@UtilityClass
public final class SnifferSyncHandler {

    public static void init() {
        // Sync all tamed sniffers to the player when they join
        PlayerEvent.PLAYER_JOIN.register(SnifferSyncHandler::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null || ReplayCompat.isReplayServer(server)) return;

        // Schedule the sync for the next tick to ensure the player is fully loaded
        server.execute(() -> syncAllSniffersToPlayer(player));
    }

    /**
     * Syncs all loaded tamed sniffers to a player.
     */
    private static void syncAllSniffersToPlayer(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            // Use a large AABB that covers the typical world bounds
            AABB worldBounds = new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000);
            // Iterate through all Sniffer entities in this level
            for (Sniffer sniffer : level.getEntitiesOfClass(Sniffer.class, worldBounds)) {
                SnifferTamingData data = SnifferTamingData.get(sniffer.getUUID());
                // Only sync if the sniffer has relevant data (tamed or has items)
                if (data.ownerUUID != null || !data.saddleItem.isEmpty() || !data.decorationItem.isEmpty()) {
                    SnifferEquipmentNetworking.sendEquipmentSync(player, sniffer.getUUID(), data);
                    AromaAffect.LOGGER.debug("Synced sniffer {} data to joining player {}",
                            sniffer.getUUID(), player.getName().getString());
                }
            }
        }
    }
}
