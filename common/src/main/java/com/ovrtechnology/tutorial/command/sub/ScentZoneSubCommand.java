package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand: /tutorial scentzone
 * Opens the scent zone GUI for the player.
 * Also syncs zone data to the client before opening.
 */
public class ScentZoneSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "scentzone";
    }

    @Override
    public String getDescription() {
        return "Open scent zone editor GUI";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.executes(context -> {
            CommandSourceStack source = context.getSource();
            if (source.getEntity() instanceof ServerPlayer player) {
                ServerLevel level = (ServerLevel) player.level();
                // Sync zones to client first
                TutorialScentZoneNetworking.syncToPlayer(player, level);
                // Tell client to open the GUI
                TutorialScentZoneNetworking.sendOpenGui(player);
                return 1;
            }
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        });
    }
}
