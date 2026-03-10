package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Subcommand to toggle map protection on/off.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial protection on} - Enable map protection (default)</li>
 *   <li>{@code /tutorial protection off} - Disable map protection (bypass)</li>
 *   <li>{@code /tutorial protection} - Show current status</li>
 * </ul>
 * <p>
 * Protection is automatically re-enabled on tutorial reset or player join.
 */
public class ProtectionSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "protection";
    }

    @Override
    public String getDescription() {
        return "Toggle map protection on/off (bypass block breaking restrictions)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("on")
                        .executes(this::executeOn))
                .then(Commands.literal("off")
                        .executes(this::executeOff))
                .executes(this::executeStatus);
    }

    private int executeOn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        TutorialRegenAreaHandler.disableBypass(source.getLevel());

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7aMap protection ENABLED"
                        + "\n\u00a77  Blocks outside regen areas cannot be broken"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeOff(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        TutorialRegenAreaHandler.enableBypass(source.getLevel());

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7cMap protection DISABLED"
                        + "\n\u00a77  All blocks can be broken (bypass mode)"
                        + "\n\u00a77  Will auto-enable on reset or player join"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        boolean bypassed = TutorialRegenAreaHandler.isBypassEnabled(source.getLevel());

        if (bypassed) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fMap protection: \u00a7cDISABLED"
                            + "\n\u00a77  Use /tutorial protection on to enable"),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fMap protection: \u00a7aENABLED"
                            + "\n\u00a77  Use /tutorial protection off to disable"),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}
