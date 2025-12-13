/**
 * Menu system for AromaCraft mod.
 * 
 * <p>This package contains all client-side menu screens for the mod:</p>
 * <ul>
 *   <li>{@link com.ovrtechnology.menu.RadialMenuScreen} - Main radial menu for selecting tracking categories</li>
 *   <li>{@link com.ovrtechnology.menu.ConfigScreen} - Configuration menu for OVR device settings</li>
 *   <li>{@link com.ovrtechnology.menu.BlocksMenuScreen} - Block selection menu for tracking</li>
 *   <li>{@link com.ovrtechnology.menu.BiomesMenuScreen} - Biome selection menu for tracking</li>
 *   <li>{@link com.ovrtechnology.menu.StructuresMenuScreen} - Structure selection menu for tracking</li>
 * </ul>
 * 
 * <p>The radial menu is the primary entry point, activated by pressing the configured hotkey (default: R).
 * From there, players can navigate to specific category menus to select what their equipped Nose should track.</p>
 * 
 * @see com.ovrtechnology.menu.MenuManager
 * @see com.ovrtechnology.menu.MenuCategory
 */
@MethodsReturnNonnullByDefault
package com.ovrtechnology.menu;

import net.minecraft.MethodsReturnNonnullByDefault;
