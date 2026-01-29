package com.ovrtechnology.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.command.SubCommand;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.lookup.LookupType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand to create a particle path towards biomes, structures, or blocks.
 * <p>
 * The path will persist until the player reaches the destination, following the terrain
 * with wave-like undulations for visual appeal.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /aromatest path biome <biome_id> [radius]} - Create path to biome</li>
 *   <li>{@code /aromatest path structure <structure_id> [radius]} - Create path to structure</li>
 *   <li>{@code /aromatest path block <block_id> [radius]} - Create path to block</li>
 *   <li>{@code /aromatest path stop} - Stop the current path</li>
 * </ul>
 */
public class PathSubCommand implements SubCommand {

    /**
     * Suggestion provider for biome IDs.
     */
    private static final SuggestionProvider<CommandSourceStack> BIOME_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getLevel() != null) {
            return SharedSuggestionProvider.suggestResource(
                    context.getSource().getLevel().registryAccess()
                            .lookupOrThrow(Registries.BIOME)
                            .listElementIds()
                            .map(key -> key.location()),
                    builder
            );
        }
        return builder.buildFuture();
    };

    /**
     * Suggestion provider for structure IDs.
     */
    private static final SuggestionProvider<CommandSourceStack> STRUCTURE_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getLevel() != null) {
            return SharedSuggestionProvider.suggestResource(
                    context.getSource().getLevel().registryAccess()
                            .lookupOrThrow(Registries.STRUCTURE)
                            .listElementIds()
                            .map(key -> key.location()),
                    builder
            );
        }
        return builder.buildFuture();
    };

    /**
     * Suggestion provider for block IDs.
     */
    private static final SuggestionProvider<CommandSourceStack> BLOCK_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    BuiltInRegistries.BLOCK.keySet(),
                    builder
            );

    @Override
    public String getName() {
        return "path";
    }

    @Override
    public String getDescription() {
        return "Create a particle path to biomes, structures, or blocks";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("biome")
                        .then(Commands.argument("biome_id", ResourceLocationArgument.id())
                                .suggests(BIOME_SUGGESTIONS)
                                .executes(ctx -> executePath(ctx, LookupType.BIOME, "biome_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 32000))
                                        .executes(ctx -> executePath(ctx, LookupType.BIOME, "biome_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )

                .then(Commands.literal("structure")
                        .then(Commands.argument("structure_id", ResourceLocationArgument.id())
                                .suggests(STRUCTURE_SUGGESTIONS)
                                .executes(ctx -> executePath(ctx, LookupType.STRUCTURE, "structure_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> executePath(ctx, LookupType.STRUCTURE, "structure_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )

                .then(Commands.literal("block")
                        .then(Commands.argument("block_id", ResourceLocationArgument.id())
                                .suggests(BLOCK_SUGGESTIONS)
                                .executes(ctx -> executePath(ctx, LookupType.BLOCK, "block_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                                        .executes(ctx -> executePath(ctx, LookupType.BLOCK, "block_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )

                .then(Commands.literal("stop")
                        .executes(this::executeStop)
                )
                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[Aroma Affect] §7Path usage:"), false);
        source.sendSuccess(() -> Component.literal("§e  /aromatest path biome <biome_id> [radius]"), false);
        source.sendSuccess(() -> Component.literal("§e  /aromatest path structure <structure_id> [radius]"), false);
        source.sendSuccess(() -> Component.literal("§e  /aromatest path block <block_id> [radius]"), false);
        source.sendSuccess(() -> Component.literal("§e  /aromatest path stop §7- Stop the current path"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§6[Aroma Affect] §cThis command can only be executed by a player"));
            return 0;
        }

        if (ActivePathManager.getInstance().hasActivePath(player.getUUID())) {
            ActivePathManager.getInstance().removePath(player.getUUID());
            source.sendSuccess(() -> Component.literal("§6[Aroma Affect] §aPath stopped successfully!"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§6[Aroma Affect] §7No active path to stop."), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executePath(
            CommandContext<CommandSourceStack> context,
            LookupType type,
            String argumentName,
            int radius
    ) {
        CommandSourceStack source = context.getSource();

        // Get the resource ID from the argument
        ResourceLocation resourceId = ResourceLocationArgument.getId(context, argumentName);

        // Get the origin position and player
        BlockPos origin;
        ServerPlayer player;
        if (source.getEntity() instanceof ServerPlayer serverPlayer) {
            player = serverPlayer;
            origin = serverPlayer.blockPosition();
        } else {
            player = null;
            origin = BlockPos.containing(source.getPosition());
        }

        ServerLevel level = source.getLevel();
        LookupTarget target = new LookupTarget(type, resourceId);

        // Send search message
        source.sendSuccess(() -> Component.literal(
                "§6[Aroma Affect] §7Searching for §e" + type.getId() + " §7'§f" + resourceId + "§7'..."
        ), false);

        // Execute asynchronous search
        LookupManager.getInstance().lookupAsync(level, origin, target, radius, result -> {
            createPath(source, result, origin, level, player);
        });

        return Command.SINGLE_SUCCESS;
    }

    private void createPath(CommandSourceStack source, LookupResult result, BlockPos origin, ServerLevel level, ServerPlayer player) {
        if (result.isSuccess()) {
            BlockPos destination = result.getPosition();

            int yLevel = LookupManager.getInstance().findYLevel(level, destination.getX(), destination.getZ(), result.target().type());
            BlockPos finalDestination = new BlockPos(destination.getX(), yLevel, destination.getZ());

            source.sendSuccess(() -> Component.literal("§6[Aroma Affect] §aCreating particle path!"), false);
            source.sendSuccess(() -> Component.literal(
                    String.format("§7  Position: §aX: %d§7, §aY: %d§7, §aZ: %d",
                            finalDestination.getX(), finalDestination.getY(), finalDestination.getZ())
            ), false);
            source.sendSuccess(() -> Component.literal(
                    "§7  Distance: §e" + result.getFormattedDistance() + " blocks"
            ), false);
            source.sendSuccess(() -> Component.literal(
                    "§7  §oThe path will guide you until you arrive. Use §e/aromatest path stop §7§oto cancel."
            ), false);

            // Create persistent path that follows terrain with undulations
            if (player != null && player.level() == level) {
                // Convert LookupType to ActivePathManager.TargetType
                ActivePathManager.TargetType targetType = convertLookupType(result.target().type());
                String targetId = result.target().resourceId().toString();

                ActivePathManager.getInstance().createPath(player, level, finalDestination, targetType, targetId);
            } else if (player == null) {
                source.sendSuccess(() -> Component.literal("§7  §o(Particles only visible to players)"), false);
            }

        } else {
            // Handle failure cases
            String reason = switch (result.failureReason()) {
                case NOT_FOUND -> "not found within search radius";
                case TIMEOUT -> "Timed out";
                case INVALID_TARGET -> "invalid target specified";
                case CANCELLED -> "search was cancelled";
                case DIMENSION_MISMATCH -> "search cannot be performed in this dimension";
                case ERROR -> "an error occurred during search";
                default -> "unknown error";
            };

            source.sendFailure(Component.literal(
                    "§6[Aroma Affect] §c" + reason + ": §7" + result.target().resourceId()
            ));
        }
    }

    /**
     * Converts a LookupType to an ActivePathManager.TargetType.
     */
    private ActivePathManager.TargetType convertLookupType(LookupType lookupType) {
        return switch (lookupType) {
            case BLOCK, FLOWER -> ActivePathManager.TargetType.BLOCK;
            case BIOME -> ActivePathManager.TargetType.BIOME;
            case STRUCTURE -> ActivePathManager.TargetType.STRUCTURE;
        };
    }
}