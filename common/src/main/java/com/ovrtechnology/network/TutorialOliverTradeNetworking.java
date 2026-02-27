package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.trade.TutorialTrade;
import com.ovrtechnology.tutorial.trade.TutorialTradeHandler;
import com.ovrtechnology.tutorial.trade.TutorialTradeManager;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Networking handler for Tutorial Oliver trade packets.
 * <p>
 * Handles:
 * <ul>
 *   <li>C2S: Player requests a trade with Oliver</li>
 *   <li>S2C: Trade result (success/failure)</li>
 * </ul>
 */
public final class TutorialOliverTradeNetworking {

    private static final ResourceLocation TRADE_REQUEST_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_oliver_trade_req");
    private static final ResourceLocation TRADE_RESULT_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_oliver_trade_res");

    private static boolean initialized = false;

    private TutorialOliverTradeNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // C2S: Player requests a trade
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                TRADE_REQUEST_PACKET,
                (buf, context) -> {
                    int entityId = buf.readVarInt();
                    String tradeId = buf.readUtf(256);

                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (!(player instanceof ServerPlayer serverPlayer)) {
                            return;
                        }
                        if (!(serverPlayer.level() instanceof ServerLevel level)) {
                            return;
                        }

                        // Validate Oliver entity exists and is in range
                        Entity entity = level.getEntity(entityId);
                        if (!(entity instanceof TutorialOliverEntity oliver)) {
                            sendTradeResult(serverPlayer, false, "Oliver not found");
                            return;
                        }

                        double distSqr = serverPlayer.distanceToSqr(oliver);
                        if (distSqr > 8.0 * 8.0) {
                            sendTradeResult(serverPlayer, false, "Too far from Oliver");
                            return;
                        }

                        // Execute the trade
                        boolean success = TutorialTradeHandler.executeTrade(serverPlayer, level, tradeId);
                        if (success) {
                            sendTradeResult(serverPlayer, true, "Trade successful!");
                            // Execute on-complete hooks
                            executeTradeOnComplete(serverPlayer, level, tradeId);
                        } else {
                            sendTradeResult(serverPlayer, false, "You don't have the required items");
                        }
                    });
                }
        );

        // S2C: Trade result
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                TRADE_RESULT_PACKET,
                (buf, context) -> {
                    boolean success = buf.readBoolean();
                    String message = buf.readUtf(256);

                    context.queue(() -> {
                        // Show message in action bar
                        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                        if (minecraft.player != null) {
                            minecraft.player.displayClientMessage(
                                    Component.literal(success ? "\u00a7a" + message : "\u00a7c" + message),
                                    true
                            );
                        }
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial Oliver trade networking initialized");
    }

    /**
     * Client sends a trade request to the server.
     */
    public static void sendTradeRequest(RegistryAccess registryAccess, int oliverEntityId, String tradeId) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeVarInt(oliverEntityId);
        buf.writeUtf(tradeId, 256);
        NetworkManager.sendToServer(TRADE_REQUEST_PACKET, buf);
    }

    /**
     * Server sends a trade result to a player.
     */
    public static void sendTradeResult(ServerPlayer player, boolean success, String message) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        buf.writeBoolean(success);
        buf.writeUtf(message, 256);
        NetworkManager.sendToPlayer(player, TRADE_RESULT_PACKET, buf);
    }

    private static void executeTradeOnComplete(ServerPlayer player, ServerLevel level, String tradeId) {
        Optional<TutorialTrade> tradeOpt = TutorialTradeManager.getTrade(level, tradeId);
        if (tradeOpt.isEmpty()) return;

        TutorialTrade trade = tradeOpt.get();

        // Oliver action
        if (trade.hasOnCompleteOliverAction()) {
            executeOliverAction(player, level, trade.getOnCompleteOliverAction());
        }

        // Cinematic
        if (trade.hasOnCompleteCinematic()) {
            TutorialCinematicHandler.startCinematic(player, trade.getOnCompleteCinematicId());
        }

        // Waypoint
        if (trade.hasOnCompleteWaypoint()) {
            String waypointId = trade.getOnCompleteWaypointId();
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
            }
        }

        // Animation
        if (trade.hasOnCompleteAnimation()) {
            TutorialAnimationHandler.play(level, trade.getOnCompleteAnimationId());
        }
    }

    private static void executeOliverAction(ServerPlayer player, ServerLevel level, String action) {
        TutorialOliverEntity oliver = findNearestOliver(player, level);
        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found", action);
            return;
        }

        String actionLower = action.toLowerCase();
        if (actionLower.equals("follow")) {
            oliver.setFollowing(player);
        } else if (actionLower.equals("stop")) {
            oliver.setStationary();
        } else if (actionLower.startsWith("walkto:")) {
            String coordsStr = action.substring(7);
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
            String dialogueId = action.substring(9).trim();
            oliver.setDialogueId(dialogueId);
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), dialogueId,
                    oliver.hasTrade(), oliver.getTradeId()
            );
        } else if (actionLower.startsWith("trade:")) {
            oliver.setTradeId(action.substring(6).trim());
        }
    }

    private static TutorialOliverEntity findNearestOliver(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(TutorialOliverEntity.class,
                player.getBoundingBox().inflate(100.0), e -> true
        ).stream().findFirst().orElse(null);
    }
}
