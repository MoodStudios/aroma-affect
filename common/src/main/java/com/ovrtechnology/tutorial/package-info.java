/**
 * OVR Tutorial Module - A ghost tutorial layer for Aroma Affect.
 * <p>
 * This module provides tutorial functionality that only activates in specially
 * prepared tutorial maps. It is designed to be completely invisible during
 * normal gameplay.
 * <p>
 * <h2>Architecture</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.tutorial.TutorialModule} - Core module entry point</li>
 *   <li>{@link com.ovrtechnology.tutorial.TutorialGameRules} - Custom GameRule registration</li>
 *   <li>{@link com.ovrtechnology.tutorial.command} - Tutorial commands (hidden from players)</li>
 * </ul>
 * <p>
 * <h2>How It Works</h2>
 * <ol>
 *   <li>The module registers a custom GameRule: {@code isOvrTutorial}</li>
 *   <li>By default, this GameRule is {@code false} in all worlds</li>
 *   <li>Map creators set it to {@code true} in their tutorial map</li>
 *   <li>When a player loads the map, the GameRule is read from {@code level.dat}</li>
 *   <li>If {@code true}, all tutorial features become available</li>
 * </ol>
 * <p>
 * <h2>For Map Creators</h2>
 * To enable tutorial mode in your map:
 * <pre>
 *   /gamerule isOvrTutorial true
 * </pre>
 * This value is saved in the world and will be active for anyone who loads the map.
 *
 * @see com.ovrtechnology.tutorial.TutorialModule
 */
package com.ovrtechnology.tutorial;
