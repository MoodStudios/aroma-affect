package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.noseequip.TutorialNoseEquipHandler;
import com.ovrtechnology.tutorial.noseequip.TutorialNoseEquipTrigger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand for managing nose equip triggers.
 * <p>
 * When a player equips a specific nose, configured actions fire automatically.
 */
public class NoseEquipSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> TRIGGER_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialNoseEquipTrigger.getAllTriggerNoseIds(level), builder);
    };

    @Override
    public String getName() {
        return "noseequip";
    }

    @Override
    public String getDescription() {
        return "Manage nose equip triggers";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial noseequip add <noseId>
                .then(Commands.literal("add")
                        .then(Commands.argument("noseId", StringArgumentType.string())
                                .executes(this::executeAdd)
                        )
                )

                // /tutorial noseequip remove <noseId>
                .then(Commands.literal("remove")
                        .then(Commands.argument("noseId", StringArgumentType.string())
                                .suggests(TRIGGER_SUGGESTIONS)
                                .executes(this::executeRemove)
                        )
                )

                // /tutorial noseequip set <noseId> waypoint|cinematic|animation|oliver <value>
                .then(Commands.literal("set")
                        .then(Commands.argument("noseId", StringArgumentType.string())
                                .suggests(TRIGGER_SUGGESTIONS)
                                .then(Commands.literal("waypoint")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> executeSet(ctx, "waypoint"))
                                        )
                                )
                                .then(Commands.literal("cinematic")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> executeSet(ctx, "cinematic"))
                                        )
                                )
                                .then(Commands.literal("animation")
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(ctx -> executeSet(ctx, "animation"))
                                        )
                                )
                                .then(Commands.literal("oliver")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> executeSet(ctx, "oliver"))
                                        )
                                )
                        )
                )

                // /tutorial noseequip info <noseId>
                .then(Commands.literal("info")
                        .then(Commands.argument("noseId", StringArgumentType.string())
                                .suggests(TRIGGER_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // /tutorial noseequip list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial noseequip trigger <noseId> - manually fire a trigger (for testing)
                .then(Commands.literal("trigger")
                        .then(Commands.argument("noseId", StringArgumentType.string())
                                .suggests(TRIGGER_SUGGESTIONS)
                                .executes(this::executeTrigger)
                        )
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose equip trigger commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial noseequip add <noseId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial noseequip remove <noseId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial noseequip set <noseId> waypoint|cinematic|animation|oliver <value>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial noseequip info <noseId>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial noseequip list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial noseequip trigger <noseId> \u00a78(manual test)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Triggers fire when a player equips a specific nose."), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String noseId = StringArgumentType.getString(context, "noseId");
        ServerLevel level = source.getLevel();

        if (TutorialNoseEquipTrigger.addTrigger(level, noseId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated nose equip trigger for \u00a7d" + noseId),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Configure with /tutorial noseequip set " + noseId + " ..."),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Trigger for '" + noseId + "' already exists"));
            return 0;
        }
    }

    private int executeRemove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String noseId = StringArgumentType.getString(context, "noseId");
        ServerLevel level = source.getLevel();

        if (TutorialNoseEquipTrigger.removeTrigger(level, noseId)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fRemoved nose equip trigger for \u00a7d" + noseId),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No trigger for '" + noseId + "'"));
            return 0;
        }
    }

    private int executeSet(CommandContext<CommandSourceStack> context, String type) {
        CommandSourceStack source = context.getSource();
        String noseId = StringArgumentType.getString(context, "noseId");
        String value = StringArgumentType.getString(context, "value");
        ServerLevel level = source.getLevel();

        boolean success = switch (type) {
            case "waypoint" -> TutorialNoseEquipTrigger.setWaypoint(level, noseId, value);
            case "cinematic" -> TutorialNoseEquipTrigger.setCinematic(level, noseId, value);
            case "animation" -> TutorialNoseEquipTrigger.setAnimation(level, noseId, value);
            case "oliver" -> TutorialNoseEquipTrigger.setOliverAction(level, noseId, value);
            default -> false;
        };

        if (success) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose \u00a7d" + noseId + " \u00a7f" + type + " set to \u00a7e" + value),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No trigger for '" + noseId + "'. Create it first with /tutorial noseequip add " + noseId));
            return 0;
        }
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String noseId = StringArgumentType.getString(context, "noseId");
        ServerLevel level = source.getLevel();

        var triggerOpt = TutorialNoseEquipTrigger.getTrigger(level, noseId);
        if (triggerOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No trigger for '" + noseId + "'"));
            return 0;
        }

        var t = triggerOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose equip trigger: \u00a7d" + noseId), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Waypoint: " + (t.hasWaypoint() ? "\u00a7e" + t.onCompleteWaypointId() : "\u00a77None")), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Cinematic: " + (t.hasCinematic() ? "\u00a7e" + t.onCompleteCinematicId() : "\u00a77None")), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Animation: " + (t.hasAnimation() ? "\u00a7e" + t.onCompleteAnimationId() : "\u00a77None")), false);
        source.sendSuccess(() -> Component.literal("\u00a77  Oliver: " + (t.hasOliverAction() ? "\u00a7e" + t.onCompleteOliverAction() : "\u00a77None")), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeTrigger(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String noseId = StringArgumentType.getString(context, "noseId");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Must be executed by a player"));
            return 0;
        }

        var triggerOpt = TutorialNoseEquipTrigger.getTrigger(level, noseId);
        if (triggerOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No trigger for '" + noseId + "'"));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fManually firing trigger for \u00a7d" + noseId),
                true
        );

        TutorialNoseEquipHandler.manualTrigger(player, level, noseId);
        return Command.SINGLE_SUCCESS;
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var ids = TutorialNoseEquipTrigger.getAllTriggerNoseIds(level);
        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No nose equip triggers defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fNose equip triggers (" + ids.size() + "):"), false);
        for (String noseId : ids) {
            source.sendSuccess(() -> Component.literal("\u00a77  - \u00a7e" + noseId), false);
        }
        return Command.SINGLE_SUCCESS;
    }
}
