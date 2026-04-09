package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
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
 * /tutorial rainbutton — registers the block you're looking at as a rain toggle button.
 * /tutorial rainbutton clear — removes the rain button.
 * /tutorial rainbutton info — shows current rain button position.
 */
public class RainButtonSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "rainbutton";
    }

    @Override
    public String getDescription() {
        return "Set/clear rain toggle button (look at button + run)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial rainbutton — set button at looked-at block
                .executes(context -> {
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
                    com.ovrtechnology.tutorial.spawn.TutorialSpawnManager.setRainButtonPos(level, target);

                    source.sendSuccess(() -> Component.literal(
                            "§a Rain button set at §e" + target.getX() + ", " + target.getY() + ", " + target.getZ()), false);
                    return 1;
                })
                // /tutorial rainbutton clear
                .then(literal("clear").executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
                    ServerLevel level = (ServerLevel) player.level();
                    com.ovrtechnology.tutorial.spawn.TutorialSpawnManager.clearRainButtonPos(level);
                    source.sendSuccess(() -> Component.literal("§a Rain button cleared"), false);
                    return 1;
                }))
                // /tutorial rainbutton info
                .then(literal("info").executes(context -> {
                    var source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
                    ServerLevel level = (ServerLevel) player.level();
                    var posOpt = com.ovrtechnology.tutorial.spawn.TutorialSpawnManager.getRainButtonPos(level);
                    if (posOpt.isPresent()) {
                        BlockPos pos = posOpt.get();
                        source.sendSuccess(() -> Component.literal(
                                "§7Rain button at §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("§7No rain button set"), false);
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

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return null;
    }
}
