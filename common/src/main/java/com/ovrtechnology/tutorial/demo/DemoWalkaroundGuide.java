package com.ovrtechnology.tutorial.demo;

import dev.architectury.networking.NetworkManager;
import net.minecraft.resources.ResourceLocation;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialPopupNetworking;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guidance for the free area (Walkaround, and the end of the guided demo, which wakes up in the same place):
 * a hint when the player walks up to something that gives a scent, a hint on arrival, a guide villager
 * that gives a hint every time it is clicked, and a cake and a stove in the house kitchen that give scents.
 * <p>
 * Everything here is placed by code, so the template map does not have to change.
 */
public final class DemoWalkaroundGuide {

    private record Hint(String id, AABB area, String text) {
        static Hint of(String id, int x1, int y1, int z1, int x2, int y2, int z2, String text) {
            return new Hint(id, new AABB(x1, y1, z1, x2 + 1, y2 + 1, z2 + 1), text);
        }
    }

    /** Shown while the player is inside the area, in this order of priority. */
    private static final List<Hint> AREA_HINTS = List.of(
            Hint.of("kitchen", 18, 20, -243, 35, 26, -238,
                    "Right click the cake on the counter, then light the stove in the corner. Something smells good!"),
            Hint.of("beef", 4, 17, -246, 14, 26, -228,
                    "Grab the raw beef from the chest and cook it in the furnace. Right click to open them."),
            Hint.of("stable", 70, 17, -251, 84, 28, -228,
                    "Take an apple from the chest, hold it and right click a horse to feed it."),
            Hint.of("flint", -7, 17, -285, 4, 26, -273,
                    "There is a flint & steel in the chest. Hold it and right click the ground."),
            Hint.of("honey", 45, 17, -275, 53, 26, -267,
                    "Take a honey bottle from the chest and drink it (hold right click)."),
            Hint.of("cat", 39, 17, -226, 48, 26, -215,
                    "Right click the cat to pet it.")
    );

    private static final String START_HINT =
            "Find all 16 scents! Open chests, use what you find and explore. "
                    + "Switch items with 1-9 or the mouse wheel. Talk to the Scent Guide for hints.";
    private static final int START_HINT_TICKS = 240;
    private static final int GUIDE_HINT_TICKS = 180;

    /** One hint per scent: where to find it and what to do there. The guide only gives the ones still missing. */
    private record ScentHint(String scent, BlockPos at, String text) {}

    private static final List<ScentHint> SCENT_HINTS = List.of(
            new ScentHint("Sweet", new BlockPos(22, 22, -239), "Something sweet is on the kitchen counter of the big house. Right click the cake!"),
            new ScentHint("Savory Spice", new BlockPos(18, 22, -243), "Light the stove in the kitchen of the big house and get cozy."),
            new ScentHint("Timber", new BlockPos(9, 21, -243), "Wooden chests smell of timber. Walk up to one and look at it."),
            new ScentHint("Barnyard", new BlockPos(77, 20, -235), "The horses at the stable love apples. Grab one from the chest and feed them."),
            new ScentHint("Kindred", new BlockPos(44, 20, -223), "The cat in the little house would love some attention. Right click it."),
            new ScentHint("Smoky", new BlockPos(-7, 19, -273), "Find the lava, or the flint & steel in the chest by the campfire."),
            new ScentHint("Machina", new BlockPos(8, 20, -293), "Mine the red redstone ore with your pickaxe."),
            new ScentHint("Terra Silva", new BlockPos(-2, 21, -290), "Mine the iron, gold or coal ores with your pickaxe."),
            new ScentHint("Evergreen", new BlockPos(-6, 21, -251), "Look for the foxes among the trees, or pick some sweet berries."),
            new ScentHint("Floral", new BlockPos(56, 20, -233), "Pick some flowers from the meadow."),
            new ScentHint("Marine", new BlockPos(25, 19, -276), "Go touch the water."),
            new ScentHint("Beach", new BlockPos(18, 20, -276), "Walk on the sand of the beach by the water."),
            new ScentHint("Winter", new BlockPos(60, 20, -282), "Feeling warm? Find the snow, where the polar bears live."),
            new ScentHint("Petrichor", new BlockPos(54, 20, -222), "Walk into the swamp."),
            new ScentHint("Citrus", new BlockPos(18, 20, -204), "Visit the jungle, where the pandas and parrots are."),
            new ScentHint("Desert", new BlockPos(-2, 23, -212), "Walk into the desert by the path.")
    );

    private static final ResourceLocation FOUND_PACKET =
            ResourceLocation.fromNamespaceAndPath(com.ovrtechnology.AromaAffect.MOD_ID, "demo_scent_found");
    /** Scents each player has found, as their counter reports them. */
    private static final Map<UUID, java.util.Set<String>> found = new ConcurrentHashMap<>();
    /** How many times each player has asked the guide since the last scent; picks the hint. */
    private static final Map<UUID, Integer> guideTurn = new ConcurrentHashMap<>();

    private static final BlockPos CAKE_POS = new BlockPos(22, 22, -239);
    private static final BlockPos GUIDE_POS = new BlockPos(23, 20, -256);
    private static final float GUIDE_YAW = 23.0f;
    private static final String GUIDE_TAG = "aromaaffect_scent_guide";
    private static final long CAKE_COOLDOWN_MS = 5000;
    /** Furnace on the kitchen counter, used as the stove. */
    private static final BlockPos STOVE_POS = new BlockPos(18, 22, -243);
    private static final int STOVE_LIT_TICKS = 20 * 12;

    private static final int CHECK_INTERVAL = 10;
    private static final String TIMED = "#timed";

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static long serverTicks = 0;

    /** Area hint id, {@link #TIMED}, or nothing when no popup of ours is up. */
    private static final Map<UUID, String> shown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> timedUntil = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastCake = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastStove = new ConcurrentHashMap<>();

    /**
     * Scents that only the biome gives in the free area. Passive mode lets nearby water, animals
     * and blocks win over the biome, so on their own these never played (Marine on the beach, the
     * llamas in the desert): walking into the biome gives the scent directly.
     */
    private static final Map<String, String> BIOME_SCENTS = Map.of(
            "minecraft:beach", "Beach",
            "minecraft:desert", "Desert",
            "minecraft:swamp", "Petrichor");
    private static final Map<UUID, String> lastBiome = new ConcurrentHashMap<>();
    /** Server tick at which the stove goes out, or -1 when it is out. */
    private static long stoveOutAt = -1;

    private DemoWalkaroundGuide() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, FOUND_PACKET, (buf, context) -> {
            String scent = buf.readUtf();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    if (found.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet()).add(scent)) {
                        // The next question starts again from the closest one still missing
                        guideTurn.remove(player.getUUID());
                    }
                }
            });
        });

        TickEvent.SERVER_POST.register(server -> {
            serverTicks++;
            if (stoveOutAt >= 0 && serverTicks >= stoveOutAt) {
                stoveOutAt = -1;
                ServerLevel overworld = server.overworld();
                if (TutorialModule.isActive(overworld)) setStoveLit(overworld, false);
            }
            if (++tickCounter < CHECK_INTERVAL) return;
            tickCounter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level() instanceof ServerLevel level && TutorialModule.isActive(level)) {
                    tickPlayer(player, level);
                }
            }
        });

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(serverPlayer.level() instanceof ServerLevel level) || !TutorialModule.isActive(level)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (pos.equals(CAKE_POS) && level.getBlockState(pos).is(Blocks.CAKE)) {
                smellCake(serverPlayer, level);
                // The cake stays whole for the next bite and the next visitor
                return InteractionResult.SUCCESS;
            }
            if (pos.equals(STOVE_POS) && level.getBlockState(pos).is(Blocks.FURNACE)) {
                lightStove(serverPlayer, level);
                // Lit by hand, no furnace screen
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        BlockEvent.BREAK.register((level, pos, state, player, xp) ->
                (pos.equals(CAKE_POS) && state.is(Blocks.CAKE)) || pos.equals(STOVE_POS)
                        ? EventResult.interruptFalse() : EventResult.pass());

        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            if (!entity.getTags().contains(GUIDE_TAG)) return EventResult.pass();
            if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
                talkToGuide(serverPlayer, entity);
            }
            // No trading screen, on either side
            return EventResult.interruptTrue();
        });
    }

    /**
     * Sets up the free area for a player who just arrived: pickaxe, cake, guide and the first hint.
     * Called once the player is standing in the area.
     */
    public static void onArrive(ServerPlayer player, boolean giveTools) {
        if (!(player.level() instanceof ServerLevel level)) return;
        // No "<player> has made the advancement ..." in the chat of a station
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_ANNOUNCE_ADVANCEMENTS)
                .set(false, level.getServer());
        if (giveTools) {
            DemoFinalArea.giveTools(player);
        }
        found.remove(player.getUUID());
        placeCake(level);
        stoveOutAt = -1;
        setStoveLit(level, false);
        ensureGuide(level);
        showTimed(player, START_HINT, START_HINT_TICKS);
    }

    public static void resetPlayer(UUID playerId) {
        shown.remove(playerId);
        timedUntil.remove(playerId);
        found.remove(playerId);
        guideTurn.remove(playerId);
        lastCake.remove(playerId);
        lastStove.remove(playerId);
        lastBiome.remove(playerId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void tickPlayer(ServerPlayer player, ServerLevel level) {
        UUID id = player.getUUID();
        String current = shown.get(id);
        if (!DemoFinalArea.contains(level, player.blockPosition())) {
            if (current != null) {
                clear(player);
            }
            return;
        }

        String biome = level.getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location().toString()).orElse("");
        if (!biome.equals(lastBiome.put(id, biome)) && BIOME_SCENTS.containsKey(biome)) {
            TutorialScentZoneNetworking.sendScentTrigger(player, BIOME_SCENTS.get(biome), 1.0, "biome_" + biome);
        }

        Hint here = null;
        for (Hint hint : AREA_HINTS) {
            if (hint.area().contains(player.getX(), player.getY(), player.getZ())) {
                here = hint;
                break;
            }
        }

        if (here != null) {
            if (!here.id().equals(current)) {
                shown.put(id, here.id());
                timedUntil.remove(id);
                TutorialPopupNetworking.sendPopup(player, here.text());
            }
        } else if (TIMED.equals(current)) {
            if (serverTicks >= timedUntil.getOrDefault(id, 0L)) {
                clear(player);
            }
        } else if (current != null) {
            clear(player);
        }
    }

    private static void showTimed(ServerPlayer player, String text, int ticks) {
        shown.put(player.getUUID(), TIMED);
        timedUntil.put(player.getUUID(), serverTicks + ticks);
        TutorialPopupNetworking.sendPopup(player, text);
    }

    private static void clear(ServerPlayer player) {
        shown.remove(player.getUUID());
        timedUntil.remove(player.getUUID());
        TutorialPopupNetworking.sendClearPopup(player);
    }

    private static void placeCake(ServerLevel level) {
        if (level.getBlockState(CAKE_POS).isAir()) {
            level.setBlock(CAKE_POS, Blocks.CAKE.defaultBlockState(), 3);
        }
    }

    private static void smellCake(ServerPlayer player, ServerLevel level) {
        level.playSound(null, CAKE_POS, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8f, 1.1f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                CAKE_POS.getX() + 0.5, CAKE_POS.getY() + 0.6, CAKE_POS.getZ() + 0.5, 6, 0.25, 0.15, 0.25, 0.0);

        long now = System.currentTimeMillis();
        Long last = lastCake.get(player.getUUID());
        if (last != null && now - last < CAKE_COOLDOWN_MS) return;
        lastCake.put(player.getUUID(), now);
        TutorialScentZoneNetworking.sendScentTrigger(player, "Sweet", 1.0, "kitchen_cake");
    }

    private static void lightStove(ServerPlayer player, ServerLevel level) {
        setStoveLit(level, true);
        stoveOutAt = serverTicks + STOVE_LIT_TICKS;
        level.playSound(null, STOVE_POS, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        level.playSound(null, STOVE_POS, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.FLAME,
                STOVE_POS.getX() + 0.5, STOVE_POS.getY() + 0.4, STOVE_POS.getZ() + 0.5, 8, 0.2, 0.15, 0.2, 0.01);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                STOVE_POS.getX() + 0.5, STOVE_POS.getY() + 1.0, STOVE_POS.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.01);

        long now = System.currentTimeMillis();
        Long last = lastStove.get(player.getUUID());
        if (last != null && now - last < CAKE_COOLDOWN_MS) return;
        lastStove.put(player.getUUID(), now);
        TutorialScentZoneNetworking.sendScentTrigger(player, "Savory Spice", 1.0, "kitchen_stove");
    }

    private static void setStoveLit(ServerLevel level, boolean lit) {
        BlockState state = level.getBlockState(STOVE_POS);
        if (state.is(Blocks.FURNACE) && state.getValue(AbstractFurnaceBlock.LIT) != lit) {
            level.setBlock(STOVE_POS, state.setValue(AbstractFurnaceBlock.LIT, lit), 3);
        }
    }

    static boolean isStoveLit(ServerLevel level) {
        BlockState state = level.getBlockState(STOVE_POS);
        return state.is(Blocks.FURNACE) && state.getValue(AbstractFurnaceBlock.LIT);
    }

    private static void ensureGuide(ServerLevel level) {
        level.getChunk(GUIDE_POS);
        AABB around = new AABB(GUIDE_POS).inflate(32);
        boolean present = !level.getEntitiesOfClass(Villager.class, around, v -> v.getTags().contains(GUIDE_TAG)).isEmpty();
        if (present) return;

        Villager guide = new Villager(EntityType.VILLAGER, level);
        guide.snapTo(GUIDE_POS.getX() + 0.5, GUIDE_POS.getY(), GUIDE_POS.getZ() + 0.5, GUIDE_YAW, 0.0f);
        guide.setYHeadRot(GUIDE_YAW);
        guide.setYBodyRot(GUIDE_YAW);
        guide.setNoAi(true);
        guide.setInvulnerable(true);
        guide.setSilent(true);
        guide.setPersistenceRequired();
        guide.setCustomName(Component.literal("Scent Guide"));
        guide.setCustomNameVisible(true);
        guide.addTag(GUIDE_TAG);
        level.addFreshEntity(guide);
        AromaAffect.LOGGER.info("Scent Guide placed at {}", GUIDE_POS);
    }

    private static void talkToGuide(ServerPlayer player, Entity guide) {
        player.level().playSound(null, guide.getX(), guide.getY(), guide.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f);
        showTimed(player, "Scent Guide: " + guideHint(player), GUIDE_HINT_TICKS);
    }

    /**
     * A hint for the closest scent the player hasn't found yet, with which way to go. Talking
     * again right away gives the next closest one, so a player stuck on one can ask for another.
     */
    private static String guideHint(ServerPlayer player) {
        java.util.Set<String> have = found.getOrDefault(player.getUUID(), java.util.Set.of());
        List<ScentHint> missing = new java.util.ArrayList<>();
        for (ScentHint hint : SCENT_HINTS) {
            if (!have.contains(hint.scent())) missing.add(hint);
        }
        if (missing.isEmpty()) {
            return "You found all 16 scents! The Omara room is open, follow the trail.";
        }
        BlockPos here = player.blockPosition();
        missing.sort(java.util.Comparator.comparingDouble(h -> h.at().distSqr(here)));
        int index = guideTurn.merge(player.getUUID(), 1, Integer::sum) - 1;
        ScentHint hint = missing.get(index % missing.size());
        String left = missing.size() == 1 ? "Just one scent left: " : missing.size() + " scents left. ";
        return left + hint.scent() + ": " + hint.text() + " " + direction(here, hint.at());
    }

    /** "About 40 blocks to the north-east", or "Right around here." */
    private static String direction(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        if (distance < 8) return "It's right around here.";
        // Minecraft: north is -Z, east is +X
        String[] names = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        int octant = (int) Math.round(angle / 45.0);
        String way = names[((octant % 8) + 8) % 8];
        return "About " + (distance + 4) / 5 * 5 + " blocks to the " + way + ".";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client side
    // ─────────────────────────────────────────────────────────────────────────

    /** Tells the server the player's counter found a new scent, so the guide knows what is left. */
    public static void reportFound(String scent) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() == null || mc.level == null) return;
        net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), mc.level.registryAccess());
        buf.writeUtf(scent);
        NetworkManager.sendToServer(FOUND_PACKET, buf);
    }
}
