package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;

/**
 * Keeps the Omara room in the final area closed until the player has found all 16 scents.
 * <p>
 * The room is a tunnel in the hill next to the rain machine. A wall of planks is placed across the tunnel
 * when the scent counter starts, far enough in that the Omara devices behind it cannot reach the player.
 * The client reports 16/16 and the server breaks the wall.
 */
public final class DemoOmaraGate {

    private static final ResourceLocation UNLOCK_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "demo_omara_unlock");

    /** Wall across the tunnel: one block thick, three wide, three high. */
    private static final int WALL_X = -12;
    private static final int WALL_MIN_Y = 21;
    private static final int WALL_MAX_Y = 23;
    private static final int WALL_MIN_Z = -265;
    private static final int WALL_MAX_Z = -263;

    /** Trail from the tunnel mouth into the room, shown once the room opens. */
    private static final List<BlockPos> ROOM_PATH = List.of(
            new BlockPos(-5, 21, -264), new BlockPos(-9, 21, -264),
            new BlockPos(-13, 21, -264), new BlockPos(-17, 21, -264));
    private static final String ROOM_PATH_ID = "omara_room";
    private static final double ROOM_ARRIVAL_DISTANCE_SQ = 3 * 3;

    private static final double HINT_DISTANCE_SQ = 6 * 6;
    private static final int HINT_INTERVAL_TICKS = 20;

    private static boolean initialized = false;
    private static boolean unlocked = false;
    private static int hintTicks = 0;
    /** Player being guided to the open room, or null. */
    private static UUID guidedPlayer = null;
    private static int guideTicks = 0;

    private DemoOmaraGate() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UNLOCK_PACKET, (buf, context) ->
                context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        unlock(player);
                    }
                }));

        TickEvent.SERVER_POST.register(server -> {
            if (guidedPlayer == null || ++guideTicks < 10) return;
            guideTicks = 0;
            ServerPlayer player = server.getPlayerList().getPlayer(guidedPlayer);
            if (player == null) {
                guidedPlayer = null;
                return;
            }
            BlockPos end = ROOM_PATH.get(ROOM_PATH.size() - 1);
            if (player.level().dimension() == Level.OVERWORLD
                    && player.distanceToSqr(end.getX() + 0.5, end.getY(), end.getZ() + 0.5) <= ROOM_ARRIVAL_DISTANCE_SQ) {
                guidedPlayer = null;
                TutorialWaypointNetworking.sendClearToPlayer(player);
            }
        });
    }

    /** Closes the room. Called when the scent counter starts. */
    public static void lock(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        unlocked = false;
        for (BlockPos pos : wall()) {
            level.setBlock(pos, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
        }
    }

    private static void unlock(ServerPlayer player) {
        if (unlocked) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || !TutorialModule.isActive(level)) return;
        unlocked = true;

        for (BlockPos pos : wall()) {
            level.destroyBlock(pos, false);
        }
        BlockPos center = new BlockPos(WALL_X, WALL_MIN_Y + 1, (WALL_MIN_Z + WALL_MAX_Z) / 2);
        level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.literal(
                "§d§lThe Omara room is open! §dFollow the trail to the hill next to the rain machine."));

        List<BlockPos> path = new java.util.ArrayList<>();
        path.add(player.blockPosition());
        path.addAll(ROOM_PATH);
        TutorialWaypointNetworking.sendWaypointToPlayer(player, ROOM_PATH_ID, path);
        guidedPlayer = player.getUUID();
        guideTicks = 0;
        AromaAffect.LOGGER.info("All 16 scents found, Omara room opened");
    }

    /** Forgets the previous world's state. */
    public static void reset() {
        unlocked = false;
        guidedPlayer = null;
    }

    private static Iterable<BlockPos> wall() {
        return BlockPos.betweenClosed(WALL_X, WALL_MIN_Y, WALL_MIN_Z, WALL_X, WALL_MAX_Y, WALL_MAX_Z);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client side
    // ─────────────────────────────────────────────────────────────────────────

    /** Sent by the client when its counter reaches the total. */
    public static void sendUnlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.level == null) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level.registryAccess());
        NetworkManager.sendToServer(UNLOCK_PACKET, buf);
    }

    /** Tells a player standing at the closed wall what opens it. */
    public static void tickClient(int found, int total) {
        if (++hintTicks < HINT_INTERVAL_TICKS) return;
        hintTicks = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;
        if (found >= total) return;

        double dx = mc.player.getX() - (WALL_X + 0.5);
        double dy = mc.player.getY() - WALL_MIN_Y;
        double dz = mc.player.getZ() - ((WALL_MIN_Z + WALL_MAX_Z) / 2.0 + 0.5);
        if (dx * dx + dy * dy + dz * dz > HINT_DISTANCE_SQ) return;

        mc.player.displayClientMessage(Component.literal(
                "§eFind all " + total + " scents to open this door §7(" + found + "/" + total + ")"), true);
    }
}
