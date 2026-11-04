/**
 * Scent Item system for Aroma Affect.
 * 
 * <p>This package provides the scent item registration system, which creates
 * Minecraft items from JSON definitions. These items are separate from the
 * scent definitions used for OVR hardware communication.</p>
 * 
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.scentitem.ScentItemDefinition} - POJO for JSON definitions</li>
 *   <li>{@link com.ovrtechnology.scentitem.ScentItemDefinitionLoader} - Loads definitions from JSON</li>
 *   <li>{@link com.ovrtechnology.scentitem.ScentItem} - Minecraft Item class</li>
 *   <li>{@link com.ovrtechnology.scentitem.ScentItemRegistry} - Central registry for scent items</li>
 * </ul>
 * 
 * <h2>JSON Configuration</h2>
 * <p>Scent items are defined in {@code data/aromaaffect/scents/scent_items.json}:</p>
 * <pre>
 * {
 *   "scents": [
 *     {
 *       "id": "winter_scent",
 *       "image": "item/scent_winter",
 *       "model": "minecraft:light_blue_dye",
 *       "fallback_name": "Winter Scent",
 *       "description": "Breathing in cold air...",
 *       "priority": 5
 *     }
 *   ]
 * }
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Initialize during mod startup (in Aroma Affect.init())
 * ScentItemRegistry.init();
 * 
 * // Access scent items
 * Optional&lt;ScentItem&gt; item = ScentItemRegistry.getScentItem("winter_scent");
 * List&lt;ScentItem&gt; allItems = ScentItemRegistry.getAllScentItemsAsList();
 * </pre>
 * 
 * <h2>Relationship with Scent System</h2>
 * <p>This system is separate from the {@code com.ovrtechnology.scent} package:</p>
 * <ul>
 *   <li>{@code scent} package - OVR hardware scent definitions (no Minecraft items)</li>
 *   <li>{@code scentitem} package - Minecraft items representing scents</li>
 * </ul>
 * 
 * @see com.ovrtechnology.scentitem.ScentItemRegistry
 * @see com.ovrtechnology.scent.ScentRegistry
 */
package com.ovrtechnology.scentitem;
