package com.ovrtechnology.tutorial.musiczone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for music zones.
 * <p>
 * Checks player positions against music zones every 10 ticks.
 * When a player enters a zone, starts the zone's sound.
 * When a player leaves a zone, stops the sound.
 */
public final class TutorialMusicZoneHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    // Track which zone each player is currently in (key = player UUID, value = zone ID)
    private static final Map<UUID, String> playerCurrentZone = new ConcurrentHashMap<>();

    // Track sound repeat timing (key = player UUID, value = last sound tick)
    private static final Map<UUID, Long> lastSoundTick = new ConcurrentHashMap<>();

    // Repeat interval for looping sounds (6 seconds = 120 ticks)
    private static final long SOUND_REPEAT_TICKS = 120;

    private static long globalTick = 0;

    private TutorialMusicZoneHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            if (ReplayCompat.isReplayServer(server)) return;
            globalTick++;
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) {
                return;
            }
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;
                checkPlayerZones(player, level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial music zone handler initialized");
    }

    private static void checkPlayerZones(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        BlockPos playerPos = player.blockPosition();

        // Find which zone the player is in
        String currentZoneId = null;
        TutorialMusicZone currentZone = null;
        for (TutorialMusicZone zone : TutorialMusicZoneManager.getCompleteZones(level)) {
            if (zone.isInsideArea(playerPos)) {
                currentZoneId = zone.getId();
                currentZone = zone;
                break;
            }
        }

        String previousZoneId = playerCurrentZone.get(playerId);

        if (currentZoneId != null) {
            // Player is in a zone
            if (!currentZoneId.equals(previousZoneId)) {
                // Entered a new zone (or switched zones)
                if (previousZoneId != null) {
                    // Stop previous zone's music
                    stopZoneMusic(player, level, previousZoneId);
                }
                // Start new zone's music
                playerCurrentZone.put(playerId, currentZoneId);
                playZoneMusic(player, level, currentZone);
                lastSoundTick.put(playerId, globalTick);
            } else {
                // Still in the same zone - repeat the music if enough time has passed
                Long lastTick = lastSoundTick.get(playerId);
                if (lastTick == null || globalTick - lastTick >= SOUND_REPEAT_TICKS) {
                    playZoneMusic(player, level, currentZone);
                    lastSoundTick.put(playerId, globalTick);
                }
            }
        } else {
            // Player is not in any zone
            if (previousZoneId != null) {
                stopZoneMusic(player, level, previousZoneId);
                playerCurrentZone.remove(playerId);
                lastSoundTick.remove(playerId);
            }
        }
    }

    private static void playZoneMusic(ServerPlayer player, ServerLevel level, TutorialMusicZone zone) {
        try {
            ResourceLocation soundLoc = ResourceLocation.parse(zone.getSoundId());
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLoc);
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    soundEvent,
                    SoundSource.MUSIC,
                    zone.getVolume(),
                    zone.getPitch()
            );
            AromaAffect.LOGGER.debug("Playing music zone '{}' sound '{}' for player {}",
                    zone.getId(), zone.getSoundId(), player.getName().getString());
        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Failed to play music zone sound '{}': {}", zone.getSoundId(), e.getMessage());
        }
    }

    private static void stopZoneMusic(ServerPlayer player, ServerLevel level, String zoneId) {
        TutorialMusicZoneManager.getZone(level, zoneId).ifPresent(zone -> {
            try {
                ResourceLocation soundLoc = ResourceLocation.parse(zone.getSoundId());
                // Send stop sound packet for this specific sound
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                        soundLoc, SoundSource.MUSIC));
                AromaAffect.LOGGER.debug("Stopped music zone '{}' for player {}", zoneId, player.getName().getString());
            } catch (Exception e) {
                AromaAffect.LOGGER.warn("Failed to stop music zone sound: {}", e.getMessage());
            }
        });
    }

    /**
     * Clears tracking for a player (e.g., on disconnect or reset).
     */
    public static void clearPlayer(UUID playerId) {
        playerCurrentZone.remove(playerId);
        lastSoundTick.remove(playerId);
    }

    /**
     * Clears all tracking state.
     */
    public static void clearAll() {
        playerCurrentZone.clear();
        lastSoundTick.clear();
    }
}
