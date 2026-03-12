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
    // Atmospheric sounds while white screen holds
    private static final int PHASE_ATMOSPHERE_1 = 55;
    private static final int PHASE_ATMOSPHERE_2 = 70;
    private static final int PHASE_TELEPORT = 85;
    // Slow, "opening eyes" fade-out with arrival particles
    private static final int PHASE_FADE_OUT_START = 120;
    private static final int PHASE_FADE_OUT_END = 220;
    private static final int PHASE_DIALOGUE = 235;
    private static final int PHASE_COMPLETE = 260;

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

            ServerLevel sLevel = (ServerLevel) player.level();

            // Phase: Fade in
            if (tick == PHASE_FADE_START) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.3f);
                // Ambient dreamy sound
                sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.AMBIENT, 1.0F, 0.5F);
            } else if (tick == PHASE_FADE_MID) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.6f);
            } else if (tick == PHASE_FADE_FULL) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 1.0f);
            }

            // Phase: Big sound
            if (tick == PHASE_SOUND) {
                sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.5F, 0.8F);
            }

            // Phase: Atmospheric sounds while white screen holds (dream ambience)
            if (tick == PHASE_ATMOSPHERE_1) {
                sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.6F, 0.5F);
            }
            if (tick == PHASE_ATMOSPHERE_2) {
                sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 0.8F, 1.2F);
            }

            // Phase: Slight pre-teleport fade so arrival particles peek through
            if (tick == PHASE_TELEPORT - 5) {
                TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.88f);
            }

            // Phase: Teleport
            if (tick == PHASE_TELEPORT) {
                teleportToEnd(player);
                // Brief darkness for disorientation ("waking up" groggy)
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS,
                        60, 0, true, false, false));
            }

            // Phase: Prolonged arrival particles during fade-out
            if (tick >= PHASE_TELEPORT + 5 && tick <= PHASE_FADE_OUT_START && tick % 8 == 0) {
                sLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        8, 2.0, 1.5, 2.0, 0.02);
                sLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        player.getX(), player.getY() + 2.5, player.getZ(),
                        6, 3.0, 1.0, 3.0, 0.01);
            }

            // Phase: Ambient destination sounds during fade-out
            if (tick == PHASE_TELEPORT + 15) {
                sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.5F, 0.7F);
            }
            if (tick == PHASE_FADE_OUT_START - 10) {
                sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.4F, 1.0F);
            }

            // Phase: Slow "opening eyes" fade-out (10 steps over 100 ticks)
            if (tick >= PHASE_FADE_OUT_START && tick <= PHASE_FADE_OUT_END) {
                int fadeRange = PHASE_FADE_OUT_END - PHASE_FADE_OUT_START;
                int elapsed = tick - PHASE_FADE_OUT_START;
                // Update every 10 ticks for smooth steps
                if (elapsed % 10 == 0) {
                    float progress = (float) elapsed / fadeRange;
                    // Ease-in curve: starts slow (eyes barely opening) then accelerates
                    float eased = progress * progress;
                    float overlay = 0.88f * (1.0f - eased);
                    TutorialDreamOverlayNetworking.sendOverlayProgress(player, Math.max(overlay, 0.0f));
                }
            }

            // Phase: Oliver dialogue
            if (tick == PHASE_DIALOGUE) {
                triggerFinalDialogue(player);
            }

            // Phase: Complete
            if (tick >= PHASE_COMPLETE) {
                TutorialDreamOverlayNetworking.sendClearOverlay(player);
                // Activate scent counter HUD (tracks unique scents from this point)
                com.ovrtechnology.network.TutorialScentCounterNetworking.sendActivate(player, 16);
                AromaAffect.LOGGER.info("Dream sequence completed for player {}, scent counter activated", player.getName().getString());
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

        double fromX = player.getX();
        double fromY = player.getY();
        double fromZ = player.getZ();

        // Stop all sounds (cuts dragon roar/ambient sounds)
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                null, null));

        // Departure effects — ethereal particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                fromX, fromY + 1.0, fromZ, 40, 0.5, 1.5, 0.5, 0.08);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                fromX, fromY + 0.5, fromZ, 60, 0.8, 1.0, 0.8, 0.15);

        // Dreamy departure sound
        level.playSound(null, fromX, fromY, fromZ,
                SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.AMBIENT, 1.2f, 0.4f);

        // Teleport
        player.teleportTo(level,
                endPos.getX() + 0.5, endPos.getY(), endPos.getZ() + 0.5,
                java.util.Set.of(), yaw, 0.0f, false);

        // Arrival effects
        double dstX = endPos.getX() + 0.5;
        double dstY = endPos.getY();
        double dstZ = endPos.getZ() + 0.5;

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                dstX, dstY + 1.5, dstZ, 50, 1.5, 2.0, 1.5, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES,
                dstX, dstY + 2.0, dstZ, 30, 2.0, 1.0, 2.0, 0.02);

        // Gentle arrival sound
        level.playSound(null, dstX, dstY, dstZ,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.0f, 0.8f);

        AromaAffect.LOGGER.info("Teleported player {} to dream end position {}", player.getName().getString(), endPos);
    }

    private static void triggerFinalDialogue(ServerPlayer player) {
        ServerLevel level = ((ServerLevel) player.level());

        // Hide all Oliver entities near the player (the dream is over, Oliver was part of it)
        AABB searchArea = new AABB(
                player.getX() - 100, player.getY() - 50, player.getZ() - 100,
                player.getX() + 100, player.getY() + 50, player.getZ() + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class, searchArea);

        for (TutorialOliverEntity oliver : olivers) {
            // Vanish particles
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    oliver.getX(), oliver.getY() + 1, oliver.getZ(), 30, 0.3, 0.5, 0.3, 0.05);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    oliver.getX(), oliver.getY() + 1.5, oliver.getZ(), 20, 0.3, 0.5, 0.3, 0.1);

            oliver.setInvisible(true);
            oliver.setCustomNameVisible(false);
            oliver.resetToHome();
        }

        // Clear inventory, keeping only the Dimensional Nose (HEAD slot preserved automatically)
        com.ovrtechnology.tutorial.trade.TutorialTradeHandler.clearInventoryKeeping(
                player, java.util.Set.of("dimensional_nose", "blaze_nose"));

        // Open self-dialogue: the player "talks to themselves" realizing it was a dream
        String dialogueId = "dream_end_wakeup";
        TutorialDialogueContentNetworking.sendOpenSelfDialogue(player, dialogueId);

        AromaAffect.LOGGER.info("Triggered self dream dialogue for player {} (Oliver hidden)", player.getName().getString());
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
