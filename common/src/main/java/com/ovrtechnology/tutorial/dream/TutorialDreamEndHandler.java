package com.ovrtechnology.tutorial.dream;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialDreamOverlayNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the dream ending sequence when the tutorial dragon is defeated.
 * <p>
 * Sequence: white screen fade in → sound → teleport to house → fade out → Oliver dialogue.
 */
public final class TutorialDreamEndHandler {

    private static boolean initialized = false;

    // Track active dream sequences per player
    private static final Map<UUID, DreamSequence> activeSequences = new HashMap<>();

    // Sequence timing (in ticks)
    private static final int PHASE_FADE_START = 0;
    private static final int PHASE_FADE_MID = 20;
    private static final int PHASE_FADE_FULL = 40;
    private static final int PHASE_SOUND = 45;
    private static final int PHASE_TELEPORT = 80;
    private static final int PHASE_FADE_OUT_START = 110;
    private static final int PHASE_FADE_OUT_MID = 140;
    private static final int PHASE_FADE_OUT_END = 170;
    private static final int PHASE_DIALOGUE = 180;
    private static final int PHASE_COMPLETE = 200;

    private TutorialDreamEndHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (!TutorialModule.isActive(level)) {
                    continue;
                }
                tickSequences(level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial dream end handler initialized");
    }

    /**
     * Starts the dream ending sequence for a player.
     */
    public static void startDreamSequence(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (activeSequences.containsKey(playerId)) {
            AromaAffect.LOGGER.warn("Dream sequence already active for player {}", player.getName().getString());
            return;
        }

        activeSequences.put(playerId, new DreamSequence(0));
        AromaAffect.LOGGER.info("Started dream ending sequence for player {}", player.getName().getString());

        // Initial overlay
        TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.1f);
    }

    private static void tickSequences(ServerLevel level) {
        activeSequences.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            DreamSequence seq = entry.getValue();

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null) {
                return true; // Player left, clean up
            }

            int tick = seq.tick;

            // Phase: Fade in
            if (tick == PHASE_FADE_START) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.3f);
                // Ambient dreamy sound
                ((ServerLevel) player.level()).playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.AMBIENT, 1.0F, 0.5F);
            } else if (tick == PHASE_FADE_MID) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.6f);
            } else if (tick == PHASE_FADE_FULL) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 1.0f);
            }

            // Phase: Big sound
            if (tick == PHASE_SOUND) {
                ((ServerLevel) player.level()).playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.5F, 0.8F);
            }

            // Phase: Teleport
            if (tick == PHASE_TELEPORT) {
                teleportToEnd(player);
            }

            // Phase: Fade out
            if (tick == PHASE_FADE_OUT_START) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.7f);
            } else if (tick == PHASE_FADE_OUT_MID) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.3f);
            } else if (tick == PHASE_FADE_OUT_END) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.0f);
            }

            // Phase: Oliver dialogue
            if (tick == PHASE_DIALOGUE) {
                triggerFinalDialogue(player);
            }

            // Phase: Complete
            if (tick >= PHASE_COMPLETE) {
                TutorialDreamOverlayNetworking.sendClearOverlay(player);
                AromaAffect.LOGGER.info("Dream sequence completed for player {}", player.getName().getString());
                return true; // Remove from active sequences
            }

            seq.tick++;
            return false;
        });
    }

    private static void teleportToEnd(ServerPlayer player) {
        ServerLevel level = ((ServerLevel) player.level());

        if (!TutorialDreamEndManager.isConfigured(level)) {
            AromaAffect.LOGGER.warn("Dream end position not configured! Use /tutorial dreamend setpos");
            return;
        }

        BlockPos endPos = TutorialDreamEndManager.getEndPos(level).orElse(null);
        if (endPos == null) return;

        float yaw = TutorialDreamEndManager.getEndYaw(level);

        player.teleportTo(level,
                endPos.getX() + 0.5, endPos.getY(), endPos.getZ() + 0.5,
                java.util.Set.of(), yaw, 0.0f, false);

        AromaAffect.LOGGER.info("Teleported player {} to dream end position {}", player.getName().getString(), endPos);
    }

    private static void triggerFinalDialogue(ServerPlayer player) {
        ServerLevel level = ((ServerLevel) player.level());

        // Find nearest Oliver
        AABB searchArea = new AABB(
                player.getX() - 100, player.getY() - 50, player.getZ() - 100,
                player.getX() + 100, player.getY() + 50, player.getZ() + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class, searchArea);

        if (!olivers.isEmpty()) {
            TutorialOliverEntity oliver = olivers.getFirst();
            String dialogueId = "dream_end_wakeup";
            oliver.setDialogueId(dialogueId);
            oliver.setTradeId("");

            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), dialogueId, false, "");

            AromaAffect.LOGGER.info("Triggered final dream dialogue for player {}", player.getName().getString());
        } else {
            AromaAffect.LOGGER.warn("No Oliver found near dream end position for final dialogue");
        }
    }

    /**
     * Checks if a player is currently in a dream sequence.
     */
    public static boolean isInDreamSequence(UUID playerId) {
        return activeSequences.containsKey(playerId);
    }

    private static class DreamSequence {
        int tick;

        DreamSequence(int startTick) {
            this.tick = startTick;
        }
    }
}
