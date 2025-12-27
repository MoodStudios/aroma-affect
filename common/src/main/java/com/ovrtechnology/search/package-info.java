/**
 * Search and tracking system for AromaCraft.
 * 
 * <p>This package contains the core search functionality that allows players
 * to activate scent-based tracking when wearing a Nose item.</p>
 * 
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.search.SearchManager} - Manages search state (on/off)</li>
 *   <li>{@link com.ovrtechnology.search.SearchKeyBindings} - Handles keybind registration and input</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <p>Players can toggle search mode by pressing the configured hotkey (default: V).
 * Search mode requires a Nose item to be equipped in the head slot. When active,
 * the search system will provide visual feedback and (in the future) integration
 * with OVR scent hardware.</p>
 * 
 * @since 0.0.1
 */
package com.ovrtechnology.search;

