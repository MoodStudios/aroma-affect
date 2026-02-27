package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.oliver.TutorialOliverRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand to spawn Tutorial Oliver NPC.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial oliverspawn} - Spawn Oliver at your position</li>
 *   <li>{@code /tutorial oliverspawn <dialogue_id>} - Spawn with specific dialogue</li>
 * </ul>
 * <p>
 * Oliver is a stationary, invincible villager who displays tutorial dialogue
 * when players interact with him. He guides players through the tutorial
 * experience, similar to the Nose Smith's initial interaction.
 */
public class OliverSpawnSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "oliverspawn";
    }

    @Override
    public String getDescription() {
        return "Spawn Tutorial Oliver NPC at your position";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial oliverspawn
                .executes(ctx -> executeSpawn(ctx, "default"))

                // /tutorial oliverspawn <dialogue_id>
                .then(Commands.argument("dialogue_id", StringArgumentType.word())
                        .executes(ctx -> executeSpawn(ctx, StringArgumentType.getString(ctx, "dialogue_id")))
                );
    }

    private int executeSpawn(CommandContext<CommandSourceStack> context, String dialogueId) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();

        // Create the Oliver entity
        TutorialOliverEntity oliver = new TutorialOliverEntity(
                TutorialOliverRegistry.TUTORIAL_OLIVER.get(),
                level
        );

        // Set position and rotation
        oliver.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        oliver.setYRot(player.getYRot());
        oliver.setYHeadRot(player.getYRot());
        oliver.setYBodyRot(player.getYRot());

        // Set dialogue ID
        oliver.setDialogueId(dialogueId);

        // Save home position (for /tutorial reset)
        oliver.setHomePos(pos, player.getYRot());

        // Add to world
        level.addFreshEntity(oliver);

        source.sendSuccess(
                () -> Component.literal("§d[OVR Tutorial] §fSpawned Oliver at §d"
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                true
        );

        if (!"default".equals(dialogueId)) {
            source.sendSuccess(
                    () -> Component.literal("§7  Dialogue ID: §e" + dialogueId),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}
