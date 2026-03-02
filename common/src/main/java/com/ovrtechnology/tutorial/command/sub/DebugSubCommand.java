package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.tutorial.boss.TutorialBossArea;
import com.ovrtechnology.tutorial.boss.TutorialBossAreaHandler;
import com.ovrtechnology.tutorial.boss.TutorialBossAreaManager;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.noseequip.TutorialNoseEquipTrigger;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.portal.TutorialPortal;
import com.ovrtechnology.tutorial.portal.TutorialPortalManager;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenArea;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaManager;
import com.ovrtechnology.tutorial.trade.TutorialTrade;
import com.ovrtechnology.tutorial.trade.TutorialTradeManager;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Debug command to show the full tutorial timeline and configuration.
 * <p>
 * Usage: /tutorial debug
 */
public class DebugSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String getDescription() {
        return "Show full tutorial debug info (waypoints, portals, bosses, trades, Oliver)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                .requires(source -> source.hasPermission(2))
                .executes(this::showDebugInfo);
    }

    private int showDebugInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        ServerPlayer player = source.getPlayer();
        ServerLevel playerLevel = (player != null && player.level() instanceof ServerLevel sl) ? sl : null;

        StringBuilder sb = new StringBuilder();

        sb.append("\n§d§l══════════════════════════════════════\n");
        sb.append("§d§l       TUTORIAL DEBUG INFO\n");
        sb.append("§d§l══════════════════════════════════════\n\n");

        // ═══════════════════════════════════════════════════════════════
        // OLIVER STATUS (only if player is present)
        // ═══════════════════════════════════════════════════════════════
        sb.append("§e§l► OLIVER STATUS\n");
        if (player != null && playerLevel != null) {
            TutorialOliverEntity oliver = findNearestOliver(playerLevel, player);
            if (oliver != null) {
                sb.append("§7  Found Oliver at: §f").append(formatPos(oliver.blockPosition())).append("\n");
                sb.append("§7  Dialogue ID: §f").append(oliver.getDialogueId()).append("\n");
                sb.append("§7  Has Trade: ").append(oliver.hasTrade() ? "§a✓ YES" : "§c✗ NO").append("\n");
                sb.append("§7  Trade ID: §f").append(oliver.getTradeId().isEmpty() ? "(none)" : oliver.getTradeId()).append("\n");
                sb.append("§7  Mode: §f").append(oliver.getMode().name()).append("\n");
            } else {
                sb.append("§c  No Oliver found nearby\n");
            }
        } else {
            sb.append("§7  (run as player to see Oliver status)\n");
        }
        sb.append("\n");

        // ═══════════════════════════════════════════════════════════════
        // WAYPOINTS (check all levels)
        // ═══════════════════════════════════════════════════════════════
        sb.append("§e§l► WAYPOINTS\n");
        boolean foundAnyWaypoints = false;
        for (ServerLevel level : server.getAllLevels()) {
            Set<String> waypointIds = TutorialWaypointManager.getAllWaypointIds(level);
            if (!waypointIds.isEmpty()) {
                String dimName = getDimensionName(level);
                sb.append("§6  [").append(dimName).append("]\n");
                foundAnyWaypoints = true;

                for (String id : waypointIds) {
                    Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, id);
                    if (wpOpt.isPresent()) {
                        TutorialWaypoint wp = wpOpt.get();
                        String status = wp.isComplete() ? "§a✓" : "§c✗";
                        String active = "";
                        if (player != null && id.equals(TutorialWaypointAreaHandler.getActiveWaypoint(player.getUUID()))) {
                            active = " §b[ACTIVE]";
                        }
                        sb.append("§7    ").append(status).append(" §f").append(id).append(active).append("\n");

                        // Show oliver action if set
                        String action = wp.getOliverAction();
                        if (action != null && !action.isEmpty()) {
                            sb.append("§7       Oliver Action: §f").append(action).append("\n");
                        }
                        // Show next waypoint chain if set
                        if (wp.hasNextWaypoint()) {
                            sb.append("§7       Next: §f").append(wp.getNextWaypointId()).append("\n");
                        }
                    }
                }
            }
        }
        if (!foundAnyWaypoints) {
            sb.append("§7  (none configured)\n");
        }
        sb.append("\n");

        // ═══════════════════════════════════════════════════════════════
        // PORTALS (check all levels)
        // ═══════════════════════════════════════════════════════════════
        sb.append("§e§l► PORTALS\n");
        boolean foundAnyPortals = false;
        for (ServerLevel level : server.getAllLevels()) {
            Set<String> portalIds = TutorialPortalManager.getAllPortalIds(level);
            if (!portalIds.isEmpty()) {
                String dimName = getDimensionName(level);
                sb.append("§6  [").append(dimName).append("]\n");
                foundAnyPortals = true;

                for (String id : portalIds) {
                    Optional<TutorialPortal> portalOpt = TutorialPortalManager.getPortal(level, id);
                    if (portalOpt.isPresent()) {
                        TutorialPortal portal = portalOpt.get();
                        String status = portal.isComplete() ? "§a✓" : "§c✗";
                        sb.append("§7    ").append(status).append(" §f").append(id).append("\n");
                        if (portal.hasDestination()) {
                            sb.append("§7       Dest: §f").append(formatPos(portal.getDestination())).append("\n");
                        }
                    }
                }
            }
        }
        if (!foundAnyPortals) {
            sb.append("§7  (none configured)\n");
        }
        sb.append("\n");

        // ═══════════════════════════════════════════════════════════════
        // BOSS AREAS (check all levels)
        // ═══════════════════════════════════════════════════════════════
        sb.append("§e§l► BOSS AREAS\n");
        boolean foundAnyBossAreas = false;
        for (ServerLevel level : server.getAllLevels()) {
            TutorialBossAreaManager bossManager = TutorialBossAreaManager.get(level);
            var bossAreas = bossManager.getAllAreas();
            if (!bossAreas.isEmpty()) {
                String dimName = getDimensionName(level);
                sb.append("§6  [").append(dimName).append("]\n");
                foundAnyBossAreas = true;

                for (TutorialBossArea area : bossAreas) {
                    String status = area.isComplete() ? "§a✓" : "§c✗";
                    String bossActive = TutorialBossAreaHandler.isBossActive(area.getId()) ? " §c[BOSS ACTIVE]" : "";
                    sb.append("§7    ").append(status).append(" §f").append(area.getId())
                      .append(" §7(").append(area.getBossType()).append(")").append(bossActive).append("\n");
                    if (area.hasSpawnPos()) {
                        sb.append("§7       Spawn: §f").append(formatPos(area.getSpawnPos())).append("\n");
                    }
                    sb.append("§7       Trigger: ").append(area.hasTriggerArea() ? "§a✓" : "§c✗").append("\n");
                    sb.append("§7       Movement: ").append(area.hasMovementArea() ? "§a✓" : "§7(default 8 blocks)").append("\n");
                }
            }
        }
        if (!foundAnyBossAreas) {
            sb.append("§7  (none configured)\n");
        }
        sb.append("\n");

        // ═══════════════════════════════════════════════════════════════
        // TRADES (check all levels)
        // ═══════════════════════════════════════════════════════════════
        sb.append("§e§l► TRADES\n");
        boolean foundAnyTrades = false;
        for (ServerLevel level : server.getAllLevels()) {
            Set<String> tradeIds = TutorialTradeManager.getAllTradeIds(level);
            if (!tradeIds.isEmpty()) {
                String dimName = getDimensionName(level);
                sb.append("§6  [").append(dimName).append("]\n");
                foundAnyTrades = true;

                for (String id : tradeIds) {
                    Optional<TutorialTrade> tradeOpt = TutorialTradeManager.getTrade(level, id);
                    if (tradeOpt.isPresent()) {
                        TutorialTrade trade = tradeOpt.get();
                        String status = trade.isComplete() ? "§a✓" : "§c✗";
                        sb.append("§7    ").append(status).append(" §f").append(id).append("\n");

                        // Show inputs
                        if (!trade.getInputs().isEmpty()) {
                            sb.append("§7       Inputs: ");
                            for (TutorialTrade.InputEntry input : trade.getInputs()) {
                                String itemName = input.itemId().contains(":") ?
                                    input.itemId().substring(input.itemId().indexOf(':') + 1) : input.itemId();
                                sb.append("§f").append(input.count()).append("x ").append(itemName).append("§7, ");
                            }
                            sb.setLength(sb.length() - 2); // Remove trailing ", "
                            sb.append("\n");
                        }

                        // Show output
                        if (trade.getOutputItemId() != null && !trade.getOutputItemId().isEmpty()) {
                            String itemName = trade.getOutputItemId().contains(":") ?
                                trade.getOutputItemId().substring(trade.getOutputItemId().indexOf(':') + 1) : trade.getOutputItemId();
                            sb.append("§7       Output: §f").append(trade.getOutputCount()).append("x ").append(itemName).append("\n");
                        }
                    }
                }
            }
        }
        if (!foundAnyTrades) {
            sb.append("§7  (none configured)\n");
        }
        sb.append("\n");

        // ═══════════════════════════════════════════════════════════════
        // NOSE EQUIP TRIGGERS (check all levels)
        // ═══════════════════════════════════════════════════════════════
        sb.append("§e§l► NOSE EQUIP TRIGGERS\n");
        boolean foundAnyNoseTriggers = false;
        for (ServerLevel level : server.getAllLevels()) {
            Set<String> noseIds = TutorialNoseEquipTrigger.getAllTriggerNoseIds(level);
            if (!noseIds.isEmpty()) {
                String dimName = getDimensionName(level);
                sb.append("§6  [").append(dimName).append("]\n");
                foundAnyNoseTriggers = true;

                for (String noseId : noseIds) {
                    Optional<TutorialNoseEquipTrigger.NoseEquipAction> triggerOpt =
                            TutorialNoseEquipTrigger.getTrigger(level, noseId);
                    if (triggerOpt.isPresent()) {
                        TutorialNoseEquipTrigger.NoseEquipAction trigger = triggerOpt.get();
                        sb.append("§7    §f").append(noseId).append("\n");

                        if (trigger.hasWaypoint()) {
                            sb.append("§7       Waypoint: §f").append(trigger.onCompleteWaypointId()).append("\n");
                        }
                        if (trigger.hasCinematic()) {
                            sb.append("§7       Cinematic: §f").append(trigger.onCompleteCinematicId()).append("\n");
                        }
                        if (trigger.hasAnimation()) {
                            sb.append("§7       Animation: §f").append(trigger.onCompleteAnimationId()).append("\n");
                        }
                        if (trigger.hasOliverAction()) {
                            sb.append("§7       Oliver: §f").append(trigger.onCompleteOliverAction()).append("\n");
                        }
                    }
                }
            }
        }
        if (!foundAnyNoseTriggers) {
            sb.append("§7  (none configured)\n");
        }
        sb.append("\n");

        // ═══════════════════════════════════════════════════════════════
        // REGEN AREAS (check all levels)
        // ═══════════════════════════════════════════════════════════════
        sb.append("§e§l► REGEN AREAS\n");
        boolean foundAnyRegenAreas = false;
        for (ServerLevel level : server.getAllLevels()) {
            var regenAreas = TutorialRegenAreaManager.getAllRegenAreas(level);
            if (!regenAreas.isEmpty()) {
                String dimName = getDimensionName(level);
                sb.append("§6  [").append(dimName).append("]\n");
                foundAnyRegenAreas = true;

                for (TutorialRegenArea area : regenAreas) {
                    String status = area.isEnabled() ? "§a✓" : "§c✗";
                    String complete = area.isComplete() ? "" : " §c(incomplete)";
                    int blocks = area.getSavedBlocks().size();
                    sb.append("§7    ").append(status).append(" §f").append(area.getId())
                      .append(complete)
                      .append(" §7(").append(blocks).append(" blocks, ")
                      .append(String.format("%.1fs", area.getRegenDelaySeconds())).append(" delay)\n");
                }
            }
        }
        if (!foundAnyRegenAreas) {
            sb.append("§7  (none configured)\n");
        }
        sb.append("\n");

        sb.append("§d§l══════════════════════════════════════\n");

        // Send the message
        source.sendSuccess(() -> Component.literal(sb.toString()), false);

        return Command.SINGLE_SUCCESS;
    }

    private String getDimensionName(ServerLevel level) {
        String fullName = level.dimension().location().toString();
        // Simplify common dimension names
        if (fullName.equals("minecraft:overworld")) return "Overworld";
        if (fullName.equals("minecraft:the_nether")) return "Nether";
        if (fullName.equals("minecraft:the_end")) return "End";
        // For custom dimensions, just show the path part
        if (fullName.contains(":")) {
            return fullName.substring(fullName.indexOf(':') + 1);
        }
        return fullName;
    }

    private TutorialOliverEntity findNearestOliver(ServerLevel level, ServerPlayer player) {
        AABB searchArea = new AABB(
                player.getX() - 100, player.getY() - 50, player.getZ() - 100,
                player.getX() + 100, player.getY() + 50, player.getZ() + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class, searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    private String formatPos(net.minecraft.core.BlockPos pos) {
        if (pos == null) return "(not set)";
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
