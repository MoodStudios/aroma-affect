package com.ovrtechnology.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.command.SubCommand;
import com.ovrtechnology.util.Texts;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class PingSubCommand implements SubCommand {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Displays your current latency to the server";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(
            LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.executes(this::execute);
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            int latency = player.connection.latency();

            String color = getLatencyColor(latency);

            source.sendSuccess(
                    () -> Texts.lit("§aPong! §7Your latency: " + color + latency + "ms"), false);
        } else {

            source.sendSuccess(() -> Texts.lit("§aPong! §7(executed from console)"), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private String getLatencyColor(int latency) {
        if (latency < 50) {
            return "§a";
        } else if (latency < 100) {
            return "§e";
        } else if (latency < 200) {
            return "§6";
        } else {
            return "§c";
        }
    }
}
