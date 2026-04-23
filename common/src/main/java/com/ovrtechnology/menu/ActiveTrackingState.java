package com.ovrtechnology.menu;

import com.ovrtechnology.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ActiveTrackingState {

    public enum TrackingStatus {
        IDLE,
        SEARCHING,
        TRACKING,
        ARRIVED,
        NOT_FOUND,
        ERROR
    }

    private static ResourceLocation targetId;
    private static Component displayName;
    private static ItemStack icon;
    private static MenuCategory category;
    private static int distance = -1;
    private static BlockPos destination;

    private static TrackingStatus status = TrackingStatus.IDLE;
    private static String statusMessage;
    private static long statusTimestamp;
    private static ResourceLocation lastDimensionId;

    private static final long AUTO_CLEAR_MS = 3000;

    private static final long ARRIVED_CLEAR_MS = 8000;

    private ActiveTrackingState() {}

    public static void set(
            ResourceLocation id, Component name, ItemStack itemIcon, MenuCategory cat) {
        targetId = id;
        displayName = name;
        icon = itemIcon;
        category = cat;
        distance = -1;
        status = TrackingStatus.SEARCHING;
        statusMessage = null;
        statusTimestamp = System.currentTimeMillis();

        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(ModSounds.SNIFF.get(), 0.8f, 1.0f);
        }
    }

    public static void setTracking(int dist) {
        setTracking(dist, null);
    }

    public static void setTracking(int dist, BlockPos dest) {
        status = TrackingStatus.TRACKING;
        distance = dist;
        destination = dest;
        statusMessage = null;
        statusTimestamp = System.currentTimeMillis();
    }

    public static void setArrived() {
        status = TrackingStatus.ARRIVED;

        statusMessage = null;
        statusTimestamp = System.currentTimeMillis();
    }

    public static void setFailed(String reason, boolean isError) {
        status = isError ? TrackingStatus.ERROR : TrackingStatus.NOT_FOUND;
        statusMessage = reason;
        statusTimestamp = System.currentTimeMillis();
    }

    public static void clear() {
        targetId = null;
        displayName = null;
        icon = null;
        category = null;
        distance = -1;
        destination = null;
        status = TrackingStatus.IDLE;
        statusMessage = null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.level() != null) {
            ResourceLocation currentDimensionId = mc.player.level().dimension().location();
            if (lastDimensionId != null
                    && !lastDimensionId.equals(currentDimensionId)
                    && status != TrackingStatus.IDLE) {
                clear();
            }
            lastDimensionId = currentDimensionId;
        } else {
            lastDimensionId = null;
        }

        if (status == TrackingStatus.ARRIVED) {
            if (System.currentTimeMillis() - statusTimestamp > ARRIVED_CLEAR_MS) {
                clear();
            }
        } else if (status == TrackingStatus.NOT_FOUND || status == TrackingStatus.ERROR) {
            if (System.currentTimeMillis() - statusTimestamp > AUTO_CLEAR_MS) {
                clear();
            }
        }
    }

    public static void setDistance(int blocks) {
        distance = blocks;
    }

    public static int getDistance() {
        return distance;
    }

    public static TrackingStatus getStatus() {
        return status;
    }

    public static String getStatusMessage() {
        return statusMessage;
    }

    public static boolean isTracking() {
        return status != TrackingStatus.IDLE;
    }

    public static boolean isActivelyTracking() {
        return status == TrackingStatus.TRACKING;
    }

    public static boolean isTracking(ResourceLocation id) {
        return targetId != null && targetId.equals(id);
    }

    public static ResourceLocation getTargetId() {
        return targetId;
    }

    public static Component getDisplayName() {
        return displayName;
    }

    public static ItemStack getIcon() {
        return icon;
    }

    public static MenuCategory getCategory() {
        return category;
    }

    public static String getCategoryId() {
        return category != null ? category.getId() : null;
    }

    public static BlockPos getDestination() {
        return destination;
    }

    public static boolean shouldShowOutline() {
        return (status == TrackingStatus.TRACKING || status == TrackingStatus.ARRIVED)
                && destination != null
                && distance >= 0
                && distance <= 32
                && (category == MenuCategory.BLOCKS
                        || category == MenuCategory.FLOWERS
                        || category == MenuCategory.STRUCTURES);
    }
}
