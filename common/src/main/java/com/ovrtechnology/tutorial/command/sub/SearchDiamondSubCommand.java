package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ovrtechnology.network.SearchDiamondNetworking;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.searchdiamond.SearchDiamondZone;
import com.ovrtechnology.tutorial.searchdiamond.SearchDiamondZoneHandler;
import com.ovrtechnology.tutorial.searchdiamond.SearchDiamondZoneManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Subcommand for managing SearchDiamond zones.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial diamond create <id>}</li>
 *   <li>{@code /tutorial diamond delete <id>}</li>
 *   <li>{@code /tutorial diamond corner <id> <1|2>}</li>
 *   <li>{@code /tutorial diamond exitpoint <id>}</li>
 *   <li>{@code /tutorial diamond trigger <id>}</li>
 *   <li>{@code /tutorial diamond test <id>}</li>
 *   <li>{@code /tutorial diamond list}</li>
 *   <li>{@code /tutorial diamond info <id>}</li>
 *   <li>{@code /tutorial diamond regenerate <id>}</li>
 * </ul>
 */
public class SearchDiamondSubCommand implements TutorialSubCommand {

    private static final SuggestionProvider<CommandSourceStack> ZONE_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        return SharedSuggestionProvider.suggest(SearchDiamondZoneManager.getAllZoneIds(level), builder);
    };

    @Override
    public String getName() {
        return "diamond";
    }

    @Override
    public String getDescription() {
        return "Manage SearchDiamond minigame zones";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(this::executeCreate)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeDelete)))
                .then(Commands.literal("corner")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .then(Commands.argument("corner", IntegerArgumentType.integer(1, 2))
                                        .executes(this::executeCorner))))
                .then(Commands.literal("exitpoint")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeExitPoint)))
                .then(Commands.literal("trigger")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeTrigger)))
                .then(Commands.literal("test")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeTest)))
                .then(Commands.literal("list")
                        .executes(this::executeList))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeInfo)))
                .then(Commands.literal("regenerate")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(this::executeRegenerate)))
                .then(Commands.literal("hologram")
                        .executes(this::executeHologram)
                        .then(Commands.literal("clear")
                                .executes(this::executeHologramClear)))
                .then(Commands.literal("testscreen")
                        .executes(this::executeTestScreen))
                .then(Commands.literal("givetools")
                        .executes(this::executeGiveTools))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "\u00a7d[OVR Tutorial] \u00a7fSearchDiamond commands: create, delete, corner, exitpoint, trigger, test, testscreen, givetools, list, info, regenerate, hologram"), false);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = context.getSource().getLevel();
        if (SearchDiamondZoneManager.createZone(level, id)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fCreated SearchDiamond zone \u00a7d" + id), true);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendFailure(Component.literal("\u00a7cZone '" + id + "' already exists"));
        return 0;
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = context.getSource().getLevel();
        if (SearchDiamondZoneManager.deleteZone(level, id)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fDeleted SearchDiamond zone \u00a7d" + id), true);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
        return 0;
    }

    private int executeCorner(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        int corner = IntegerArgumentType.getInteger(context, "corner");
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (SearchDiamondZoneManager.setCorner(level, id, corner, player.blockPosition())) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet SearchDiamond zone \u00a7d" + id +
                            " \u00a7fcorner " + corner + " to \u00a7e" + player.blockPosition().toShortString()), true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
        return 0;
    }

    private int executeExitPoint(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (SearchDiamondZoneManager.setExitPoint(level, id, player.blockPosition())) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet SearchDiamond zone \u00a7d" + id +
                            " \u00a7fexit point to \u00a7e" + player.blockPosition().toShortString()), true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
        return 0;
    }

    private int executeTrigger(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        // Get the block the player is looking at
        var hitResult = player.pick(5.0, 0.0f, false);
        if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("\u00a7cLook at a button block"));
            return 0;
        }

        net.minecraft.core.BlockPos buttonPos = ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
        ServerLevel level = source.getLevel();

        if (SearchDiamondZoneManager.setTriggerButton(level, id, buttonPos)) {
            source.sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a7fSet SearchDiamond zone \u00a7d" + id +
                            " \u00a7ftrigger button to \u00a7e" + buttonPos.toShortString()), true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
        return 0;
    }

    private int executeTest(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        SearchDiamondZone zone = SearchDiamondZoneManager.getZone(level, id);
        if (zone == null) {
            source.sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
            return 0;
        }

        if (!zone.isComplete()) {
            source.sendFailure(Component.literal("\u00a7cZone '" + id + "' is not complete (needs corners and exit point)"));
            return 0;
        }

        SearchDiamondZoneHandler.startGame(player, zone);
        source.sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fStarted SearchDiamond game for zone \u00a7d" + id), true);
        return Command.SINGLE_SUCCESS;
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        var ids = SearchDiamondZoneManager.getAllZoneIds(level);
        if (ids.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7d[OVR Tutorial] \u00a77No SearchDiamond zones defined"), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fSearchDiamond zones (" + ids.size() + "):"), false);
        for (String id : ids) {
            SearchDiamondZone zone = SearchDiamondZoneManager.getZone(level, id);
            String status = zone != null && zone.isComplete() ? "\u00a7a[COMPLETE]" : "\u00a7c[INCOMPLETE]";
            context.getSource().sendSuccess(() -> Component.literal(
                    "\u00a77  \u00a7e" + id + " " + status), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = context.getSource().getLevel();
        SearchDiamondZone zone = SearchDiamondZoneManager.getZone(level, id);
        if (zone == null) {
            context.getSource().sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fSearchDiamond zone: \u00a7d" + id), false);
        String c1 = zone.getCorner1() != null ? zone.getCorner1().toShortString() : "not set";
        String c2 = zone.getCorner2() != null ? zone.getCorner2().toShortString() : "not set";
        String exit = zone.getExitPoint() != null ? zone.getExitPoint().toShortString() : "not set";
        String trigger = zone.getTriggerButton() != null ? zone.getTriggerButton().toShortString() : "not set";
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Corner 1: \u00a7e" + c1), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Corner 2: \u00a7e" + c2), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Exit Point: \u00a7e" + exit), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Trigger Button: \u00a7e" + trigger), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a77  Complete: " + (zone.isComplete() ? "\u00a7aYes" : "\u00a7cNo")), false);
        return Command.SINGLE_SUCCESS;
    }

    private int executeRegenerate(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        ServerLevel level = context.getSource().getLevel();
        SearchDiamondZone zone = SearchDiamondZoneManager.getZone(level, id);
        if (zone == null) {
            context.getSource().sendFailure(Component.literal("\u00a7cZone '" + id + "' not found"));
            return 0;
        }

        SearchDiamondZoneHandler.fullRegenerateZone(level, id);
        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fRegenerated SearchDiamond zone \u00a7d" + id), true);
        return Command.SINGLE_SUCCESS;
    }

    private int executeHologram(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        net.minecraft.core.BlockPos pos = player.blockPosition();
        var server = source.getServer();

        // Broadcast hologram position to all players
        SearchDiamondNetworking.broadcastHologramPosition(server, pos);

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fPlaced 'Do you want Diamonds?' hologram at \u00a7e" + pos.toShortString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private int executeHologramClear(CommandContext<CommandSourceStack> context) {
        var server = context.getSource().getServer();

        // Broadcast clear hologram to all players
        SearchDiamondNetworking.broadcastClearHologram(server);

        context.getSource().sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fCleared 'Do you want Diamonds?' hologram"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int executeTestScreen(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        // Directly send start screen packet to test
        SearchDiamondNetworking.sendStartScreen(player);
        source.sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fSent SearchDiamond start screen packet"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveTools(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("\u00a7cMust be a player"));
            return 0;
        }

        // Give Iron Pickaxe
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));

        // Give Iron Shovel
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL));

        // Give Netherite Nose (custom item)
        net.minecraft.resources.ResourceLocation noseId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                com.ovrtechnology.AromaAffect.MOD_ID, "netherite_nose");
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(noseId).ifPresent(noseItem -> {
            player.getInventory().add(new net.minecraft.world.item.ItemStack(noseItem));
        });

        source.sendSuccess(() -> Component.literal(
                "\u00a7d[OVR Tutorial] \u00a7fGave diamond search tools to player"), true);
        return Command.SINGLE_SUCCESS;
    }
}
