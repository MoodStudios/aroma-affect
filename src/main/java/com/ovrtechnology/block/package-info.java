/**
 * Block tracking system for Aroma Affect.
 * 
 * <p>This package contains all classes related to trackable block definitions.
 * Blocks define which Minecraft blocks can be detected by the Nose system,
 * along with their associated scents and display colors for the UI.</p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.block.BlockDefinition} - Data class representing a trackable block</li>
 *   <li>{@link com.ovrtechnology.block.BlockDefinitionLoader} - JSON parsing and validation utilities</li>
 *   <li>{@link com.ovrtechnology.block.BlockRegistry} - Central registry and API for accessing blocks</li>
 * </ul>
 * 
 * <h2>JSON Configuration</h2>
 * <p>Blocks are defined in JSON files located at {@code data/aromaaffect/blocks/blocks.json}.
 * Each block entry includes:</p>
 * <ul>
 *   <li>{@code block_id} - Minecraft block identifier (e.g., "minecraft:diamond_ore")</li>
 *   <li>{@code color_html} - HTML hex color for UI display (e.g., "#5DECF5")</li>
 *   <li>{@code scent_id} - Reference to a scent from scents.json</li>
 *   <li>{@code fallback_name} - Optional display name fallback</li>
 * </ul>
 * 
 * <h2>Validation</h2>
 * <p>The loader performs several validations:</p>
 * <ul>
 *   <li>Duplicate block_id detection</li>
 *   <li>HTML color format validation (#RGB, #RRGGBB, #RRGGBBAA)</li>
 *   <li>Scent ID reference validation against ScentRegistry</li>
 * </ul>
 * 
 * <h2>Initialization Order</h2>
 * <p>BlockRegistry must be initialized after ScentRegistry, as it validates
 * scent references during loading. The recommended initialization order is:</p>
 * <ol>
 *   <li>ScentRegistry.init()</li>
 *   <li>BlockRegistry.init()</li>
 *   <li>NoseRegistry.init() (validates block references)</li>
 * </ol>
 * 
 * <h2>Integration with Nose System</h2>
 * <p>The Nose system references block IDs in its unlock conditions. The
 * NoseDefinitionLoader validates that all referenced blocks exist in the
 * BlockRegistry.</p>
 * 
 * @since 1.0.0
 * @see com.ovrtechnology.block.BlockRegistry
 * @see com.ovrtechnology.scent.ScentRegistry
 * @see com.ovrtechnology.nose.NoseRegistry
 */
package com.ovrtechnology.block;

