package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_prefs_c2s"));
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
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_prefs_s2c"));
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

    /**
     * Initializes the networking handler.
     * Must be called on both client and server during mod initialization.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // S2C: Server tells client about a player's nose preferences
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NosePrefsS2C.TYPE, NosePrefsS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
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
                });

        // C2S: Client tells server their nose preferences
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, NosePrefsC2S.TYPE, NosePrefsC2S.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                            UUID uuid = serverPlayer.getUUID();
                            NoseRenderPreferencesManager.setServerPrefs(uuid, payload.noseEnabled(), payload.strapEnabled());

                            // Broadcast to all connected players
                            MinecraftServer server = serverPlayer.level().getServer();
                            if (server != null) {
                                broadcastPrefs(server, uuid, payload.noseEnabled(), payload.strapEnabled());
                            }
                        }
                    });
                });

        // When a player joins, send them all existing player preferences
        PlayerEvent.PLAYER_JOIN.register(serverPlayer -> {
            for (Map.Entry<UUID, NoseRenderPreferencesManager.NosePrefs> entry
                    : NoseRenderPreferencesManager.getAllServerPrefs()) {
                sendPrefsToPlayer(serverPlayer, entry.getKey(),
                        entry.getValue().noseEnabled(), entry.getValue().strapEnabled());
            }
        });

        // When a player leaves, clean up server-side data
        PlayerEvent.PLAYER_QUIT.register(serverPlayer -> {
            NoseRenderPreferencesManager.removeServerPrefs(serverPlayer.getUUID());
        });

        AromaAffect.LOGGER.info("NoseRenderNetworking initialized");
    }

    /**
     * Sends the local player's nose preferences to the server.
     * Called from the client when preferences change or when joining a world.
     */
    public static void sendPrefsToServer(net.minecraft.core.RegistryAccess registryAccess,
                                          boolean noseEnabled, boolean strapEnabled) {
        NetworkManager.sendToServer(new NosePrefsC2S(noseEnabled, strapEnabled));
    }

    private static void broadcastPrefs(MinecraftServer server, UUID playerUuid,
                                         boolean noseEnabled, boolean strapEnabled) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            sendPrefsToPlayer(target, playerUuid, noseEnabled, strapEnabled);
        }
    }

    private static void sendPrefsToPlayer(ServerPlayer target, UUID playerUuid,
                                            boolean noseEnabled, boolean strapEnabled) {
        if (!NetworkManager.canPlayerReceive(target, NosePrefsS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(target, new NosePrefsS2C(playerUuid, noseEnabled, strapEnabled));
    }
}
