package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Subcommand for managing tutorial waypoints with multi-point paths.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial waypoint create <id>} - Create a new waypoint</li>
 *   <li>{@code /tutorial waypoint setpos <id> <index>} - Set position at index (1, 2, 3...)</li>
 *   <li>{@code /tutorial waypoint removepos <id> <index>} - Remove position at index</li>
 *   <li>{@code /tutorial waypoint delete <id>} - Delete a waypoint</li>
 *   <li>{@code /tutorial waypoint list} - List all waypoints</li>
 *   <li>{@code /tutorial waypoint info <id>} - Show waypoint details</li>
 *   <li>{@code /tutorial waypoint activate <id> [players]} - Show waypoint path</li>
 *   <li>{@code /tutorial waypoint deactivate [players]} - Hide waypoint path</li>
 *   <li>{@code /tutorial waypoint area <id> <1|2>} - Set area detection corner</li>
 *   <li>{@code /tutorial waypoint cleararea <id>} - Remove area definition</li>
 *   <li>{@code /tutorial waypoint chain <id> <nextId>} - Chain waypoints together</li>
 *   <li>{@code /tutorial waypoint unchain <id>} - Remove chain from waypoint</li>
 *   <li>{@code /tutorial waypoint oliver <id> <action>} - Set Oliver action on completion</li>
 *   <li>{@code /tutorial waypoint clearoliver <id>} - Clear Oliver action</li>
 * </ul>
 * <p>
 * Position indices work as follows:
 * <ul>
 *   <li>Position 1 = Start point</li>
 *   <li>Position 2, 3, 4... = Intermediate points (creates curves)</li>
 *   <li>Highest number = Destination (end point)</li>
 * </ul>
 * <p>
 * Area detection:
 * <ul>
 *   <li>Define a cuboid with two corners using area command</li>
 *   <li>When a player enters the area, the waypoint auto-deactivates</li>
 *   <li>Use cleararea to remove the detection zone</li>
 * </ul>
 * <p>
 * Waypoint chaining:
 * <ul>
 *   <li>Chain waypoints together so completing one auto-activates the next</li>
 *   <li>Plays level up sound when reaching a waypoint</li>
 *   <li>Creates seamless multi-waypoint paths</li>
 * </ul>
 */
public class WaypointSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> WAYPOINT_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialWaypointManager.getAllWaypointIds(level), builder);
    };

    @Override
    public String getName() {
        return "waypoint";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial waypoints (multi-point paths)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial waypoint create <id>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeCreate)
                        )
                )

                // /tutorial waypoint setpos <id> <index>
                .then(Commands.literal("setpos")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("index", IntegerArgumentType.integer(1, 99))
                                        .executes(this::executeSetPos)
                                )
                        )
                )

                // /tutorial waypoint removepos <id> <index>
                .then(Commands.literal("removepos")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("index", IntegerArgumentType.integer(1, 99))
                                        .executes(this::executeRemovePos)
                                )
                        )
                )

                // /tutorial waypoint delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial waypoint list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial waypoint info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // /tutorial waypoint activate <id> [players]
                .then(Commands.literal("activate")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeActivateSelf)
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(this::executeActivatePlayers)
                                )
                        )
                )

                // /tutorial waypoint deactivate [players]
                .then(Commands.literal("deactivate")
                        .executes(this::executeDeactivateSelf)
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(this::executeDeactivatePlayers)
                        )
                )

                // /tutorial waypoint area <id> <1|2>
                .then(Commands.literal("area")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("corner", IntegerArgumentType.integer(1, 2))
                                        .executes(this::executeSetArea)
                                )
                        )
                )

                // /tutorial waypoint cleararea <id>
                .then(Commands.literal("cleararea")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeClearArea)
                        )
                )

                // /tutorial waypoint chain <id> <nextId>
                .then(Commands.literal("chain")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("nextId", StringArgumentType.word())
                                        .suggests(WAYPOINT_SUGGESTIONS)
                                        .executes(this::executeChain)
                                )
                        )
                )

                // /tutorial waypoint unchain <id>
                .then(Commands.literal("unchain")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeUnchain)
                        )
                )

                // /tutorial waypoint oliver <id> <action>
                .then(Commands.literal("oliver")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("action", StringArgumentType.greedyString())
                                        .executes(this::executeSetOliver)
                                )
                        )
                )

                // /tutorial waypoint clearoliver <id>
                .then(Commands.literal("clearoliver")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeClearOliver)
                        )
                )

                // /tutorial waypoint nosechain <id> <noseId> <nextWaypointId>
                .then(Commands.literal("nosechain")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("noseId", StringArgumentType.word())
                                        .then(Commands.argument("nextWaypointId", StringArgumentType.word())
                                                .suggests(WAYPOINT_SUGGESTIONS)
                                                .executes(this::executeAddNoseChain)
                                        )
                                )
                        )
                )

                // /tutorial waypoint removenosechain <id> <noseId>
                .then(Commands.literal("removenosechain")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("noseId", StringArgumentType.word())
                                        .executes(this::executeRemoveNoseChain)
                                )
                        )
                )

                // /tutorial waypoint defaultchain <id> <nextWaypointId>
                .then(Commands.literal("defaultchain")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("nextWaypointId", StringArgumentType.word())
                                        .suggests(WAYPOINT_SUGGESTIONS)
                                        .executes(this::executeSetDefaultChain)
                                )
                        )
                )

                // /tutorial waypoint cleardefaultchain <id>
                .then(Commands.literal("cleardefaultchain")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeClearDefaultChain)
                        )
                )

                // /tutorial waypoint cinematic <id> <cinematicId>
                .then(Commands.literal("cinematic")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .then(Commands.argument("cinematicId", StringArgumentType.word())
                                        .executes(this::executeSetCinematic)
                                )
                        )
                )

                // /tutorial waypoint clearcinematic <id>
                .then(Commands.literal("clearcinematic")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(WAYPOINT_SUGGESTIONS)
                                .executes(this::executeClearCinematic)
                        )
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fWaypoint commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint create <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint setpos <id> <1|2|3...>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint removepos <id> <index>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint info <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint activate <id> [players]"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint deactivate [players]"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint area <id> <1|2>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint cleararea <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint chain <id> <nextId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint unchain <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint oliver <id> <action>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint clearoliver <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint nosechain <id> <noseId> <nextWaypointId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint removenosechain <id> <noseId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint defaultchain <id> <nextWaypointId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint cinematic <id> <cinematicId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial waypoint clearcinematic <id>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Position 1 = Start, highest = End"), false);
        source.sendSuccess(() -> Component.literal("\u00a77Area = auto-deactivation zone, Chain = auto-activate next"), false);
        source.sendSuccess(() -> Component.literal("\u00a77Oliver actions: follow, stop, walkto:x,y,z, dialogue:id, trade:id, lookup:blockId"), false);
        source.sendSuccess(() -> Component.literal("\u00a77Nosechain = conditional chain based on equipped nose"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialWaypointManager.createWaypoint(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated waypoint \u00a7d" + id),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Use \u00a7e/tutorial waypoint setpos " + id + " 1\u00a77, \u00a7e2\u00a77, \u00a7e3\u00a77... to set positions"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeSetPos(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int index = IntegerArgumentType.getInteger(context, "index");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();

        if (TutorialWaypointManager.setPosition(level, id, index, pos)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet waypoint \u00a7d" + id + " \u00a7fposition \u00a7d" + index
                            + " \u00a7fto \u00a7d" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );

            // Show waypoint status
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
            if (wpOpt.isPresent()) {
                TutorialWaypoint wp = wpOpt.get();
                int count = wp.getPositionCount();
                if (wp.isComplete()) {
                    double dist = wp.getTotalDistance();
                    source.sendSuccess(
                            () -> Component.literal("\u00a7a  \u2713 Path complete! " + count + " points, " + String.format("%.1f", dist) + " blocks total"),
                            false
                    );
                } else {
                    source.sendSuccess(
                            () -> Component.literal("\u00a7e  \u26a0 Need at least 2 positions for a valid path"),
                            false
                    );
                }
            }

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found. Create it first."));
            return 0;
        }
    }

    private int executeRemovePos(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int index = IntegerArgumentType.getInteger(context, "index");
        ServerLevel level = source.getLevel();

        if (TutorialWaypointManager.removePosition(level, id, index)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRemoved position \u00a7d" + index + " \u00a7ffrom waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Position not found or waypoint doesn't exist"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialWaypointManager.deleteWaypoint(level, id)) {
            // Clear waypoint visuals for all online players
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                TutorialWaypointNetworking.sendClearToPlayer(player);
            }

            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialWaypointManager.getAllWaypointIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No waypoints defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fWaypoints (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
            if (wpOpt.isPresent()) {
                TutorialWaypoint wp = wpOpt.get();
                String status = wp.isComplete() ? "\u00a7a\u2713" : "\u00a7c\u2717";
                int count = wp.getPositionCount();
                source.sendSuccess(
                        () -> Component.literal("\u00a77  " + status + " \u00a7e" + id + " \u00a77(" + count + " points)"),
                        false
                );
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);

        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        TutorialWaypoint wp = wpOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fWaypoint: \u00a7d" + id), false);

        // Show all defined positions
        var indices = wp.getDefinedIndices();
        if (indices.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a77  No positions defined"), false);
        } else {
            for (int idx : indices) {
                BlockPos pos = wp.getPosition(idx);
                if (pos != null) {
                    String label = idx == 1 ? " (Start)" : (idx == wp.getMaxIndex() ? " (End)" : "");
                    source.sendSuccess(
                            () -> Component.literal("\u00a77  Position " + idx + ": \u00a7a" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "\u00a7e" + label),
                            false
                    );
                }
            }
        }

        if (wp.isComplete()) {
            double dist = wp.getTotalDistance();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Total distance: \u00a7e" + String.format("%.1f", dist) + " blocks"),
                    false
            );
            source.sendSuccess(() -> Component.literal("\u00a77  Status: \u00a7aComplete \u2713"), false);
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Status: \u00a7cIncomplete (need 2+ positions)"), false);
        }

        // Show area info
        if (wp.hasArea()) {
            BlockPos c1 = wp.getAreaCorner1();
            BlockPos c2 = wp.getAreaCorner2();
            source.sendSuccess(() -> Component.literal("\u00a77  Area: \u00a7aEnabled \u2713"), false);
            source.sendSuccess(
                    () -> Component.literal("\u00a77    Corner 1: \u00a7e" + c1.getX() + ", " + c1.getY() + ", " + c1.getZ()),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77    Corner 2: \u00a7e" + c2.getX() + ", " + c2.getY() + ", " + c2.getZ()),
                    false
            );
        } else {
            BlockPos c1 = wp.getAreaCorner1();
            BlockPos c2 = wp.getAreaCorner2();
            if (c1 != null || c2 != null) {
                source.sendSuccess(() -> Component.literal("\u00a77  Area: \u00a7ePartially defined"), false);
                if (c1 != null) {
                    source.sendSuccess(
                            () -> Component.literal("\u00a77    Corner 1: \u00a7e" + c1.getX() + ", " + c1.getY() + ", " + c1.getZ()),
                            false
                    );
                }
                if (c2 != null) {
                    source.sendSuccess(
                            () -> Component.literal("\u00a77    Corner 2: \u00a7e" + c2.getX() + ", " + c2.getY() + ", " + c2.getZ()),
                            false
                    );
                }
            } else {
                source.sendSuccess(() -> Component.literal("\u00a77  Area: \u00a77Not defined"), false);
            }
        }

        // Show chain info
        if (wp.hasNextWaypoint()) {
            String nextId = wp.getNextWaypointId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Chain: \u00a7a" + id + " \u2192 " + nextId),
                    false
            );
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Chain: \u00a77Not chained"), false);
        }

        // Show Oliver action info
        if (wp.hasOliverAction()) {
            String oliverAction = wp.getOliverAction();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Oliver action: \u00a7e" + oliverAction),
                    false
            );
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Oliver action: \u00a77None"), false);
        }

        // Show nose chain info
        if (wp.hasNoseChains()) {
            source.sendSuccess(() -> Component.literal("\u00a77  Nose chains:"), false);
            for (var entry : wp.getNoseChains().entrySet()) {
                String noseId = entry.getKey();
                String nextId = entry.getValue();
                source.sendSuccess(
                        () -> Component.literal("\u00a77    \u00a7e" + noseId + " \u00a77\u2192 \u00a7a" + nextId),
                        false
                );
            }
        }

        // Show default chain info
        if (wp.hasDefaultNextWaypoint()) {
            String defaultNext = wp.getDefaultNextWaypointId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Default chain: \u00a7a" + defaultNext),
                    false
            );
        }

        // Show cinematic info
        if (wp.hasActivateCinematic()) {
            String cinematicId = wp.getActivateCinematicId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Cinematic: \u00a7d" + cinematicId),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeActivateSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        String id = StringArgumentType.getString(context, "id");
        return activateForPlayers(source, id, java.util.List.of(player));
    }

    private int executeActivatePlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");

        Collection<ServerPlayer> players;
        try {
            players = EntityArgument.getPlayers(context, "players");
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid player selection"));
            return 0;
        }

        return activateForPlayers(source, id, players);
    }

    private int activateForPlayers(CommandSourceStack source, String id, Collection<ServerPlayer> players) {
        ServerLevel level = source.getLevel();
        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);

        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        TutorialWaypoint wp = wpOpt.get();
        if (!wp.isComplete()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' is incomplete (needs at least 2 positions)"));
            return 0;
        }

        List<BlockPos> positions = wp.getValidPositions();

        for (ServerPlayer player : players) {
            TutorialWaypointNetworking.sendWaypointToPlayer(player, id, positions);
            // Register with area handler for auto-deactivation
            TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), id);
        }

        int count = players.size();
        int pointCount = positions.size();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fActivated waypoint \u00a7d" + id + " \u00a7f(" + pointCount + " points) for \u00a7d"
                        + count + " \u00a7fplayer" + (count == 1 ? "" : "s")),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeDeactivateSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        return deactivateForPlayers(source, java.util.List.of(player));
    }

    private int executeDeactivatePlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        Collection<ServerPlayer> players;
        try {
            players = EntityArgument.getPlayers(context, "players");
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid player selection"));
            return 0;
        }

        return deactivateForPlayers(source, players);
    }

    private int deactivateForPlayers(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            TutorialWaypointNetworking.sendClearToPlayer(player);
            TutorialWaypointAreaHandler.clearActiveWaypoint(player.getUUID());
        }

        int count = players.size();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeactivated waypoint for \u00a7d"
                        + count + " \u00a7fplayer" + (count == 1 ? "" : "s")),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeSetArea(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int corner = IntegerArgumentType.getInteger(context, "corner");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();

        if (TutorialWaypointManager.setAreaCorner(level, id, corner, pos)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet waypoint \u00a7d" + id + " \u00a7farea corner \u00a7d" + corner
                            + " \u00a7fto \u00a7d" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );

            // Check if area is complete
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
            if (wpOpt.isPresent() && wpOpt.get().hasArea()) {
                source.sendSuccess(
                        () -> Component.literal("\u00a7a  \u2713 Area defined! Players will auto-complete when entering this zone"),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal("\u00a7e  Set corner " + (corner == 1 ? "2" : "1") + " to complete the area"),
                        false
                );
            }

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }
    }

    private int executeClearArea(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialWaypointManager.clearArea(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared area for waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }
    }

    private int executeChain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String nextId = StringArgumentType.getString(context, "nextId");
        ServerLevel level = source.getLevel();

        // Validate both waypoints exist
        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        Optional<TutorialWaypoint> nextWpOpt = TutorialWaypointManager.getWaypoint(level, nextId);
        if (nextWpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Target waypoint '" + nextId + "' not found"));
            return 0;
        }

        // Prevent self-chaining
        if (id.equals(nextId)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cannot chain a waypoint to itself"));
            return 0;
        }

        if (TutorialWaypointManager.setNextWaypoint(level, id, nextId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fChained: \u00a7d" + id + " \u00a7f\u2192 \u00a7d" + nextId),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a7a  \u2713 When player completes " + id + ", " + nextId + " will auto-activate"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to chain waypoints"));
            return 0;
        }
    }

    private int executeUnchain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        if (!wpOpt.get().hasNextWaypoint()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' is not chained"));
            return 0;
        }

        if (TutorialWaypointManager.clearChain(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fUnchained waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to unchain waypoint"));
            return 0;
        }
    }

    private int executeSetOliver(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String action = StringArgumentType.getString(context, "action");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        // Validate action format
        String actionLower = action.toLowerCase();
        boolean validAction = actionLower.equals("follow") ||
                actionLower.equals("stop") ||
                actionLower.startsWith("walkto:") ||
                actionLower.startsWith("dialogue:") ||
                actionLower.startsWith("trade:") ||
                actionLower.startsWith("lookup:");

        if (!validAction) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid action. Use: follow, stop, walkto:x,y,z, dialogue:id, trade:id, or lookup:blockId"));
            return 0;
        }

        if (TutorialWaypointManager.setOliverAction(level, id, action)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet Oliver action for waypoint \u00a7d" + id + "\u00a7f: \u00a7e" + action),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a7a  \u2713 When player completes this waypoint, Oliver will: " + action),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to set Oliver action"));
            return 0;
        }
    }

    private int executeClearOliver(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        if (!wpOpt.get().hasOliverAction()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' has no Oliver action"));
            return 0;
        }

        if (TutorialWaypointManager.clearOliverAction(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared Oliver action for waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to clear Oliver action"));
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Nose Chain Commands
    // ─────────────────────────────────────────────────────────────────────────────

    private int executeAddNoseChain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String noseId = StringArgumentType.getString(context, "noseId");
        String nextWaypointId = StringArgumentType.getString(context, "nextWaypointId");
        ServerLevel level = source.getLevel();

        // Validate waypoint exists
        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        // Validate next waypoint exists
        Optional<TutorialWaypoint> nextWpOpt = TutorialWaypointManager.getWaypoint(level, nextWaypointId);
        if (nextWpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Target waypoint '" + nextWaypointId + "' not found"));
            return 0;
        }

        if (TutorialWaypointManager.addNoseChain(level, id, noseId, nextWaypointId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fAdded nose chain to \u00a7d" + id + "\u00a7f:"),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a7a  \u2713 When player has \u00a7e" + noseId + "\u00a7a equipped \u2192 \u00a7d" + nextWaypointId),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to add nose chain"));
            return 0;
        }
    }

    private int executeRemoveNoseChain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String noseId = StringArgumentType.getString(context, "noseId");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        if (!wpOpt.get().getNoseChains().containsKey(noseId)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No nose chain for '" + noseId + "' on waypoint '" + id + "'"));
            return 0;
        }

        if (TutorialWaypointManager.removeNoseChain(level, id, noseId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRemoved nose chain \u00a7e" + noseId + " \u00a7ffrom waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to remove nose chain"));
            return 0;
        }
    }

    private int executeSetDefaultChain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String nextWaypointId = StringArgumentType.getString(context, "nextWaypointId");
        ServerLevel level = source.getLevel();

        // Validate waypoint exists
        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        // Validate next waypoint exists
        Optional<TutorialWaypoint> nextWpOpt = TutorialWaypointManager.getWaypoint(level, nextWaypointId);
        if (nextWpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Target waypoint '" + nextWaypointId + "' not found"));
            return 0;
        }

        if (TutorialWaypointManager.setDefaultChain(level, id, nextWaypointId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet default chain for \u00a7d" + id + " \u00a7f\u2192 \u00a7d" + nextWaypointId),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a7a  \u2713 Used when no nose chain matches"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to set default chain"));
            return 0;
        }
    }

    private int executeClearDefaultChain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        if (!wpOpt.get().hasDefaultNextWaypoint()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' has no default chain"));
            return 0;
        }

        if (TutorialWaypointManager.clearDefaultChain(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared default chain for waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to clear default chain"));
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Cinematic Commands
    // ─────────────────────────────────────────────────────────────────────────────

    private int executeSetCinematic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String cinematicId = StringArgumentType.getString(context, "cinematicId");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        if (TutorialWaypointManager.setActivateCinematic(level, id, cinematicId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet cinematic for waypoint \u00a7d" + id + "\u00a7f: \u00a7d" + cinematicId),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a7a  \u2713 Cinematic will play when player completes this waypoint"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to set cinematic"));
            return 0;
        }
    }

    private int executeClearCinematic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
        if (wpOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' not found"));
            return 0;
        }

        if (!wpOpt.get().hasActivateCinematic()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Waypoint '" + id + "' has no cinematic"));
            return 0;
        }

        if (TutorialWaypointManager.clearActivateCinematic(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared cinematic for waypoint \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to clear cinematic"));
            return 0;
        }
    }
}
