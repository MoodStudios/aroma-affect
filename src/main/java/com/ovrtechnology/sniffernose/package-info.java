/**
 * Sniffer Nose system for Aroma Affect.
 * 
 * <p>This package provides the sniffer nose item registration system.
 * Sniffer noses are items designed for the Sniffer mob and are NOT equippable by players.</p>
 * 
 * <h2>Difference from Regular Noses</h2>
 * <ul>
 *   <li>{@code nose} package - Player-equippable noses worn as helmets</li>
 *   <li>{@code sniffernose} package - Items for the Sniffer mob (not equippable)</li>
 * </ul>
 * 
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.sniffernose.SnifferNoseDefinition} - POJO for JSON definitions</li>
 *   <li>{@link com.ovrtechnology.sniffernose.SnifferNoseDefinitionLoader} - Loads definitions from JSON</li>
 *   <li>{@link com.ovrtechnology.sniffernose.SnifferNoseItem} - Minecraft Item class (not equippable)</li>
 *   <li>{@link com.ovrtechnology.sniffernose.SnifferNoseRegistry} - Central registry for sniffer nose items</li>
 * </ul>
 * 
 * <h2>JSON Configuration</h2>
 * <p>Sniffer noses are defined in {@code data/aromaaffect/noses/sniffer_noses.json}:</p>
 * <pre>
 * {
 *   "sniffer_noses": [
 *     {
 *       "id": "enhanced_sniffer_nose",
 *       "image": "item/enhanced_sniffer_nose",
 *       "model": "minecraft:leather_helmet",
 *       "tier": 1,
 *       "durability": 100
 *     }
 *   ]
 * }
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Initialize during mod startup (in Aroma Affect.init())
 * SnifferNoseRegistry.init();
 * 
 * // Access sniffer nose items
 * Optional&lt;SnifferNoseItem&gt; item = SnifferNoseRegistry.getSnifferNose("enhanced_sniffer_nose");
 * List&lt;SnifferNoseItem&gt; allItems = SnifferNoseRegistry.getAllSnifferNosesAsList();
 * </pre>
 * 
 * @see com.ovrtechnology.sniffernose.SnifferNoseRegistry
 * @see com.ovrtechnology.nose.NoseRegistry
 */
package com.ovrtechnology.sniffernose;
