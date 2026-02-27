package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.animation.TutorialAnimation;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.animation.TutorialAnimationManager;
import com.ovrtechnology.tutorial.animation.TutorialAnimationType;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Subcommand for managing tutorial animations.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial animation create <id> <type>} - Create animation</li>
 *   <li>{@code /tutorial animation delete <id>} - Delete animation</li>
 *   <li>{@code /tutorial animation corner1 <id>} - Set corner1 to player pos</li>
 *   <li>{@code /tutorial animation corner2 <id>} - Set corner2 to player pos</li>
 *   <li>{@code /tutorial animation play <id>} - Play the animation</li>
 *   <li>{@code /tutorial animation reset <id>} - Mark as unplayed</li>
 *   <li>{@code /tutorial animation list} - List all animations</li>
 *   <li>{@code /tutorial animation info <id>} - Show details</li>
 * </ul>
 */
public class AnimationSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> ANIMATION_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialAnimationManager.getAllAnimationIds(level), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> TYPE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(TutorialAnimationType.values())
                            .map(t -> t.name().toLowerCase())
                            .collect(Collectors.toList()),
                    builder
            );

    @Override
    public String getName() {
        return "animation";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial animations (dramatic block removal with effects)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial animation create <id> <type>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPE_SUGGESTIONS)
                                        .executes(this::executeCreate)
                                )
                        )
                )

                // /tutorial animation delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ANIMATION_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial animation corner1 <id>
                .then(Commands.literal("corner1")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ANIMATION_SUGGESTIONS)
                                .executes(this::executeCorner1)
                        )
                )

                // /tutorial animation corner2 <id>
                .then(Commands.literal("corner2")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ANIMATION_SUGGESTIONS)
                                .executes(this::executeCorner2)
                        )
                )

                // /tutorial animation play <id>
                .then(Commands.literal("play")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ANIMATION_SUGGESTIONS)
                                .executes(this::executePlay)
                        )
                )

                // /tutorial animation reset <id>
                .then(Commands.literal("reset")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ANIMATION_SUGGESTIONS)
                                .executes(this::executeReset)
                        )
                )

                // /tutorial animation oncomplete <id> ...
                .then(Commands.literal("oncomplete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ANIMATION_SUGGESTIONS)
                                // waypoint <waypointId>
                                .then(Commands.literal("waypoint")
                                        .then(Commands.argument("waypointId", StringArgumentType.word())
                                                .executes(this::executeSetOnCompleteWaypoint)
                                        )
                                )
                                // cinematic <cinematicId>
                                .then(Commands.literal("cinematic")
                                        .then(Commands.argument("cinematicId", StringArgumentType.word())
                                                .executes(this::executeSetOnCompleteCinematic)
                                        )
                                )
                                // oliver <action>
                                .then(Commands.literal("oliver")
                                        .then(Commands.argument("action", StringArgumentType.greedyString())
                                                .executes(this::executeSetOnCompleteOliver)
                                        )
                                )
                                // clear
                                .then(Commands.literal("clear")
                                        .executes(this::executeClearOnComplete)
                                )
                        )
                )

                // /tutorial animation list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial animation info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ANIMATION_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fAnimation commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation create <id> <type>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation corner1 <id> \u00a78- Set to your position"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation corner2 <id> \u00a78- Set to your position"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation play <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation reset <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation oncomplete <id> waypoint <waypointId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation oncomplete <id> cinematic <cinematicId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation oncomplete <id> oliver <action>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation oncomplete <id> clear"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial animation info <id>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(
                "\u00a77Types: \u00a7ewall_break\u00a77, \u00a7edoor_open\u00a77, \u00a7edebris_clear"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String typeName = StringArgumentType.getString(context, "type");
        ServerLevel level = source.getLevel();

        TutorialAnimationType type = TutorialAnimationType.byName(typeName);
        if (type == null) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Invalid type '" + typeName +
                    "'. Valid: wall_break, door_open, debris_clear"
            ));
            return 0;
        }

        if (TutorialAnimationManager.createAnimation(level, id, type)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated animation \u00a7d" + id +
                            " \u00a7f(type: \u00a7e" + type.name().toLowerCase() + "\u00a7f)"),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Set corners with \u00a7e/tutorial animation corner1 " + id +
                            " \u00a77and \u00a7ecorner2"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' already exists"
            ));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialAnimationManager.deleteAnimation(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted animation \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeCorner1(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] This command must be executed by a player"
            ));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        if (TutorialAnimationManager.setCorner1(level, id, pos)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet corner1 of \u00a7d" + id +
                            " \u00a7fto \u00a7e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeCorner2(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] This command must be executed by a player"
            ));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        if (TutorialAnimationManager.setCorner2(level, id, pos)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet corner2 of \u00a7d" + id +
                            " \u00a7fto \u00a7e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executePlay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialAnimation> animOpt = TutorialAnimationManager.getAnimation(level, id);
        if (animOpt.isEmpty()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' not found"
            ));
            return 0;
        }

        TutorialAnimation animation = animOpt.get();
        if (!animation.isComplete()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' needs both corners set"
            ));
            return 0;
        }

        if (TutorialAnimationHandler.play(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPlaying animation \u00a7d" + id +
                            " \u00a7f(\u00a7e" + animation.getType().name().toLowerCase() + "\u00a7f)"),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Failed to play animation '" + id + "' (already playing?)"
            ));
            return 0;
        }
    }

    private int executeReset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialAnimationManager.resetAnimation(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fReset animation \u00a7d" + id +
                            " \u00a7fto unplayed state"),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeSetOnCompleteWaypoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String waypointId = StringArgumentType.getString(context, "waypointId");
        ServerLevel level = source.getLevel();

        if (TutorialAnimationManager.setOnCompleteWaypoint(level, id, waypointId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet animation \u00a7d" + id + " \u00a7fon-complete waypoint: \u00a7e" + waypointId),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Animation '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetOnCompleteCinematic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String cinematicId = StringArgumentType.getString(context, "cinematicId");
        ServerLevel level = source.getLevel();

        if (TutorialAnimationManager.setOnCompleteCinematic(level, id, cinematicId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet animation \u00a7d" + id + " \u00a7fon-complete cinematic: \u00a7e" + cinematicId),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Animation '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetOnCompleteOliver(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String action = StringArgumentType.getString(context, "action");
        ServerLevel level = source.getLevel();

        if (TutorialAnimationManager.setOnCompleteOliverAction(level, id, action)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet animation \u00a7d" + id + " \u00a7fon-complete oliver: \u00a7e" + action),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Animation '" + id + "' not found"));
            return 0;
        }
    }

    private int executeClearOnComplete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        boolean cleared = TutorialAnimationManager.setOnCompleteWaypoint(level, id, null);
        cleared |= TutorialAnimationManager.setOnCompleteCinematic(level, id, null);
        cleared |= TutorialAnimationManager.setOnCompleteOliverAction(level, id, null);

        if (cleared) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared on-complete actions for animation \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Animation '" + id + "' not found"));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialAnimationManager.getAllAnimationIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No animations defined"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fAnimations (" + ids.size() + "):"),
                false
        );

        for (String animId : ids) {
            Optional<TutorialAnimation> animOpt = TutorialAnimationManager.getAnimation(level, animId);
            if (animOpt.isPresent()) {
                TutorialAnimation a = animOpt.get();
                String status = a.isPlayed() ? "\u00a7c\u2717" : "\u00a7a\u2713";
                String complete = a.isComplete() ? "\u00a7aready" : "\u00a7cincomplete";
                String typeName = a.getType().name().toLowerCase();

                source.sendSuccess(
                        () -> Component.literal("\u00a77  " + status + " \u00a7e" + animId +
                                " \u00a77(" + typeName + ", " + complete + "\u00a77)"),
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

        Optional<TutorialAnimation> animOpt = TutorialAnimationManager.getAnimation(level, id);

        if (animOpt.isEmpty()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Animation '" + id + "' not found"
            ));
            return 0;
        }

        TutorialAnimation a = animOpt.get();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fAnimation: \u00a7d" + id),
                false
        );

        // Type
        source.sendSuccess(
                () -> Component.literal("\u00a77  Type: \u00a7e" + a.getType().name().toLowerCase()),
                false
        );

        // Status
        String status = a.isPlayed() ? "\u00a7cPlayed" : "\u00a7aUnplayed";
        source.sendSuccess(
                () -> Component.literal("\u00a77  Status: " + status),
                false
        );

        // Corner1
        if (a.getCorner1() != null) {
            BlockPos c1 = a.getCorner1();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner1: \u00a7e" +
                            c1.getX() + ", " + c1.getY() + ", " + c1.getZ()),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner1: \u00a7cNot set"),
                    false
            );
        }

        // Corner2
        if (a.getCorner2() != null) {
            BlockPos c2 = a.getCorner2();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner2: \u00a7e" +
                            c2.getX() + ", " + c2.getY() + ", " + c2.getZ()),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner2: \u00a7cNot set"),
                    false
            );
        }

        // Dimensions
        if (a.isComplete()) {
            BlockPos c1 = a.getCorner1();
            BlockPos c2 = a.getCorner2();
            int sizeX = Math.abs(c2.getX() - c1.getX()) + 1;
            int sizeY = Math.abs(c2.getY() - c1.getY()) + 1;
            int sizeZ = Math.abs(c2.getZ() - c1.getZ()) + 1;
            int totalBlocks = sizeX * sizeY * sizeZ;
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Size: \u00a7e" + sizeX + "x" + sizeY + "x" + sizeZ +
                            " \u00a77(" + totalBlocks + " blocks)"),
                    false
            );
        }

        // On-complete info
        if (a.hasOnCompleteWaypoint()) {
            String wpId = a.getOnCompleteWaypointId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete waypoint: \u00a7a" + wpId),
                    false
            );
        }
        if (a.hasOnCompleteCinematic()) {
            String cinId = a.getOnCompleteCinematicId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete cinematic: \u00a7e" + cinId),
                    false
            );
        }
        if (a.hasOnCompleteOliverAction()) {
            String action = a.getOnCompleteOliverAction();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete oliver: \u00a7e" + action),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}
