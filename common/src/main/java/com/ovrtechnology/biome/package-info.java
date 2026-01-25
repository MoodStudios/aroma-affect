/**
 * Biome tracking system for Aroma Affect.
 * 
 * <p>This package contains all classes related to trackable biome definitions.
 * Biomes define which Minecraft biomes can be detected by the Nose system,
 * along with their associated scents and display colors for the UI.</p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.biome.BiomeDefinition} - Data class representing a trackable biome</li>
 *   <li>{@link com.ovrtechnology.biome.BiomeDefinitionLoader} - JSON parsing and validation utilities</li>
 *   <li>{@link com.ovrtechnology.biome.BiomeRegistry} - Central registry and API for accessing biomes</li>
 * </ul>
 * 
 * <h2>JSON Configuration</h2>
 * <p>Biomes are defined in JSON files located at {@code data/aromaaffect/biomes/biomes.json}.
 * Each biome entry includes:</p>
 * <ul>
 *   <li>{@code biome_id} - Minecraft biome identifier (e.g., "minecraft:jungle")</li>
 *   <li>{@code image} - Texture path for UI display</li>
 *   <li>{@code fallback_name} - Display name fallback</li>
 *   <li>{@code color_html} - HTML hex color for UI display (often matches grass color)</li>
 *   <li>{@code scent_id} - Reference to a scent from scents.json</li>
 * </ul>
 * 
 * <h2>Validation</h2>
 * <p>The loader performs several validations:</p>
 * <ul>
 *   <li>Duplicate biome_id detection</li>
 *   <li>Biome ID format validation (namespace:path)</li>
 *   <li>HTML color format validation (#RGB, #RRGGBB, #RRGGBBAA)</li>
 *   <li>Scent ID reference validation against ScentRegistry</li>
 * </ul>
 * 
 * <h2>Modded Biome Support</h2>
 * <p>The system supports biomes from any mod. Biome IDs use the standard
 * Minecraft ResourceLocation format (namespace:path), allowing modded biomes
 * to be registered alongside vanilla ones. Examples:</p>
 * <ul>
 *   <li>{@code minecraft:jungle} - Vanilla jungle biome</li>
 *   <li>{@code minecraft:snowy_plains} - Vanilla snowy plains biome</li>
 *   <li>{@code terralith:volcanic_peaks} - Terralith mod biome (example)</li>
 *   <li>{@code biomes_o_plenty:redwood_forest} - Biomes O' Plenty mod biome (example)</li>
 * </ul>
 * 
 * <h2>Scent Associations</h2>
 * <p>Each biome is associated with a scent that matches its atmosphere:</p>
 * <ul>
 *   <li>Forest biomes → evergreen scent</li>
 *   <li>Ocean/Beach biomes → marine/beach scent</li>
 *   <li>Desert biomes → desert scent</li>
 *   <li>Snowy biomes → winter scent</li>
 *   <li>Nether biomes → smoky scent</li>
 *   <li>Swamp biomes → petrichor scent</li>
 * </ul>
 * 
 * <h2>Initialization Order</h2>
 * <p>BiomeRegistry must be initialized after ScentRegistry.
 * The recommended initialization order is:</p>
 * <ol>
 *   <li>ScentRegistry.init()</li>
 *   <li>BlockRegistry.init()</li>
 *   <li>BiomeRegistry.init()</li>
 *   <li>StructureRegistry.init()</li>
 *   <li>NoseRegistry.init() (validates biome references)</li>
 * </ol>
 * 
 * @since 1.0.0
 * @see com.ovrtechnology.biome.BiomeRegistry
 * @see com.ovrtechnology.scent.ScentRegistry
 */
package com.ovrtechnology.biome;

