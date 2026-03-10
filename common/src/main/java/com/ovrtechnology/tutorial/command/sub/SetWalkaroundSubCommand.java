package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand to set the walkaround spawn point for free exploration mode.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial setwalkaround} - Set walkaround spawn at current position</li>
 * </ul>
 */
public class SetWalkaroundSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "setwalkaround";
    }

    @Override
    public String getDescription() {
        return "Set the walkaround spawn point for free exploration mode";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.executes(this::execute);
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        ServerLevel level = (ServerLevel) player.level();
        TutorialSpawnManager.setWalkaroundSpawn(level, pos, yaw, pitch);

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fWalkaround spawn set at \u00a7d"
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        + "\n\u00a77  Players clicking WALKAROUND will teleport here"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }
}
