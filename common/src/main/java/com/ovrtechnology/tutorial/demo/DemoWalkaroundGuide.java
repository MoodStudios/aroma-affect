package com.ovrtechnology.tutorial.demo;

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

    /** What the guide says, one per click, starting over after the last. */
    private static final List<String> GUIDE_HINTS = List.of(
            "There are 16 scents around here. Each new one adds to the counter at the top right.",
            "Something sweet is waiting in the kitchen of the big house.",
            "Feeling cold? Light the stove in the kitchen and get cozy.",
            "The horses at the stable love apples. There is a chest full of them right next to them.",
            "Raw beef is in a chest by the furnace, west of the big house. Cook it!",
            "Mine the ores with your pickaxe. Different rocks smell different.",
            "Bees make honey. Find the honey bottles and take a sip.",
            "The cat in the little house would love some attention.",
            "Look for the chest by the campfire. A flint & steel makes things smoky.",
            "Not every scent is in a chest: walk by the water, the flowers, the trees and the snow."
    );

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
    private static final Map<UUID, Integer> nextGuideHint = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastCake = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastStove = new ConcurrentHashMap<>();
    /** Server tick at which the stove goes out, or -1 when it is out. */
    private static long stoveOutAt = -1;

    private DemoWalkaroundGuide() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

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
        if (giveTools) {
            DemoFinalArea.giveTools(player);
        }
        placeCake(level);
        stoveOutAt = -1;
        setStoveLit(level, false);
        ensureGuide(level);
        showTimed(player, START_HINT, START_HINT_TICKS);
    }

    public static void resetPlayer(UUID playerId) {
        shown.remove(playerId);
        timedUntil.remove(playerId);
        nextGuideHint.remove(playerId);
        lastCake.remove(playerId);
        lastStove.remove(playerId);
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
        int index = nextGuideHint.getOrDefault(player.getUUID(), 0);
        nextGuideHint.put(player.getUUID(), (index + 1) % GUIDE_HINTS.size());

        player.level().playSound(null, guide.getX(), guide.getY(), guide.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f);
        showTimed(player, "Scent Guide: " + GUIDE_HINTS.get(index), GUIDE_HINT_TICKS);
    }
}
