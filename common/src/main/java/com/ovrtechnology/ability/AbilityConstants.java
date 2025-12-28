package com.ovrtechnology.ability;

/**
 * Constants for ability identifiers used throughout AromaCraft.
 * 
 * <p>
 * These IDs must match the ability strings defined in the nose JSON
 * configuration
 * files (data/aromacraft/noses/noses.json).
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
 */
public final class AbilityConstants {

    private AbilityConstants() {}

    public static final String PRECISE_SNIFFER = "precise_sniffer";

}
