package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

/**
 * Handles client-server networking for per-player nose render preferences.
 *
 * <p>When a player changes their nose/strap visibility, a C2S packet is sent
 * to the server. The server stores the preferences and broadcasts them to
 * all connected players via S2C packets.</p>
 */
public final class NoseRenderNetworking {

    public record NosePrefsC2S(boolean noseEnabled, boolean strapEnabled) implements CustomPacketPayload {
        public static final Type<NosePrefsC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_prefs_c2s"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NosePrefsC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.noseEnabled);
                    buf.writeBoolean(payload.strapEnabled);
                },
                buf -> new NosePrefsC2S(buf.readBoolean(), buf.readBoolean())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record NosePrefsS2C(UUID playerUuid, boolean noseEnabled, boolean strapEnabled) implements CustomPacketPayload {
        public static final Type<NosePrefsS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_prefs_s2c"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NosePrefsS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUUID(payload.playerUuid);
                    buf.writeBoolean(payload.noseEnabled);
                    buf.writeBoolean(payload.strapEnabled);
                },
                buf -> new NosePrefsS2C(buf.readUUID(), buf.readBoolean(), buf.readBoolean())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private NoseRenderNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // S2C: Server tells client about a player's nose preferences
        Balm.networking().registerClientboundPacket(
                NosePrefsS2C.TYPE,
                NosePrefsS2C.class,
                NosePrefsS2C.STREAM_CODEC,
                (player, payload) -> {
                    UUID localUuid = net.minecraft.client.Minecraft.getInstance().player != null
                            ? net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
                    boolean isSelf = localUuid != null && payload.playerUuid().equals(localUuid);

                    // Local player preferences are authoritative on this client.
                    // Ignore echoed server packets for self to avoid desync when toggling rapidly.
                    if (isSelf) {
                        return;
                    }
                    NoseRenderPreferencesManager.setClientPrefs(payload.playerUuid(), payload.noseEnabled(), payload.strapEnabled());
                });

        // C2S: Client tells server their nose preferences
        Balm.networking().registerServerboundPacket(
                NosePrefsC2S.TYPE,
                NosePrefsC2S.class,
                NosePrefsC2S.STREAM_CODEC,
                (serverPlayer, payload) -> {
                    UUID uuid = serverPlayer.getUUID();
                    NoseRenderPreferencesManager.setServerPrefs(uuid, payload.noseEnabled(), payload.strapEnabled());

                    MinecraftServer server = serverPlayer.level().getServer();
                    if (server != null) {
                        broadcastPrefs(server, uuid, payload.noseEnabled(), payload.strapEnabled());
                    }
                });

        // When a player joins, send them all existing player preferences
        ServerPlayerCallback.Join.EVENT.register(serverPlayer -> {
            for (Map.Entry<UUID, NoseRenderPreferencesManager.NosePrefs> entry
                    : NoseRenderPreferencesManager.getAllServerPrefs()) {
                Balm.networking().sendTo(serverPlayer, new NosePrefsS2C(
                        entry.getKey(),
                        entry.getValue().noseEnabled(),
                        entry.getValue().strapEnabled()));
            }
        });

        // When a player leaves, clean up server-side data
        ServerPlayerCallback.Leave.EVENT.register(serverPlayer -> {
            NoseRenderPreferencesManager.removeServerPrefs(serverPlayer.getUUID());
        });

        AromaAffect.LOGGER.info("NoseRenderNetworking initialized");
    }

    public static void sendPrefsToServer(net.minecraft.core.RegistryAccess registryAccess,
                                          boolean noseEnabled, boolean strapEnabled) {
        Balm.networking().sendToServer(new NosePrefsC2S(noseEnabled, strapEnabled));
    }

    private static void broadcastPrefs(MinecraftServer server, UUID playerUuid,
                                         boolean noseEnabled, boolean strapEnabled) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            Balm.networking().sendTo(target, new NosePrefsS2C(playerUuid, noseEnabled, strapEnabled));
        }
    }
}
