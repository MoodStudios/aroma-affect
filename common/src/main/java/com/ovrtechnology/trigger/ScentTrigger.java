package com.ovrtechnology.trigger;

import java.util.Objects;

/**
 * Represents an active or pending scent trigger.
 * 
 * <p>A trigger contains all the information needed to activate a scent
 * on the OVR hardware and manage its lifecycle.</p>
 * 
 * @param scentName     The exact OVR scent name (e.g., "Winter", "Terra Silva")
 * @param source        The source that triggered this scent
 * @param priority      The priority level for conflict resolution
 * @param durationTicks Duration in game ticks (-1 for indefinite)
 * @param triggeredAt   Timestamp when this trigger was created (for tie-breaking)
 */
public record ScentTrigger(
    String scentName,
    ScentTriggerSource source,
    ScentPriority priority,
    int durationTicks,
    long triggeredAt
) {
    
    /**
     * Creates a new scent trigger with the current timestamp.
     * 
     * @param scentName     The exact OVR scent name
     * @param source        The source of the trigger
     * @param priority      The priority level
     * @param durationTicks Duration in ticks (-1 for indefinite)
     * @return a new ScentTrigger instance
     */
    public static ScentTrigger create(
            String scentName,
            ScentTriggerSource source,
            ScentPriority priority,
            int durationTicks) {
        return new ScentTrigger(
            Objects.requireNonNull(scentName, "scentName cannot be null"),
            Objects.requireNonNull(source, "source cannot be null"),
            Objects.requireNonNull(priority, "priority cannot be null"),
            durationTicks,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Creates a high-priority trigger from item use.
     * 
     * @param scentName     The exact OVR scent name
     * @param durationTicks Duration in ticks
     * @return a new ScentTrigger for item use
     */
    public static ScentTrigger fromItemUse(String scentName, int durationTicks) {
        return create(scentName, ScentTriggerSource.ITEM_USE, ScentPriority.HIGH, durationTicks);
    }
    
    /**
     * Checks if this trigger has indefinite duration.
     * 
     * @return true if duration is -1 (indefinite)
     */
    public boolean isIndefinite() {
        return durationTicks < 0;
    }
    
    /**
     * Checks if this trigger should replace another based on priority and timing.
     * 
     * <p>A trigger replaces another if:</p>
     * <ul>
     *   <li>It has higher priority, OR</li>
     *   <li>It has equal priority and was triggered more recently</li>
     * </ul>
     * 
     * @param other the other trigger to compare (can be null)
     * @return true if this trigger should replace the other
     */
    public boolean shouldReplace(ScentTrigger other) {
        if (other == null) {
            return true;
        }
        
        int priorityComparison = this.priority.comparePriority(other.priority);
        
        if (priorityComparison > 0) {
            return true; // This has higher priority
        }
        if (priorityComparison < 0) {
            return false; // Other has higher priority
        }
        
        // Equal priority: most recent wins
        return this.triggeredAt >= other.triggeredAt;
    }
    
    @Override
    public String toString() {
        return "ScentTrigger{" +
                "scentName='" + scentName + '\'' +
                ", source=" + source +
                ", priority=" + priority +
                ", durationTicks=" + durationTicks +
                '}';
    }
}
