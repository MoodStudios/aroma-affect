package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Subcommand to control Tutorial Oliver NPC behavior.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial oliver stop} - Make Oliver stationary</li>
 *   <li>{@code /tutorial oliver follow [player]} - Make Oliver follow a player</li>
 *   <li>{@code /tutorial oliver walkto <x> <y> <z>} - Make Oliver walk to position</li>
 *   <li>{@code /tutorial oliver dialogue <id>} - Set Oliver's dialogue</li>
 *   <li>{@code /tutorial oliver info} - Show Oliver's current state</li>
 * </ul>
 */
public class OliverControlSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "oliver";
    }

    @Override
    public String getDescription() {
        return "Control Tutorial Oliver NPC behavior";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial oliver stop
                .then(Commands.literal("stop")
                        .executes(this::executeStop)
                )

                // /tutorial oliver follow [player]
                .then(Commands.literal("follow")
                        .executes(this::executeFollowNearest)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(this::executeFollowPlayer)
                        )
                )

                // /tutorial oliver walkto <pos>
                .then(Commands.literal("walkto")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(this::executeWalkTo)
                        )
                )

                // /tutorial oliver dialogue <id>
                .then(Commands.literal("dialogue")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeSetDialogue)
                        )
                )

                // /tutorial oliver info
                .then(Commands.literal("info")
                        .executes(this::executeInfo)
                )

                // /tutorial oliver tp - teleport Oliver to player
                .then(Commands.literal("tp")
                        .executes(this::executeTeleport)
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fOliver commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial oliver stop"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial oliver follow [player]"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial oliver walkto <x> <y> <z>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial oliver dialogue <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial oliver tp"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial oliver info"), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Finds the nearest Oliver entity to the command source.
     */
    private TutorialOliverEntity findNearestOliver(CommandSourceStack source) {
        if (!(source.getLevel() instanceof ServerLevel level)) {
            return null;
        }

        // Search in a large area
        AABB searchArea = new AABB(
                source.getPosition().x - 100, source.getPosition().y - 50, source.getPosition().z - 100,
                source.getPosition().x + 100, source.getPosition().y + 50, source.getPosition().z + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class,
                searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        // Return the nearest one
        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(source.getPosition());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    private int executeStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TutorialOliverEntity oliver = findNearestOliver(source);

        if (oliver == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No Oliver found nearby"));
            return 0;
        }

        oliver.setStationary();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fOliver is now \u00a7estationary"),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executeFollowNearest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TutorialOliverEntity oliver = findNearestOliver(source);

        if (oliver == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No Oliver found nearby"));
            return 0;
        }

        oliver.setFollowingNearest();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fOliver is now \u00a7efollowing the nearest player"),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executeFollowPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TutorialOliverEntity oliver = findNearestOliver(source);

        if (oliver == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No Oliver found nearby"));
            return 0;
        }

        ServerPlayer target;
        try {
            target = EntityArgument.getPlayer(context, "player");
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Player not found"));
            return 0;
        }

        oliver.setFollowing(target);
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fOliver is now \u00a7efollowing " + target.getName().getString()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executeWalkTo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TutorialOliverEntity oliver = findNearestOliver(source);

        if (oliver == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No Oliver found nearby"));
            return 0;
        }

        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        oliver.setWalkingTo(pos);

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fOliver is \u00a7ewalking to "
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executeSetDialogue(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TutorialOliverEntity oliver = findNearestOliver(source);

        if (oliver == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No Oliver found nearby"));
            return 0;
        }

        String dialogueId = StringArgumentType.getString(context, "id");
        oliver.setDialogueId(dialogueId);

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fOliver dialogue set to \u00a7e" + dialogueId),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executeTeleport(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        TutorialOliverEntity oliver = findNearestOliver(source);

        if (oliver == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No Oliver found nearby"));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        oliver.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        oliver.setStationary();

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fTeleported Oliver to your position"),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TutorialOliverEntity oliver = findNearestOliver(source);

        if (oliver == null) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No Oliver found nearby"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fOliver Status:"), false);

        // Position
        BlockPos pos = oliver.blockPosition();
        source.sendSuccess(
                () -> Component.literal("\u00a77  Position: \u00a7e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                false
        );

        // Mode
        TutorialOliverEntity.Mode mode = oliver.getMode();
        String modeStr = switch (mode) {
            case STATIONARY -> "\u00a7aStationary";
            case FOLLOWING -> {
                var target = oliver.getFollowTarget();
                yield "\u00a7eFollowing " + (target != null ? target.getName().getString() : "unknown");
            }
            case WALKING_TO -> {
                var walkTarget = oliver.getWalkTarget();
                yield "\u00a7eWalking to " + (walkTarget != null ?
                        walkTarget.getX() + ", " + walkTarget.getY() + ", " + walkTarget.getZ() : "unknown");
            }
        };
        source.sendSuccess(() -> Component.literal("\u00a77  Mode: " + modeStr), false);

        // Dialogue
        source.sendSuccess(
                () -> Component.literal("\u00a77  Dialogue: \u00a7e" + oliver.getDialogueId()),
                false
        );

        // In dialogue?
        if (oliver.isInDialogue()) {
            source.sendSuccess(() -> Component.literal("\u00a77  Currently in dialogue: \u00a7aYes"), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}
