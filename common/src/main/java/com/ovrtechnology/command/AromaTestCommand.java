package com.ovrtechnology.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.command.sub.LookupSubCommand;
import com.ovrtechnology.command.sub.PingSubCommand;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.minecraft.commands.Commands.literal;

/**
 * Main command handler for /aromatest command.
 * <p>
 * This provides a testing/debug interface for AromaCraft functionality.
 * Subcommands can be easily added by implementing {@link SubCommand}
 * and registering them in {@link #SUB_COMMANDS}.
 */
public final class AromaTestCommand {
    
    /**
     * Registry of all subcommands.
     * Add new subcommands here to make them available under /aromatest.
     */
    private static final Map<String, SubCommand> SUB_COMMANDS = new LinkedHashMap<>();
    
    static {
        // Register all subcommands here
        register(new PingSubCommand());
        register(new LookupSubCommand());
    }
    
    private AromaTestCommand() {
        // Utility class
    }
    
    /**
     * Registers a subcommand.
     */
    private static void register(SubCommand subCommand) {
        SUB_COMMANDS.put(subCommand.getName(), subCommand);
    }
    
    /**
     * Initializes the command registration listener.
     * Should be called during mod initialization.
     */
    public static void init() {
        CommandRegistrationEvent.EVENT.register(AromaTestCommand::registerCommands);
        AromaCraft.LOGGER.info("AromaCraft test commands initialized");
    }
    
    /**
     * Registers the /aromatest command with all subcommands.
     */
    private static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registry,
            Commands.CommandSelection selection
    ) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("aromatest");
        
        // Add all registered subcommands
        for (SubCommand subCommand : SUB_COMMANDS.values()) {
            builder = builder.then(subCommand.build(literal(subCommand.getName())));
        }
        
        // Default execution (no subcommand) shows available subcommands
        builder.executes(context -> {
            context.getSource().sendSuccess(
                    () -> Component.literal("§6[AromaCraft] §7Available subcommands:"),
                    false
            );
            for (SubCommand subCommand : SUB_COMMANDS.values()) {
                context.getSource().sendSuccess(
                        () -> Component.literal("§7  - §e/aromatest " + subCommand.getName() + " §8- " + subCommand.getDescription()),
                        false
                );
            }
            return SUB_COMMANDS.size();
        });
        
        dispatcher.register(builder);
        AromaCraft.LOGGER.debug("Registered /aromatest command with {} subcommands", SUB_COMMANDS.size());
    }
}

