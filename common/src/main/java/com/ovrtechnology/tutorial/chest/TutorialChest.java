package com.ovrtechnology.tutorial.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a tutorial chest that can be placed in the world.
 * <p>
 * Tutorial chests:
 * <ul>
 *   <li>Display particle effects (question mark) above them</li>
 *   <li>Give items to players when opened</li>
 *   <li>Can activate waypoints and/or cinematics</li>
 *   <li>Track whether they've been opened (consumed)</li>
 * </ul>
 */
public class TutorialChest {

    private final String id;
    private BlockPos position;
    private final List<ItemStack> rewards = new ArrayList<>();
    @Nullable
    private String activateWaypointId;
    @Nullable
    private String activateCinematicId;
    private boolean consumed;

    public TutorialChest(String id) {
        this.id = id;
        this.position = BlockPos.ZERO;
        this.consumed = false;
    }

    public TutorialChest(String id, BlockPos position, List<ItemStack> rewards,
                         @Nullable String activateWaypointId, @Nullable String activateCinematicId,
                         boolean consumed) {
        this.id = id;
        this.position = position;
        if (rewards != null) {
            this.rewards.addAll(rewards);
        }
        this.activateWaypointId = activateWaypointId;
        this.activateCinematicId = activateCinematicId;
        this.consumed = consumed;
    }

    public String getId() {
        return id;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Position
    // ─────────────────────────────────────────────────────────────────────────────

    public BlockPos getPosition() {
        return position;
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Rewards
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets the rewards for this chest.
     *
     * @return unmodifiable list of rewards
     */
    public List<ItemStack> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    /**
     * Adds a reward to this chest.
     *
     * @param item the item to add (will be copied)
     */
    public void addReward(ItemStack item) {
        if (item != null && !item.isEmpty()) {
            rewards.add(item.copy());
        }
    }

    /**
     * Clears all rewards from this chest.
     */
    public void clearRewards() {
        rewards.clear();
    }

    /**
     * Checks if this chest has any rewards.
     */
    public boolean hasRewards() {
        return !rewards.isEmpty();
    }

    /**
     * Gets the number of rewards.
     */
    public int getRewardCount() {
        return rewards.size();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Activation
    // ─────────────────────────────────────────────────────────────────────────────

    @Nullable
    public String getActivateWaypointId() {
        return activateWaypointId;
    }

    public void setActivateWaypointId(@Nullable String waypointId) {
        this.activateWaypointId = waypointId;
    }

    public boolean hasActivateWaypoint() {
        return activateWaypointId != null && !activateWaypointId.isEmpty();
    }

    @Nullable
    public String getActivateCinematicId() {
        return activateCinematicId;
    }

    public void setActivateCinematicId(@Nullable String cinematicId) {
        this.activateCinematicId = cinematicId;
    }

    public boolean hasActivateCinematic() {
        return activateCinematicId != null && !activateCinematicId.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Consumed State
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Checks if this chest has been consumed (opened).
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Sets whether this chest has been consumed.
     */
    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    /**
     * Marks this chest as consumed.
     */
    public void consume() {
        this.consumed = true;
    }

    /**
     * Resets this chest to unconsumed state.
     */
    public void reset() {
        this.consumed = false;
    }
}
