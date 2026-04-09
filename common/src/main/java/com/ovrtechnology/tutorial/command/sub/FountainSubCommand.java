package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.literal;

/**
 * /tutorial fountain button — register the button you're looking at
 * /tutorial fountain block — register the block to replace with water
 * /tutorial fountain info — show current config
 * /tutorial fountain clear — remove config
 */
public class FountainSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "fountain";
    }

    @Override
    public String getDescription() {
        return "Set up water fountain button + target block";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(literal("button").executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        source.sendFailure(Component.literal("Only players can use this"));
                        return 0;
                    }
                    BlockPos target = getLookedAtBlock(player);
                    if (target == null) {
                        source.sendFailure(Component.literal("§cLook at a block first!"));
                        return 0;
                    }
                    ServerLevel level = (ServerLevel) player.level();
                    TutorialSpawnManager.setFountainButtonPos(level, target);
                    source.sendSuccess(() -> Component.literal(
                            "§a Fountain button set at §e" + target.getX() + ", " + target.getY() + ", " + target.getZ()), false);
                    return 1;
                }))
                .then(literal("block").executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        source.sendFailure(Component.literal("Only players can use this"));
                        return 0;
                    }
                    BlockPos target = getLookedAtBlock(player);
                    if (target == null) {
                        source.sendFailure(Component.literal("§cLook at a block first!"));
                        return 0;
                    }
                    ServerLevel level = (ServerLevel) player.level();
                    TutorialSpawnManager.setFountainBlockPos(level, target);
                    source.sendSuccess(() -> Component.literal(
                            "§a Fountain water block set at §e" + target.getX() + ", " + target.getY() + ", " + target.getZ()), false);
                    return 1;
                }))
                .then(literal("info").executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
                    ServerLevel level = (ServerLevel) player.level();
                    var btnOpt = TutorialSpawnManager.getFountainButtonPos(level);
                    var blkOpt = TutorialSpawnManager.getFountainBlockPos(level);
                    String btnStr = btnOpt.map(p -> "§e" + p.getX() + ", " + p.getY() + ", " + p.getZ()).orElse("§7not set");
                    String blkStr = blkOpt.map(p -> "§e" + p.getX() + ", " + p.getY() + ", " + p.getZ()).orElse("§7not set");
                    source.sendSuccess(() -> Component.literal("§7Fountain button: " + btnStr + "\n§7Fountain block: " + blkStr), false);
                    return 1;
                }))
                .then(literal("clear").executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
                    ServerLevel level = (ServerLevel) player.level();
                    TutorialSpawnManager.clearFountainButtonPos(level);
                    TutorialSpawnManager.clearFountainBlockPos(level);
                    source.sendSuccess(() -> Component.literal("§a Fountain config cleared"), false);
                    return 1;
                }));
    }

    private static BlockPos getLookedAtBlock(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(5.0));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return null;
    }
}
