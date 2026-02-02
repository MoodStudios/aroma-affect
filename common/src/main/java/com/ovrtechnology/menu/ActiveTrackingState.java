package com.ovrtechnology.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side singleton that holds the currently active tracking target.
 *
 * <p>Only one target can be tracked at a time across all categories (blocks, biomes,
 * structures, flowers). All selection menu screens write to this state when the
 * player picks a target, and clear it on deselection or path stop.</p>
 *
 * <p>The radial menu reads this state to display an active tracking info panel
 * and to mark which category slice has an active track.</p>
 */
public final class ActiveTrackingState {

    /**
     * Tracking status state machine.
     *
     * <pre>
     * IDLE → SEARCHING → TRACKING → ARRIVED → (auto-clear) → IDLE
     *                   → NOT_FOUND → (auto-clear) → IDLE
     *                   → ERROR → (auto-clear) → IDLE
     * </pre>
     */
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

    private static TrackingStatus status = TrackingStatus.IDLE;
    private static String statusMessage;
    private static long statusTimestamp;

    /** Duration in milliseconds before terminal states auto-clear. */
    private static final long AUTO_CLEAR_MS = 3000;

    private ActiveTrackingState() {}

    /**
     * Sets the currently tracked target and transitions to SEARCHING state.
     *
     * @param id       resource location of the target (e.g. minecraft:dandelion)
     * @param name     display name shown in the UI
     * @param itemIcon item icon for the target
     * @param cat      the category this target belongs to
     */
    public static void set(ResourceLocation id, Component name, ItemStack itemIcon, MenuCategory cat) {
        targetId = id;
        displayName = name;
        icon = itemIcon;
        category = cat;
        distance = -1;
        status = TrackingStatus.SEARCHING;
        statusMessage = null;
        statusTimestamp = System.currentTimeMillis();
    }

    /**
     * Transitions to TRACKING state when the server confirms a path was found.
     *
     * @param dist initial distance in blocks
     */
    public static void setTracking(int dist) {
        status = TrackingStatus.TRACKING;
        distance = dist;
        statusMessage = null;
        statusTimestamp = System.currentTimeMillis();
    }

    /**
     * Transitions to ARRIVED state. Auto-clears after 3 seconds.
     */
    public static void setArrived() {
        status = TrackingStatus.ARRIVED;
        statusMessage = null;
        statusTimestamp = System.currentTimeMillis();
    }

    /**
     * Transitions to NOT_FOUND or ERROR state with an optional reason message.
     * Auto-clears after 3 seconds.
     *
     * @param reason human-readable reason for the failure
     * @param isError true for ERROR state, false for NOT_FOUND
     */
    public static void setFailed(String reason, boolean isError) {
        status = isError ? TrackingStatus.ERROR : TrackingStatus.NOT_FOUND;
        statusMessage = reason;
        statusTimestamp = System.currentTimeMillis();
    }

    /**
     * Clears the active tracking state, resetting to IDLE.
     */
    public static void clear() {
        targetId = null;
        displayName = null;
        icon = null;
        category = null;
        distance = -1;
        status = TrackingStatus.IDLE;
        statusMessage = null;
    }

    /**
     * Called each client tick to auto-clear terminal states (ARRIVED, NOT_FOUND, ERROR)
     * after {@link #AUTO_CLEAR_MS} milliseconds.
     */
    public static void tick() {
        if (status == TrackingStatus.ARRIVED || status == TrackingStatus.NOT_FOUND || status == TrackingStatus.ERROR) {
            if (System.currentTimeMillis() - statusTimestamp > AUTO_CLEAR_MS) {
                clear();
            }
        }
    }

    /**
     * Updates the distance to the tracked target (in blocks).
     * Called from the client-side network handler when the server sends updates.
     *
     * @param blocks distance in blocks, or -1 if unknown
     */
    public static void setDistance(int blocks) {
        distance = blocks;
    }

    /**
     * @return distance to target in blocks, or -1 if unknown
     */
    public static int getDistance() {
        return distance;
    }

    /**
     * @return the current tracking status
     */
    public static TrackingStatus getStatus() {
        return status;
    }

    /**
     * @return optional status message (e.g. failure reason), may be null
     */
    public static String getStatusMessage() {
        return statusMessage;
    }

    /**
     * @return true if a target is currently being tracked (any non-IDLE state)
     */
    public static boolean isTracking() {
        return status != TrackingStatus.IDLE;
    }

    /**
     * @return true if actively tracking with a live path (TRACKING state only)
     */
    public static boolean isActivelyTracking() {
        return status == TrackingStatus.TRACKING;
    }

    /**
     * @return true if the given target ID is the one currently being tracked
     */
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

    /**
     * @return the category ID string of the tracked target, or null
     */
    public static String getCategoryId() {
        return category != null ? category.getId() : null;
    }
}
