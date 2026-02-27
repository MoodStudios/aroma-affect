package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.network.TutorialChestNetworking;
import com.ovrtechnology.tutorial.chest.TutorialChest;
import com.ovrtechnology.tutorial.chest.TutorialChestManager;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Subcommand for managing tutorial chests.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial chest link <id>} - Link chest you're looking at</li>
 *   <li>{@code /tutorial chest delete <id>} - Delete a chest</li>
 *   <li>{@code /tutorial chest reward <id> add} - Add held item as reward</li>
 *   <li>{@code /tutorial chest reward <id> clear} - Clear all rewards</li>
 *   <li>{@code /tutorial chest activate <id> waypoint <waypointId>} - Activate waypoint on open</li>
 *   <li>{@code /tutorial chest activate <id> cinematic <cinematicId>} - Activate cinematic on open</li>
 *   <li>{@code /tutorial chest activate <id> clear} - Clear activations</li>
 *   <li>{@code /tutorial chest list} - List all chests</li>
 *   <li>{@code /tutorial chest info <id>} - Show chest details</li>
 *   <li>{@code /tutorial chest reset <id>} - Reset chest to unconsumed</li>
 *   <li>{@code /tutorial chest sync} - Sync chests to all players</li>
 * </ul>
 */
public class ChestSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> CHEST_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialChestManager.getAllChestIds(level), builder);
    };

    @Override
    public String getName() {
        return "chest";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial chests (reward containers with particles)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial chest link <id> - Link chest you're looking at
                .then(Commands.literal("link")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeLink)
                        )
                )

                // /tutorial chest delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHEST_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial chest reward <id> add|clear
                .then(Commands.literal("reward")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHEST_SUGGESTIONS)
                                .then(Commands.literal("add")
                                        .executes(this::executeRewardAdd)
                                )
                                .then(Commands.literal("clear")
                                        .executes(this::executeRewardClear)
                                )
                        )
                )

                // /tutorial chest activate <id> ...
                .then(Commands.literal("activate")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHEST_SUGGESTIONS)
                                .then(Commands.literal("waypoint")
                                        .then(Commands.argument("waypointId", StringArgumentType.word())
                                                .executes(this::executeActivateWaypoint)
                                        )
                                )
                                .then(Commands.literal("cinematic")
                                        .then(Commands.argument("cinematicId", StringArgumentType.word())
                                                .executes(this::executeActivateCinematic)
                                        )
                                )
                                .then(Commands.literal("clear")
                                        .executes(this::executeActivateClear)
                                )
                        )
                )

                // /tutorial chest list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial chest info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHEST_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // /tutorial chest reset <id>
                .then(Commands.literal("reset")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CHEST_SUGGESTIONS)
                                .executes(this::executeReset)
                        )
                )

                // /tutorial chest sync
                .then(Commands.literal("sync")
                        .executes(this::executeSync)
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fChest commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest link <id> \u00a78- Look at a chest to link it"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest reward <id> add"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest reward <id> clear"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest activate <id> waypoint <waypointId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest activate <id> cinematic <cinematicId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest activate <id> clear"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest info <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest reset <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial chest sync"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Chests give items and activate waypoints/cinematics when opened."), false);
        source.sendSuccess(() -> Component.literal("\u00a77Each chest is independent, not part of any chain."), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeLink(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        // Raycast to find the block the player is looking at
        double reachDistance = 5.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookDir.scale(reachDistance));

        BlockHitResult hitResult = level.clip(new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] You must be looking at a block"));
            return 0;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        // Check if it's a chest-like block
        if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) &&
            !state.is(Blocks.BARREL) && !state.is(Blocks.ENDER_CHEST)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] You must be looking at a chest, barrel, or ender chest"));
            return 0;
        }

        if (TutorialChestManager.createChest(level, id, pos)) {
            String blockName = state.getBlock().getName().getString();
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fLinked " + blockName + " as \u00a7d" + id + " \u00a7fat " +
                            pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Add rewards with \u00a7e/tutorial chest reward " + id + " add"),
                    false
            );

            // Sync to all players
            syncToAllPlayers(source);

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialChestManager.deleteChest(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted chest \u00a7d" + id),
                    true
            );

            // Sync to all players
            syncToAllPlayers(source);

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }
    }

    private int executeRewardAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] You must hold an item to add as reward"));
            return 0;
        }

        Optional<TutorialChest> chestOpt = TutorialChestManager.getChest(level, id);
        if (chestOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }

        if (TutorialChestManager.addReward(level, id, heldItem)) {
            String itemName = heldItem.getHoverName().getString();
            int count = heldItem.getCount();
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fAdded reward to chest \u00a7d" + id + "\u00a7f: \u00a7e" +
                            count + "x " + itemName),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Failed to add reward"));
            return 0;
        }
    }

    private int executeRewardClear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialChestManager.clearRewards(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared all rewards from chest \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }
    }

    private int executeActivateWaypoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String waypointId = StringArgumentType.getString(context, "waypointId");
        ServerLevel level = source.getLevel();

        if (TutorialChestManager.setActivateWaypoint(level, id, waypointId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fChest \u00a7d" + id + " \u00a7fwill activate waypoint \u00a7e" + waypointId + " \u00a7fwhen opened"),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }
    }

    private int executeActivateCinematic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String cinematicId = StringArgumentType.getString(context, "cinematicId");
        ServerLevel level = source.getLevel();

        if (TutorialChestManager.setActivateCinematic(level, id, cinematicId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fChest \u00a7d" + id + " \u00a7fwill activate cinematic \u00a7d" + cinematicId + " \u00a7fwhen opened"),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }
    }

    private int executeActivateClear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        boolean clearedWp = TutorialChestManager.setActivateWaypoint(level, id, null);
        boolean clearedCin = TutorialChestManager.setActivateCinematic(level, id, null);

        if (clearedWp || clearedCin) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared activations for chest \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialChestManager.getAllChestIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No chests defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fChests (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialChest> chestOpt = TutorialChestManager.getChest(level, id);
            if (chestOpt.isPresent()) {
                TutorialChest c = chestOpt.get();
                String status = c.isConsumed() ? "\u00a7c\u2717" : "\u00a7a\u2713";
                BlockPos pos = c.getPosition();
                int rewardCount = c.getRewardCount();

                source.sendSuccess(
                        () -> Component.literal("\u00a77  " + status + " \u00a7e" + id +
                                " \u00a77at " + pos.getX() + "," + pos.getY() + "," + pos.getZ() +
                                " (" + rewardCount + " rewards)"),
                        false
                );
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialChest> chestOpt = TutorialChestManager.getChest(level, id);

        if (chestOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }

        TutorialChest c = chestOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fChest: \u00a7d" + id), false);

        // Position
        BlockPos pos = c.getPosition();
        source.sendSuccess(
                () -> Component.literal("\u00a77  Position: \u00a7e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                false
        );

        // Status
        String status = c.isConsumed() ? "\u00a7cConsumed" : "\u00a7aAvailable";
        source.sendSuccess(
                () -> Component.literal("\u00a77  Status: " + status),
                false
        );

        // Rewards
        if (c.hasRewards()) {
            source.sendSuccess(() -> Component.literal("\u00a77  Rewards (" + c.getRewardCount() + "):"), false);
            for (ItemStack reward : c.getRewards()) {
                String itemName = reward.getHoverName().getString();
                int count = reward.getCount();
                source.sendSuccess(
                        () -> Component.literal("\u00a77    - \u00a7e" + count + "x " + itemName),
                        false
                );
            }
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Rewards: \u00a77None"), false);
        }

        // Activations
        if (c.hasActivateWaypoint()) {
            String wpId = c.getActivateWaypointId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Activate waypoint: \u00a7a" + wpId),
                    false
            );
        }
        if (c.hasActivateCinematic()) {
            String cinId = c.getActivateCinematicId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Activate cinematic: \u00a7d" + cinId),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeReset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialChestManager.resetChest(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fReset chest \u00a7d" + id + " \u00a7fto unconsumed state"),
                    true
            );

            // Sync to all players
            syncToAllPlayers(source);

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Chest '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSync(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        syncToAllPlayers(source);
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSynced chest positions to all players"),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private void syncToAllPlayers(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        List<TutorialChest> unconsumed = TutorialChestManager.getUnconsumedChests(level);

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            TutorialChestNetworking.sendChestsToPlayer(player, unconsumed);
        }
    }
}
