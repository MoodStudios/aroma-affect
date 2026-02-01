package com.ovrtechnology.guide;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import org.jetbrains.annotations.Nullable;

/**
 * Client-side tracker that locates the nearest village and displays
 * the distance on the action bar while the player holds an Aroma Guide.
 * <p>
 * The compass needle direction is handled by {@link AromaGuideCompassBehavior}
 * which hooks into the item property system.
 * <p>
 * Village lookup runs on the integrated server thread every 100 ticks
 * (~5 seconds) to avoid performance issues.
 */
public final class AromaGuideTracker {

    private static final int SEARCH_INTERVAL_TICKS = 100;
    private static final int SEARCH_RADIUS = 100; // in chunks

    @Nullable
    private static BlockPos nearestVillagePos = null;
    private static int tickCounter = 0;

    private AromaGuideTracker() {}

    /**
     * Returns the current nearest village position, or null if none found.
     * Used by the compass angle calculation.
     */
    @Nullable
    public static BlockPos getNearestVillagePos() {
        return nearestVillagePos;
    }

    /**
     * Initialize the client tick listener.
     * Call from {@link com.ovrtechnology.AromaCraftClient#init()}.
     */
    public static void init() {
        ClientTickEvent.CLIENT_POST.register(AromaGuideTracker::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        boolean holdingGuide = isHoldingAromaGuide(player);
        if (!holdingGuide) {
            // Reset when not holding
            nearestVillagePos = null;
            return;
        }

        tickCounter++;

        // Perform the actual structure search on the server thread (integrated server).
        // For dedicated servers, the server sends locate results via the vanilla system;
        // here we piggyback on the integrated server for singleplayer / LAN.
        if (tickCounter >= SEARCH_INTERVAL_TICKS) {
            tickCounter = 0;
            locateNearestVillage(client, player);
        }

        // Display distance on action bar every tick (smooth updates)
        if (nearestVillagePos != null) {
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(nearestVillagePos));
            player.displayClientMessage(
                    Component.translatable("item.aromacraft.aroma_guide.distance", distance),
                    true // action bar (above hotbar)
            );
        } else {
            player.displayClientMessage(
                    Component.translatable("item.aromacraft.aroma_guide.searching"),
                    true
            );
        }
    }

    private static boolean isHoldingAromaGuide(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return (!mainHand.isEmpty() && mainHand.getItem() instanceof AromaGuideItem)
                || (!offHand.isEmpty() && offHand.getItem() instanceof AromaGuideItem);
    }

    private static void locateNearestVillage(Minecraft client, LocalPlayer player) {
        // Only works on integrated server (singleplayer / LAN host)
        if (client.getSingleplayerServer() == null) return;

        ServerLevel serverLevel = client.getSingleplayerServer().getLevel(player.level().dimension());
        if (serverLevel == null) return;

        BlockPos playerPos = player.blockPosition();

        // Use the server's structure manager to find the nearest village.
        // BuiltinStructures.VILLAGE is the tag/key for all village variants.
        var result = serverLevel.getChunkSource().getGenerator()
                .findNearestMapStructure(
                        serverLevel,
                        serverLevel.registryAccess()
                                .lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                                .getOrThrow(net.minecraft.tags.StructureTags.VILLAGE),
                        playerPos,
                        SEARCH_RADIUS,
                        false
                );

        if (result != null) {
            nearestVillagePos = result.getFirst();
        } else {
            nearestVillagePos = null;
        }
    }
}
