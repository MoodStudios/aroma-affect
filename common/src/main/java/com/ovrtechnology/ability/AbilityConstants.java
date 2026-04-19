package com.ovrtechnology.ability;

/**
 * Constants for ability identifiers used throughout Aroma Affect.
 * 
 * <p>
 * These IDs must match the ability strings defined in the nose JSON
 * configuration files (data/aromaaffect/noses/noses.json).
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * if (noseItem.hasAbility(AbilityConstants.PRECISE_SNIFFER)) {
 *     // Execute precise sniffer logic
 * }
 * }</pre>
 * 
 * @see com.ovrtechnology.nose.NoseAbilityResolver#hasAbility(String, String)
 * @see AbilityRegistry
 */
public final class AbilityConstants {

    private AbilityConstants() {}

    /**
     * Basic scent detection ability.
     * Allows detection of basic scents like water and lava.
     */
    public static final String BASIC_SCENT = "basic_scent";

    /**
     * Active tracking ability.
     * Enables active tracking mode for enhanced scent detection.
     */
    public static final String ACTIVE_TRACKING = "active_tracking";

    /**
     * Structure compass ability.
     * Allows detection of nearby structures.
     */
    public static final String STRUCTURE_COMPASS = "structure_compass";

    /**
     * Danger sense ability.
     * Provides warnings about nearby dangers and hostile entities.
     */
    public static final String DANGER_SENSE = "danger_sense";

    /**
     * Precise sniffer ability.
     * Allows players to "sniff out" loot from Suspicious Sand
     * with an increased chance for Sniffer Eggs.
     */
    public static final String PRECISE_SNIFFER = "precise_sniffer";
}
