package com.ovrtechnology.command.sub;

import com.ovrtechnology.util.Texts;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.biome.BiomeRegistry;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.command.SubCommand;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.structure.StructureRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for listing registered items from various registries.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /aromatest registry structure} - List all registered structures</li>
 *   <li>{@code /aromatest registry block} - List all registered blocks</li>
 *   <li>{@code /aromatest registry biome} - List all registered biomes</li>
 *   <li>{@code /aromatest registry scent} - List all registered scents</li>
 *   <li>{@code /aromatest registry nose} - List all registered noses</li>
 * </ul>
 */
public class RegistrySubCommand implements SubCommand {
    
    @Override
    public String getName() {
        return "registry";
    }
    
    @Override
    public String getDescription() {
        return "List registered structures, blocks, biomes, scents, or noses";
    }
    
    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("structure")
                        .executes(ctx -> executeList(ctx, RegistryType.STRUCTURE)))
                .then(Commands.literal("block")
                        .executes(ctx -> executeList(ctx, RegistryType.BLOCK)))
                .then(Commands.literal("biome")
                        .executes(ctx -> executeList(ctx, RegistryType.BIOME)))
                .then(Commands.literal("scent")
                        .executes(ctx -> executeList(ctx, RegistryType.SCENT)))
                .then(Commands.literal("nose")
                        .executes(ctx -> executeList(ctx, RegistryType.NOSE)))
                .executes(this::showUsage);
    }
    
    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Texts.lit("§6[Aroma Affect] §7Registry usage:"), false);
        source.sendSuccess(() -> Texts.lit("§e  /aromatest registry structure §8- List structures"), false);
        source.sendSuccess(() -> Texts.lit("§e  /aromatest registry block §8- List blocks"), false);
        source.sendSuccess(() -> Texts.lit("§e  /aromatest registry biome §8- List biomes"), false);
        source.sendSuccess(() -> Texts.lit("§e  /aromatest registry scent §8- List scents"), false);
        source.sendSuccess(() -> Texts.lit("§e  /aromatest registry nose §8- List noses"), false);
        return Command.SINGLE_SUCCESS;
    }
    
    private int executeList(CommandContext<CommandSourceStack> context, RegistryType type) {
        CommandSourceStack source = context.getSource();
        
        List<String> ids = getRegisteredIds(type);
        int count = ids.size();
        
        if (count == 0) {
            source.sendSuccess(() -> Texts.lit(
                    "§6[Aroma Affect] §7No " + type.getDisplayName() + " registered."
            ), false);
            return Command.SINGLE_SUCCESS;
        }
        
        // Header
        source.sendSuccess(() -> Texts.lit(
                "§6[Aroma Affect] §7Registered " + type.getDisplayName() + " §8(§e" + count + "§8):"
        ), false);
        
        // List IDs comma-separated
        String idList = String.join(", ", ids);
        source.sendSuccess(() -> Texts.lit("§f" + idList), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private List<String> getRegisteredIds(RegistryType type) {
        List<String> ids = new ArrayList<>();
        
        switch (type) {
            case STRUCTURE -> {
                for (String id : StructureRegistry.getAllStructureIds()) {
                    ids.add(id);
                }
            }
            case BLOCK -> {
                for (String id : BlockRegistry.getAllBlockIds()) {
                    ids.add(id);
                }
            }
            case BIOME -> {
                for (String id : BiomeRegistry.getAllBiomeIds()) {
                    ids.add(id);
                }
            }
            case SCENT -> {
                for (String id : ScentRegistry.getAllScentIds()) {
                    ids.add(id);
                }
            }
            case NOSE -> {
                for (String id : NoseRegistry.getAllNoseIds()) {
                    ids.add(id);
                }
            }
        }
        
        return ids;
    }
    
    /**
     * Enum for registry types.
     */
    private enum RegistryType {
        STRUCTURE("structures"),
        BLOCK("blocks"),
        BIOME("biomes"),
        SCENT("scents"),
        NOSE("noses");
        
        private final String displayName;
        
        RegistryType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}

