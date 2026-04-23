package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.irongolem.IronGolemNoseTracker;
import com.ovrtechnology.util.Ids;
import dev.architectury.networking.NetworkManager;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class IronGolemNoseNetworking {

    public record IronGolemNoseSyncS2C(UUID golemUUID, boolean hasNose)
            implements CustomPacketPayload {
        public static final Type<IronGolemNoseSyncS2C> TYPE =
                new Type<>(Ids.mod("iron_golem_nose_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, IronGolemNoseSyncS2C>
                STREAM_CODEC =
                        StreamCodec.of(
                                (buf, payload) -> {
                                    buf.writeUUID(payload.golemUUID);
                                    buf.writeBoolean(payload.hasNose);
                                },
                                buf -> new IronGolemNoseSyncS2C(buf.readUUID(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static boolean initialized = false;

    private IronGolemNoseNetworking() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                IronGolemNoseSyncS2C.TYPE,
                IronGolemNoseSyncS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () ->
                                    IronGolemNoseTracker.setHasNose(
                                            payload.golemUUID(), payload.hasNose()));
                });

        AromaAffect.LOGGER.info("IronGolemNoseNetworking initialized");
    }

    public static void sendNoseSync(ServerPlayer player, UUID golemUUID, boolean hasNose) {
        if (!NetworkManager.canPlayerReceive(player, IronGolemNoseSyncS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(player, new IronGolemNoseSyncS2C(golemUUID, hasNose));
    }

    public static void broadcastNoseSync(
            UUID golemUUID, boolean hasNose, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            sendNoseSync(player, golemUUID, hasNose);
        }
    }
}
