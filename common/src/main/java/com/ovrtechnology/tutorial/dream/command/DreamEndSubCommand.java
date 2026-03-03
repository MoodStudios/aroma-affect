package com.ovrtechnology.tutorial.dream.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.dream.TutorialDreamEndHandler;
import com.ovrtechnology.tutorial.dream.TutorialDreamEndManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Subcommand to configure the dream ending sequence.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial dreamend setpos <x> <y> <z>} - Set teleport destination</li>
 *   <li>{@code /tutorial dreamend setyaw <yaw>} - Set facing direction</li>
 *   <li>{@code /tutorial dreamend info} - Show current config</li>
 *   <li>{@code /tutorial dreamend test} - Manually trigger dream sequence</li>
 * </ul>
 */
public class DreamEndSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "dreamend";
    }

    @Override
    public String getDescription() {
        return "Configure the dream ending sequence (teleport after dragon kill)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("setpos")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(this::executeSetPos)
                        )
                )
                .then(Commands.literal("setyaw")
                        .then(Commands.argument("yaw", FloatArgumentType.floatArg(-180, 180))
                                .executes(this::executeSetYaw)
                        )
                )
                .then(Commands.literal("info")
                        .executes(this::executeInfo)
                )
                .then(Commands.literal("test")
                        .executes(this::executeTest)
                );
    }

    private int executeSetPos(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

        TutorialDreamEndManager.setEndPos(level, pos);

        source.sendSuccess(
                () -> Component.literal("§d[OVR Tutorial] §fDream end position set to §d"
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeSetYaw(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        float yaw = FloatArgumentType.getFloat(context, "yaw");

        TutorialDreamEndManager.setEndYaw(level, yaw);

        source.sendSuccess(
                () -> Component.literal("§d[OVR Tutorial] §fDream end yaw set to §d" + yaw),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        StringBuilder sb = new StringBuilder();
        sb.append("§d[OVR Tutorial] §6Dream End Configuration\n");

        Optional<BlockPos> pos = TutorialDreamEndManager.getEndPos(level);
        if (pos.isPresent()) {
            BlockPos p = pos.get();
            sb.append("§d  Position: §f").append(p.getX()).append(", ").append(p.getY()).append(", ").append(p.getZ()).append("\n");
            sb.append("§d  Yaw: §f").append(TutorialDreamEndManager.getEndYaw(level));
        } else {
            sb.append("§c  Not configured! Use /tutorial dreamend setpos <x> <y> <z>");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (!TutorialDreamEndManager.isConfigured(level)) {
            source.sendFailure(Component.literal("§c[OVR Tutorial] Dream end position not configured! Use /tutorial dreamend setpos first"));
            return 0;
        }

        TutorialDreamEndHandler.startDreamSequence(player);

        source.sendSuccess(
                () -> Component.literal("§d[OVR Tutorial] §fStarted dream ending sequence (test)"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }
}
