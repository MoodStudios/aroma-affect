package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.popupzone.TutorialPopupZone;
import com.ovrtechnology.tutorial.popupzone.TutorialPopupZoneManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;

/**
 * Subcommand for managing tutorial popup HUD zones.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial popup create <id> <text>} - Create a popup zone</li>
 *   <li>{@code /tutorial popup delete <id>} - Delete a popup zone</li>
 *   <li>{@code /tutorial popup corner <id> <1|2>} - Set area corner at player position</li>
 *   <li>{@code /tutorial popup text <id> <text>} - Change popup text</li>
 *   <li>{@code /tutorial popup list} - List all popup zones</li>
 *   <li>{@code /tutorial popup info <id>} - Show zone details</li>
 * </ul>
 */
public class PopupZoneSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> ZONE_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialPopupZoneManager.getAllZoneIds(level), builder);
    };

    @Override
    public String getName() {
        return "popup";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial popup HUD zones (informative text at top-left)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(this::executeCreate))))

                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeDelete)))

                .then(Commands.literal("corner")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .then(Commands.argument("corner", IntegerArgumentType.integer(1, 2))
                                        .executes(this::executeSetCorner))))

                .then(Commands.literal("text")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(this::executeSetText))))

                .then(Commands.literal("list")
                        .executes(this::executeList))

                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeInfo)))

                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPopup Zone commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial popup create <id> <text>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial popup delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial popup corner <id> <1|2>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial popup text <id> <new text>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial popup list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial popup info <id>"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String text = StringArgumentType.getString(context, "text");
        ServerLevel level = source.getLevel();

        if (TutorialPopupZoneManager.createZone(level, id, text)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fCreated popup zone \u00a7d" + id
            ), true);
            source.sendSuccess(() -> Component.literal(
                    "\u00a77  Text: \"" + text + "\""
            ), false);
            source.sendSuccess(() -> Component.literal(
                    "\u00a77  Set area corners: /tutorial popup corner " + id + " 1 (then 2)"
            ), false);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Popup zone '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialPopupZoneManager.deleteZone(level, id)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fDeleted popup zone \u00a7d" + id
            ), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Popup zone '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetCorner(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int corner = IntegerArgumentType.getInteger(context, "corner");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();

        if (TutorialPopupZoneManager.setCorner(level, id, corner, pos)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet popup zone \u00a7d" + id + " \u00a7fcorner \u00a7d" + corner
                            + " \u00a7fto \u00a7d" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
            ), true);

            Optional<TutorialPopupZone> zoneOpt = TutorialPopupZoneManager.getZone(level, id);
            if (zoneOpt.isPresent() && zoneOpt.get().isComplete()) {
                source.sendSuccess(() -> Component.literal("\u00a7a  \u2713 Popup zone complete and active!"), false);
            }
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Popup zone '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetText(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        String text = StringArgumentType.getString(context, "text");
        ServerLevel level = source.getLevel();

        if (TutorialPopupZoneManager.setText(level, id, text)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet popup zone \u00a7d" + id + " \u00a7ftext to: \"" + text + "\""
            ), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Popup zone '" + id + "' not found"));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        Set<String> ids = TutorialPopupZoneManager.getAllZoneIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No popup zones defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPopup Zones (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialPopupZone> zoneOpt = TutorialPopupZoneManager.getZone(level, id);
            if (zoneOpt.isPresent()) {
                TutorialPopupZone zone = zoneOpt.get();
                String status = zone.isComplete() ? "\u00a7a\u2713" : "\u00a7c\u2717";
                String shortText = zone.getText().length() > 30
                        ? zone.getText().substring(0, 30) + "..."
                        : zone.getText();
                source.sendSuccess(() -> Component.literal(
                        "\u00a77  " + status + " \u00a7e" + id + " \u00a77\"" + shortText + "\""
                ), false);
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialPopupZone> zoneOpt = TutorialPopupZoneManager.getZone(level, id);

        if (zoneOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Popup zone '" + id + "' not found"));
            return 0;
        }

        TutorialPopupZone zone = zoneOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPopup Zone: \u00a7d" + id), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Text: \u00a7f\"" + zone.getText() + "\""), false);

        if (zone.hasArea()) {
            BlockPos c1 = zone.getCorner1();
            BlockPos c2 = zone.getCorner2();
            source.sendSuccess(() -> Component.literal("\u00a77  Area: \u00a7aComplete"), false);
            source.sendSuccess(() -> Component.literal("\u00a77    Corner 1: \u00a7e" + c1.getX() + ", " + c1.getY() + ", " + c1.getZ()), false);
            source.sendSuccess(() -> Component.literal("\u00a77    Corner 2: \u00a7e" + c2.getX() + ", " + c2.getY() + ", " + c2.getZ()), false);
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Area: \u00a7cNot defined"), false);
        }

        source.sendSuccess(() -> Component.literal("\u00a77  Status: " + (zone.isComplete() ? "\u00a7aActive \u2713" : "\u00a7cIncomplete")), false);

        return Command.SINGLE_SUCCESS;
    }
}
