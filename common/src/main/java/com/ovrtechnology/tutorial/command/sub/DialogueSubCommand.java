package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.dialogue.TutorialDialogue;
import com.ovrtechnology.tutorial.dialogue.TutorialDialogueManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Subcommand for managing tutorial dialogues.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial dialogue create <id> <text...>} - Create dialogue</li>
 *   <li>{@code /tutorial dialogue delete <id>} - Delete dialogue</li>
 *   <li>{@code /tutorial dialogue text <id> <text...>} - Update text</li>
 *   <li>{@code /tutorial dialogue oncomplete <id> waypoint|cinematic|animation|oliver <value>} - Set hooks</li>
 *   <li>{@code /tutorial dialogue oncomplete <id> clear} - Clear hooks</li>
 *   <li>{@code /tutorial dialogue info <id>} - Show info</li>
 *   <li>{@code /tutorial dialogue list} - List all</li>
 * </ul>
 */
public class DialogueSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> DIALOGUE_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialDialogueManager.getAllDialogueIds(level), builder);
    };

    @Override
    public String getName() {
        return "dialogue";
    }

    @Override
    public String getDescription() {
        return "Manage custom tutorial dialogues for Oliver";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial dialogue create <id> <text...>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(this::executeCreate)
                                )
                        )
                )

                // /tutorial dialogue delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(DIALOGUE_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial dialogue text <id> <text...>
                .then(Commands.literal("text")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(DIALOGUE_SUGGESTIONS)
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(this::executeSetText)
                                )
                        )
                )

                // /tutorial dialogue oncomplete <id> ...
                .then(Commands.literal("oncomplete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(DIALOGUE_SUGGESTIONS)
                                .then(Commands.literal("waypoint")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(this::executeOnCompleteWaypoint)
                                        )
                                )
                                .then(Commands.literal("cinematic")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(this::executeOnCompleteCinematic)
                                        )
                                )
                                .then(Commands.literal("animation")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(this::executeOnCompleteAnimation)
                                        )
                                )
                                .then(Commands.literal("oliver")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(this::executeOnCompleteOliver)
                                        )
                                )
                                .then(Commands.literal("clear")
                                        .executes(this::executeOnCompleteClear)
                                )
                        )
                )

                // /tutorial dialogue info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(DIALOGUE_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // /tutorial dialogue list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDialogue commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue create <id> <text...>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue text <id> <text...>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue oncomplete <id> waypoint <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue oncomplete <id> cinematic <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue oncomplete <id> animation <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue oncomplete <id> oliver <action>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue oncomplete <id> clear"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue info <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial dialogue list"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Custom dialogues override hardcoded text for Oliver."), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String text = StringArgumentType.getString(context, "text");
        ServerLevel level = source.getLevel();

        if (TutorialDialogueManager.createDialogue(level, id, text)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated dialogue \u00a7d" + id),
                    true
            );

            // Sync to all players
            TutorialDialogueContentNetworking.syncToAllPlayers(level);

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialDialogueManager.deleteDialogue(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted dialogue \u00a7d" + id),
                    true
            );

            TutorialDialogueContentNetworking.syncToAllPlayers(level);

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetText(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String text = StringArgumentType.getString(context, "text");
        ServerLevel level = source.getLevel();

        if (TutorialDialogueManager.setText(level, id, text)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fUpdated dialogue \u00a7d" + id + " \u00a7ftext"),
                    true
            );

            TutorialDialogueContentNetworking.syncToAllPlayers(level);

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }
    }

    private int executeOnCompleteWaypoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String value = StringArgumentType.getString(context, "value");
        ServerLevel level = source.getLevel();

        if (TutorialDialogueManager.setOnCompleteWaypoint(level, id, value)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet dialogue \u00a7d" + id + " \u00a7fon-complete waypoint: \u00a7e" + value),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }
    }

    private int executeOnCompleteCinematic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String value = StringArgumentType.getString(context, "value");
        ServerLevel level = source.getLevel();

        if (TutorialDialogueManager.setOnCompleteCinematic(level, id, value)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet dialogue \u00a7d" + id + " \u00a7fon-complete cinematic: \u00a7e" + value),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }
    }

    private int executeOnCompleteAnimation(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String value = StringArgumentType.getString(context, "value");
        ServerLevel level = source.getLevel();

        if (TutorialDialogueManager.setOnCompleteAnimation(level, id, value)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet dialogue \u00a7d" + id + " \u00a7fon-complete animation: \u00a7e" + value),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }
    }

    private int executeOnCompleteOliver(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String value = StringArgumentType.getString(context, "value");
        ServerLevel level = source.getLevel();

        if (TutorialDialogueManager.setOnCompleteOliverAction(level, id, value)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet dialogue \u00a7d" + id + " \u00a7fon-complete oliver: \u00a7e" + value),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }
    }

    private int executeOnCompleteClear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        boolean cleared = TutorialDialogueManager.setOnCompleteWaypoint(level, id, null);
        cleared |= TutorialDialogueManager.setOnCompleteCinematic(level, id, null);
        cleared |= TutorialDialogueManager.setOnCompleteAnimation(level, id, null);
        cleared |= TutorialDialogueManager.setOnCompleteOliverAction(level, id, null);

        if (cleared) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCleared on-complete actions for dialogue \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialDialogue> dialogueOpt = TutorialDialogueManager.getDialogue(level, id);
        if (dialogueOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Dialogue '" + id + "' not found"));
            return 0;
        }

        TutorialDialogue d = dialogueOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDialogue: \u00a7d" + id), false);

        // Text (truncated for display)
        String displayText = d.getText();
        if (displayText.length() > 80) {
            displayText = displayText.substring(0, 80) + "...";
        }
        String finalText = displayText;
        source.sendSuccess(
                () -> Component.literal("\u00a77  Text: \u00a7f\"" + finalText + "\""),
                false
        );

        // On-complete hooks
        if (d.hasOnCompleteWaypoint()) {
            String wpId = d.getOnCompleteWaypointId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete waypoint: \u00a7a" + wpId),
                    false
            );
        }
        if (d.hasOnCompleteCinematic()) {
            String cinId = d.getOnCompleteCinematicId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete cinematic: \u00a7d" + cinId),
                    false
            );
        }
        if (d.hasOnCompleteAnimation()) {
            String animId = d.getOnCompleteAnimationId();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete animation: \u00a7e" + animId),
                    false
            );
        }
        if (d.hasOnCompleteOliverAction()) {
            String action = d.getOnCompleteOliverAction();
            source.sendSuccess(
                    () -> Component.literal("\u00a77  On complete oliver: \u00a7e" + action),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialDialogueManager.getAllDialogueIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No custom dialogues defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDialogues (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialDialogue> dialogueOpt = TutorialDialogueManager.getDialogue(level, id);
            if (dialogueOpt.isPresent()) {
                TutorialDialogue d = dialogueOpt.get();
                String textPreview = d.getText();
                if (textPreview.length() > 40) {
                    textPreview = textPreview.substring(0, 40) + "...";
                }
                String finalPreview = textPreview;
                source.sendSuccess(
                        () -> Component.literal("\u00a77  \u00a7e" + id + " \u00a77- \"" + finalPreview + "\""),
                        false
                );
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}
