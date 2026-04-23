package com.ovrtechnology.guide;

import com.ovrtechnology.network.AromaGuideNetworking;
import com.ovrtechnology.util.Texts;
import dev.architectury.event.events.client.ClientTickEvent;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import org.jetbrains.annotations.Nullable;

public final class AromaGuideTracker {

    private static final int SEARCH_INTERVAL_TICKS = 100;

    @Nullable private static BlockPos nearestVillagePos = null;

    @Nullable private static BlockPos compassTargetPos = null;

    private static int tickCounter = 0;
    private static boolean wasHolding = false;

    private AromaGuideTracker() {}

    @Nullable
    public static BlockPos getCompassTargetPos() {
        return compassTargetPos;
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(AromaGuideTracker::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        boolean holdingGuide = isHoldingAromaGuide(player);
        if (!holdingGuide) {

            wasHolding = false;
            return;
        }

        if (!wasHolding) {
            wasHolding = true;
            tickCounter = SEARCH_INTERVAL_TICKS;
        }

        tickCounter++;

        if (tickCounter >= SEARCH_INTERVAL_TICKS) {
            tickCounter = 0;
            AromaGuideNetworking.requestGuideTarget(player.registryAccess());
        }

        updateCompassComponent(player);

        if (compassTargetPos != null) {
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(compassTargetPos));
            player.displayClientMessage(
                    Texts.tr("item.aromaaffect.aroma_guide.distance", distance), true);
        } else if (nearestVillagePos != null) {
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(nearestVillagePos));
            player.displayClientMessage(
                    Texts.tr("item.aromaaffect.aroma_guide.distance", distance), true);
        } else {
            player.displayClientMessage(Texts.tr("item.aromaaffect.aroma_guide.searching"), true);
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
            stack.set(
                    DataComponents.LODESTONE_TRACKER,
                    new LodestoneTracker(Optional.of(globalPos), false));
        } else {

            stack.remove(DataComponents.LODESTONE_TRACKER);
        }
    }

    private static boolean isHoldingAromaGuide(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return (!mainHand.isEmpty() && mainHand.getItem() instanceof AromaGuideItem)
                || (!offHand.isEmpty() && offHand.getItem() instanceof AromaGuideItem);
    }

    public static void applyServerTarget(
            @Nullable BlockPos nearestVillage, @Nullable BlockPos compassTarget) {
        nearestVillagePos = nearestVillage;
        compassTargetPos = compassTarget;
    }
}
