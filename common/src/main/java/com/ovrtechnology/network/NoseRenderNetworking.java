package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

    private static final ResourceLocation NOSE_PREFS_C2S =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_prefs_c2s");
    private static final ResourceLocation NOSE_PREFS_S2C =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_prefs_s2c");

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
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NOSE_PREFS_S2C, (buf, context) -> {
            UUID playerUuid = buf.readUUID();
            boolean noseEnabled = buf.readBoolean();
            boolean strapEnabled = buf.readBoolean();

            context.queue(() -> {
                UUID localUuid = net.minecraft.client.Minecraft.getInstance().player != null
                        ? net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
                boolean isSelf = localUuid != null && playerUuid.equals(localUuid);

                // Local player preferences are authoritative on this client.
                // Ignore echoed server packets for self to avoid desync when toggling rapidly.
                if (isSelf) {
                    return;
                }
                NoseRenderPreferencesManager.setClientPrefs(playerUuid, noseEnabled, strapEnabled);
            });
        });

        // C2S: Client tells server their nose preferences
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, NOSE_PREFS_C2S, (buf, context) -> {
            boolean noseEnabled = buf.readBoolean();
            boolean strapEnabled = buf.readBoolean();

            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    UUID uuid = serverPlayer.getUUID();
                    NoseRenderPreferencesManager.setServerPrefs(uuid, noseEnabled, strapEnabled);

                    // Broadcast to all connected players
                    MinecraftServer server = serverPlayer.level().getServer();
                    if (server != null) {
                        broadcastPrefs(server, uuid, noseEnabled, strapEnabled);
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
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registryAccess);
        buf.writeBoolean(noseEnabled);
        buf.writeBoolean(strapEnabled);
        NetworkManager.sendToServer(NOSE_PREFS_C2S, buf);
    }

    private static void broadcastPrefs(MinecraftServer server, UUID playerUuid,
                                         boolean noseEnabled, boolean strapEnabled) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            sendPrefsToPlayer(target, playerUuid, noseEnabled, strapEnabled);
        }
    }

    private static void sendPrefsToPlayer(ServerPlayer target, UUID playerUuid,
                                            boolean noseEnabled, boolean strapEnabled) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), target.registryAccess());
        buf.writeUUID(playerUuid);
        buf.writeBoolean(noseEnabled);
        buf.writeBoolean(strapEnabled);

        if (!NetworkManager.canPlayerReceive(target, NOSE_PREFS_S2C)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(target, NOSE_PREFS_S2C, buf);
    }
}
