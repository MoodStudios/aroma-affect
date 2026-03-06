package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.dialogue.TutorialDialogue;
import com.ovrtechnology.tutorial.dialogue.TutorialDialogueManager;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Networking handler for custom tutorial dialogue content.
 * <p>
 * Handles:
 * <ul>
 *   <li>S2C: Sync dialogue text map to clients on join/change</li>
 *   <li>S2C: Tell client to open a dialogue with specific params</li>
 *   <li>C2S: Client reports dialogue closed (triggers on-complete hooks)</li>
 * </ul>
 */
public final class TutorialDialogueContentNetworking {

    // ── Payload records ──────────────────────────────────────────────────────

    public record DialogueClosedC2S(String dialogueId) implements CustomPacketPayload {
        public static final Type<DialogueClosedC2S> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_dialogue_closed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogueClosedC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.dialogueId, 256),
                buf -> new DialogueClosedC2S(buf.readUtf(256))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DialogueSyncS2C(Map<String, String> texts) implements CustomPacketPayload {
        public static final Type<DialogueSyncS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_dialogue_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogueSyncS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.texts.size());
                    for (Map.Entry<String, String> entry : payload.texts.entrySet()) {
                        buf.writeUtf(entry.getKey(), 256);
                        buf.writeUtf(entry.getValue(), 8192);
                    }
                },
                buf -> {
                    int count = buf.readVarInt();
                    Map<String, String> texts = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        String id = buf.readUtf(256);
                        String text = buf.readUtf(8192);
                        texts.put(id, text);
                    }
                    return new DialogueSyncS2C(texts);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DialogueOpenS2C(int entityId, String dialogueId, boolean hasTrade, String tradeId) implements CustomPacketPayload {
        public static final Type<DialogueOpenS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_dialogue_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogueOpenS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.entityId);
                    buf.writeUtf(payload.dialogueId, 256);
                    buf.writeBoolean(payload.hasTrade);
                    buf.writeUtf(payload.tradeId, 256);
                },
                buf -> new DialogueOpenS2C(
                        buf.readVarInt(),
                        buf.readUtf(256),
                        buf.readBoolean(),
                        buf.readUtf(256)
                )
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DialogueSelfS2C(String dialogueId) implements CustomPacketPayload {
        public static final Type<DialogueSelfS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_dialogue_self"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogueSelfS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.dialogueId, 256),
                buf -> new DialogueSelfS2C(buf.readUtf(256))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    /** Client-side cache of custom dialogue texts (id -> text). */
    private static final Map<String, String> clientDialogueCache = new ConcurrentHashMap<>();

    private static boolean initialized = false;

    private TutorialDialogueContentNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // C2S: Client tells server that player closed a dialogue
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DialogueClosedC2S.TYPE, DialogueClosedC2S.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (player == null || !(player instanceof ServerPlayer serverPlayer)) {
                            return;
                        }
                        if (!(serverPlayer.level() instanceof ServerLevel level)) {
                            return;
                        }

                        onDialogueClosed(serverPlayer, level, payload.dialogueId());
                    });
                }
        );

        // S2C: Sync dialogue map to client
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DialogueSyncS2C.TYPE, DialogueSyncS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        clientDialogueCache.clear();
                        clientDialogueCache.putAll(payload.texts());
                    });
                }
        );

        // S2C: Open dialogue with params
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DialogueOpenS2C.TYPE, DialogueOpenS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        openDialogueOnClient(payload.entityId(), payload.dialogueId(), payload.hasTrade(), payload.tradeId());
                    });
                }
        );

        // S2C: Open self-dialogue (player talks to themselves - dream end)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DialogueSelfS2C.TYPE, DialogueSelfS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        openSelfDialogueOnClient(payload.dialogueId());
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial dialogue content networking initialized");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // S2C: Sync dialogue map
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sends the full dialogue text map to a player.
     */
    public static void syncToPlayer(ServerPlayer player, ServerLevel level) {
        Map<String, String> texts = TutorialDialogueManager.getAllDialogueTexts(level);
        NetworkManager.sendToPlayer(player, new DialogueSyncS2C(texts));
    }

    /**
     * Syncs dialogue map to all online players.
     */
    public static void syncToAllPlayers(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            syncToPlayer(player, level);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // S2C: Open dialogue
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Tells a client to open a dialogue screen with the given parameters.
     */
    public static void sendOpenDialogue(ServerPlayer player, int entityId,
                                         String dialogueId, boolean hasTrade, String tradeId) {
        NetworkManager.sendToPlayer(player, new DialogueOpenS2C(
                entityId, dialogueId, hasTrade, tradeId != null ? tradeId : ""));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // S2C: Open self-dialogue (player talks to themselves)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Tells a client to open a self-dialogue screen (player's own skin as portrait).
     * Used for the dream ending sequence.
     */
    public static void sendOpenSelfDialogue(ServerPlayer player, String dialogueId) {
        NetworkManager.sendToPlayer(player, new DialogueSelfS2C(dialogueId));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // C2S: Dialogue closed
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Client sends this when the player closes a dialogue screen.
     */
    public static void sendDialogueClosed(RegistryAccess registryAccess, String dialogueId) {
        NetworkManager.sendToServer(new DialogueClosedC2S(dialogueId));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Client cache access
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets the client-side cached dialogue text for a given ID.
     *
     * @param dialogueId the dialogue ID
     * @return the custom text, or null if not in cache
     */
    public static String getClientCachedText(String dialogueId) {
        return clientDialogueCache.get(dialogueId);
    }

    /**
     * Gets the keys in the client cache (for debugging).
     */
    public static java.util.Set<String> getClientCacheKeys() {
        return new java.util.HashSet<>(clientDialogueCache.keySet());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Server-side: dialogue closed handler
    // ─────────────────────────────────────────────────────────────────────────────

    private static void onDialogueClosed(ServerPlayer player, ServerLevel level, String dialogueId) {
        AromaAffect.LOGGER.info("Dialogue '{}' closed by player {}", dialogueId, player.getName().getString());

        // Special: dream end dialogue makes Oliver vanish
        if ("dream_end_wakeup".equals(dialogueId)) {
            vanishNearestOliver(player, level);
        }

        // Skip on-complete hooks for boss dialogues that have trade buttons —
        // the trade flow handles its own on-complete via TutorialTradeHandler
        if (dialogueId != null && dialogueId.startsWith("boss_") && dialogueId.endsWith("_killed")) {
            AromaAffect.LOGGER.info("Skipping on-complete hooks for boss trade dialogue '{}'", dialogueId);
            return;
        }

        Optional<TutorialDialogue> dialogueOpt = TutorialDialogueManager.getDialogue(level, dialogueId);
        if (dialogueOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("Dialogue '{}' not found in manager - on-complete hooks will NOT fire", dialogueId);
            return;
        }

        TutorialDialogue dialogue = dialogueOpt.get();

        // Execute on-complete Oliver action
        if (dialogue.hasOnCompleteOliverAction()) {
            AromaAffect.LOGGER.info("Executing on-complete oliver action '{}' for dialogue '{}'",
                    dialogue.getOnCompleteOliverAction(), dialogueId);
            executeOliverAction(player, level, dialogue.getOnCompleteOliverAction());
        } else {
            AromaAffect.LOGGER.info("Dialogue '{}' has no on-complete oliver action", dialogueId);
        }

        // Start on-complete cinematic
        if (dialogue.hasOnCompleteCinematic()) {
            TutorialCinematicHandler.startCinematic(player, dialogue.getOnCompleteCinematicId());
        }

        // Activate on-complete waypoint
        if (dialogue.hasOnCompleteWaypoint()) {
            String waypointId = dialogue.getOnCompleteWaypointId();
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
                AromaAffect.LOGGER.debug("Activated waypoint {} after dialogue {}", waypointId, dialogueId);
            }
        }

        // Play on-complete animation
        if (dialogue.hasOnCompleteAnimation()) {
            TutorialAnimationHandler.play(level, dialogue.getOnCompleteAnimationId());
        }
    }

    private static void executeOliverAction(ServerPlayer player, ServerLevel level, String action) {
        TutorialOliverEntity oliver = findNearestOliver(player, level);
        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found within 100 blocks of player {}", action, player.getName().getString());
            return;
        }
        AromaAffect.LOGGER.info("Found Oliver at {}, executing action '{}'", oliver.blockPosition(), action);

        // Support multiple actions separated by semicolon
        String[] actions = action.split(";");
        String pendingDialogueId = null;

        for (String singleAction : actions) {
            singleAction = singleAction.trim();
            if (singleAction.isEmpty()) continue;

            String actionLower = singleAction.toLowerCase();

            if (actionLower.equals("follow")) {
                oliver.setFollowing(player);
                AromaAffect.LOGGER.info("Oliver is now following player {}", player.getName().getString());
            } else if (actionLower.equals("stop")) {
                oliver.setStationary();
            } else if (actionLower.startsWith("walkto:")) {
                String coordsStr = singleAction.substring(7);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        oliver.setWalkingTo(new BlockPos(x, y, z));
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid walkto coordinates: {}", coordsStr);
                    }
                }
            } else if (actionLower.startsWith("dialogue:")) {
                // Defer dialogue opening until after all other actions (especially trade:)
                pendingDialogueId = singleAction.substring(9).trim();
                oliver.setDialogueId(pendingDialogueId);
            } else if (actionLower.startsWith("trade:")) {
                String tradeId = singleAction.substring(6).trim();
                oliver.setTradeId(tradeId);
                AromaAffect.LOGGER.debug("Oliver action: trade set to {}", tradeId);
            } else if (actionLower.equals("cleartrade")) {
                oliver.setTradeId("");
                AromaAffect.LOGGER.debug("Oliver action: trade cleared");
            } else if (actionLower.startsWith("teleportplayer:")) {
                String coordsStr = singleAction.substring(15);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        player.teleportTo(level, x + 0.5, y, z + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
                        AromaAffect.LOGGER.debug("Teleported player to {}, {}, {}", x, y, z);
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid teleportplayer coordinates: {}", coordsStr);
                    }
                }
            } else if (actionLower.startsWith("lookup:")) {
                String blockId = singleAction.substring(7).trim();
                triggerLookup(player, level, blockId);
            } else {
                AromaAffect.LOGGER.warn("Unknown Oliver action: {}", singleAction);
            }
        }

        // Open dialogue AFTER processing all actions (so trade is set first)
        if (pendingDialogueId != null) {
            AromaAffect.LOGGER.info("Sending open dialogue packet to player {} for dialogue '{}' (hasTrade: {}, tradeId: '{}')",
                    player.getName().getString(), pendingDialogueId, oliver.hasTrade(), oliver.getTradeId());
            sendOpenDialogue(
                    player, oliver.getId(), pendingDialogueId,
                    oliver.hasTrade(), oliver.getTradeId()
            );
        }
    }

    private static void triggerLookup(ServerPlayer player, ServerLevel level, String blockId) {
        LookupTarget target = LookupTarget.block(ResourceLocation.parse(blockId));
        BlockPos origin = player.blockPosition();

        LookupManager.getInstance().lookupAsync(level, origin, target, result -> {
            if (result.isSuccess()) {
                BlockPos destination = result.getPosition();
                ActivePathManager.getInstance().createPath(
                        player, level, destination,
                        ActivePathManager.TargetType.BLOCK, blockId
                );
                int distance = (int) Math.sqrt(origin.distSqr(destination));
                PathScentNetworking.sendPathFound(player, distance, destination);
                AromaAffect.LOGGER.debug("Tutorial lookup found {} at {} for player {}",
                        blockId, destination, player.getName().getString());
            } else {
                AromaAffect.LOGGER.warn("Tutorial lookup failed for {}: {}",
                        blockId, result.failureReason());
            }
        });
    }

    private static TutorialOliverEntity findNearestOliver(ServerPlayer player, ServerLevel level) {
        AABB searchArea = new AABB(
                player.getX() - 100, player.getY() - 50, player.getZ() - 100,
                player.getX() + 100, player.getY() + 50, player.getZ() + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class,
                searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    /**
     * Makes the nearest Oliver vanish with particles and sound (dream genie effect).
     */
    private static void vanishNearestOliver(ServerPlayer player, ServerLevel level) {
        TutorialOliverEntity oliver = findNearestOliver(player, level);
        if (oliver == null) {
            AromaAffect.LOGGER.warn("No Oliver found to vanish");
            return;
        }

        double x = oliver.getX();
        double y = oliver.getY();
        double z = oliver.getZ();

        // Smoke/magic particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                x, y + 1, z, 30, 0.3, 0.5, 0.3, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                x, y + 1, z, 50, 0.5, 1.0, 0.5, 0.5);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                x, y + 1.5, z, 20, 0.3, 0.5, 0.3, 0.1);

        // Magical vanish sound
        level.playSound(null, x, y, z,
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.2f);

        // Hide Oliver instead of removing (so /tutorial reset can still find him)
        oliver.setInvisible(true);
        oliver.setCustomNameVisible(false);
        oliver.resetToHome();

        AromaAffect.LOGGER.info("Oliver vanished at {}, {}, {}", x, y, z);
    }

    /**
     * Opens a self-dialogue on the client side via reflection.
     */
    private static void openSelfDialogueOnClient(String dialogueId) {
        try {
            Class<?> dialogueClass = Class.forName(
                    "com.ovrtechnology.tutorial.oliver.client.dialogue.TutorialOliverDialogueClient"
            );
            dialogueClass.getMethod("openSelfDialogue", String.class)
                    .invoke(null, dialogueId);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to open self dialogue via network packet", e);
        }
    }

    /**
     * Opens the dialogue on the client side via reflection.
     */
    private static void openDialogueOnClient(int entityId, String dialogueId,
                                               boolean hasTrade, String tradeId) {
        try {
            Class<?> dialogueClass = Class.forName(
                    "com.ovrtechnology.tutorial.oliver.client.dialogue.TutorialOliverDialogueClient"
            );
            dialogueClass.getMethod("openWithParams", int.class, String.class, boolean.class, String.class)
                    .invoke(null, entityId, dialogueId, hasTrade, tradeId);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to open dialogue via network packet", e);
        }
    }
}
