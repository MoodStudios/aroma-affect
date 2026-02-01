package com.ovrtechnology.command;

import com.ovrtechnology.AromaCraft;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import static net.minecraft.commands.Commands.literal;

/**
 * Registers the /aromaguide command which opens the guide UI on the client.
 * Since the guide is a client-side screen, the command sends a success message
 * and opens the screen through reflection to avoid server-side class loading issues.
 */
public final class AromaGuideCommand {

    private AromaGuideCommand() {
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register(AromaGuideCommand::registerCommand);
        AromaCraft.LOGGER.info("AromaCraft guide command initialized");
    }

    private static void registerCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registry,
            Commands.CommandSelection selection
    ) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal("aromaguide")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer) {
                        // Open the guide on the client side
                        openGuideForPlayer();
                    }
                    return Command.SINGLE_SUCCESS;
                });

        dispatcher.register(builder);
        AromaCraft.LOGGER.debug("Registered /aromaguide command");
    }

    /**
     * Opens the guide via reflection to keep it client-safe.
     */
    private static void openGuideForPlayer() {
        try {
            Class<?> clazz = Class.forName("com.ovrtechnology.guide.GuideManager");
            clazz.getMethod("openGuideClient").invoke(null);
        } catch (ReflectiveOperationException e) {
            AromaCraft.LOGGER.debug("Failed to open guide UI (expected on dedicated server)", e);
        }
    }
}
