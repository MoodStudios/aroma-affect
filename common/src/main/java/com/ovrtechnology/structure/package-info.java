/**
 * Structure tracking system for Aroma Affect.
 * 
 * <p>This package contains all classes related to trackable structure definitions.
 * Structures define which Minecraft structures (villages, strongholds, dungeons, etc.)
 * can be detected by the Nose system, along with their associated scents, display colors,
 * and characteristic blocks.</p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.structure.StructureDefinition} - Data class representing a trackable structure</li>
 *   <li>{@link com.ovrtechnology.structure.StructureDefinitionLoader} - JSON parsing and validation utilities</li>
 *   <li>{@link com.ovrtechnology.structure.StructureRegistry} - Central registry and API for accessing structures</li>
 * </ul>
 * 
 * <h2>JSON Configuration</h2>
 * <p>Structures are defined in JSON files located at {@code data/aromaaffect/structures/structures.json}.
 * Each structure entry includes:</p>
 * <ul>
 *   <li>{@code structure_id} - Minecraft structure identifier (e.g., "minecraft:stronghold")</li>
 *   <li>{@code image} - Texture path for UI display</li>
 *   <li>{@code fallback_name} - Display name fallback</li>
 *   <li>{@code color_html} - HTML hex color for UI display</li>
 *   <li>{@code scent_id} - Reference to a scent from scents.json</li>
 *   <li>{@code blocks} - Optional list of characteristic block IDs</li>
 * </ul>
 * 
 * <h2>Validation</h2>
 * <p>The loader performs several validations:</p>
 * <ul>
 *   <li>Duplicate structure_id detection</li>
 *   <li>Structure ID format validation (namespace:path)</li>
 *   <li>HTML color format validation (#RGB, #RRGGBB, #RRGGBBAA)</li>
 *   <li>Scent ID reference validation against ScentRegistry</li>
 *   <li>Block ID format validation for blocks list entries</li>
 * </ul>
 * 
 * <h2>Modded Structure Support</h2>
 * <p>The system supports structures from any mod. Structure IDs use the standard
 * Minecraft ResourceLocation format (namespace:path), allowing modded structures
 * to be registered alongside vanilla ones. Examples:</p>
 * <ul>
 *   <li>{@code minecraft:stronghold} - Vanilla stronghold</li>
 *   <li>{@code minecraft:village_plains} - Vanilla plains village</li>
 *   <li>{@code create:contraption_base} - Create mod structure (example)</li>
 *   <li>{@code mekanism:factory} - Mekanism mod structure (example)</li>
 * </ul>
 * 
 * <h2>Initialization Order</h2>
 * <p>StructureRegistry must be initialized after ScentRegistry and BlockRegistry.
 * The recommended initialization order is:</p>
 * <ol>
 *   <li>ScentRegistry.init()</li>
 *   <li>BlockRegistry.init()</li>
 *   <li>StructureRegistry.init()</li>
 *   <li>NoseRegistry.init() (validates structure references)</li>
 * </ol>
 * 
 * @since 1.0.0
 * @see com.ovrtechnology.structure.StructureRegistry
 * @see com.ovrtechnology.scent.ScentRegistry
 * @see com.ovrtechnology.block.BlockRegistry
 * @see com.ovrtechnology.nose.NoseRegistry
 */
package com.ovrtechnology.structure;

