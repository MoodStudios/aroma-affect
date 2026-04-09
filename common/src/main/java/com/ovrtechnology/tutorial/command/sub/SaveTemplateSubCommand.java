package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.demo.DemoWorldManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * /tutorial savetemplate — saves the current world to the template folder
 * without closing the world. Use this while in Edit mode.
 */
public class SaveTemplateSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "savetemplate";
    }

    @Override
    public String getDescription() {
        return "Save current world to template (without closing)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.executes(context -> {
            var source = context.getSource();

            if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                source.sendFailure(Component.literal("Only players"));
                return 0;
            }

            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();

            source.sendSuccess(() -> Component.literal("§eSaving data to template..."), false);

            try {
                // Force save all levels to disk (chunks + data)
                for (var sl : level.getServer().getAllLevels()) {
                    sl.save(null, false, false);
                }

                // Copy entire world to template
                DemoWorldManager.saveDataToTemplate(level);

                source.sendSuccess(() -> Component.literal("§aTemplate saved! (world + data)"), false);
            } catch (Exception e) {
                source.sendFailure(Component.literal("§cFailed: " + e.getMessage()));
            }

            return 1;
        });
    }
}
