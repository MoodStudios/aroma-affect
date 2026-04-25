package com.ovrtechnology.command;

import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.sub.EventTestSubCommand;
import com.ovrtechnology.command.sub.GiveVariantSubCommand;
import com.ovrtechnology.command.sub.LookupSubCommand;
import com.ovrtechnology.command.sub.PathSubCommand;
import com.ovrtechnology.command.sub.PingSubCommand;
import com.ovrtechnology.util.Texts;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class AromaTestCommand {

    private static final Map<String, SubCommand> SUB_COMMANDS = new LinkedHashMap<>();

    static {
        register(new PingSubCommand());
        register(new LookupSubCommand());
        register(new PathSubCommand());
        register(new GiveVariantSubCommand());
        register(new EventTestSubCommand());
    }

    private AromaTestCommand() {}

    private static void register(SubCommand subCommand) {
        SUB_COMMANDS.put(subCommand.getName(), subCommand);
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register(AromaTestCommand::registerCommands);
        AromaAffect.LOGGER.info("Aroma Affect test commands initialized");
    }

    private static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registry,
            Commands.CommandSelection selection) {
        LiteralArgumentBuilder<CommandSourceStack> builder =
                literal("aromatest").requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS));

        for (SubCommand subCommand : SUB_COMMANDS.values()) {
            builder = builder.then(subCommand.build(literal(subCommand.getName())));
        }

        builder.executes(
                context -> {
                    context.getSource()
                            .sendSuccess(
                                    () -> Texts.lit("§6[Aroma Affect] §7Available subcommands:"),
                                    false);
                    for (SubCommand subCommand : SUB_COMMANDS.values()) {
                        context.getSource()
                                .sendSuccess(
                                        () ->
                                                Texts.lit(
                                                        "§7  - §e/aromatest "
                                                                + subCommand.getName()
                                                                + " §8- "
                                                                + subCommand.getDescription()),
                                        false);
                    }
                    return SUB_COMMANDS.size();
                });

        dispatcher.register(builder);
        AromaAffect.LOGGER.debug(
                "Registered /aromatest command with {} subcommands", SUB_COMMANDS.size());
    }
}
