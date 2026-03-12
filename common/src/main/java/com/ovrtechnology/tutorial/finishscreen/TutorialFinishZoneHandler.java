package com.ovrtechnology.tutorial.finishscreen;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.networking.NetworkManager;
import dev.architectury.event.events.common.TickEvent;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for finish screen trigger zones.
 * <p>
 * When a player enters a finish zone, sends a S2C packet to open
 * the finish/thank you screen. Each player can only trigger it once
 * until reset.
 */
public final class TutorialFinishZoneHandler {

    private static final ResourceLocation FINISH_SCREEN_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_finish_screen");

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    // Track which players have already seen the finish screen
    private static final Set<UUID> triggeredPlayers = ConcurrentHashMap.newKeySet();

    private TutorialFinishZoneHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register S2C receiver (client will handle opening the screen)
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                FINISH_SCREEN_PACKET,
                (buf, context) -> {
                    context.queue(() -> openFinishScreenOnClient());
                }
        );

        // Tick handler: check player positions
        TickEvent.SERVER_POST.register(server -> {
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;
                checkPlayer(player, level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial finish zone handler initialized");
    }

    private static void checkPlayer(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        if (triggeredPlayers.contains(playerId)) return;

        BlockPos playerPos = player.blockPosition();
        for (TutorialFinishZone zone : TutorialFinishZoneManager.getCompleteZones(level)) {
            if (zone.isInsideArea(playerPos)) {
                triggeredPlayers.add(playerId);
                sendFinishScreen(player);
                AromaAffect.LOGGER.info("Player {} entered finish zone '{}', showing finish screen",
                        player.getName().getString(), zone.getId());
                return;
            }
        }
    }

    private static void sendFinishScreen(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), player.registryAccess());
        NetworkManager.sendToPlayer(player, FINISH_SCREEN_PACKET, buf);
    }

    private static void openFinishScreenOnClient() {
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.finishscreen.client.TutorialFinishScreen");
            clientClass.getMethod("open").invoke(null);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to open finish screen on client", e);
        }
    }

    public static void resetPlayer(UUID playerId) {
        triggeredPlayers.remove(playerId);
    }

    public static void resetAll() {
        triggeredPlayers.clear();
    }
}
