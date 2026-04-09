package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.demo.DemoTimerHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SkipTimerSubCommand implements TutorialSubCommand {
    @Override public String getName() { return "skiptimer"; }
    @Override public String getDescription() { return "Skip timer to 10 seconds (testing)"; }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.executes(context -> {
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                DemoTimerHandler.skipTimer(player);
                context.getSource().sendSuccess(() -> Component.literal("§aTimer skipped to 10 seconds"), false);
                return 1;
            }
            return 0;
        });
    }
}
