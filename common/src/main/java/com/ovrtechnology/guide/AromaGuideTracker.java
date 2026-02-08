package com.ovrtechnology.guide;

import com.ovrtechnology.network.AromaGuideNetworking;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Client-side tracker that locates the nearest village and displays
 * the distance on the action bar while the player holds an Aroma Guide.
 * <p>
 * The compass needle rotation is driven by writing a {@link LodestoneTracker}
 * component onto the held item stack, which the vanilla {@code minecraft:compass}
 * range_dispatch property reads automatically.
 * <p>
 * When the player is within ~100 blocks of a village, the compass will point
 * to the Nose Smith NPC if one is alive and loaded nearby. Otherwise it falls
 * back to the village center position.
 * <p>
 * Village lookup is performed on the server and synced to this tracker
 * every 100 ticks (~5 seconds) while the player is holding the guide.
 */
public final class AromaGuideTracker {

    private static final int SEARCH_INTERVAL_TICKS = 100;

    @Nullable
    private static BlockPos nearestVillagePos = null;

    /** The actual position the compass needle points at (Nose Smith or village). */
    @Nullable
    private static BlockPos compassTargetPos = null;

    private static int tickCounter = 0;
    private static boolean wasHolding = false;

    private AromaGuideTracker() {}

    @Nullable
    public static BlockPos getNearestVillagePos() {
        return nearestVillagePos;
    }

    /**
     * Returns the position the compass should point at.
     * This is the Nose Smith if nearby and alive, otherwise the village center.
     */
    @Nullable
    public static BlockPos getCompassTargetPos() {
        return compassTargetPos;
    }

    /**
     * Initialize the client tick listener.
     */
    public static void init() {
        ClientTickEvent.CLIENT_POST.register(AromaGuideTracker::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        boolean holdingGuide = isHoldingAromaGuide(player);
        if (!holdingGuide) {
            // Keep positions cached so the compass retains its last direction
            // instead of spinning randomly when briefly un-hovered in inventory.
            wasHolding = false;
            return;
        }

        // Trigger an immediate search when the player first picks up / switches to the guide
        if (!wasHolding) {
            wasHolding = true;
            tickCounter = SEARCH_INTERVAL_TICKS; // force search on next tick
        }

        tickCounter++;

        if (tickCounter >= SEARCH_INTERVAL_TICKS) {
            tickCounter = 0;
            AromaGuideNetworking.requestGuideTarget(player.registryAccess());
        }

        // Update the lodestone tracker component on held Aroma Guide stacks
        // so the vanilla compass property system rotates the needle.
        updateCompassComponent(player);

        // Display distance on action bar
        if (compassTargetPos != null) {
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(compassTargetPos));
            player.displayClientMessage(
                    Component.translatable("item.aromaaffect.aroma_guide.distance", distance),
                    true
            );
        } else if (nearestVillagePos != null) {
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(nearestVillagePos));
            player.displayClientMessage(
                    Component.translatable("item.aromaaffect.aroma_guide.distance", distance),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.translatable("item.aromaaffect.aroma_guide.searching"),
                    true
            );
        }
    }

    private static void updateCompassComponent(LocalPlayer player) {
        updateStackComponent(player.getMainHandItem(), player);
        updateStackComponent(player.getOffhandItem(), player);
    }

    private static void updateStackComponent(ItemStack stack, LocalPlayer player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AromaGuideItem)) return;

        BlockPos target = compassTargetPos != null ? compassTargetPos : nearestVillagePos;
        if (target != null) {
            GlobalPos globalPos = GlobalPos.of(player.level().dimension(), target);
            stack.set(DataComponents.LODESTONE_TRACKER,
                    new LodestoneTracker(Optional.of(globalPos), false));
        } else {
            // Remove component so the needle spins slowly (handled by AromaGuideCompassBehavior)
            stack.remove(DataComponents.LODESTONE_TRACKER);
        }
    }

    private static boolean isHoldingAromaGuide(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return (!mainHand.isEmpty() && mainHand.getItem() instanceof AromaGuideItem)
                || (!offHand.isEmpty() && offHand.getItem() instanceof AromaGuideItem);
    }

    public static void applyServerTarget(@Nullable BlockPos nearestVillage, @Nullable BlockPos compassTarget) {
        nearestVillagePos = nearestVillage;
        compassTargetPos = compassTarget;
    }
}
