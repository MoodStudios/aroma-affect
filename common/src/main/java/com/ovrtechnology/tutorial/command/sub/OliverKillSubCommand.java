package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Subcommand to remove/kill Tutorial Oliver entities.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial oliverkill} - Kill all Olivers within 32 blocks</li>
 *   <li>{@code /tutorial oliverkill <radius>} - Kill all Olivers within specified radius</li>
 *   <li>{@code /tutorial oliverkill all} - Kill ALL Olivers in the world</li>
 * </ul>
 */
public class OliverKillSubCommand implements TutorialSubCommand {

    private static final int DEFAULT_RADIUS = 32;

    @Override
    public String getName() {
        return "oliverkill";
    }

    @Override
    public String getDescription() {
        return "Remove Tutorial Oliver NPCs";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial oliverkill - default radius
                .executes(ctx -> executeKillNearby(ctx, DEFAULT_RADIUS))

                // /tutorial oliverkill all - kill all in world
                .then(Commands.literal("all")
                        .executes(this::executeKillAll))

                // /tutorial oliverkill <radius> - custom radius
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                        .executes(ctx -> executeKillNearby(ctx, IntegerArgumentType.getInteger(ctx, "radius")))
                );
    }

    private int executeKillNearby(CommandContext<CommandSourceStack> context, int radius) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        // Create bounding box around the command source position
        AABB searchArea = new AABB(
                pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius
        );

        // Find all Oliver entities in the area
        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class,
                searchArea
        );

        if (olivers.isEmpty()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] No Oliver entities found within " + radius + " blocks"
            ));
            return 0;
        }

        // Remove all found entities
        int count = 0;
        for (TutorialOliverEntity oliver : olivers) {
            oliver.discard();
            count++;
        }

        final int removedCount = count;
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRemoved \u00a7d" + removedCount
                        + "\u00a7f Oliver entit" + (removedCount == 1 ? "y" : "ies")),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeKillAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // Iterate through all entities in the level
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof TutorialOliverEntity oliver) {
                oliver.discard();
                count++;
            }
        }

        if (count == 0) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] No Oliver entities found in this world"
            ));
            return 0;
        }

        final int removedCount = count;
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRemoved \u00a7dall \u00a7d" + removedCount
                        + "\u00a7f Oliver entit" + (removedCount == 1 ? "y" : "ies") + " from the world"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }
}
