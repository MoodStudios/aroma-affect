package com.ovrtechnology.tutorial.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.minecraft.commands.Commands.literal;

/**
 * Main command handler for the /tutorial command.
 * <p>
 * This command is only available when the {@code isOvrTutorial} GameRule is set to
 * {@code true}. In normal gameplay, the command is completely invisible - it won't
 * appear in tab completion and will not execute.
 * <p>
 * This design allows tutorial maps to use command blocks to control the tutorial
 * experience, while keeping all tutorial functionality hidden from players in
 * regular survival/creative worlds.
 */
public final class TutorialCommand {

    /**
     * Registry of all tutorial subcommands.
     */
    private static final Map<String, TutorialSubCommand> SUB_COMMANDS = new LinkedHashMap<>();

    private TutorialCommand() {
        // Utility class
    }

    /**
     * Registers a subcommand.
     *
     * @param subCommand the subcommand to register
     */
    public static void register(TutorialSubCommand subCommand) {
        SUB_COMMANDS.put(subCommand.getName(), subCommand);
    }

    /**
     * Initializes the command registration listener.
     * <p>
     * Should be called during tutorial module initialization.
     */
    public static void init() {
        CommandRegistrationEvent.EVENT.register(TutorialCommand::registerCommands);
        AromaAffect.LOGGER.debug("Tutorial commands initialized");
    }

    /**
     * Registers the /tutorial command with all subcommands.
     */
    private static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registry,
            Commands.CommandSelection selection
    ) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("tutorial")
                // Command only exists when isOvrTutorial GameRule is true
                .requires(TutorialCommand::isTutorialActive);

        // Add all registered subcommands
        for (TutorialSubCommand subCommand : SUB_COMMANDS.values()) {
            builder = builder.then(subCommand.build(literal(subCommand.getName())));
        }

        // Default execution (no subcommand) shows status
        builder.executes(context -> {
            context.getSource().sendSuccess(
                    () -> Component.literal("§6[OVR Tutorial] §aTutorial mode is active"),
                    false
            );

            if (!SUB_COMMANDS.isEmpty()) {
                context.getSource().sendSuccess(
                        () -> Component.literal("§7Available commands:"),
                        false
                );
                for (TutorialSubCommand subCommand : SUB_COMMANDS.values()) {
                    context.getSource().sendSuccess(
                            () -> Component.literal("§7  - §e/tutorial " + subCommand.getName()
                                    + " §8- " + subCommand.getDescription()),
                            false
                    );
                }
            }

            return 1;
        });

        dispatcher.register(builder);
        AromaAffect.LOGGER.debug("Registered /tutorial command with {} subcommands", SUB_COMMANDS.size());
    }

    /**
     * Checks if tutorial mode is active for the given command source.
     * <p>
     * This is used as the requirement predicate for the command, ensuring
     * the command is completely invisible when tutorial mode is disabled.
     *
     * @param source the command source
     * @return {@code true} if tutorial mode is active
     */
    private static boolean isTutorialActive(CommandSourceStack source) {
        return TutorialModule.isActive(source.getLevel());
    }
}
