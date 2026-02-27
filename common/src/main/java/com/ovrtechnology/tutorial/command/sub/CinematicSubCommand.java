package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.cinematic.CinematicFrame;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematic;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicManager;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Optional;

/**
 * Subcommand for managing tutorial cinematics.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial cinematic create <id>} - Create a new cinematic</li>
 *   <li>{@code /tutorial cinematic delete <id>} - Delete a cinematic</li>
 *   <li>{@code /tutorial cinematic addframe <id> <duration>} - Add a frame</li>
 *   <li>{@code /tutorial cinematic removeframe <id> <frameIndex>} - Remove a frame</li>
 *   <li>{@code /tutorial cinematic setframe <id> <frameIndex> title <text>} - Set frame title</li>
 *   <li>{@code /tutorial cinematic setframe <id> <frameIndex> subtitle <text>} - Set frame subtitle</li>
 *   <li>{@code /tutorial cinematic setframe <id> <frameIndex> color <hex>} - Set title color</li>
 *   <li>{@code /tutorial cinematic setframe <id> <frameIndex> sound <soundId>} - Set sound</li>
 *   <li>{@code /tutorial cinematic setframe <id> <frameIndex> oliver <action>} - Set Oliver action</li>
 *   <li>{@code /tutorial cinematic setframe <id> <frameIndex> camera} - Set camera to current position</li>
 *   <li>{@code /tutorial cinematic setframe <id> <frameIndex> clearcamera} - Clear camera position</li>
 *   <li>{@code /tutorial cinematic oncomplete <id> waypoint <waypointId>} - Set on-complete waypoint</li>
 *   <li>{@code /tutorial cinematic oncomplete <id> oliver <action>} - Set on-complete Oliver action</li>
 *   <li>{@code /tutorial cinematic play <id> [players]} - Play cinematic</li>
 *   <li>{@code /tutorial cinematic stop [players]} - Stop cinematic</li>
 *   <li>{@code /tutorial cinematic list} - List all cinematics</li>
 *   <li>{@code /tutorial cinematic info <id>} - Show cinematic details</li>
 * </ul>
 */
public class CinematicSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> CINEMATIC_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialCinematicManager.getAllCinematicIds(level), builder);
    };

    @Override
    public String getName() {
        return "cinematic";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial cinematics (cutscenes with titles)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial cinematic create <id>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeCreate)
                        )
                )

                // /tutorial cinematic delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial cinematic addframe <id> <duration>
                .then(Commands.literal("addframe")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 6000))
                                        .executes(this::executeAddFrame)
                                )
                        )
                )

                // /tutorial cinematic removeframe <id> <frameIndex>
                .then(Commands.literal("removeframe")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                .then(Commands.argument("frameIndex", IntegerArgumentType.integer(1, 100))
                                        .executes(this::executeRemoveFrame)
                                )
                        )
                )

                // /tutorial cinematic setframe <id> <frameIndex> ...
                .then(Commands.literal("setframe")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                .then(Commands.argument("frameIndex", IntegerArgumentType.integer(1, 100))
                                        // title <text>
                                        .then(Commands.literal("title")
                                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                                        .executes(this::executeSetFrameTitle)
                                                )
                                        )
                                        // subtitle <text>
                                        .then(Commands.literal("subtitle")
                                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                                        .executes(this::executeSetFrameSubtitle)
                                                )
                                        )
                                        // color <hex>
                                        .then(Commands.literal("color")
                                                .then(Commands.argument("hex", StringArgumentType.word())
                                                        .executes(this::executeSetFrameColor)
                                                )
                                        )
                                        // sound <soundId>
                                        .then(Commands.literal("sound")
                                                .then(Commands.argument("soundId", StringArgumentType.greedyString())
                                                        .executes(this::executeSetFrameSound)
                                                )
                                        )
                                        // oliver <action>
                                        .then(Commands.literal("oliver")
                                                .then(Commands.argument("action", StringArgumentType.greedyString())
                                                        .executes(this::executeSetFrameOliver)
                                                )
                                        )
                                        // fadein <ticks> - set camera transition time
                                        .then(Commands.literal("fadein")
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 6000))
                                                        .executes(this::executeSetFrameFadeIn)
                                                )
                                        )
                                        // fadeout <ticks>
                                        .then(Commands.literal("fadeout")
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 6000))
                                                        .executes(this::executeSetFrameFadeOut)
                                                )
                                        )
                                        // duration <ticks> - set hold time at this position
                                        .then(Commands.literal("duration")
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 6000))
                                                        .executes(this::executeSetFrameDuration)
                                                )
                                        )
                                        // camera - set camera position to current player position
                                        .then(Commands.literal("camera")
                                                .executes(this::executeSetFrameCamera)
                                        )
                                        // clearcamera - clear camera position
                                        .then(Commands.literal("clearcamera")
                                                .executes(this::executeClearFrameCamera)
                                        )
                                )
                        )
                )

                // /tutorial cinematic oncomplete <id> ...
                .then(Commands.literal("oncomplete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                // waypoint <waypointId>
                                .then(Commands.literal("waypoint")
                                        .then(Commands.argument("waypointId", StringArgumentType.word())
                                                .executes(this::executeSetOnCompleteWaypoint)
                                        )
                                )
                                // oliver <action>
                                .then(Commands.literal("oliver")
                                        .then(Commands.argument("action", StringArgumentType.greedyString())
                                                .executes(this::executeSetOnCompleteOliver)
                                        )
                                )
                                // animation <animationId>
                                .then(Commands.literal("animation")
                                        .then(Commands.argument("animationId", StringArgumentType.word())
                                                .executes(this::executeSetOnCompleteAnimation)
                                        )
                                )
                                // clear
                                .then(Commands.literal("clear")
                                        .executes(this::executeClearOnComplete)
                                )
                        )
                )

                // /tutorial cinematic play <id> [players]
                .then(Commands.literal("play")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                .executes(this::executePlaySelf)
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(this::executePlayPlayers)
                                )
                        )
                )

                // /tutorial cinematic stop [players]
                .then(Commands.literal("stop")
                        .executes(this::executeStopSelf)
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(this::executeStopPlayers)
                        )
                )

                // /tutorial cinematic list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial cinematic info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCinematic commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic create <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic addframe <id> <duration>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic removeframe <id> <index>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> title <text>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> subtitle <text>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> color <hex>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> sound <soundId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> oliver <action>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> fadein <ticks>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> fadeout <ticks>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> duration <ticks>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> camera"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic setframe <id> <index> clearcamera"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic oncomplete <id> waypoint <waypointId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic oncomplete <id> oliver <action>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic oncomplete <id> animation <animationId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic oncomplete <id> clear"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic play <id> [players]"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic stop [players]"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial cinematic info <id>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Frame index starts at 1. Duration in ticks (20 = 1 sec)."), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialCinematicManager.createCinematic(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated cinematic \u00a7d" + id),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Use \u00a7e/tutorial cinematic addframe " + id + " <duration>\u00a77 to add frames"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialCinematicManager.deleteCinematic(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted cinematic \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
    }

    private int executeAddFrame(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int duration = IntegerArgumentType.getInteger(context, "duration");
        ServerLevel level = source.getLevel();

        if (TutorialCinematicManager.addFrame(level, id, duration)) {
            Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
            int frameCount = cinematicOpt.map(TutorialCinematic::getFrameCount).orElse(0);

            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fAdded frame " + frameCount + " to cinematic \u00a7d" + id + " \u00a77(duration: " + duration + " ticks)"),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
    }

    private int executeRemoveFrame(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1; // Convert to 0-based
        ServerLevel level = source.getLevel();

        if (TutorialCinematicManager.removeFrame(level, id, frameIndex)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRemoved frame " + (frameIndex + 1) + " from cinematic \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic or frame not found"));
            return 0;
        }
    }

    private int executeSetFrameTitle(CommandContext<CommandSourceStack> context) {
        return setFrameProperty(context, "title", (frame, value) -> frame.withTitle(value));
    }

    private int executeSetFrameSubtitle(CommandContext<CommandSourceStack> context) {
        return setFrameProperty(context, "subtitle", (frame, value) -> frame.withSubtitle(value));
    }

    private int executeSetFrameColor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1;
        String hexStr = StringArgumentType.getString(context, "hex");
        ServerLevel level = source.getLevel();

        // Parse hex color
        int color;
        try {
            String hex = hexStr.startsWith("0x") ? hexStr.substring(2) :
                    hexStr.startsWith("#") ? hexStr.substring(1) : hexStr;
            color = Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid hex color: " + hexStr));
            return 0;
        }

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }

        CinematicFrame frame = cinematicOpt.get().getFrame(frameIndex);
        if (frame == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Frame " + (frameIndex + 1) + " not found"));
            return 0;
        }

        CinematicFrame newFrame = frame.withTitleColor(color);
        if (TutorialCinematicManager.updateFrame(level, id, frameIndex, newFrame)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet frame " + (frameIndex + 1) + " color to \u00a7e#" + Integer.toHexString(color).toUpperCase()),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to update frame"));
            return 0;
        }
    }

    private int executeSetFrameSound(CommandContext<CommandSourceStack> context) {
        return setFrameProperty(context, "sound", (frame, value) -> frame.withSound(value));
    }

    private int executeSetFrameOliver(CommandContext<CommandSourceStack> context) {
        return setFrameProperty(context, "oliver", (frame, value) -> frame.withOliverAction(value));
    }

    private int executeSetFrameFadeIn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1;
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        ServerLevel level = source.getLevel();

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
        CinematicFrame frame = cinematicOpt.get().getFrame(frameIndex);
        if (frame == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Frame " + (frameIndex + 1) + " not found"));
            return 0;
        }
        CinematicFrame newFrame = frame.withFades(ticks, frame.fadeOut());
        if (TutorialCinematicManager.updateFrame(level, id, frameIndex, newFrame)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet frame " + (frameIndex + 1) + " fadeIn to \u00a7e" + ticks + " ticks"),
                    true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to update frame"));
        return 0;
    }

    private int executeSetFrameFadeOut(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1;
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        ServerLevel level = source.getLevel();

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
        CinematicFrame frame = cinematicOpt.get().getFrame(frameIndex);
        if (frame == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Frame " + (frameIndex + 1) + " not found"));
            return 0;
        }
        CinematicFrame newFrame = frame.withFades(frame.fadeIn(), ticks);
        if (TutorialCinematicManager.updateFrame(level, id, frameIndex, newFrame)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet frame " + (frameIndex + 1) + " fadeOut to \u00a7e" + ticks + " ticks"),
                    true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to update frame"));
        return 0;
    }

    private int executeSetFrameDuration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1;
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        ServerLevel level = source.getLevel();

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
        CinematicFrame frame = cinematicOpt.get().getFrame(frameIndex);
        if (frame == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Frame " + (frameIndex + 1) + " not found"));
            return 0;
        }
        CinematicFrame newFrame = frame.withDuration(ticks);
        if (TutorialCinematicManager.updateFrame(level, id, frameIndex, newFrame)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet frame " + (frameIndex + 1) + " duration to \u00a7e" + ticks + " ticks"),
                    true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to update frame"));
        return 0;
    }

    private int executeSetFrameCamera(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1;
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }

        CinematicFrame frame = cinematicOpt.get().getFrame(frameIndex);
        if (frame == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Frame " + (frameIndex + 1) + " not found"));
            return 0;
        }

        // Use player's current position and rotation as camera
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        CinematicFrame newFrame = frame.withCameraPosition(x, y, z, yaw, pitch);
        if (TutorialCinematicManager.updateFrame(level, id, frameIndex, newFrame)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet frame " + (frameIndex + 1) + " camera to: \u00a7e" +
                            String.format("%.2f, %.2f, %.2f \u00a77(yaw: %.1f, pitch: %.1f)", x, y, z, yaw, pitch)),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to update frame"));
            return 0;
        }
    }

    private int executeClearFrameCamera(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1;
        ServerLevel level = source.getLevel();

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }

        CinematicFrame frame = cinematicOpt.get().getFrame(frameIndex);
        if (frame == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Frame " + (frameIndex + 1) + " not found"));
            return 0;
        }

        CinematicFrame newFrame = frame.withoutCameraPosition();
        if (TutorialCinematicManager.updateFrame(level, id, frameIndex, newFrame)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared camera position from frame " + (frameIndex + 1)),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to update frame"));
            return 0;
        }
    }

    @FunctionalInterface
    private interface FrameModifier {
        CinematicFrame apply(CinematicFrame frame, String value);
    }

    private int setFrameProperty(CommandContext<CommandSourceStack> context, String propertyName, FrameModifier modifier) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int frameIndex = IntegerArgumentType.getInteger(context, "frameIndex") - 1;
        String value = StringArgumentType.getString(context, propertyName.equals("title") ? "text" :
                propertyName.equals("subtitle") ? "text" :
                        propertyName.equals("sound") ? "soundId" : "action");
        ServerLevel level = source.getLevel();

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }

        CinematicFrame frame = cinematicOpt.get().getFrame(frameIndex);
        if (frame == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Frame " + (frameIndex + 1) + " not found"));
            return 0;
        }

        CinematicFrame newFrame = modifier.apply(frame, value);
        if (TutorialCinematicManager.updateFrame(level, id, frameIndex, newFrame)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet frame " + (frameIndex + 1) + " " + propertyName + " to \u00a7e" + value),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to update frame"));
            return 0;
        }
    }

    private int executeSetOnCompleteWaypoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String waypointId = StringArgumentType.getString(context, "waypointId");
        ServerLevel level = source.getLevel();

        if (TutorialCinematicManager.setOnCompleteWaypoint(level, id, waypointId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet cinematic \u00a7d" + id + " \u00a7fon-complete waypoint: \u00a7e" + waypointId),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetOnCompleteOliver(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String action = StringArgumentType.getString(context, "action");
        ServerLevel level = source.getLevel();

        if (TutorialCinematicManager.setOnCompleteOliverAction(level, id, action)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet cinematic \u00a7d" + id + " \u00a7fon-complete oliver: \u00a7e" + action),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetOnCompleteAnimation(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String animationId = StringArgumentType.getString(context, "animationId");
        ServerLevel level = source.getLevel();

        if (TutorialCinematicManager.setOnCompleteAnimation(level, id, animationId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet cinematic \u00a7d" + id + " \u00a7fon-complete animation: \u00a7e" + animationId),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
    }

    private int executeClearOnComplete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        boolean cleared = TutorialCinematicManager.setOnCompleteWaypoint(level, id, null);
        cleared |= TutorialCinematicManager.setOnCompleteOliverAction(level, id, null);
        cleared |= TutorialCinematicManager.setOnCompleteAnimation(level, id, null);

        if (cleared) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared on-complete actions for cinematic \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }
    }

    private int executePlaySelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        String id = StringArgumentType.getString(context, "id");
        return playForPlayers(source, id, java.util.List.of(player));
    }

    private int executePlayPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");

        Collection<ServerPlayer> players;
        try {
            players = EntityArgument.getPlayers(context, "players");
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid player selection"));
            return 0;
        }

        return playForPlayers(source, id, players);
    }

    private int playForPlayers(CommandSourceStack source, String id, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (TutorialCinematicHandler.startCinematic(player, id)) {
                count++;
            }
        }

        if (count > 0) {
            int finalCount = count;
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fStarted cinematic \u00a7d" + id + " \u00a7ffor \u00a7d" + finalCount + " \u00a7fplayer" + (finalCount == 1 ? "" : "s")),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to start cinematic (not found or players already in cinematic)"));
            return 0;
        }
    }

    private int executeStopSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        return stopForPlayers(source, java.util.List.of(player));
    }

    private int executeStopPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        Collection<ServerPlayer> players;
        try {
            players = EntityArgument.getPlayers(context, "players");
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid player selection"));
            return 0;
        }

        return stopForPlayers(source, players);
    }

    private int stopForPlayers(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (TutorialCinematicHandler.stopCinematic(player)) {
                count++;
            }
        }

        if (count > 0) {
            int finalCount = count;
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fStopped cinematic for \u00a7d" + finalCount + " \u00a7fplayer" + (finalCount == 1 ? "" : "s")),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No players were in a cinematic"));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialCinematicManager.getAllCinematicIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No cinematics defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCinematics (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);
            if (cinematicOpt.isPresent()) {
                TutorialCinematic c = cinematicOpt.get();
                int frameCount = c.getFrameCount();
                int totalDuration = c.getTotalDuration();
                float seconds = totalDuration / 20.0f;
                source.sendSuccess(
                        () -> Component.literal("\u00a77  \u00a7e" + id + " \u00a77(" + frameCount + " frames, " + String.format("%.1f", seconds) + "s)"),
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

        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, id);

        if (cinematicOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Cinematic '" + id + "' not found"));
            return 0;
        }

        TutorialCinematic c = cinematicOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCinematic: \u00a7d" + id), false);

        int totalDuration = c.getTotalDuration();
        float totalSeconds = totalDuration / 20.0f;
        source.sendSuccess(
                () -> Component.literal("\u00a77  Total duration: \u00a7e" + totalDuration + " ticks \u00a77(" + String.format("%.1f", totalSeconds) + "s)"),
                false
        );

        if (c.hasFrames()) {
            source.sendSuccess(() -> Component.literal("\u00a77  Frames (" + c.getFrameCount() + "):"), false);
            int frameNum = 1;
            for (CinematicFrame frame : c.getFrames()) {
                int num = frameNum;
                StringBuilder info = new StringBuilder();
                info.append("\u00a77    ").append(num).append(": ");

                if (frame.title() != null && !frame.title().isEmpty()) {
                    info.append("\u00a7a\"").append(frame.title()).append("\" ");
                }
                if (frame.subtitle() != null && !frame.subtitle().isEmpty()) {
                    info.append("\u00a7b(").append(frame.subtitle()).append(") ");
                }

                info.append("\u00a77[").append(frame.getTotalTime()).append("t]");

                if (frame.hasCameraPosition()) {
                    info.append(" \u00a7c[cam]");
                }
                if (frame.sound() != null) {
                    info.append(" \u00a7d[sound]");
                }
                if (frame.oliverAction() != null) {
                    info.append(" \u00a7e[oliver]");
                }

                String finalInfo = info.toString();
                source.sendSuccess(() -> Component.literal(finalInfo), false);
                frameNum++;
            }
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  No frames"), false);
        }

        // On-complete info
        if (c.hasOnCompleteWaypoint()) {
            String wpId = c.getOnCompleteWaypointId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete waypoint: \u00a7a" + wpId),
                    false
            );
        }
        if (c.hasOnCompleteOliverAction()) {
            String action = c.getOnCompleteOliverAction();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete oliver: \u00a7e" + action),
                    false
            );
        }
        if (c.hasOnCompleteAnimation()) {
            String animId = c.getOnCompleteAnimationId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete animation: \u00a7e" + animId),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}
