package com.ovrtechnology.tutorial.boss.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.boss.TutorialBossArea;
import com.ovrtechnology.tutorial.boss.TutorialBossAreaHandler;
import com.ovrtechnology.tutorial.boss.TutorialBossAreaManager;
import com.ovrtechnology.tutorial.boss.TutorialBossSpawner;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;

/**
 * Subcommand for spawning tutorial bosses and managing boss areas.
 * <p>
 * Usage:
 * <ul>
 *   <li>/tutorial boss spawn blaze <pos> - Spawn tutorial blaze at position</li>
 *   <li>/tutorial boss spawn dragon <pos> - Spawn tutorial dragon at position</li>
 *   <li>/tutorial boss area create <id> <type> - Create a new boss area</li>
 *   <li>/tutorial boss area trigger1 <id> <pos> - Set trigger corner 1</li>
 *   <li>/tutorial boss area trigger2 <id> <pos> - Set trigger corner 2</li>
 *   <li>/tutorial boss area setspawn <id> <pos> - Set spawn position</li>
 *   <li>/tutorial boss area movement1 <id> <pos> - Set movement corner 1</li>
 *   <li>/tutorial boss area movement2 <id> <pos> - Set movement corner 2</li>
 *   <li>/tutorial boss area list - List all boss areas</li>
 *   <li>/tutorial boss area remove <id> - Remove a boss area</li>
 *   <li>/tutorial boss area info <id> - Show area details</li>
 * </ul>
 */
public class BossSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> AREA_ID_SUGGESTIONS = (ctx, builder) -> {
        if (ctx.getSource().getLevel() instanceof ServerLevel level) {
            TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);
            return SharedSuggestionProvider.suggest(manager.getAreaIds(), builder);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> BOSS_TYPE_SUGGESTIONS = (ctx, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{"blaze", "dragon"}, builder);
    };

    @Override
    public String getName() {
        return "boss";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial boss encounters and spawn areas";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .requires(source -> source.hasPermission(2))
                // Direct spawn commands
                .then(Commands.literal("spawn")
                        .then(Commands.literal("blaze")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> spawnBoss(ctx, "blaze"))))
                        .then(Commands.literal("dragon")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> spawnBoss(ctx, "dragon")))))
                // Area management commands
                .then(Commands.literal("area")
                        // Create area
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(BOSS_TYPE_SUGGESTIONS)
                                                .executes(this::createArea))))
                        // Set trigger corners
                        .then(Commands.literal("trigger1")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(AREA_ID_SUGGESTIONS)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> setTriggerCorner(ctx, 1)))))
                        .then(Commands.literal("trigger2")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(AREA_ID_SUGGESTIONS)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> setTriggerCorner(ctx, 2)))))
                        // Set spawn position
                        .then(Commands.literal("setspawn")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(AREA_ID_SUGGESTIONS)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(this::setSpawnPos))))
                        // Set movement corners
                        .then(Commands.literal("movement1")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(AREA_ID_SUGGESTIONS)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> setMovementCorner(ctx, 1)))))
                        .then(Commands.literal("movement2")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(AREA_ID_SUGGESTIONS)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> setMovementCorner(ctx, 2)))))
                        // List areas
                        .then(Commands.literal("list")
                                .executes(this::listAreas))
                        // Remove area
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(AREA_ID_SUGGESTIONS)
                                        .executes(this::removeArea)))
                        // Show area info
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(AREA_ID_SUGGESTIONS)
                                        .executes(this::showAreaInfo)))
                        // Reset (clear all active bosses)
                        .then(Commands.literal("reset")
                                .executes(this::resetBosses)));
    }

    private int spawnBoss(CommandContext<CommandSourceStack> ctx, String bossType) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

        boolean success = TutorialBossSpawner.spawnBoss(level, bossType, pos);

        if (success) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[Tutorial] \u00a7fSpawned " + bossType + " at " + pos.toShortString()
            ), true);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("Unknown boss type: " + bossType));
            return 0;
        }
    }

    private int createArea(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        String type = StringArgumentType.getString(ctx, "type").toLowerCase();

        if (!type.equals("blaze") && !type.equals("dragon")) {
            source.sendFailure(Component.literal("Invalid boss type. Use 'blaze' or 'dragon'"));
            return 0;
        }

        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);

        if (manager.getArea(id).isPresent()) {
            source.sendFailure(Component.literal("Area '" + id + "' already exists"));
            return 0;
        }

        TutorialBossArea area = new TutorialBossArea(id, type);
        manager.addArea(area);

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[Tutorial] \u00a7fCreated boss area '" + id + "' (type: " + type + ")\n" +
                "\u00a77Use /tutorial boss area settrigger, setspawn, setmovement to configure"
        ), true);

        return Command.SINGLE_SUCCESS;
    }

    private int setTriggerCorner(CommandContext<CommandSourceStack> ctx, int corner) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);
        TutorialBossArea area = manager.getArea(id).orElse(null);

        if (area == null) {
            source.sendFailure(Component.literal("Area '" + id + "' not found"));
            return 0;
        }

        if (corner == 1) {
            area.setTriggerCorner1(pos);
        } else {
            area.setTriggerCorner2(pos);
        }
        manager.addArea(area); // Mark dirty

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[Tutorial] \u00a7fSet trigger corner " + corner + " for '" + id + "': " + pos.toShortString()
        ), true);

        if (area.hasTriggerArea()) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7a✓ Trigger area complete"
            ), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int setSpawnPos(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);
        TutorialBossArea area = manager.getArea(id).orElse(null);

        if (area == null) {
            source.sendFailure(Component.literal("Area '" + id + "' not found"));
            return 0;
        }

        area.setSpawnPos(pos);
        manager.addArea(area); // Mark dirty

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[Tutorial] \u00a7fSet spawn position for '" + id + "': " + pos.toShortString()
        ), true);

        if (area.isComplete()) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7a\u00a7l✓ Area '" + id + "' is now complete and active!"
            ), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int setMovementCorner(CommandContext<CommandSourceStack> ctx, int corner) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);
        TutorialBossArea area = manager.getArea(id).orElse(null);

        if (area == null) {
            source.sendFailure(Component.literal("Area '" + id + "' not found"));
            return 0;
        }

        if (corner == 1) {
            area.setMovementCorner1(pos);
        } else {
            area.setMovementCorner2(pos);
        }
        manager.addArea(area); // Mark dirty

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[Tutorial] \u00a7fSet movement corner " + corner + " for '" + id + "': " + pos.toShortString()
        ), true);

        if (area.hasMovementArea()) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7a✓ Movement area complete"
            ), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int listAreas(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);
        Collection<TutorialBossArea> areas = manager.getAllAreas();

        if (areas.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[Tutorial] \u00a77No boss areas configured"
            ), false);
            return Command.SINGLE_SUCCESS;
        }

        StringBuilder sb = new StringBuilder("\u00a7d[Tutorial] \u00a7fBoss Areas:\n");
        for (TutorialBossArea area : areas) {
            String status = area.isComplete() ? "\u00a7a✓" : "\u00a7c✗";
            String bossActive = TutorialBossAreaHandler.isBossActive(area.getId()) ? " \u00a7c[ACTIVE]" : "";
            sb.append("\u00a77- ").append(status).append(" \u00a7f")
              .append(area.getId()).append(" \u00a77(").append(area.getBossType()).append(")")
              .append(bossActive).append("\n");
        }

        source.sendSuccess(() -> Component.literal(sb.toString().trim()), false);

        return Command.SINGLE_SUCCESS;
    }

    private int removeArea(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");

        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);

        if (manager.getArea(id).isEmpty()) {
            source.sendFailure(Component.literal("Area '" + id + "' not found"));
            return 0;
        }

        manager.removeArea(id);
        TutorialBossAreaHandler.removeBoss(id);

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[Tutorial] \u00a7fRemoved boss area '" + id + "'"
        ), true);

        return Command.SINGLE_SUCCESS;
    }

    private int showAreaInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");

        TutorialBossAreaManager manager = TutorialBossAreaManager.get(level);
        TutorialBossArea area = manager.getArea(id).orElse(null);

        if (area == null) {
            source.sendFailure(Component.literal("Area '" + id + "' not found"));
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\u00a7d[Tutorial] \u00a7fBoss Area: \u00a7e").append(area.getId()).append("\n");
        sb.append("\u00a77Type: \u00a7f").append(area.getBossType()).append("\n");
        sb.append("\u00a77Status: ").append(area.isComplete() ? "\u00a7aComplete" : "\u00a7cIncomplete").append("\n");
        sb.append("\u00a77Boss Active: ").append(TutorialBossAreaHandler.isBossActive(id) ? "\u00a7cYes" : "\u00a7aNo").append("\n");

        if (area.hasTriggerArea()) {
            sb.append("\u00a77Trigger: \u00a7f").append(area.isInTriggerArea(BlockPos.ZERO) ? "Set" : "Set").append("\n");
        } else {
            sb.append("\u00a7cTrigger: Not set\n");
        }

        if (area.hasSpawnPos()) {
            sb.append("\u00a77Spawn: \u00a7f").append(area.getSpawnPos().toShortString()).append("\n");
        } else {
            sb.append("\u00a7cSpawn: Not set\n");
        }

        if (area.hasMovementArea()) {
            sb.append("\u00a77Movement: \u00a7f").append(area.getMovementMin().toShortString())
              .append(" to ").append(area.getMovementMax().toShortString()).append("\n");
        } else {
            sb.append("\u00a77Movement: \u00a7fNot set (boss moves freely)\n");
        }

        source.sendSuccess(() -> Component.literal(sb.toString().trim()), false);

        return Command.SINGLE_SUCCESS;
    }

    private int resetBosses(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        TutorialBossAreaHandler.clearAllBosses();

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[Tutorial] \u00a7fCleared all active boss tracking"
        ), true);

        return Command.SINGLE_SUCCESS;
    }
}
