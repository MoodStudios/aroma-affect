package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.network.TutorialFinishNetworking;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.finishscreen.TutorialFinishZone;
import com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler;
import com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand for managing finish screen trigger zones.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial finishzone create <id>}</li>
 *   <li>{@code /tutorial finishzone delete <id>}</li>
 *   <li>{@code /tutorial finishzone corner <id> <1|2>}</li>
 *   <li>{@code /tutorial finishzone list}</li>
 *   <li>{@code /tutorial finishzone info <id>}</li>
 * </ul>
 */
public class FinishZoneSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> ZONE_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialFinishZoneManager.getAllZoneIds(level), builder);
    };

    @Override
    public String getName() {
        return "finishzone";
    }

    @Override
    public String getDescription() {
        return "Manage finish screen trigger zones";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeCreate)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeDelete)))
                .then(Commands.literal("corner")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .then(Commands.argument("corner", IntegerArgumentType.integer(1, 2))
                                        .executes(this::executeCorner))))
                .then(Commands.literal("list")
                        .executes(this::executeList))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeInfo)))
                .then(Commands.literal("test")
                        .executes(this::executeTest))
                .then(Commands.literal("reset")
                        .executes(this::executeReset))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7d[OVR Tutorial] \u00a7fFinish zone commands: create, delete, corner, list, info"), false);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = context.getSource().getLevel();
        if (TutorialFinishZoneManager.createZone(level, id)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fCreated finish zone \u00a7d" + id), true);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendFailure(Component.literal("\u00a7cZone '" + id + "' already exists"));
        return 0;
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = context.getSource().getLevel();
        if (TutorialFinishZoneManager.deleteZone(level, id)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fDeleted finish zone \u00a7d" + id), true);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
        return 0;
    }

    private int executeCorner(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        int corner = IntegerArgumentType.getInteger(context, "corner");
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (TutorialFinishZoneManager.setCorner(level, id, corner, player.blockPosition())) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet finish zone \u00a7d" + id +
                            " \u00a7fcorner " + corner + " to \u00a7e" + player.blockPosition().toShortString()), true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
        return 0;
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        var ids = TutorialFinishZoneManager.getAllZoneIds(level);
        if (ids.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a77No finish zones defined"), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fFinish zones (" + ids.size() + "):"), false);
        for (String id : ids) {
            TutorialFinishZone zone = TutorialFinishZoneManager.getZone(level, id);
            String status = zone != null && zone.isComplete() ? "\u00a7a[COMPLETE]" : "\u00a7c[INCOMPLETE]";
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a77  \u00a7e" + id + " " + status), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = context.getSource().getLevel();
        TutorialFinishZone zone = TutorialFinishZoneManager.getZone(level, id);
        if (zone == null) {
            context.getSource().sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fFinish zone: \u00a7d" + id), false);
        String c1 = zone.getCorner1() != null ? zone.getCorner1().toShortString() : "not set";
        String c2 = zone.getCorner2() != null ? zone.getCorner2().toShortString() : "not set";
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Corner 1: \u00a7e" + c1), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Corner 2: \u00a7e" + c2), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Complete: " + (zone.isComplete() ? "\u00a7aYes" : "\u00a7cNo")), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        TutorialFinishNetworking.sendOpenFinish(player);

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fSent finish screen to player"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int executeReset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        TutorialFinishZoneHandler.resetPlayer(player.getUUID());
        source.sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fReset finish screen trigger for player"), true);
        return Command.SINGLE_SUCCESS;
    }
}
