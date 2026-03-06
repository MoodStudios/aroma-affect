package com.ovrtechnology.tutorial.waypoint;

/**
 * Represents a conditional chain based on equipped nose.
 * <p>
 * When a player completes a waypoint with nose chains defined,
 * the system checks which nose they have equipped and activates
 * the corresponding next waypoint.
 *
 * @param noseId         the nose ID to match (e.g., "prospectors_nose", "jewelers_nose")
 * @param nextWaypointId the waypoint ID to activate if the nose matches
 */
public record NoseChainCondition(String noseId, String nextWaypointId) {

    /**
     * Creates a new nose chain condition.
     *
     * @param noseId         the nose ID to match
     * @param nextWaypointId the waypoint ID to activate
     * @throws IllegalArgumentException if either parameter is null or empty
     */
    public NoseChainCondition {
        if (noseId == null || noseId.isEmpty()) {
            throw new IllegalArgumentException("noseId cannot be null or empty");
        }
        if (nextWaypointId == null || nextWaypointId.isEmpty()) {
            throw new IllegalArgumentException("nextWaypointId cannot be null or empty");
        }
    }
}
