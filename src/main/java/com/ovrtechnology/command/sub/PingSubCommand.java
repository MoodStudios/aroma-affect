package com.ovrtechnology.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.command.SubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ping-pong subcommand that displays server latency for the executing player.
 * <p>
 * Usage: /aromatest ping
 */
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
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.executes(this::execute);
    }
    
    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayer player) {
            int latency = player.connection.latency();
            
            // Color code based on latency quality
            String color = getLatencyColor(latency);
            
            source.sendSuccess(() -> Component.literal("§aPong! §7Your latency: " + color + latency + "ms"), false);
        } else {
            // Console or non-player execution
            source.sendSuccess(() -> Component.literal("§aPong! §7(executed from console)"), false);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Returns a color code based on latency thresholds.
     */
    private String getLatencyColor(int latency) {
        if (latency < 50) {
            return "§a"; // Green - excellent
        } else if (latency < 100) {
            return "§e"; // Yellow - good
        } else if (latency < 200) {
            return "§6"; // Orange - moderate
        } else {
            return "§c"; // Red - poor
        }
    }
}

