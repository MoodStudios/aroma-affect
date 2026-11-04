package com.ovrtechnology.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/**
 * Interface for creating subcommands under the /aromatest parent command.
 * <p>
 * To create a new subcommand:
 * <ol>
 *   <li>Create a class implementing this interface</li>
 *   <li>Return the subcommand name from {@link #getName()}</li>
 *   <li>Build the command structure in {@link #build(LiteralArgumentBuilder)}</li>
 *   <li>Register it in {@link AromaTestCommand#SUB_COMMANDS}</li>
 * </ol>
 */
public interface SubCommand {
    
    /**
     * Gets the name of this subcommand.
     * This will be the literal argument after /aromatest.
     * 
     * @return the subcommand name (e.g., "ping" for /aromatest ping)
     */
    String getName();
    
    /**
     * Gets a brief description of what this subcommand does.
     * Used for help text generation.
     * 
     * @return a short description of the subcommand
     */
    String getDescription();
    
    /**
     * Builds the command structure for this subcommand.
     * The provided builder already has the subcommand literal set.
     * 
     * @param builder the literal argument builder with the subcommand name
     * @return the built argument structure
     */
    ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder);
}

