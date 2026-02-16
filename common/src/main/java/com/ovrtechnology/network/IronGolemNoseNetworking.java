package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.irongolem.IronGolemNoseTracker;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Handles server-to-client networking for Iron Golem nose state synchronization.
 */
public final class IronGolemNoseNetworking {

    private static final ResourceLocation IRON_GOLEM_NOSE_SYNC_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "iron_golem_nose_sync");

    private static boolean initialized = false;

    private IronGolemNoseNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, IRON_GOLEM_NOSE_SYNC_ID, (buf, context) -> {
            UUID golemUUID = buf.readUUID();
            boolean hasNose = buf.readBoolean();

            context.queue(() -> IronGolemNoseTracker.setHasNose(golemUUID, hasNose));
        });

        AromaAffect.LOGGER.info("IronGolemNoseNetworking initialized");
    }

    /**
     * Sends the Iron Golem nose state to a specific player.
     */
    public static void sendNoseSync(ServerPlayer player, UUID golemUUID, boolean hasNose) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeUUID(golemUUID);
        buf.writeBoolean(hasNose);

        if (!NetworkManager.canPlayerReceive(player, IRON_GOLEM_NOSE_SYNC_ID)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, IRON_GOLEM_NOSE_SYNC_ID, buf);
    }

    /**
     * Sends the Iron Golem nose state to all provided players.
     */
    public static void broadcastNoseSync(UUID golemUUID, boolean hasNose, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            sendNoseSync(player, golemUUID, hasNose);
        }
    }
}
