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
import com.ovrtechnology.util.Texts;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class LookupSubCommand implements SubCommand {

    private static final SuggestionProvider<CommandSourceStack> BIOME_SUGGESTIONS =
            (context, builder) -> {
                if (context.getSource().getLevel() != null) {
                    return SharedSuggestionProvider.suggestResource(
                            context.getSource()
                                    .getLevel()
                                    .registryAccess()
                                    .lookupOrThrow(Registries.BIOME)
                                    .listElementIds()
                                    .map(key -> key.location()),
                            builder);
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> STRUCTURE_SUGGESTIONS =
            (context, builder) -> {
                if (context.getSource().getLevel() != null) {
                    return SharedSuggestionProvider.suggestResource(
                            context.getSource()
                                    .getLevel()
                                    .registryAccess()
                                    .lookupOrThrow(Registries.STRUCTURE)
                                    .listElementIds()
                                    .map(key -> key.location()),
                            builder);
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> BLOCK_SUGGESTIONS =
            (context, builder) ->
                    SharedSuggestionProvider.suggestResource(
                            BuiltInRegistries.BLOCK.keySet(), builder);

    @Override
    public String getName() {
        return "lookup";
    }

    @Override
    public String getDescription() {
        return "Find biomes, structures, or blocks nearby";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(
            LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.then(
                        Commands.literal("biome")
                                .then(
                                        Commands.argument("biome_id", ResourceLocationArgument.id())
                                                .suggests(BIOME_SUGGESTIONS)
                                                .executes(
                                                        ctx ->
                                                                executeLookup(
                                                                        ctx,
                                                                        LookupType.BIOME,
                                                                        "biome_id",
                                                                        -1))
                                                .then(
                                                        Commands.argument(
                                                                        "radius",
                                                                        IntegerArgumentType.integer(
                                                                                1, 32000))
                                                                .executes(
                                                                        ctx ->
                                                                                executeLookup(
                                                                                        ctx,
                                                                                        LookupType
                                                                                                .BIOME,
                                                                                        "biome_id",
                                                                                        IntegerArgumentType
                                                                                                .getInteger(
                                                                                                        ctx,
                                                                                                        "radius"))))))
                .then(
                        Commands.literal("structure")
                                .then(
                                        Commands.argument(
                                                        "structure_id",
                                                        ResourceLocationArgument.id())
                                                .suggests(STRUCTURE_SUGGESTIONS)
                                                .executes(
                                                        ctx ->
                                                                executeLookup(
                                                                        ctx,
                                                                        LookupType.STRUCTURE,
                                                                        "structure_id",
                                                                        -1))
                                                .then(
                                                        Commands.argument(
                                                                        "radius",
                                                                        IntegerArgumentType.integer(
                                                                                1, 10000))
                                                                .executes(
                                                                        ctx ->
                                                                                executeLookup(
                                                                                        ctx,
                                                                                        LookupType
                                                                                                .STRUCTURE,
                                                                                        "structure_id",
                                                                                        IntegerArgumentType
                                                                                                .getInteger(
                                                                                                        ctx,
                                                                                                        "radius"))))))
                .then(
                        Commands.literal("block")
                                .then(
                                        Commands.argument("block_id", ResourceLocationArgument.id())
                                                .suggests(BLOCK_SUGGESTIONS)
                                                .executes(
                                                        ctx ->
                                                                executeLookup(
                                                                        ctx,
                                                                        LookupType.BLOCK,
                                                                        "block_id",
                                                                        -1))
                                                .then(
                                                        Commands.argument(
                                                                        "radius",
                                                                        IntegerArgumentType.integer(
                                                                                1, 1024))
                                                                .executes(
                                                                        ctx ->
                                                                                executeLookup(
                                                                                        ctx,
                                                                                        LookupType
                                                                                                .BLOCK,
                                                                                        "block_id",
                                                                                        IntegerArgumentType
                                                                                                .getInteger(
                                                                                                        ctx,
                                                                                                        "radius"))))))
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Texts.lit("§6[Aroma Affect] §7Lookup usage:"), false);
        source.sendSuccess(
                () -> Texts.lit("§e  /aromatest lookup biome <biome_id> [radius]"), false);
        source.sendSuccess(
                () -> Texts.lit("§e  /aromatest lookup structure <structure_id> [radius]"), false);
        source.sendSuccess(
                () -> Texts.lit("§e  /aromatest lookup block <block_id> [radius]"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeLookup(
            CommandContext<CommandSourceStack> context,
            LookupType type,
            String argumentName,
            int radius) {
        CommandSourceStack source = context.getSource();

        ResourceLocation resourceId = ResourceLocationArgument.getId(context, argumentName);

        BlockPos origin;
        if (source.getEntity() instanceof ServerPlayer player) {
            origin = player.blockPosition();
        } else {
            origin = BlockPos.containing(source.getPosition());
        }

        ServerLevel level = source.getLevel();
        LookupTarget target = new LookupTarget(type, resourceId);

        source.sendSuccess(
                () ->
                        Texts.lit(
                                "§6[Aroma Affect] §7Searching for §e"
                                        + type.getId()
                                        + " §7'§f"
                                        + resourceId
                                        + "§7'..."),
                false);

        LookupManager.getInstance()
                .lookupAsync(
                        level,
                        origin,
                        target,
                        radius,
                        result -> {
                            sendResult(source, result);
                        });

        return Command.SINGLE_SUCCESS;
    }

    private void sendResult(CommandSourceStack source, LookupResult result) {
        if (result.isSuccess()) {

            BlockPos pos = result.getPosition();

            ServerLevel serverLevel = source.getLevel();

            int yLevel =
                    LookupManager.getInstance()
                            .findYLevel(
                                    serverLevel, pos.getX(), pos.getZ(), result.target().type());

            source.sendSuccess(() -> Texts.lit("§6[Aroma Affect] §aFound! "), false);
            source.sendSuccess(
                    () ->
                            Texts.lit(
                                    String.format(
                                            "§7  Position: §aX: %d§7, §aY: %d§7, §aZ: %d",
                                            pos.getX(), yLevel, pos.getZ())),
                    false);
            source.sendSuccess(
                    () -> Texts.lit("§7  Distance: §e" + result.getFormattedDistance() + " blocks"),
                    false);

            String tpCommand = String.format("/tp @s %d %d %d", pos.getX(), pos.getY(), pos.getZ());
            MutableComponent teleportText =
                    Texts.lit("§8  Teleport: ")
                            .append(
                                    Texts.lit("§b§n" + tpCommand)
                                            .withStyle(
                                                    Style.EMPTY
                                                            .withClickEvent(
                                                                    new ClickEvent.RunCommand(
                                                                            tpCommand))
                                                            .withHoverEvent(
                                                                    new HoverEvent.ShowText(
                                                                            Texts.lit(
                                                                                    "§aClick to teleport!")))));
            source.sendSuccess(() -> teleportText, false);

            if (result.fromCache()) {
                source.sendSuccess(() -> Texts.lit("§8  (cached result)"), false);
            } else {
                source.sendSuccess(
                        () -> Texts.lit("§8  (search took " + result.searchTimeMs() + "ms)"),
                        false);
            }
        } else {

            String reason =
                    switch (result.failureReason()) {
                        case NOT_FOUND -> "Not found within search radius";
                        case TIMEOUT -> "Search timed out";
                        case INVALID_TARGET -> "Invalid target specified";
                        case CANCELLED -> "Search was cancelled";
                        case DIMENSION_MISMATCH -> "Cannot search in this dimension";
                        case ERROR -> "An error occurred during search";
                        default -> "Unknown error";
                    };

            source.sendFailure(
                    Texts.lit(
                            "§6[Aroma Affect] §c"
                                    + reason
                                    + ": §7"
                                    + result.target().resourceId()));
        }
    }
}
