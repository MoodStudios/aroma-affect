package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentCounterNetworking;
import com.ovrtechnology.tutorial.dream.TutorialDreamEndHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/**
 * Test rig step for the free area at the end of the demo, run by {@link DemoAutopilot} when the game is
 * started with {@code AROMAAFFECT_DEMO_FINALCHECK=1}. It skips to the end of the dream, checks the closed
 * Omara room, mines an ore, picks berries, pets the cat, completes the 16 scents and checks the room opens.
 * Results are logged with the {@code [finalcheck]} prefix.
 */
final class DemoFinalAreaCheck {

    private static final String TAG = "[finalcheck] ";
    private static final BlockPos WALL_CENTER = new BlockPos(-12, 22, -264);
    private static final BlockPos ORE = new BlockPos(-2, 21, -293);
    private static final BlockPos DIRT = new BlockPos(-6, 21, -293);

    private static final List<String> ALL_SCENTS = List.of(
            "Desert", "Evergreen", "Citrus", "Savory Spice", "Barnyard", "Smoky", "Machina", "Winter",
            "Petrichor", "Beach", "Marine", "Floral", "Terra Silva", "Timber", "Sweet", "Kindred");

    private static int failures = 0;

    private DemoFinalAreaCheck() {
    }

    static boolean enabled() {
        String value = System.getenv("AROMAAFFECT_DEMO_FINALCHECK");
        return value != null && !value.isBlank() && !value.equals("0");
    }

    /** @return true once the check is over */
    static boolean tick(Minecraft mc, int ticks, java.util.function.Consumer<String> shot) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return false;

        switch (ticks) {
            case 40 -> onServer(server, player -> {
                AromaAffect.LOGGER.info(TAG + "starting the end of the dream");
                TutorialDreamEndHandler.startDreamSequence(player);
            });
            case 600 -> {
                if (mc.screen != null) {
                    AromaAffect.LOGGER.info(TAG + "closing {}", mc.screen.getClass().getSimpleName());
                    mc.setScreen(null);
                }
                check("scent counter is on after waking up", TutorialScentCounterNetworking.isClientCounterActive());
                onServer(server, player -> {
                    check("player was given a pickaxe", player.getInventory().contains(new ItemStack(Items.IRON_PICKAXE)));
                    check("Omara room is closed", level(player).getBlockState(WALL_CENTER).is(Blocks.SPRUCE_PLANKS));
                    player.teleportTo(level(player), -8.5, 21, -263.5, Set.of(), 90.0F, 5.0F, false);
                });
            }
            case 680 -> shot.accept("omara_closed");
            case 700 -> onServer(server, player -> {
                ServerLevel level = level(player);
                player.teleportTo(level, -2.5, 21, -290.5, Set.of(), 180.0F, 20.0F, false);
                player.gameMode.destroyBlock(ORE); // gives the scent, the ore stays so there is no hole to fall through
                check("mined ore stays in place", !level.getBlockState(ORE).isAir());
                boolean dirtBroken = player.gameMode.destroyBlock(DIRT);
                check("dirt next to it is still protected", !dirtBroken && !level.getBlockState(DIRT).isAir());

                player.getInventory().add(new ItemStack(Items.SWEET_BERRIES, 2));

                List<Cat> cats = level.getEntitiesOfClass(Cat.class, new AABB(WALL_CENTER).inflate(120));
                check("there is a cat to pet", !cats.isEmpty());
                if (!cats.isEmpty()) {
                    Cat cat = cats.get(0);
                    player.teleportTo(level, cat.getX() + 1, cat.getY(), cat.getZ(), Set.of(), 90.0F, 20.0F, false);
                    player.interactOn(cat, InteractionHand.MAIN_HAND);
                }
            });
            case 760 -> {
                int found = TutorialScentCounterNetworking.getClientDetectedCount();
                AromaAffect.LOGGER.info(TAG + "scents found by mining, berries and the cat: {}", found);
                for (String scent : ALL_SCENTS) {
                    TutorialScentCounterNetworking.onScentTriggered(scent);
                }
                check("counter reached 16", TutorialScentCounterNetworking.getClientDetectedCount() == 16);
            }
            case 840 -> onServer(server, player -> {
                check("Omara room opened", level(player).getBlockState(WALL_CENTER).isAir());
                player.teleportTo(level(player), -8.5, 21, -263.5, Set.of(), 90.0F, 5.0F, false);
            });
            case 880 -> check("trail to the room is showing",
                    com.ovrtechnology.tutorial.waypoint.client.TutorialWaypointRenderer.hasActiveWaypoint());
            case 900 -> shot.accept("omara_open");
            case 920 -> {
                if (failures == 0) {
                    AromaAffect.LOGGER.info(TAG + "PASSED");
                } else {
                    AromaAffect.LOGGER.error(TAG + "FAILED: {} checks", failures);
                }
                return true;
            }
            default -> { }
        }
        return false;
    }

    private static ServerLevel level(ServerPlayer player) {
        return player.level().getServer().getLevel(Level.OVERWORLD);
    }

    private static void onServer(MinecraftServer server, java.util.function.Consumer<ServerPlayer> action) {
        server.execute(() -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (!players.isEmpty()) {
                action.accept(players.get(0));
            }
        });
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            AromaAffect.LOGGER.info(TAG + "ok: {}", what);
        } else {
            failures++;
            AromaAffect.LOGGER.error(TAG + "FAILED: {}", what);
        }
    }
}
