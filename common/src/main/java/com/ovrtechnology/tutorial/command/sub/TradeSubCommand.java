package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.trade.TutorialTrade;
import com.ovrtechnology.tutorial.trade.TutorialTradeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Subcommand for managing tutorial trades.
 * <p>
 * Trades support multiple input items and a single output item.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial trade create <id>} - Create empty trade</li>
 *   <li>{@code /tutorial trade delete <id>} - Delete trade</li>
 *   <li>{@code /tutorial trade addinput <id>} - Add held item as input requirement</li>
 *   <li>{@code /tutorial trade removeinput <id> <itemId>} - Remove an input item</li>
 *   <li>{@code /tutorial trade clearinputs <id>} - Clear all inputs</li>
 *   <li>{@code /tutorial trade output <id>} - Set output from held item</li>
 *   <li>{@code /tutorial trade info <id>} - Show trade details</li>
 *   <li>{@code /tutorial trade list} - List all trades</li>
 * </ul>
 */
public class TradeSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> TRADE_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialTradeManager.getAllTradeIds(level), builder);
    };

    @Override
    public String getName() {
        return "trade";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial trades for Oliver";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial trade create <id>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeCreate)
                        )
                )

                // /tutorial trade delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRADE_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial trade addinput <id> - add held item as input
                .then(Commands.literal("addinput")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRADE_SUGGESTIONS)
                                .executes(this::executeAddInput)
                        )
                )

                // /tutorial trade removeinput <id> <itemId>
                .then(Commands.literal("removeinput")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRADE_SUGGESTIONS)
                                .then(Commands.argument("itemId", StringArgumentType.word())
                                        .executes(this::executeRemoveInput)
                                )
                        )
                )

                // /tutorial trade clearinputs <id>
                .then(Commands.literal("clearinputs")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRADE_SUGGESTIONS)
                                .executes(this::executeClearInputs)
                        )
                )

                // /tutorial trade output <id>
                .then(Commands.literal("output")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRADE_SUGGESTIONS)
                                .executes(this::executeOutput)
                        )
                )

                // /tutorial trade info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRADE_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // /tutorial trade list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial trade oncomplete <id> waypoint <waypointId>
                .then(Commands.literal("oncomplete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRADE_SUGGESTIONS)
                                .then(Commands.literal("waypoint")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> executeSetOnComplete(ctx, "waypoint"))
                                        )
                                )
                                .then(Commands.literal("cinematic")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> executeSetOnComplete(ctx, "cinematic"))
                                        )
                                )
                                .then(Commands.literal("animation")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> executeSetOnComplete(ctx, "animation"))
                                        )
                                )
                                .then(Commands.literal("oliver")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> executeSetOnComplete(ctx, "oliver"))
                                        )
                                )
                                .then(Commands.literal("clear")
                                        .executes(this::executeClearOnComplete)
                                )
                        )
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fTrade commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade create <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade addinput <id> \u00a78- Hold item to add as required input"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade removeinput <id> <itemId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade clearinputs <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade output <id> \u00a78- Hold item to set as output"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade info <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade oncomplete <id> waypoint|cinematic|animation|oliver <value>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial trade oncomplete <id> clear"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Trades let Oliver exchange items with players."), false);
        source.sendSuccess(() -> Component.literal("\u00a77Multiple input items can be required per trade."), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialTradeManager.createTrade(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated trade \u00a7d" + id),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Add inputs with \u00a7e/tutorial trade addinput " + id),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialTradeManager.deleteTrade(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted trade \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' not found"));
            return 0;
        }
    }

    private int executeAddInput(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] You must hold an item to add as trade input"));
            return 0;
        }

        if (TutorialTradeManager.addInput(level, id, heldItem)) {
            String itemName = heldItem.getHoverName().getString();
            int count = heldItem.getCount();
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fAdded input to trade \u00a7d" + id + "\u00a7f: \u00a7e" +
                            count + "x " + itemName),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' not found"));
            return 0;
        }
    }

    private int executeRemoveInput(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String itemId = StringArgumentType.getString(context, "itemId");
        ServerLevel level = source.getLevel();

        if (TutorialTradeManager.removeInput(level, id, itemId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRemoved input \u00a7e" + itemId + " \u00a7ffrom trade \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade or input not found"));
            return 0;
        }
    }

    private int executeClearInputs(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialTradeManager.clearInputs(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared all inputs from trade \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' not found"));
            return 0;
        }
    }

    private int executeOutput(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command must be executed by a player"));
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] You must hold an item to set as trade output"));
            return 0;
        }

        if (TutorialTradeManager.setOutput(level, id, heldItem)) {
            String itemName = heldItem.getHoverName().getString();
            int count = heldItem.getCount();
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet trade \u00a7d" + id + " \u00a7foutput: \u00a7e" +
                            count + "x " + itemName),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' not found"));
            return 0;
        }
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialTrade> tradeOpt = TutorialTradeManager.getTrade(level, id);
        if (tradeOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' not found"));
            return 0;
        }

        TutorialTrade t = tradeOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fTrade: \u00a7d" + id), false);

        // Status
        String status = t.isComplete() ? "\u00a7aReady" : "\u00a7cIncomplete";
        source.sendSuccess(
                () -> Component.literal("\u00a77  Status: " + status),
                false
        );

        // Inputs
        if (t.hasInputs()) {
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Inputs (" + t.getInputs().size() + "):"),
                    false
            );
            for (TutorialTrade.InputEntry entry : t.getInputs()) {
                source.sendSuccess(
                        () -> Component.literal("\u00a77    - \u00a7e" + entry.count() + "x " + entry.itemId()),
                        false
                );
            }
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Inputs: \u00a77None"), false);
        }

        // Output
        if (t.hasOutput()) {
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Output: \u00a7e" + t.getOutputCount() + "x " + t.getOutputItemId()),
                    false
            );
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Output: \u00a77Not set"), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialTradeManager.getAllTradeIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No trades defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fTrades (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialTrade> tradeOpt = TutorialTradeManager.getTrade(level, id);
            if (tradeOpt.isPresent()) {
                TutorialTrade t = tradeOpt.get();
                String statusIcon = t.isComplete() ? "\u00a7a\u2713" : "\u00a7c\u2717";
                int inputCount = t.getInputs().size();
                source.sendSuccess(
                        () -> Component.literal("\u00a77  " + statusIcon + " \u00a7e" + id +
                                " \u00a77(" + inputCount + " input" + (inputCount != 1 ? "s" : "") + ")"),
                        false
                );
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeSetOnComplete(CommandContext<CommandSourceStack> context, String type) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String value = StringArgumentType.getString(context, "value");
        ServerLevel level = source.getLevel();

        boolean success = switch (type) {
            case "waypoint" -> TutorialTradeManager.setOnCompleteWaypoint(level, id, value);
            case "cinematic" -> TutorialTradeManager.setOnCompleteCinematic(level, id, value);
            case "animation" -> TutorialTradeManager.setOnCompleteAnimation(level, id, value);
            case "oliver" -> TutorialTradeManager.setOnCompleteOliverAction(level, id, value);
            default -> false;
        };

        if (success) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fTrade \u00a7d" + id + " \u00a7fonComplete " + type + " set to \u00a7e" + value),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' not found"));
            return 0;
        }
    }

    private int executeClearOnComplete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialTradeManager.clearOnComplete(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared all onComplete hooks from trade \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trade '" + id + "' not found"));
            return 0;
        }
    }
}
