package com.ovrtechnology.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public interface SubCommand {

    String getName();

    String getDescription();

    ArgumentBuilder<CommandSourceStack, ?> build(
            LiteralArgumentBuilder<CommandSourceStack> builder);
}
