package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.waypoint.client.TutorialStaticArrowManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /tutorial arrow add <id> — adds a static arrow at the block you're looking at
 * /tutorial arrow remove <id> — removes a static arrow
 * /tutorial arrow list — lists all static arrows
 */
public class ArrowSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "arrow";
    }

    @Override
    public String getDescription() {
        return "Manage static floating arrows";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(literal("add").then(argument("id", StringArgumentType.word()).executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        source.sendFailure(Component.literal("Only players"));
                        return 0;
                    }
                    BlockPos target = getLookedAtBlock(player);
                    if (target == null) {
                        source.sendFailure(Component.literal("§cLook at a block first!"));
                        return 0;
                    }
                    String id = StringArgumentType.getString(context, "id");
                    ServerLevel level = (ServerLevel) player.level();
                    if (TutorialStaticArrowManager.addArrow(level, id, target)) {
                        TutorialStaticArrowManager.syncToAllPlayers(level);
                        source.sendSuccess(() -> Component.literal(
                                "§aArrow '" + id + "' added at §e" + target.getX() + ", " + target.getY() + ", " + target.getZ()), false);
                    } else {
                        source.sendFailure(Component.literal("§cArrow '" + id + "' already exists"));
                    }
                    return 1;
                })))
                .then(literal("remove").then(argument("id", StringArgumentType.word()).executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
                    String id = StringArgumentType.getString(context, "id");
                    ServerLevel level = (ServerLevel) player.level();
                    if (TutorialStaticArrowManager.removeArrow(level, id)) {
                        TutorialStaticArrowManager.syncToAllPlayers(level);
                        source.sendSuccess(() -> Component.literal("§aArrow '" + id + "' removed"), false);
                    } else {
                        source.sendFailure(Component.literal("§cArrow '" + id + "' not found"));
                    }
                    return 1;
                })))
                .then(literal("list").executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
                    ServerLevel level = (ServerLevel) player.level();
                    var arrows = TutorialStaticArrowManager.getAllArrows(level);
                    if (arrows.isEmpty()) {
                        source.sendSuccess(() -> Component.literal("§7No static arrows"), false);
                    } else {
                        for (var entry : arrows.entrySet()) {
                            BlockPos p = entry.getValue();
                            source.sendSuccess(() -> Component.literal(
                                    "§7- §e" + entry.getKey() + " §7at " + p.getX() + ", " + p.getY() + ", " + p.getZ()), false);
                        }
                    }
                    return 1;
                }));
    }

    private static BlockPos getLookedAtBlock(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(5.0));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }
}
