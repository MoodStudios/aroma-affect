package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import dev.architectury.event.events.common.PlayerEvent;
import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.phys.AABB;

@UtilityClass
public final class SnifferSyncHandler {

    public static void init() {

        PlayerEvent.PLAYER_JOIN.register(SnifferSyncHandler::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) return;

        server.execute(() -> syncAllSniffersToPlayer(player));
    }

    private static void syncAllSniffersToPlayer(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {

            AABB worldBounds = new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000);

            for (Sniffer sniffer : level.getEntitiesOfClass(Sniffer.class, worldBounds)) {
                SnifferTamingData data = SnifferTamingData.get(sniffer.getUUID());

                if (data.ownerUUID != null
                        || !data.saddleItem.isEmpty()
                        || !data.decorationItem.isEmpty()) {
                    SnifferEquipmentNetworking.sendEquipmentSync(player, sniffer.getUUID(), data);
                    AromaAffect.LOGGER.debug(
                            "Synced sniffer {} data to joining player {}",
                            sniffer.getUUID(),
                            player.getName().getString());
                }
            }
        }
    }
}
