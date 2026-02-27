package com.ovrtechnology.tutorial.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/**
 * Interface for creating subcommands under the /tutorial parent command.
 * <p>
 * To create a new tutorial subcommand:
 * <ol>
 *   <li>Create a class implementing this interface</li>
 *   <li>Return the subcommand name from {@link #getName()}</li>
 *   <li>Provide a description in {@link #getDescription()}</li>
 *   <li>Build the command structure in {@link #build(LiteralArgumentBuilder)}</li>
 *   <li>Register it using {@link TutorialCommand#register(TutorialSubCommand)}</li>
 * </ol>
 * <p>
 * All tutorial subcommands inherit the parent command's requirement that
 * the {@code isOvrTutorial} GameRule must be {@code true}.
 */
public interface TutorialSubCommand {

    /**
     * Gets the name of this subcommand.
     * <p>
     * This will be the literal argument after /tutorial.
     *
     * @return the subcommand name (e.g., "warp" for /tutorial warp)
     */
    String getName();

    /**
     * Gets a brief description of what this subcommand does.
     * <p>
     * Used for help text generation.
     *
     * @return a short description of the subcommand
     */
    String getDescription();

    /**
     * Builds the command structure for this subcommand.
     * <p>
     * The provided builder already has the subcommand literal set.
     *
     * @param builder the literal argument builder with the subcommand name
     * @return the built argument structure
     */
    ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder);
}
