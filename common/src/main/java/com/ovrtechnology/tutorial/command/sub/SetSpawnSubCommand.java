package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand to set the tutorial spawn point.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial setspawn} - Set spawn at current position</li>
 *   <li>{@code /tutorial setspawn <waypointId>} - Set spawn + first waypoint to activate on join</li>
 * </ul>
 */
public class SetSpawnSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> WAYPOINT_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialWaypointManager.getAllWaypointIds(level), builder);
    };

    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public String getDescription() {
        return "Set the tutorial spawn point at your current position";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial setspawn <waypointId> - set spawn + first waypoint
                .then(Commands.argument("waypointId", StringArgumentType.word())
                        .suggests(WAYPOINT_SUGGESTIONS)
                        .executes(this::executeWithWaypoint)
                )
                // /tutorial setspawn - just set spawn
                .executes(this::execute);
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        ServerLevel level = (ServerLevel) player.level();
        TutorialSpawnManager.setSpawn(level, pos, yaw, pitch);

        source.sendSuccess(
                () -> Component.literal("§d[OVR Tutorial] §fSpawn point set at §d"
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeWithWaypoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        String waypointId = StringArgumentType.getString(context, "waypointId");
        BlockPos pos = player.blockPosition();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        ServerLevel level = (ServerLevel) player.level();
        TutorialSpawnManager.setSpawn(level, pos, yaw, pitch);
        TutorialSpawnManager.setFirstWaypoint(level, waypointId);

        source.sendSuccess(
                () -> Component.literal("§d[OVR Tutorial] §fSpawn set at §d"
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        + "\n§d[OVR Tutorial] §fFirst waypoint: §d" + waypointId
                        + "\n§7  This waypoint will activate automatically when a player joins"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }
}
