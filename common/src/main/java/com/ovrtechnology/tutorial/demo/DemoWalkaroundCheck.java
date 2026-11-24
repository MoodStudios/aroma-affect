package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialPopupNetworking;
import com.ovrtechnology.network.TutorialScentCounterNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Test of the free area as a visitor gets it from the Start button, run by {@link DemoAutopilot} when the game
 * starts with {@code AROMAAFFECT_DEMO_WALKCHECK=1}: start hint, guide, cake, stable, counter animation.
 * Logged with the {@code [walkcheck]} prefix, with screenshots along the way.
 */
final class DemoWalkaroundCheck {

    private static final String TAG = "[walkcheck] ";
    private static final BlockPos CAKE = new BlockPos(22, 22, -239);

    private static int failures = 0;
    private static int countBeforeCake = 0;
    private static int countBeforeHorse = 0;

    private DemoWalkaroundCheck() {
    }

    static boolean enabled() {
        String value = System.getenv("AROMAAFFECT_DEMO_WALKCHECK");
        return value != null && !value.isBlank() && !value.equals("0");
    }

    /** @return true once the check is over */
    static boolean tick(Minecraft mc, int ticks, java.util.function.Consumer<String> shot) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return false;

        switch (ticks) {
            case 140 -> {
                check("scent counter is on", TutorialScentCounterNetworking.isClientCounterActive());
                check("start hint is showing", TutorialPopupNetworking.hasClientPopup());
                onServer(server, player -> {
                    ServerLevel level = level(player);
                    check("player was given a pickaxe", player.getInventory().contains(new ItemStack(Items.IRON_PICKAXE)));
                    check("cake is on the kitchen counter", level.getBlockState(CAKE).is(Blocks.CAKE));
                    check("guide is standing by the spawn", !guides(level).isEmpty());
                });
                shot.accept("walk_arrival");
            }
            case 160 -> onServer(server, player -> {
                List<Villager> guides = guides(level(player));
                if (!guides.isEmpty()) {
                    player.interactOn(guides.get(0), InteractionHand.MAIN_HAND);
                }
            });
            case 190 -> {
                check("guide hint is showing", TutorialPopupNetworking.getClientPopupText().startsWith("Scent Guide"));
                check("no trading screen", mc.screen == null);
                shot.accept("walk_guide");
            }
            case 260 -> onServer(server, player ->
                    player.teleportTo(level(player), 25.5, 21, -239.5, Set.of(), 90.0F, 25.0F, false));
            case 300 -> {
                countBeforeCake = TutorialScentCounterNetworking.getClientDetectedCount();
                onServer(server, player -> {
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(CAKE), Direction.EAST, CAKE, false);
                    player.gameMode.useItemOn(player, level(player), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                });
            }
            case 306 -> shot.accept("walk_cake_bump");
            case 330 -> {
                check("cake found a scent", TutorialScentCounterNetworking.getClientDetectedCount() > countBeforeCake
                        || countBeforeCake >= 16);
                check("kitchen hint is showing", TutorialPopupNetworking.getClientPopupText().contains("cake"));
                onServer(server, player -> check("cake is still whole", level(player).getBlockState(CAKE).is(Blocks.CAKE)));
                shot.accept("walk_kitchen");
            }
            case 360 -> onServer(server, player ->
                    player.teleportTo(level(player), 77.5, 20, -235.5, Set.of(), -90.0F, 15.0F, false));
            case 400 -> {
                countBeforeHorse = TutorialScentCounterNetworking.getClientDetectedCount();
                check("stable hint is showing", TutorialPopupNetworking.getClientPopupText().contains("horse"));
                shot.accept("walk_stable");
                onServer(server, player -> {
                    List<Horse> horses = level(player).getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(12));
                    check("there is a horse to feed", !horses.isEmpty());
                    if (!horses.isEmpty()) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
                        player.interactOn(horses.get(0), InteractionHand.MAIN_HAND);
                    }
                });
            }
            case 440 -> check("feeding the horse found a scent",
                    TutorialScentCounterNetworking.getClientDetectedCount() > countBeforeHorse || countBeforeHorse >= 16);
            case 460 -> {
                AromaAffect.LOGGER.info(TAG + "scents found: {}", TutorialScentCounterNetworking.getClientDetectedCount());
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

    private static List<Villager> guides(ServerLevel level) {
        return level.getEntitiesOfClass(Villager.class, new AABB(new BlockPos(23, 20, -256)).inflate(16),
                v -> v.getTags().contains("aromaaffect_scent_guide"));
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
