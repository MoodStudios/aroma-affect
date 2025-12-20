package com.ovrtechnology.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.command.SubCommand;
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
import net.minecraft.world.level.levelgen.Heightmap;


/**
 * Subcommand for looking up biomes, structures, and blocks.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /aromatest lookup biome <biome_id> [radius]} - Find nearest biome</li>
 *   <li>{@code /aromatest lookup structure <structure_id> [radius]} - Find nearest structure</li>
 *   <li>{@code /aromatest lookup block <block_id> [radius]} - Find nearest block</li>
 * </ul>
 */
public class LookupSubCommand implements SubCommand {
    
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
        return "lookup";
    }
    
    @Override
    public String getDescription() {
        return "Find biomes, structures, or blocks nearby";
    }
    
    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // Biome lookup: /aromatest lookup biome <id> [radius]
                .then(Commands.literal("biome")
                        .then(Commands.argument("biome_id", ResourceLocationArgument.id())
                                .suggests(BIOME_SUGGESTIONS)
                                .executes(ctx -> executeLookup(ctx, LookupType.BIOME, "biome_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 32000))
                                        .executes(ctx -> executeLookup(ctx, LookupType.BIOME, "biome_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )
                // Structure lookup: /aromatest lookup structure <id> [radius]
                // Radius up to 10000 blocks (same as Explorer's Compass default)
                .then(Commands.literal("structure")
                        .then(Commands.argument("structure_id", ResourceLocationArgument.id())
                                .suggests(STRUCTURE_SUGGESTIONS)
                                .executes(ctx -> executeLookup(ctx, LookupType.STRUCTURE, "structure_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> executeLookup(ctx, LookupType.STRUCTURE, "structure_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )
                // Block lookup: /aromatest lookup block <id> [radius]
                .then(Commands.literal("block")
                        .then(Commands.argument("block_id", ResourceLocationArgument.id())
                                .suggests(BLOCK_SUGGESTIONS)
                                .executes(ctx -> executeLookup(ctx, LookupType.BLOCK, "block_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                                        .executes(ctx -> executeLookup(ctx, LookupType.BLOCK, "block_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )
                // Default: show usage
                .executes(this::showUsage);
    }
    
    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6[AromaCraft] §7Lookup usage:"), false);
        source.sendSuccess(() -> Component.literal("§e  /aromatest lookup biome <biome_id> [radius]"), false);
        source.sendSuccess(() -> Component.literal("§e  /aromatest lookup structure <structure_id> [radius]"), false);
        source.sendSuccess(() -> Component.literal("§e  /aromatest lookup block <block_id> [radius]"), false);
        return Command.SINGLE_SUCCESS;
    }
    
    private int executeLookup(
            CommandContext<CommandSourceStack> context,
            LookupType type,
            String argumentName,
            int radius
    ) {
        CommandSourceStack source = context.getSource();
        
        // Get the resource ID from the argument using ResourceLocationArgument
        ResourceLocation resourceId = ResourceLocationArgument.getId(context, argumentName);
        
        // Get the origin position
        BlockPos origin;
        if (source.getEntity() instanceof ServerPlayer player) {
            origin = player.blockPosition();
        } else {
            origin = BlockPos.containing(source.getPosition());
        }
        
        ServerLevel level = source.getLevel();
        LookupTarget target = new LookupTarget(type, resourceId);
        
        // Send "searching" message
        source.sendSuccess(() -> Component.literal(
                "§6[AromaCraft] §7Searching for §e" + type.getId() + " §7'§f" + resourceId + "§7'..."
        ), false);
        
        // Execute async lookup
        LookupManager.getInstance().lookupAsync(level, origin, target, radius, result -> {
            sendResult(source, result);
        });
        
        return Command.SINGLE_SUCCESS;
    }


    /**
     * Sends the lookup result to the command source.
     */
    private void sendResult(CommandSourceStack source, LookupResult result) {
        if (result.isSuccess()) {

            BlockPos pos = result.getPosition();

            ServerLevel serverLevel = source.getLevel();

            //get structure Y
            int yLevel = LookupManager.getInstance().findYLevel(serverLevel, pos.getX(), pos.getZ(), result.target().type());

            // Build result message with position
            source.sendSuccess(() -> Component.literal("§6[AromaCraft] §aFound! "), false);
            source.sendSuccess(() -> Component.literal(
                    String.format("§7  Position: §aX: %d§7, §aY: %d§7, §aZ: %d", 
                            pos.getX(), yLevel, pos.getZ())
            ), false);
            source.sendSuccess(() -> Component.literal(
                    "§7  Distance: §e" + result.getFormattedDistance() + " blocks"
            ), false);
            source.sendSuccess(() -> Component.literal(
                    String.format("§8  Teleport: §7/tp @s %d %d %d", pos.getX(), yLevel, pos.getZ())
            ), false);
            
            if (result.fromCache()) {
                source.sendSuccess(() -> Component.literal("§8  (cached result)"), false);
            } else {
                source.sendSuccess(() -> Component.literal(
                        "§8  (search took " + result.searchTimeMs() + "ms)"
                ), false);
            }
        } else {
            // Handle failure cases
            String reason = switch (result.failureReason()) {
                case NOT_FOUND -> "Not found within search radius";
                case TIMEOUT -> "Search timed out";
                case INVALID_TARGET -> "Invalid target specified";
                case CANCELLED -> "Search was cancelled";
                case DIMENSION_MISMATCH -> "Cannot search in this dimension";
                case ERROR -> "An error occurred during search";
                default -> "Unknown error";
            };
            
            source.sendFailure(Component.literal(
                    "§6[AromaCraft] §c" + reason + ": §7" + result.target().resourceId()
            ));
        }
    }
}

