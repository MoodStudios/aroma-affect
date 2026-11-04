/**
 * Ability system for Aroma Affect nose items.
 * 
 * <p>
 * This package contains the implementation of special abilities that can be
 * unlocked by equipping different nose tiers. Abilities provide unique
 * mechanics
 * that go beyond simple detection capabilities.
 * </p>
 * 
 * <h2>Current Abilities:</h2>
 * <ul>
 * <li><b>Precise Sniffer</b> (Tier 5 - Netherite Nose): When interacting with
 * Suspicious Sand, replaces the brushing mechanic with a "sniffing" action
 * that has a high chance to obtain Sniffer Eggs.</li>
 * </ul>
 * 
 * <h2>Architecture:</h2>
 * <ul>
 * <li>{@link com.ovrtechnology.ability.AbilityConstants} - Ability ID
 * constants</li>
 * <li>{@link com.ovrtechnology.ability.AbilityHandler} - Event hooks for
 * ability activation</li>
 * <li>{@link com.ovrtechnology.ability.PreciseSnifferAbility} - Precise Sniffer
 * implementation</li>
 * <li>{@link com.ovrtechnology.ability.SnifferLootTable} - Custom loot table
 * with Sniffer Egg bias</li>
 * </ul>
 * 
 * @see com.ovrtechnology.nose.NoseAbilityResolver
 * @since 1.0.0
 */
package com.ovrtechnology.ability;
