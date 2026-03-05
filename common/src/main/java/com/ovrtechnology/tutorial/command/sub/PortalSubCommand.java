package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.portal.TutorialPortal;
import com.ovrtechnology.tutorial.portal.TutorialPortalManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;

/**
 * Subcommand for managing tutorial portals (teleportation zones).
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial portal create <id>} - Create a new portal</li>
 *   <li>{@code /tutorial portal delete <id>} - Delete a portal</li>
 *   <li>{@code /tutorial portal source <id> <1|2>} - Set source area corner</li>
 *   <li>{@code /tutorial portal dest <id>} - Set destination to current position</li>
 *   <li>{@code /tutorial portal list} - List all portals</li>
 *   <li>{@code /tutorial portal info <id>} - Show portal details</li>
 * </ul>
 */
public class PortalSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> PORTAL_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(TutorialPortalManager.getAllPortalIds(level), builder);
    };

    @Override
    public String getName() {
        return "portal";
    }

    @Override
    public String getDescription() {
        return "Manage tutorial portals (teleportation zones)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial portal create <id>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeCreate)
                        )
                )

                // /tutorial portal delete <id>
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(PORTAL_SUGGESTIONS)
                                .executes(this::executeDelete)
                        )
                )

                // /tutorial portal source <id> <1|2>
                .then(Commands.literal("source")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(PORTAL_SUGGESTIONS)
                                .then(Commands.argument("corner", IntegerArgumentType.integer(1, 2))
                                        .executes(this::executeSetSource)
                                )
                        )
                )

                // /tutorial portal dest <id>
                .then(Commands.literal("dest")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(PORTAL_SUGGESTIONS)
                                .executes(this::executeSetDest)
                        )
                )

                // /tutorial portal list
                .then(Commands.literal("list")
                        .executes(this::executeList)
                )

                // /tutorial portal info <id>
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(PORTAL_SUGGESTIONS)
                                .executes(this::executeInfo)
                        )
                )

                // /tutorial portal delay <id> <ticks>
                .then(Commands.literal("delay")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(PORTAL_SUGGESTIONS)
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 600))
                                        .executes(this::executeSetDelay)
                                )
                        )
                )

                // Default: show usage
                .executes(this::showUsage);
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPortal commands:"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial portal create <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial portal delete <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial portal source <id> <1|2>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial portal dest <id>"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial portal list"), false);
        source.sendSuccess(() -> Component.literal("\u00a77  /tutorial portal info <id>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("\u00a77Source = entry zone (cuboid), Dest = teleport target"), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialPortalManager.createPortal(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fCreated portal \u00a7d" + id),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  1. Set source area: /tutorial portal source " + id + " 1, then 2"),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  2. Set destination: /tutorial portal dest " + id),
                    false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Portal '" + id + "' already exists"));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (TutorialPortalManager.deletePortal(level, id)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fDeleted portal \u00a7d" + id),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Portal '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetSource(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int corner = IntegerArgumentType.getInteger(context, "corner");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();

        if (TutorialPortalManager.setSourceCorner(level, id, corner, pos)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet portal \u00a7d" + id + " \u00a7fsource corner \u00a7d" + corner
                            + " \u00a7fto \u00a7d" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );

            // Check if source area is complete
            Optional<TutorialPortal> portalOpt = TutorialPortalManager.getPortal(level, id);
            if (portalOpt.isPresent()) {
                TutorialPortal portal = portalOpt.get();
                if (portal.hasSourceArea()) {
                    if (portal.hasDestination()) {
                        source.sendSuccess(
                                () -> Component.literal("\u00a7a  \u2713 Portal complete and active!"),
                                false
                        );
                    } else {
                        source.sendSuccess(
                                () -> Component.literal("\u00a7e  Source area set. Now set destination with /tutorial portal dest " + id),
                                false
                        );
                    }
                } else {
                    source.sendSuccess(
                            () -> Component.literal("\u00a7e  Set corner " + (corner == 1 ? "2" : "1") + " to complete source area"),
                            false
                    );
                }
            }

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Portal '" + id + "' not found"));
            return 0;
        }
    }

    private int executeSetDest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] This command can only be executed by a player"));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (TutorialPortalManager.setDestination(level, id, pos, yaw, pitch)) {
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet portal \u00a7d" + id + " \u00a7fdestination to \u00a7d"
                            + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    true
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Rotation: yaw=" + String.format("%.1f", yaw) + ", pitch=" + String.format("%.1f", pitch)),
                    false
            );

            // Check if portal is complete
            Optional<TutorialPortal> portalOpt = TutorialPortalManager.getPortal(level, id);
            if (portalOpt.isPresent() && portalOpt.get().isComplete()) {
                source.sendSuccess(
                        () -> Component.literal("\u00a7a  \u2713 Portal complete and active!"),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal("\u00a7e  Destination set. Set source area to activate portal"),
                        false
                );
            }

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Portal '" + id + "' not found"));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        Set<String> ids = TutorialPortalManager.getAllPortalIds(level);

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a77No portals defined"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPortals (" + ids.size() + "):"), false);

        for (String id : ids) {
            Optional<TutorialPortal> portalOpt = TutorialPortalManager.getPortal(level, id);
            if (portalOpt.isPresent()) {
                TutorialPortal portal = portalOpt.get();
                String status = portal.isComplete() ? "\u00a7a\u2713" : "\u00a7c\u2717";
                String details = "";
                if (!portal.hasSourceArea()) {
                    details = " (no source)";
                } else if (!portal.hasDestination()) {
                    details = " (no dest)";
                }
                String finalDetails = details;
                source.sendSuccess(
                        () -> Component.literal("\u00a77  " + status + " \u00a7e" + id + "\u00a77" + finalDetails),
                        false
                );
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeSetDelay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        ServerLevel level = source.getLevel();

        if (TutorialPortalManager.setDelay(level, id, ticks)) {
            float seconds = ticks / 20.0f;
            source.sendSuccess(
                    () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fSet portal \u00a7d" + id
                            + " \u00a7fdelay to \u00a7d" + ticks + " ticks \u00a77(" + String.format("%.1f", seconds) + "s)"
                            + (ticks == 0 ? " \u00a77(using default)" : "")),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Portal '" + id + "' not found"));
            return 0;
        }
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = source.getLevel();

        Optional<TutorialPortal> portalOpt = TutorialPortalManager.getPortal(level, id);

        if (portalOpt.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Portal '" + id + "' not found"));
            return 0;
        }

        TutorialPortal portal = portalOpt.get();
        source.sendSuccess(() -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fPortal: \u00a7d" + id), false);

        // Source area
        if (portal.hasSourceArea()) {
            BlockPos c1 = portal.getSourceCorner1();
            BlockPos c2 = portal.getSourceCorner2();
            source.sendSuccess(() -> Component.literal("\u00a77  Source: \u00a7aComplete \u2713"), false);
            source.sendSuccess(
                    () -> Component.literal("\u00a77    Corner 1: \u00a7e" + c1.getX() + ", " + c1.getY() + ", " + c1.getZ()),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77    Corner 2: \u00a7e" + c2.getX() + ", " + c2.getY() + ", " + c2.getZ()),
                    false
            );
        } else {
            BlockPos c1 = portal.getSourceCorner1();
            BlockPos c2 = portal.getSourceCorner2();
            if (c1 != null || c2 != null) {
                source.sendSuccess(() -> Component.literal("\u00a77  Source: \u00a7ePartially defined"), false);
                if (c1 != null) {
                    source.sendSuccess(
                            () -> Component.literal("\u00a77    Corner 1: \u00a7e" + c1.getX() + ", " + c1.getY() + ", " + c1.getZ()),
                            false
                    );
                }
                if (c2 != null) {
                    source.sendSuccess(
                            () -> Component.literal("\u00a77    Corner 2: \u00a7e" + c2.getX() + ", " + c2.getY() + ", " + c2.getZ()),
                            false
                    );
                }
            } else {
                source.sendSuccess(() -> Component.literal("\u00a77  Source: \u00a77Not defined"), false);
            }
        }

        // Destination
        if (portal.hasDestination()) {
            BlockPos dest = portal.getDestination();
            source.sendSuccess(() -> Component.literal("\u00a77  Destination: \u00a7aSet \u2713"), false);
            source.sendSuccess(
                    () -> Component.literal("\u00a77    Position: \u00a7e" + dest.getX() + ", " + dest.getY() + ", " + dest.getZ()),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("\u00a77    Rotation: yaw=" + String.format("%.1f", portal.getDestYaw())
                            + ", pitch=" + String.format("%.1f", portal.getDestPitch())),
                    false
            );
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Destination: \u00a77Not defined"), false);
        }

        // Delay
        if (portal.hasCustomDelay()) {
            float seconds = portal.getDelayTicks() / 20.0f;
            source.sendSuccess(
                    () -> Component.literal("\u00a77  Delay: \u00a7e" + portal.getDelayTicks() + " ticks (" + String.format("%.1f", seconds) + "s)"),
                    false
            );
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Delay: \u00a77Default (1.5s)"), false);
        }

        // Status
        if (portal.isComplete()) {
            source.sendSuccess(() -> Component.literal("\u00a77  Status: \u00a7aActive \u2713"), false);
        } else {
            source.sendSuccess(() -> Component.literal("\u00a77  Status: \u00a7cIncomplete"), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}
