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

        if (System.getenv("AROMAAFFECT_DEMO_SURVEY") != null && ticks >= TOUR_START) {
            return tour(mc, server, ticks - TOUR_START, shot);
        }
        switch (ticks) {
            case 100 -> onServer(server, player -> {
                // Every scent zone in the world and how far it is from where the walkaround starts
                ServerLevel level = level(player);
                for (var zone : com.ovrtechnology.tutorial.scentzone.TutorialScentZoneManager.getAllZones(level)) {
                    AromaAffect.LOGGER.info(TAG + "zone {} scent={} at {} r={}/{}/{} enabled={} dist={}", zone.getId(),
                            zone.getScentName(), zone.getPosition().toShortString(), zone.getRadiusX(), zone.getRadiusY(),
                            zone.getRadiusZ(), zone.isEnabled(), (int) Math.sqrt(zone.getPosition().distSqr(player.blockPosition())));
                }
                AromaAffect.LOGGER.info(TAG + "player at {}", player.blockPosition().toShortString());
                if (System.getenv("AROMAAFFECT_DEMO_SURVEY") != null) survey(level, player.blockPosition());
            });
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
            case 320 -> onServer(server, player -> {
                BlockPos stove = new BlockPos(18, 22, -243);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(stove), Direction.EAST, stove, false);
                player.gameMode.useItemOn(player, level(player), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                check("stove is lit", DemoWalkaroundGuide.isStoveLit(level(player)));
            });
            case 330 -> {
                check("cake found a scent", TutorialScentCounterNetworking.getClientDetectedCount() > countBeforeCake
                        || countBeforeCake >= 16);
                check("cake and stove found two scents", TutorialScentCounterNetworking.getClientDetectedCount() >= countBeforeCake + 2
                        || countBeforeCake >= 15);
                check("kitchen hint is showing", TutorialPopupNetworking.getClientPopupText().contains("cake"));
                check("no furnace screen", mc.screen == null);
                onServer(server, player -> check("cake is still whole", level(player).getBlockState(CAKE).is(Blocks.CAKE)));
                shot.accept("walk_kitchen");
            }
            case 360 -> onServer(server, player ->
                    player.teleportTo(level(player), 77.5, standingY(level(player), 77, -236, 20), -235.5, Set.of(), -90.0F, 15.0F, false));
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
                if (System.getenv("AROMAAFFECT_DEMO_SURVEY") != null) return false;
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

    /** First height from {@code fromY} up with room to stand, so a teleport never lands inside a wall. */
    private static int standingY(ServerLevel level, int x, int z, int fromY) {
        for (int y = fromY; y < fromY + 16; y++) {
            BlockPos feet = new BlockPos(x, y, z);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return y;
            }
        }
        return fromY;
    }

    private static final int TOUR_START = 500;
    private static final int TOUR_STEP = 100;

    /** Name, where to look and stand next to; one stop per scent source the survey found. */
    private static final Object[][] TOUR = {
            {"Timber (chest)", 9, 21, -243},
            {"Savory Spice (llama)", -7, 20, -234},
            {"Evergreen (fox)", -6, 21, -251},
            {"Smoky (lava, quartz)", -7, 19, -273},
            {"Machina (redstone)", 8, 20, -293},
            {"Terra Silva (ores)", -2, 21, -290},
            {"Marine (water)", 25, 19, -276},
            {"Beach (biome)", 18, 20, -276},
            {"Winter (snowy plains, polar bear)", 60, 20, -282},
            {"Sweet (honey)", 49, 21, -267},
            {"Floral (flowers)", 56, 20, -233},
            {"Petrichor (swamp)", 54, 20, -222},
            {"Kindred (cat)", 44, 20, -223},
            {"Barnyard (farm animals)", 79, 20, -247},
            {"Citrus (jungle, pandas)", 18, 20, -204},
            {"Desert (scent zone by the path)", -4, 23, -212},
    };

    /** Walks the free area stop by stop and logs what the counter picked up at each one. */
    private static boolean tour(Minecraft mc, MinecraftServer server, int t, java.util.function.Consumer<String> shot) {
        DemoIdleHandler.poke();
        int stop = t / TOUR_STEP;
        if (stop >= TOUR.length) {
            if (t == TOUR.length * TOUR_STEP) {
                java.util.List<String> found = TutorialScentCounterNetworking.getClientDetectedScents();
                java.util.List<String> missing = new java.util.ArrayList<>();
                for (String scent : com.ovrtechnology.trigger.config.ScentTriggerConfigLoader.VALID_OVR_SCENTS) {
                    if (!found.contains(scent)) missing.add(scent);
                }
                AromaAffect.LOGGER.info(TAG + "tour: {} of 16 found {}, missing {}", found.size(), found, missing);
                check("the tour found all 16 scents", missing.isEmpty());
            }
            if (t == TOUR.length * TOUR_STEP + 80) {
                onServer(server, player -> check("Omara room opened",
                        level(player).getBlockState(new BlockPos(-12, 22, -264)).isAir()));
                shot.accept("tour_end");
            }
            // Walk away from where the room opened: the trail has to follow
            if (t == TOUR.length * TOUR_STEP + 100) {
                onServer(server, player -> {
                    ServerLevel level = level(player);
                    player.teleportTo(level, 60.5, standingY(level, 60, -282, 20), -281.5, Set.of(), 90.0F, 20.0F, false);
                });
            }
            if (t == TOUR.length * TOUR_STEP + 180) {
                check("trail is showing after walking away",
                        com.ovrtechnology.tutorial.waypoint.client.TutorialWaypointRenderer.hasActiveWaypoint());
                shot.accept("trail_after_moving");
            }
            if (t == TOUR.length * TOUR_STEP + 200) {
                AromaAffect.LOGGER.info(failures == 0 ? TAG + "TOUR PASSED" : TAG + "TOUR FAILED: {} checks", failures);
                return true;
            }
            return false;
        }
        int at = t % TOUR_STEP;
        Object[] s = TOUR[stop];
        int x = (int) s[1], y = (int) s[2], z = (int) s[3];
        if (at == 0) {
            onServer(server, player -> {
                ServerLevel level = level(player);
                // Two blocks east of the source, facing it
                int standX = x + 2;
                int standY = standingY(level, standX, z, y);
                player.teleportTo(level, standX + 0.5, standY, z + 0.5, Set.of(), 90.0F, 20.0F, false);
            });
        }
        if (at == TOUR_STEP - 2) {
            onServer(server, player -> AromaAffect.LOGGER.info(TAG + "tour stop {} standing at {} in {}", stop + 1,
                    player.blockPosition().toShortString(),
                    level(player).getBiome(player.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("?")));
        }
        if (at == TOUR_STEP - 1) {
            AromaAffect.LOGGER.info(TAG + "tour stop {}: {} -> counter {} {}", stop + 1, s[0],
                    TutorialScentCounterNetworking.getClientDetectedCount(), TutorialScentCounterNetworking.getClientDetectedScents());
        }
        return false;
    }

    /**
     * Which scents the free area can give through passive mode: every block, biome and mob with a
     * scent within reach of the start, with how many there are and the nearest one.
     */
    private static void survey(ServerLevel level, BlockPos center) {
        int radius = 90;
        java.util.Map<String, String> blockScent = new java.util.HashMap<>();
        for (var t : com.ovrtechnology.trigger.config.ScentTriggerConfigLoader.getAllBlockTriggers()) {
            blockScent.put(t.getBlockId(), t.getScentName());
        }
        java.util.Map<String, int[]> found = new java.util.TreeMap<>();
        java.util.Map<String, BlockPos> nearest = new java.util.HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                for (int y = center.getY() - 15; y <= center.getY() + 25; y++) {
                    pos.set(x, y, z);
                    String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                            .getKey(level.getBlockState(pos).getBlock()).toString();
                    String scent = blockScent.get(id);
                    if (scent == null) continue;
                    String key = scent + " <- block " + id;
                    found.computeIfAbsent(key, k -> new int[1])[0]++;
                    BlockPos best = nearest.get(key);
                    if (best == null || pos.distSqr(center) < best.distSqr(center)) nearest.put(key, pos.immutable());
                }
                if (x % 6 == 0 && z % 6 == 0) {
                    pos.set(x, center.getY(), z);
                    String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?");
                    var trigger = com.ovrtechnology.trigger.config.ScentTriggerConfigLoader.getBiomeTrigger(biome);
                    String key = trigger.map(t -> t.getScentName()).orElse("(none)") + " <- biome " + biome;
                    found.computeIfAbsent(key, k -> new int[1])[0]++;
                    BlockPos best = nearest.get(key);
                    if (best == null || pos.distSqr(center) < best.distSqr(center)) nearest.put(key, pos.immutable());
                }
            }
        }
        for (var entity : level.getEntities((net.minecraft.world.entity.Entity) null,
                new AABB(center).inflate(radius), e -> true)) {
            String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            var trigger = com.ovrtechnology.trigger.config.ScentTriggerConfigLoader.getMobTrigger(type);
            if (trigger.isEmpty()) continue;
            String key = trigger.get().getScentName() + " <- mob " + type;
            found.computeIfAbsent(key, k -> new int[1])[0]++;
            BlockPos best = nearest.get(key);
            if (best == null || entity.blockPosition().distSqr(center) < best.distSqr(center)) nearest.put(key, entity.blockPosition());
        }
        for (var entry : found.entrySet()) {
            BlockPos best = nearest.get(entry.getKey());
            AromaAffect.LOGGER.info(TAG + "survey {} x{} nearest {} dist {}", entry.getKey(), entry.getValue()[0],
                    best.toShortString(), (int) Math.sqrt(best.distSqr(center)));
        }
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
