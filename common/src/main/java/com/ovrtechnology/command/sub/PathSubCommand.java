package com.ovrtechnology.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.biome.BiomeDefinition;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinition;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.command.SubCommand;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.flower.FlowerDefinition;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.lookup.LookupType;
import com.ovrtechnology.lookup.StructurePositionRefiner;
import com.ovrtechnology.network.BlacklistSyncManager;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.structure.StructureDefinition;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.tracking.TrackingConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

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
     * When true, path search results are printed to the player's chat.
     * Toggle with {@code /aromatest path verbose}.
     */
    private static boolean verbose = false;

    /**
     * @return true if verbose chat messages are enabled
     */
    public static boolean isVerbose() {
        return verbose;
    }

    /**
     * Suggestion provider for biome IDs.
     */
    private static final SuggestionProvider<CommandSourceStack> BIOME_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getLevel() != null) {
            return SharedSuggestionProvider.suggestResource(
                    context.getSource().getLevel().registryAccess()
                            .lookupOrThrow(Registries.BIOME)
                            .listElementIds()
                            .map(key -> key.identifier()),
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
                            .map(key -> key.identifier()),
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
                        .then(Commands.argument("biome_id", IdentifierArgument.id())
                                .suggests(BIOME_SUGGESTIONS)
                                .executes(ctx -> executePath(ctx, LookupType.BIOME, "biome_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 32000))
                                        .executes(ctx -> executePath(ctx, LookupType.BIOME, "biome_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )

                .then(Commands.literal("structure")
                        .then(Commands.argument("structure_id", IdentifierArgument.id())
                                .suggests(STRUCTURE_SUGGESTIONS)
                                .executes(ctx -> executePath(ctx, LookupType.STRUCTURE, "structure_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> executePath(ctx, LookupType.STRUCTURE, "structure_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )

                .then(Commands.literal("block")
                        .then(Commands.argument("block_id", IdentifierArgument.id())
                                .suggests(BLOCK_SUGGESTIONS)
                                .executes(ctx -> executePath(ctx, LookupType.BLOCK, "block_id", -1))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                                        .executes(ctx -> executePath(ctx, LookupType.BLOCK, "block_id",
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
                )

                .then(Commands.literal("flower")
                        .then(Commands.argument("flower_id", IdentifierArgument.id())
                                .suggests(BLOCK_SUGGESTIONS)
                                .executes(ctx -> executePath(ctx, LookupType.FLOWER, "flower_id", -1))
                        )
                )

                .then(Commands.literal("recall")
                        .then(Commands.argument("target_id", IdentifierArgument.id())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .then(Commands.argument("dimension", IdentifierArgument.id())
                                                                .executes(this::executeRecall)
                                                        )
                                                        .executes(this::executeRecall)
                                                )
                                        )
                                )
                        )
                )

                .then(Commands.literal("stop")
                        .executes(this::executeStop)
                )

                .then(Commands.literal("verbose")
                        .executes(this::toggleVerbose)
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
        source.sendSuccess(() -> Component.literal("§e  /aromatest path verbose §7- Toggle chat messages (" + (verbose ? "§aON" : "§cOFF") + "§7)"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int toggleVerbose(CommandContext<CommandSourceStack> context) {
        verbose = !verbose;
        String state = verbose ? "§aEnabled" : "§cDisabled";
        context.getSource().sendSuccess(() -> Component.literal("§6[Aroma Affect] §7Verbose path messages: " + state), false);
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
            if (verbose) {
                source.sendSuccess(() -> Component.literal("§6[Aroma Affect] §aPath stopped successfully!"), false);
            }
        } else {
            if (verbose) {
                source.sendSuccess(() -> Component.literal("§6[Aroma Affect] §7No active path to stop."), false);
            }
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
        Identifier resourceId = IdentifierArgument.getId(context, argumentName);

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

        // Server-side cost validation before searching
        if (player != null) {
            int targetCost = resolveTrackCost(type, resourceId.toString());

            // Check nose durability
            ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
            if (headStack.getItem() instanceof NoseItem) {
                if (headStack.isDamageableItem()) {
                    int remaining = headStack.getMaxDamage() - headStack.getDamageValue();
                    if (remaining < targetCost) {
                        PathScentNetworking.sendPathNotFound(player, "Not enough nose durability");
                        return Command.SINGLE_SUCCESS;
                    }
                }
            }

            // Check required item
            RequiredItem req = resolveRequiredItem(type, resourceId.toString());
            if (req != null && req.getItemId() != null) {
                if (!playerHasItem(player, req)) {
                    PathScentNetworking.sendPathNotFound(player, "Missing required item");
                    return Command.SINGLE_SUCCESS;
                }
            }
        }

        // Send search message (verbose only)
        if (verbose) {
            source.sendSuccess(() -> Component.literal(
                    "§6[Aroma Affect] §7Searching for §e" + type.getId() + " §7'§f" + resourceId + "§7'..."
            ), false);
        }

        // Collect excluded positions from blacklist sync
        Set<BlockPos> excludedPositions = player != null
                ? BlacklistSyncManager.getInstance().getExcludedPositionsForTarget(
                    player.getUUID(), resourceId.toString())
                : Set.of();

        // Execute asynchronous search (with exclusions for block/flower types)
        if ((type == LookupType.BLOCK || type == LookupType.FLOWER) && !excludedPositions.isEmpty()) {
            LookupManager.getInstance().lookupAsyncWithExclusions(
                    level, origin, target, radius, excludedPositions, result -> {
                        createPath(source, result, origin, level, player, 0);
                    });
        } else {
            LookupManager.getInstance().lookupAsync(level, origin, target, radius, result -> {
                createPath(source, result, origin, level, player, 0);
            });
        }

        return Command.SINGLE_SUCCESS;
    }

    private static final int MAX_BLACKLIST_RETRIES = 3;
    private static final int STRUCTURE_EXCLUSION_THRESHOLD = 128;
    /** How often (in blocks) to sample biome along the line between two points. */
    private static final int BIOME_SAMPLE_INTERVAL = 64;
    /** Beyond this distance, assume two points cannot be in the same biome region. */
    private static final int BIOME_MAX_CONTIGUITY_DISTANCE = 10000;
    /** How far past a blacklisted biome to shift the search origin on retry. */
    private static final int BIOME_SHIFT_DISTANCE = 1500;

    private void createPath(CommandSourceStack source, LookupResult result, BlockPos origin,
                             ServerLevel level, ServerPlayer player, int retryCount) {
        if (result.isSuccess()) {
            BlockPos destination = result.getPosition();

            // For BLOCK and FLOWER, the search already found the exact block position.
            // Only use findYLevel for STRUCTURE and BIOME where we need a walkable surface.
            BlockPos finalDestination;
            LookupType lookupType = result.target().type();
            if (lookupType == LookupType.BLOCK || lookupType == LookupType.FLOWER) {
                finalDestination = destination;
            } else if (lookupType == LookupType.STRUCTURE) {
                BlockPos refined = StructurePositionRefiner.refine(level, destination, result.target().resourceId());
                finalDestination = refined;
            } else {
                int yLevel = LookupManager.getInstance().findYLevel(level, destination.getX(), destination.getZ(), lookupType);
                finalDestination = new BlockPos(destination.getX(), yLevel, destination.getZ());
            }

            // Check if this destination is blacklisted (for structures/biomes only;
            // blocks are already filtered by BlockLookupStrategy exclusions)
            if (player != null && (lookupType == LookupType.STRUCTURE || lookupType == LookupType.BIOME)) {
                String targetId = result.target().resourceId().toString();

                boolean isBlacklisted;
                if (lookupType == LookupType.BIOME) {
                    // For biomes, check contiguity: sample biome along the line between
                    // blacklisted and found positions. If same biome the whole way, it's
                    // the same region — reject even if far apart.
                    isBlacklisted = isBiomeBlacklistedByContiguity(
                            level, player.getUUID(), targetId, result.target().resourceId(), finalDestination);
                } else {
                    isBlacklisted = BlacklistSyncManager.getInstance().isExcludedNearby(
                            player.getUUID(), targetId, finalDestination, STRUCTURE_EXCLUSION_THRESHOLD);
                }

                if (isBlacklisted) {
                    if (retryCount < MAX_BLACKLIST_RETRIES) {
                        // Retry with shifted origin — search past the blacklisted position
                        int shiftDistance = lookupType == LookupType.BIOME ? BIOME_SHIFT_DISTANCE : 500;
                        BlockPos shiftedOrigin = computeShiftedOrigin(origin, finalDestination, shiftDistance);
                        AromaAffect.LOGGER.debug("Blacklisted position found at {}, retrying with shifted origin {} (attempt {})",
                                finalDestination, shiftedOrigin, retryCount + 1);
                        LookupManager.getInstance().lookupAsync(level, shiftedOrigin, result.target(), -1, retryResult -> {
                            createPath(source, retryResult, origin, level, player, retryCount + 1);
                        });
                        return;
                    }
                    // Max retries — all nearby locations are blacklisted
                    if (verbose) {
                        source.sendFailure(Component.literal(
                                "§6[Aroma Affect] §cAll nearby locations are blacklisted"));
                    }
                    PathScentNetworking.sendPathNotFound(player, "All nearby locations are blacklisted");
                    return;
                }
            }

            if (verbose) {
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
            }

            // Create persistent path that follows terrain with undulations
            if (player != null && player.level() == level) {
                // Convert LookupType to ActivePathManager.TargetType
                ActivePathManager.TargetType targetType = convertLookupType(result.target().type());
                String targetId = result.target().resourceId().toString();

                ActivePathManager.getInstance().createPath(player, level, finalDestination, targetType, targetId);

                // Deduct per-target durability from equipped nose
                int targetCost = resolveTrackCost(result.target().type(), targetId);
                ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
                if (headStack.getItem() instanceof NoseItem) {
                    headStack.hurtAndBreak(targetCost, player, EquipmentSlot.HEAD);
                }

                // Consume required item (if any)
                consumeRequiredItem(player, result.target().type(), targetId);

                // Notify client that path was found (use original player origin for distance)
                int dist = (int) Math.sqrt(
                        Math.pow(origin.getX() - finalDestination.getX(), 2) +
                        Math.pow(origin.getZ() - finalDestination.getZ(), 2)
                );
                PathScentNetworking.sendPathFound(player, dist, finalDestination);
            } else if (player == null && verbose) {
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

            if (verbose) {
                source.sendFailure(Component.literal(
                        "§6[Aroma Affect] §c" + reason + ": §7" + result.target().resourceId()
                ));
            }

            // Notify client that search failed
            if (source.getEntity() instanceof ServerPlayer failedPlayer) {
                PathScentNetworking.sendPathNotFound(failedPlayer, reason);
            }
        }
    }

    /**
     * Computes a search origin that's past the blacklisted position,
     * so the next search finds a different target.
     *
     * @param shiftDistance how far (in blocks) past the blacklisted position to shift
     */
    private BlockPos computeShiftedOrigin(BlockPos playerOrigin, BlockPos blacklistedPos, int shiftDistance) {
        double dx = blacklistedPos.getX() - playerOrigin.getX();
        double dz = blacklistedPos.getZ() - playerOrigin.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 1.0) {
            // Player is at the blacklisted position, shift in arbitrary direction
            return new BlockPos(blacklistedPos.getX() + shiftDistance, playerOrigin.getY(), blacklistedPos.getZ());
        }

        // Shift past the blacklisted position in the same direction
        double nx = dx / dist;
        double nz = dz / dist;
        int shiftX = blacklistedPos.getX() + (int) (nx * shiftDistance);
        int shiftZ = blacklistedPos.getZ() + (int) (nz * shiftDistance);
        return new BlockPos(shiftX, playerOrigin.getY(), shiftZ);
    }

    /**
     * Checks whether a found biome position belongs to the same contiguous biome region
     * as any blacklisted position for that biome type.
     * <p>
     * Instead of a fixed-radius check, this samples the biome at intervals along the line
     * between each blacklisted position and the found position. If the biome is continuous
     * the entire way (no gap of a different biome), the two points are in the same region.
     */
    private boolean isBiomeBlacklistedByContiguity(ServerLevel level, java.util.UUID playerId,
                                                    String targetId, Identifier biomeId,
                                                    BlockPos foundPos) {
        Set<BlockPos> excluded = BlacklistSyncManager.getInstance()
                .getExcludedPositionsForTarget(playerId, targetId);
        if (excluded.isEmpty()) return false;

        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);

        for (BlockPos blacklistedPos : excluded) {
            if (isSameBiomeRegion(level, blacklistedPos, foundPos, biomeKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if two positions are in the same contiguous biome region by sampling
     * the biome along the straight line between them. If every sample point has the
     * target biome, the two points are considered part of the same region.
     */
    private boolean isSameBiomeRegion(ServerLevel level, BlockPos a, BlockPos b,
                                       ResourceKey<Biome> biomeKey) {
        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Very far apart — assume different regions (optimization)
        if (distance > BIOME_MAX_CONTIGUITY_DISTANCE) return false;

        // Sample biome along the line at intervals
        int samples = Math.max(2, (int) (distance / BIOME_SAMPLE_INTERVAL));
        int seaLevel = level.getSeaLevel();

        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            int x = (int) (a.getX() + dx * t);
            int z = (int) (a.getZ() + dz * t);
            BlockPos samplePos = new BlockPos(x, seaLevel, z);

            if (!level.getBiome(samplePos).is(biomeKey)) {
                return false; // Gap found — different biome region
            }
        }

        return true; // Continuous — same biome region
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

    // ── Per-target cost resolution ────────────────────────────────────────

    private int resolveTrackCost(LookupType type, String targetId) {
        return switch (type) {
            case BLOCK -> {
                BlockDefinition block = BlockDefinitionLoader.getBlockById(targetId);
                yield block != null ? block.getTrackCost() : 10;
            }
            case BIOME -> {
                BiomeDefinition biome = BiomeDefinitionLoader.getBiomeById(targetId);
                yield biome != null ? biome.getTrackCost() : 10;
            }
            case STRUCTURE -> {
                StructureDefinition structure = StructureDefinitionLoader.getStructureById(targetId);
                yield structure != null ? structure.getTrackCost() : 10;
            }
            case FLOWER -> {
                FlowerDefinition flower = FlowerDefinitionLoader.getFlowerById(targetId);
                yield flower != null ? flower.getTrackCost() : 10;
            }
        };
    }

    private RequiredItem resolveRequiredItem(LookupType type, String targetId) {
        return switch (type) {
            case BLOCK -> {
                BlockDefinition block = BlockDefinitionLoader.getBlockById(targetId);
                yield block != null ? block.getRequiredItem() : null;
            }
            case BIOME -> {
                BiomeDefinition biome = BiomeDefinitionLoader.getBiomeById(targetId);
                yield biome != null ? biome.getRequiredItem() : null;
            }
            case STRUCTURE -> {
                StructureDefinition structure = StructureDefinitionLoader.getStructureById(targetId);
                yield structure != null ? structure.getRequiredItem() : null;
            }
            case FLOWER -> {
                FlowerDefinition flower = FlowerDefinitionLoader.getFlowerById(targetId);
                yield flower != null ? flower.getRequiredItem() : null;
            }
        };
    }

    private boolean playerHasItem(ServerPlayer player, RequiredItem req) {
        if (req == null || req.getItemId() == null) return true;
        Identifier itemId = Identifier.parse(req.getItemId());
        var itemOpt = BuiltInRegistries.ITEM.get(itemId);
        if (itemOpt.isEmpty()) return false;

        int needed = req.getCount();
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(itemOpt.get())) {
                found += stack.getCount();
                if (found >= needed) return true;
            }
        }
        return false;
    }

    private void consumeRequiredItem(ServerPlayer player, LookupType type, String targetId) {
        RequiredItem req = resolveRequiredItem(type, targetId);
        if (req == null || req.getItemId() == null) return;

        Identifier itemId = Identifier.parse(req.getItemId());
        var itemOpt = BuiltInRegistries.ITEM.get(itemId);
        if (itemOpt.isEmpty()) return;

        int toRemove = req.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(itemOpt.get())) {
                int take = Math.min(stack.getCount(), toRemove);
                stack.shrink(take);
                toRemove -= take;
            }
        }
    }

    // ── Recall subcommand (history re-track) ──────────────────────────────

    private int executeRecall(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§6[Aroma Affect] §cThis command can only be executed by a player"));
            return 0;
        }

        Identifier targetId = IdentifierArgument.getId(context, "target_id");
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        BlockPos destination = new BlockPos(x, y, z);

        // Validate dimension if provided
        Identifier expectedDimension = null;
        try {
            expectedDimension = IdentifierArgument.getId(context, "dimension");
        } catch (IllegalArgumentException ignored) {
            // dimension argument not provided (legacy command format)
        }

        ServerLevel level = source.getLevel();

        if (expectedDimension != null) {
            String currentDimension = level.dimension().identifier().toString();
            if (!currentDimension.equals(expectedDimension.toString())) {
                PathScentNetworking.sendPathNotFound(player, "Wrong dimension");
                return Command.SINGLE_SUCCESS;
            }
        }

        // Determine target type by checking which registry contains the ID
        String idStr = targetId.toString();
        ActivePathManager.TargetType targetType;
        if (StructureDefinitionLoader.hasStructureId(idStr)) {
            targetType = ActivePathManager.TargetType.STRUCTURE;
        } else if (BiomeDefinitionLoader.hasBiomeId(idStr)) {
            targetType = ActivePathManager.TargetType.BIOME;
        } else {
            targetType = ActivePathManager.TargetType.BLOCK;
        }

        // Deduct reduced history retrack cost
        int retrackCost = TrackingConfig.getInstance().getHistoryRetrackCost();
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.getItem() instanceof NoseItem) {
            if (headStack.isDamageableItem()) {
                int remaining = headStack.getMaxDamage() - headStack.getDamageValue();
                if (remaining < retrackCost) {
                    PathScentNetworking.sendPathNotFound(player, "Not enough nose durability");
                    return Command.SINGLE_SUCCESS;
                }
            }
            headStack.hurtAndBreak(retrackCost, player, EquipmentSlot.HEAD);
        }

        // Create path directly to known coordinates (no search needed)
        ActivePathManager.getInstance().createPath(player, level, destination, targetType, idStr);

        // Notify client
        BlockPos origin = player.blockPosition();
        int dist = (int) Math.sqrt(
                Math.pow(origin.getX() - destination.getX(), 2) +
                Math.pow(origin.getZ() - destination.getZ(), 2)
        );
        PathScentNetworking.sendPathFound(player, dist, destination);

        if (verbose) {
            source.sendSuccess(() -> Component.literal("§6[Aroma Affect] §aRecalling path to known location!"), false);
            source.sendSuccess(() -> Component.literal(
                    String.format("§7  Position: §aX: %d§7, §aY: %d§7, §aZ: %d", x, y, z)
            ), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}