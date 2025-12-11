/**
 * Nose system for AromaCraft.
 * 
 * <p>This package contains all classes related to the "Nose" equipment system,
 * which is the core mechanic of the AromaCraft mod. Noses are equippable items
 * that provide scent detection abilities in the game.</p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.nose.NoseDefinition} - Data class representing a nose configuration loaded from JSON</li>
 *   <li>{@link com.ovrtechnology.nose.NoseItem} - The actual Minecraft item representing a nose (equippable in head slot)</li>
 *   <li>{@link com.ovrtechnology.nose.NoseRegistry} - Central registry for all nose items</li>
 *   <li>{@link com.ovrtechnology.nose.NoseDefinitionLoader} - JSON parsing utilities with texture fallback</li>
 *   <li>{@link com.ovrtechnology.nose.NoseUnlock} - Unlock conditions (blocks, biomes, structures, abilities, nose inheritance)</li>
 *   <li>{@link com.ovrtechnology.nose.NoseRecipe} - Recipe definition for crafting noses</li>
 *   <li>{@link com.ovrtechnology.nose.NoseAbilityResolver} - Resolves and caches inherited abilities with circular dependency detection</li>
 * </ul>
 * 
 * <h2>JSON Configuration</h2>
 * <p>Noses are defined in JSON files located at {@code data/aromacraft/noses/noses.json}.
 * Each nose definition specifies the item's properties, tier, unlock conditions, and crafting recipe.</p>
 * 
 * <h2>Ability Inheritance</h2>
 * <p>Noses can inherit abilities from other noses using the "noses" field in unlock configuration.
 * The {@link com.ovrtechnology.nose.NoseAbilityResolver} handles circular dependency detection and caching.</p>
 * 
 * @since 1.0.0
 */
package com.ovrtechnology.nose;
