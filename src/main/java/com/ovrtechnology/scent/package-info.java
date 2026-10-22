/**
 * Scent system for Aroma Affect.
 * 
 * <p>This package contains all classes related to scent definitions and management.
 * Scents represent the different aromas that can be emitted through OVR Technology's
 * scent hardware, providing an immersive sensory experience in Minecraft.</p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.scent.ScentDefinition} - Data class representing a scent loaded from JSON</li>
 *   <li>{@link com.ovrtechnology.scent.ScentDefinitionLoader} - JSON parsing and validation utilities</li>
 *   <li>{@link com.ovrtechnology.scent.ScentRegistry} - Central registry and API for accessing scents</li>
 * </ul>
 * 
 * <h2>JSON Configuration</h2>
 * <p>Scents are defined in JSON files located at {@code data/aromaaffect/scents/scents.json}.
 * Each scent entry includes:</p>
 * <ul>
 *   <li>{@code id} - Unique identifier used for OVR hardware communication and localization</li>
 *   <li>{@code fallback_name} - Display name when localization is not available</li>
 *   <li>{@code description} - Optional description for tooltips</li>
 *   <li>{@code priority} - Priority level (1-10) for scent emission ordering</li>
 * </ul>
 * 
 * <h2>OVR Hardware Integration</h2>
 * <p>The scent IDs defined here correspond to OVR Technology's scent cassette identifiers.
 * When a scent event is triggered in-game, the corresponding scent ID is sent to the
 * OVR hardware via WebSocket connection.</p>
 * 
 * <h2>Localization</h2>
 * <p>Scent names can be localized using Minecraft's language system. The localization key
 * format is {@code scent.aromaaffect.<id>} for names and {@code scent.aromaaffect.<id>.description}
 * for descriptions.</p>
 * 
 * <h2>Available Scents (OVR Common Scents Cassette)</h2>
 * <ul>
 *   <li>{@code winter} - Cold air, mint, ozone - Winter ambience, alertness</li>
 *   <li>{@code barnyard} - Warm hay, farm animals - Animal interactions</li>
 *   <li>{@code sweet} - Vanilla, cream, chocolate - Rewards, cooking</li>
 *   <li>{@code floral} - Flower garden blooms - Spring ambience, flowers</li>
 *   <li>{@code beach} - Sand, sunscreen, coconut - Relaxation, happiness</li>
 *   <li>{@code kindred} - Baby powder, lavender - Home feel, connection</li>
 *   <li>{@code petrichor} - Rain on pavement - Immersion, weather cues</li>
 *   <li>{@code marine} - Ocean spray, seaweed - Underwater, ocean nearby</li>
 *   <li>{@code evergreen} - Pine trees, greenery - Forest exploration</li>
 *   <li>{@code terra_silva} - Fresh dirt, wet moss - Farming, digging</li>
 *   <li>{@code citrus} - Lemon, orange, grapefruit - Fruits, energy</li>
 *   <li>{@code desert} - Dry, herbal, sand heat - Desert immersion</li>
 *   <li>{@code savory_spice} - Cooking spices - Food, cooking games</li>
 *   <li>{@code timber} - Dried wood, sandalwood - Chopping trees, cabins</li>
 *   <li>{@code smoky} - Campfire, creosote - Fire interactions</li>
 *   <li>{@code machina} - Diesel, industrial - Machines, danger</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see com.ovrtechnology.scent.ScentRegistry
 */
package com.ovrtechnology.scent;

