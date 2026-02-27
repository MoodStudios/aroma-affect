package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialIntroNetworking;
import com.ovrtechnology.tutorial.cinematic.CinematicFrame;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicManager;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.spawn.TutorialJoinHandler;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class IntroSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> CINEMATIC_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialCinematicManager.getAllCinematicIds(level), builder);
    };

    @Override
    public String getName() {
        return "intro";
    }

    @Override
    public String getDescription() {
        return "Manage the tutorial intro screen and cinematic";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial intro setcinematic <cinematicId>
                .then(Commands.literal("setcinematic")
                        .then(Commands.argument("cinematicId", StringArgumentType.word())
                                .suggests(CINEMATIC_SUGGESTIONS)
                                .executes(this::executeSetCinematic)
                        )
                )

                // /tutorial intro clearcinematic
                .then(Commands.literal("clearcinematic")
                        .executes(this::executeClearCinematic)
                )

                // /tutorial intro play [players]
                .then(Commands.literal("play")
                        .executes(this::executePlaySelf)
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(this::executePlayPlayers)
                        )
                )

                // /tutorial intro stop [players]
                .then(Commands.literal("stop")
                        .executes(this::executeStopSelf)
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(this::executeStopPlayers)
                        )
                )

                // /tutorial intro generate [radius] [height] [points] [speed]
                .then(Commands.literal("generate")
                        .executes(ctx -> executeGenerate(ctx, 30, 15, 12, 80))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(5, 200))
                                .executes(ctx -> executeGenerate(ctx,
                                        IntegerArgumentType.getInteger(ctx, "radius"), 15, 12, 80))
                                .then(Commands.argument("height", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> executeGenerate(ctx,
                                                IntegerArgumentType.getInteger(ctx, "radius"),
                                                IntegerArgumentType.getInteger(ctx, "height"), 12, 80))
                                        .then(Commands.argument("points", IntegerArgumentType.integer(4, 36))
                                                .executes(ctx -> executeGenerate(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                                        IntegerArgumentType.getInteger(ctx, "height"),
                                                        IntegerArgumentType.getInteger(ctx, "points"), 80))
                                                .then(Commands.argument("speed", IntegerArgumentType.integer(20, 400))
                                                        .executes(ctx -> executeGenerate(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "radius"),
                                                                IntegerArgumentType.getInteger(ctx, "height"),
                                                                IntegerArgumentType.getInteger(ctx, "points"),
                                                                IntegerArgumentType.getInteger(ctx, "speed")))
                                                )
                                        )
                                )
                        )
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fIntro commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial intro setcinematic <cinematicId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial intro clearcinematic"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial intro play [players]"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial intro stop [players]"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial intro generate [radius] [height] [points] [speed]"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeSetCinematic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String cinematicId = StringArgumentType.getString(context, "cinematicId");
        ServerLevel level = source.getLevel();

        TutorialSpawnManager.setIntroCinematic(level, cinematicId);
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fIntro cinematic set to \u00a7d" + cinematicId),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executeClearCinematic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        TutorialSpawnManager.setIntroCinematic(level, "");
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fIntro cinematic cleared (default join behavior restored)"),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executePlaySelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }
        return playForPlayers(source, java.util.List.of(player));
    }

    private int executePlayPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> players;
        try {
            players = EntityArgument.getPlayers(context, "players");
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid player selection"));
            return 0;
        }
        return playForPlayers(source, players);
    }

    private int playForPlayers(CommandSourceStack source, Collection<ServerPlayer> players) {
        ServerLevel level = source.getLevel();
        var cinematicIdOpt = TutorialSpawnManager.getIntroCinematicId(level);

        if (cinematicIdOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No intro cinematic configured. Use /tutorial intro setcinematic <id>"));
            return 0;
        }

        String cinematicId = cinematicIdOpt.get();
        int count = 0;
        for (ServerPlayer player : players) {
            if (TutorialCinematicHandler.startCinematic(player, cinematicId, true)) {
                TutorialDialogueContentNetworking.syncToPlayer(player, level);
                TutorialIntroNetworking.sendOpenIntro(player);
                count++;
            }
        }

        if (count > 0) {
            int finalCount = count;
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fStarted intro for \u00a7d" + finalCount
                            + " \u00a7fplayer" + (finalCount == 1 ? "" : "s")),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to start intro (cinematic not found or players already in cinematic)"));
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
                TutorialJoinHandler.removeFromIntro(player.getUUID());
                count++;
            }
        }

        if (count > 0) {
            int finalCount = count;
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fStopped intro for \u00a7d" + finalCount
                            + " \u00a7fplayer" + (finalCount == 1 ? "" : "s")),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No players were in the intro"));
            return 0;
        }
    }

    /**
     * Generates a circular orbit cinematic around the tutorial spawn point.
     *
     * @param radius radius of the orbit in blocks
     * @param height height above the spawn point in blocks
     * @param points number of points (frames) around the circle
     * @param speed  fadeIn ticks per frame (camera transition time between points)
     */
    private int executeGenerate(CommandContext<CommandSourceStack> context, int radius, int height, int points, int speed) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // Need a spawn point as the center of the orbit
        var spawnOpt = TutorialSpawnManager.getSpawn(level);
        if (spawnOpt.isEmpty()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] No spawn point set. Use /tutorial setspawn first"));
            return 0;
        }

        var spawn = spawnOpt.get();
        double centerX = spawn.pos().getX() + 0.5;
        double centerY = spawn.pos().getY() + height;
        double centerZ = spawn.pos().getZ() + 0.5;

        String cinematicId = "intro_fly";

        // Delete existing cinematic if it exists, then create fresh
        TutorialCinematicManager.deleteCinematic(level, cinematicId);
        TutorialCinematicManager.createCinematic(level, cinematicId);

        // Generate circular orbit frames
        // Camera looks toward the center (spawn) from each point on the circle
        double pitchAngle = -20.0f; // Look slightly downward

        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI * i) / points;
            double camX = centerX + radius * Math.cos(angle);
            double camZ = centerZ + radius * Math.sin(angle);

            // Calculate yaw: camera faces toward center
            // Minecraft yaw: 0 = south (+Z), 90 = west (-X), 180 = north (-Z), 270 = east (+X)
            double dx = centerX - camX;
            double dz = centerZ - camZ;
            float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));

            // First frame: short fadeIn (snap to position quickly), rest: smooth transition
            int frameFadeIn = (i == 0) ? 10 : speed;
            CinematicFrame frame = new CinematicFrame(0) // duration=0, no pause at each point
                    .withFades(frameFadeIn, 0)            // fadeIn=transition time, fadeOut=0
                    .withCameraPosition(camX, centerY, camZ, yaw, (float) pitchAngle);

            TutorialCinematicManager.addFrame(level, cinematicId, 0);
            // Update the frame we just added (last index)
            var cinematic = TutorialCinematicManager.getCinematic(level, cinematicId);
            if (cinematic.isPresent()) {
                int frameIndex = cinematic.get().getFrameCount() - 1;
                TutorialCinematicManager.updateFrame(level, cinematicId, frameIndex, frame);
            }
        }

        // Set this as the intro cinematic
        TutorialSpawnManager.setIntroCinematic(level, cinematicId);

        int totalTicks = speed * points;
        float totalSeconds = totalTicks / 20.0f;

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fGenerated intro cinematic '\u00a7dintro_fly\u00a7f' with \u00a7d"
                        + points + "\u00a7f points, radius \u00a7d" + radius
                        + "\u00a7f, height \u00a7d+" + height
                        + "\u00a7f, ~\u00a7d" + String.format("%.1f", totalSeconds) + "s\u00a7f per loop"
        ), true);
        source.sendSuccess(() -> Component.literal(
                "\u00a77  Use /tutorial intro play to test it"
        ), false);

        return Command.SINGLE_SUCCESS;
    }
}
