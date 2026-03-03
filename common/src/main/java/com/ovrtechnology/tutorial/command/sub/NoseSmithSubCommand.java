package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Subcommand to configure the tutorial Nose Smith.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial nosesmith setspawn} - Set NoseSmith spawn at your position</li>
 *   <li>{@code /tutorial nosesmith setflower} - Set flower position at your position</li>
 *   <li>{@code /tutorial nosesmith setflower <x> <y> <z>} - Set flower position at coords</li>
 *   <li>{@code /tutorial nosesmith info} - Show current config</li>
 * </ul>
 */
public class NoseSmithSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "nosesmith";
    }

    @Override
    public String getDescription() {
        return "Configure the tutorial Nose Smith spawn and flower position";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("setspawn")
                        .executes(this::executeSetSpawn))
                .then(Commands.literal("setflower")
                        .executes(this::executeSetFlowerHere)
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(this::executeSetFlowerCoords)))))
                .then(Commands.literal("info")
                        .executes(this::executeInfo));
    }

    private int executeSetSpawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos pos = player.blockPosition();
        float yaw = player.getYRot();

        TutorialNoseSmithManager.setSpawnPos(level, pos, yaw);

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose Smith spawn set to \u00a7d"
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        + " \u00a77(yaw: " + String.format("%.1f", yaw) + ")"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeSetFlowerHere(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos pos = player.blockPosition();

        TutorialNoseSmithManager.setFlowerPos(level, pos);

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose Smith flower position set to \u00a7d"
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeSetFlowerCoords(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        BlockPos pos = new BlockPos(x, y, z);

        TutorialNoseSmithManager.setFlowerPos(level, pos);

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose Smith flower position set to \u00a7d"
                        + x + ", " + y + ", " + z),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        Optional<BlockPos> spawn = TutorialNoseSmithManager.getSpawnPos(level);
        float yaw = TutorialNoseSmithManager.getSpawnYaw(level);
        Optional<BlockPos> flower = TutorialNoseSmithManager.getFlowerPos(level);

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose Smith Config:"), false);

        if (spawn.isPresent()) {
            BlockPos s = spawn.get();
            source.sendSuccess(() -> Component.literal("\u00a77  Spawn: \u00a7e"
                    + s.getX() + ", " + s.getY() + ", " + s.getZ()
                    + " \u00a77(yaw: " + String.format("%.1f", yaw) + ")"), false);
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Spawn: \u00a7cNot set"), false);
        }

        if (flower.isPresent()) {
            BlockPos f = flower.get();
            source.sendSuccess(() -> Component.literal("\u00a77  Flower: \u00a7e"
                    + f.getX() + ", " + f.getY() + ", " + f.getZ()), false);
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Flower: \u00a7cNot set"), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}
