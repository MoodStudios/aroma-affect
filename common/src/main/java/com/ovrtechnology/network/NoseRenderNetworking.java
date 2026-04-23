package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.util.Ids;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class NoseRenderNetworking {

    public record NosePrefsC2S(boolean noseEnabled, boolean strapEnabled)
            implements CustomPacketPayload {
        public static final Type<NosePrefsC2S> TYPE = new Type<>(Ids.mod("nose_prefs_c2s"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NosePrefsC2S> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeBoolean(payload.noseEnabled);
                            buf.writeBoolean(payload.strapEnabled);
                        },
                        buf -> new NosePrefsC2S(buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record NosePrefsS2C(UUID playerUuid, boolean noseEnabled, boolean strapEnabled)
            implements CustomPacketPayload {
        public static final Type<NosePrefsS2C> TYPE = new Type<>(Ids.mod("nose_prefs_s2c"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NosePrefsS2C> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeUUID(payload.playerUuid);
                            buf.writeBoolean(payload.noseEnabled);
                            buf.writeBoolean(payload.strapEnabled);
                        },
                        buf ->
                                new NosePrefsS2C(
                                        buf.readUUID(), buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static boolean initialized = false;

    private NoseRenderNetworking() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                NosePrefsS2C.TYPE,
                NosePrefsS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                UUID localUuid =
                                        net.minecraft.client.Minecraft.getInstance().player != null
                                                ? net.minecraft.client.Minecraft.getInstance()
                                                        .player
                                                        .getUUID()
                                                : null;
                                boolean isSelf =
                                        localUuid != null && payload.playerUuid().equals(localUuid);

                                if (isSelf) {
                                    return;
                                }
                                NoseRenderPreferencesManager.setClientPrefs(
                                        payload.playerUuid(),
                                        payload.noseEnabled(),
                                        payload.strapEnabled());
                            });
                });

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                NosePrefsC2S.TYPE,
                NosePrefsC2S.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                                    UUID uuid = serverPlayer.getUUID();
                                    NoseRenderPreferencesManager.setServerPrefs(
                                            uuid, payload.noseEnabled(), payload.strapEnabled());

                                    MinecraftServer server = serverPlayer.level().getServer();
                                    if (server != null) {
                                        broadcastPrefs(
                                                server,
                                                uuid,
                                                payload.noseEnabled(),
                                                payload.strapEnabled());
                                    }
                                }
                            });
                });

        PlayerEvent.PLAYER_JOIN.register(
                serverPlayer -> {
                    for (Map.Entry<UUID, NoseRenderPreferencesManager.NosePrefs> entry :
                            NoseRenderPreferencesManager.getAllServerPrefs()) {
                        sendPrefsToPlayer(
                                serverPlayer,
                                entry.getKey(),
                                entry.getValue().noseEnabled(),
                                entry.getValue().strapEnabled());
                    }
                });

        PlayerEvent.PLAYER_QUIT.register(
                serverPlayer -> {
                    NoseRenderPreferencesManager.removeServerPrefs(serverPlayer.getUUID());
                });

        AromaAffect.LOGGER.info("NoseRenderNetworking initialized");
    }

    public static void sendPrefsToServer(
            net.minecraft.core.RegistryAccess registryAccess,
            boolean noseEnabled,
            boolean strapEnabled) {
        NetworkManager.sendToServer(new NosePrefsC2S(noseEnabled, strapEnabled));
    }

    private static void broadcastPrefs(
            MinecraftServer server, UUID playerUuid, boolean noseEnabled, boolean strapEnabled) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            sendPrefsToPlayer(target, playerUuid, noseEnabled, strapEnabled);
        }
    }

    private static void sendPrefsToPlayer(
            ServerPlayer target, UUID playerUuid, boolean noseEnabled, boolean strapEnabled) {
        if (!NetworkManager.canPlayerReceive(target, NosePrefsS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(
                target, new NosePrefsS2C(playerUuid, noseEnabled, strapEnabled));
    }
}
