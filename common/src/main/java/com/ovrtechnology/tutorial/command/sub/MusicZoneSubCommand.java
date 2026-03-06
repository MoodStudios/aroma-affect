package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.musiczone.TutorialMusicZone;
import com.ovrtechnology.tutorial.musiczone.TutorialMusicZoneManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;

/**
 * Subcommand for managing tutorial music zones.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial musiczone create <id> <soundId>} - Create a music zone</li>
 *   <li>{@code /tutorial musiczone delete <id>} - Delete a music zone</li>
 *   <li>{@code /tutorial musiczone corner <id> <1|2>} - Set area corner at player position</li>
 *   <li>{@code /tutorial musiczone volume <id> <0.0-2.0>} - Set volume</li>
 *   <li>{@code /tutorial musiczone sound <id> <soundId>} - Change sound ID</li>
 *   <li>{@code /tutorial musiczone list} - List all music zones</li>
 *   <li>{@code /tutorial musiczone info <id>} - Show zone details</li>
 * </ul>
 */
public class MusicZoneSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> ZONE_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialMusicZoneManager.getAllZoneIds(level), builder);
    };

    @Override
    public String getName() {
        return "musiczone";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial music zones (area-based sound playback)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("soundId", StringArgumentType.greedyString())
                                        .executes(this::executeCreate))))

                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeDelete)))

                .then(Commands.literal("corner")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .then(Commands.argument("corner", IntegerArgumentType.integer(1, 2))
                                        .executes(this::executeSetCorner))))

                .then(Commands.literal("volume")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 2.0f))
                                        .executes(this::executeSetVolume))))

                .then(Commands.literal("sound")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .then(Commands.argument("soundId", StringArgumentType.greedyString())
                                        .executes(this::executeSetSound))))

                .then(Commands.literal("list")
                        .executes(this::executeList))

                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeInfo)))

                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fMusic Zone commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial musiczone create <id> <soundId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial musiczone delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial musiczone corner <id> <1|2>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial musiczone volume <id> <0.0-2.0>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial musiczone sound <id> <soundId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial musiczone list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial musiczone info <id>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Sound files go in: assets/aromaaffect/sounds/"), false);
        source.sendSuccess(() -> Component.literal("\u00a77Sound ID format: aromaaffect:music.zone_name"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String soundId = StringArgumentType.getString(context, "soundId");
        ServerLevel level = source.getLevel();

        if (TutorialMusicZoneManager.createZone(level, id, soundId)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fCreated music zone \u00a7d" + id + " \u00a7fwith sound \u00a7e" + soundId
            ), true);
            source.sendSuccess(() -> Component.literal(
                    "\u00a77  Set area corners: /tutorial musiczone corner " + id + " 1 (then 2)"
            ), false);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Music zone '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialMusicZoneManager.deleteZone(level, id)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fDeleted music zone \u00a7d" + id
            ), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Music zone '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetCorner(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int corner = IntegerArgumentType.getInteger(context, "corner");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();

        if (TutorialMusicZoneManager.setCorner(level, id, corner, pos)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet music zone \u00a7d" + id + " \u00a7fcorner \u00a7d" + corner
                            + " \u00a7fto \u00a7d" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
            ), true);

            Optional<TutorialMusicZone> zoneOpt = TutorialMusicZoneManager.getZone(level, id);
            if (zoneOpt.isPresent() && zoneOpt.get().isComplete()) {
                source.sendSuccess(() -> Component.literal("\u00a7a  \u2713 Music zone complete and active!"), false);
            }
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Music zone '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetVolume(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        float volume = FloatArgumentType.getFloat(context, "volume");
        ServerLevel level = source.getLevel();

        if (TutorialMusicZoneManager.setVolume(level, id, volume)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet music zone \u00a7d" + id + " \u00a7fvolume to \u00a7d" + String.format("%.1f", volume)
            ), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Music zone '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetSound(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String soundId = StringArgumentType.getString(context, "soundId");
        ServerLevel level = source.getLevel();

        if (TutorialMusicZoneManager.setSoundId(level, id, soundId)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet music zone \u00a7d" + id + " \u00a7fsound to \u00a7e" + soundId
            ), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Music zone '" + id + "' not found"));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        Set<String> ids = TutorialMusicZoneManager.getAllZoneIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No music zones defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fMusic Zones (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialMusicZone> zoneOpt = TutorialMusicZoneManager.getZone(level, id);
            if (zoneOpt.isPresent()) {
                TutorialMusicZone zone = zoneOpt.get();
                String status = zone.isComplete() ? "\u00a7a\u2713" : "\u00a7c\u2717";
                source.sendSuccess(() -> Component.literal(
                        "\u00a77  " + status + " \u00a7e" + id + " \u00a77(" + zone.getSoundId() + ")"
                ), false);
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialMusicZone> zoneOpt = TutorialMusicZoneManager.getZone(level, id);

        if (zoneOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Music zone '" + id + "' not found"));
            return 0;
        }

        TutorialMusicZone zone = zoneOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fMusic Zone: \u00a7d" + id), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Sound: \u00a7e" + zone.getSoundId()), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Volume: \u00a7f" + String.format("%.1f", zone.getVolume())), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Pitch: \u00a7f" + String.format("%.1f", zone.getPitch())), false);

        if (zone.hasArea()) {
            BlockPos c1 = zone.getCorner1();
            BlockPos c2 = zone.getCorner2();
            source.sendSuccess(() -> Component.literal("\u00a77  Area: \u00a7aComplete"), false);
            source.sendSuccess(() -> Component.literal("\u00a77    Corner 1: \u00a7e" + c1.getX() + ", " + c1.getY() + ", " + c1.getZ()), false);
            source.sendSuccess(() -> Component.literal("\u00a77    Corner 2: \u00a7e" + c2.getX() + ", " + c2.getY() + ", " + c2.getZ()), false);
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Area: \u00a7cNot defined"), false);
        }

        source.sendSuccess(() -> Component.literal("\u00a77  Status: " + (zone.isComplete() ? "\u00a7aActive \u2713" : "\u00a7cIncomplete")), false);

        return Command.SINGLE_SUCCESS;
    }
}
