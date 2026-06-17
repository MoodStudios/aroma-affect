package com.ovrtechnology.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ovrtechnology.command.SubCommand;
import com.ovrtechnology.network.ScentEventNetworking;
import com.ovrtechnology.trigger.event.EventDefinition;
import com.ovrtechnology.trigger.event.EventDefinitionLoader;
import com.ovrtechnology.util.Texts;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class EventTestSubCommand implements SubCommand {

    @Override
    public String getName() {
        return "event";
    }

    @Override
    public String getDescription() {
        return "Test event triggers (test <event_id>, list, throttle)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(
            LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.then(Commands.literal("list").executes(this::executeList))
                .then(Commands.literal("throttle").executes(this::executeThrottle))
                .then(
                        Commands.literal("test")
                                .then(
                                        Commands.argument(
                                                        "event_id",
                                                        StringArgumentType.greedyString())
                                                .suggests(EVENT_ID_SUGGESTIONS)
                                                .executes(this::executeTest)))
                .executes(this::executeList);
    }

    private static final SuggestionProvider<CommandSourceStack> EVENT_ID_SUGGESTIONS =
            (ctx, builder) -> suggest(builder);

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder) {
        for (EventDefinition def : EventDefinitionLoader.getLoadedEvents()) {
            builder.suggest(def.getEventId());
        }
        return builder.buildFuture();
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var events = EventDefinitionLoader.getLoadedEvents();
        source.sendSuccess(
                () -> Texts.lit("§6[Aroma Affect] §7Loaded " + events.size() + " events:"), false);
        for (EventDefinition def : events) {
            source.sendSuccess(
                    () ->
                            Texts.lit(
                                    "§7  - §e"
                                            + def.getEventId()
                                            + " §8("
                                            + def.getCategory()
                                            + " / "
                                            + def.getTriggerType()
                                            + " -> "
                                            + def.getScentId()
                                            + ")"),
                    false);
        }
        return events.size();
    }

    private int executeThrottle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int current = com.ovrtechnology.trigger.event.EventThrottle.getCurrentCount();
        int limit =
                com.ovrtechnology.trigger.event.EventTriggersConfig.getInstance()
                        .getGlobalThrottlePerMinute();
        source.sendSuccess(
                () ->
                        Texts.lit(
                                "§6[Aroma Affect] §7Throttle: §e"
                                        + current
                                        + "§7 / §e"
                                        + limit
                                        + " §7per minute"),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String eventId = StringArgumentType.getString(context, "event_id").trim();

        EventDefinition def = EventDefinitionLoader.getById(eventId).orElse(null);
        if (def == null) {
            source.sendFailure(Texts.lit("§c[Aroma Affect] Unknown event_id: " + eventId));
            return 0;
        }

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Texts.lit("§c[Aroma Affect] Must be run by a player"));
            return 0;
        }

        ScentEventNetworking.sendEvent(player, eventId);
        source.sendSuccess(
                () ->
                        Texts.lit(
                                "§a[Aroma Affect] Sent event §e"
                                        + eventId
                                        + " §a("
                                        + def.getScentId()
                                        + " / "
                                        + def.getCategory()
                                        + ")"),
                false);
        return Command.SINGLE_SUCCESS;
    }
}
