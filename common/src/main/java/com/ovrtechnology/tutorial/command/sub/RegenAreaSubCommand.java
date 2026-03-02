package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenArea;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Subcommand for managing tutorial block regeneration areas.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial regenarea create <id>} - Create regen area</li>
 *   <li>{@code /tutorial regenarea delete <id>} - Delete regen area</li>
 *   <li>{@code /tutorial regenarea corner1 <id>} - Set corner1 to player pos</li>
 *   <li>{@code /tutorial regenarea corner2 <id>} - Set corner2 to player pos</li>
 *   <li>{@code /tutorial regenarea delay <id> <seconds>} - Set regen delay</li>
 *   <li>{@code /tutorial regenarea enable <id>} - Enable regen area</li>
 *   <li>{@code /tutorial regenarea disable <id>} - Disable regen area</li>
 *   <li>{@code /tutorial regenarea snapshot <id>} - Save current blocks</li>
 *   <li>{@code /tutorial regenarea restore <id>} - Restore all blocks</li>
 *   <li>{@code /tutorial regenarea list} - List all regen areas</li>
 *   <li>{@code /tutorial regenarea info <id>} - Show details</li>
 * </ul>
 */
public class RegenAreaSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> AREA_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialRegenAreaManager.getAllRegenAreaIds(level), builder);
    };

    @Override
    public String getName() {
        return "regenarea";
    }

    @Override
    public String getDescription() {
        return "Manage block regeneration areas (auto-respawn broken blocks)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial regenarea create <id>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeCreate)
                        )
                )

                // /tutorial regenarea delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial regenarea corner1 <id>
                .then(Commands.literal("corner1")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeCorner1)
                        )
                )

                // /tutorial regenarea corner2 <id>
                .then(Commands.literal("corner2")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeCorner2)
                        )
                )

                // /tutorial regenarea delay <id> <seconds>
                .then(Commands.literal("delay")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 300))
                                        .executes(this::executeSetDelay)
                                )
                        )
                )

                // /tutorial regenarea enable <id>
                .then(Commands.literal("enable")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeEnable)
                        )
                )

                // /tutorial regenarea disable <id>
                .then(Commands.literal("disable")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeDisable)
                        )
                )

                // /tutorial regenarea snapshot <id>
                .then(Commands.literal("snapshot")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeSnapshot)
                        )
                )

                // /tutorial regenarea restore <id>
                .then(Commands.literal("restore")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeRestore)
                        )
                )

                // /tutorial regenarea list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial regenarea info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(AREA_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // /tutorial regenarea pending
                .then(Commands.literal("pending")
                        .executes(this::executePending)
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRegen Area commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea create <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea corner1 <id> \u00a78- Set to your position"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea corner2 <id> \u00a78- Set to your position"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea delay <id> <seconds> \u00a78- Regen delay (1-300s)"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea enable <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea disable <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea snapshot <id> \u00a78- Save current blocks"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea restore <id> \u00a78- Restore all blocks"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea info <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial regenarea pending \u00a78- Show pending regens"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialRegenAreaManager.createRegenArea(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated regen area \u00a7d" + id),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Set corners with \u00a7e/tutorial regenarea corner1 " + id +
                            " \u00a77and \u00a7ecorner2"),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Then run \u00a7e/tutorial regenarea snapshot " + id +
                            " \u00a77to save blocks"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' already exists"
            ));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialRegenAreaManager.deleteRegenArea(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted regen area \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeCorner1(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] This command must be executed by a player"
            ));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        if (TutorialRegenAreaManager.setCorner1(level, id, pos)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet corner1 of \u00a7d" + id +
                            " \u00a7fto \u00a7e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeCorner2(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] This command must be executed by a player"
            ));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        if (TutorialRegenAreaManager.setCorner2(level, id, pos)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet corner2 of \u00a7d" + id +
                            " \u00a7fto \u00a7e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeSetDelay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        ServerLevel level = source.getLevel();

        int ticks = seconds * 20;
        if (TutorialRegenAreaManager.setRegenDelay(level, id, ticks)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet regen delay of \u00a7d" + id +
                            " \u00a7fto \u00a7e" + seconds + " seconds"),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeEnable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialRegenAreaManager.setEnabled(level, id, true)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fEnabled regen area \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeDisable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialRegenAreaManager.setEnabled(level, id, false)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDisabled regen area \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }
    }

    private int executeSnapshot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialRegenArea> areaOpt = TutorialRegenAreaManager.getRegenArea(level, id);
        if (areaOpt.isEmpty()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }

        TutorialRegenArea area = areaOpt.get();
        if (!area.isComplete()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' needs both corners set first"
            ));
            return 0;
        }

        if (TutorialRegenAreaManager.snapshotBlocks(level, id)) {
            int blockCount = area.getSavedBlocks().size();
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSnapshotted \u00a7e" + blockCount +
                            " \u00a7fblocks for regen area \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Failed to snapshot blocks"
            ));
            return 0;
        }
    }

    private int executeRestore(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        int restored = TutorialRegenAreaManager.restoreAllBlocks(level, id);
        if (restored > 0) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRestored \u00a7e" + restored +
                            " \u00a7fblocks for regen area \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No blocks to restore for regen area \u00a7d" + id),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialRegenAreaManager.getAllRegenAreaIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No regen areas defined"),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRegen Areas (" + ids.size() + "):"),
                false
        );

        for (String areaId : ids) {
            Optional<TutorialRegenArea> areaOpt = TutorialRegenAreaManager.getRegenArea(level, areaId);
            if (areaOpt.isPresent()) {
                TutorialRegenArea a = areaOpt.get();
                String status = a.isEnabled() ? "\u00a7a\u2713" : "\u00a7c\u2717";
                String complete = a.isComplete() ? "\u00a7aready" : "\u00a7cincomplete";
                int savedCount = a.getSavedBlocks().size();
                float delaySeconds = a.getRegenDelaySeconds();

                source.sendSuccess(
                        () -> Component.literal("\u00a77  " + status + " \u00a7e" + areaId +
                                " \u00a77(" + complete + "\u00a77, " + savedCount + " blocks, " +
                                String.format("%.1fs", delaySeconds) + " delay)"),
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

        Optional<TutorialRegenArea> areaOpt = TutorialRegenAreaManager.getRegenArea(level, id);

        if (areaOpt.isEmpty()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] Regen area '" + id + "' not found"
            ));
            return 0;
        }

        TutorialRegenArea a = areaOpt.get();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRegen Area: \u00a7d" + id),
                false
        );

        // Status
        String status = a.isEnabled() ? "\u00a7aEnabled" : "\u00a7cDisabled";
        source.sendSuccess(
                () -> Component.literal("\u00a77  Status: " + status),
                false
        );

        // Delay
        float delaySeconds = a.getRegenDelaySeconds();
        source.sendSuccess(
                () -> Component.literal("\u00a77  Regen delay: \u00a7e" + String.format("%.1f", delaySeconds) + "s"),
                false
        );

        // Corner1
        if (a.getCorner1() != null) {
            BlockPos c1 = a.getCorner1();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner1: \u00a7e" +
                            c1.getX() + ", " + c1.getY() + ", " + c1.getZ()),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner1: \u00a7cNot set"),
                    false
            );
        }

        // Corner2
        if (a.getCorner2() != null) {
            BlockPos c2 = a.getCorner2();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner2: \u00a7e" +
                            c2.getX() + ", " + c2.getY() + ", " + c2.getZ()),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Corner2: \u00a7cNot set"),
                    false
            );
        }

        // Dimensions
        if (a.isComplete()) {
            BlockPos c1 = a.getCorner1();
            BlockPos c2 = a.getCorner2();
            int sizeX = Math.abs(c2.getX() - c1.getX()) + 1;
            int sizeY = Math.abs(c2.getY() - c1.getY()) + 1;
            int sizeZ = Math.abs(c2.getZ() - c1.getZ()) + 1;
            int totalBlocks = sizeX * sizeY * sizeZ;
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Size: \u00a7e" + sizeX + "x" + sizeY + "x" + sizeZ +
                            " \u00a77(" + totalBlocks + " blocks)"),
                    false
            );
        }

        // Saved blocks
        int savedCount = a.getSavedBlocks().size();
        source.sendSuccess(
                () -> Component.literal("\u00a77  Saved blocks: \u00a7e" + savedCount),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executePending(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int pending = TutorialRegenAreaHandler.getPendingCount();

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPending block regenerations: \u00a7e" + pending),
                false
        );

        return Command.SINGLE_SUCCESS;
    }
}
