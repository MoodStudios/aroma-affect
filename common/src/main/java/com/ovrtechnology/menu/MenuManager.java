package com.ovrtechnology.menu;

import com.ovrtechnology.AromaCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Central manager for opening and managing AromaCraft menus.
 * 
 * <p>This class provides a unified API for opening any menu in the mod.
 * All menu opening logic should go through this class to ensure proper
 * state management and transitions.</p>
 */
public final class MenuManager {
    
    private MenuManager() {
        // Utility class
    }
    
    /**
     * Opens the radial menu.
     * This is the main entry point for the menu system, typically triggered by a hotkey.
     */
    public static void openRadialMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaCraft.LOGGER.debug("Cannot open radial menu: no player");
            return;
        }
        
        if (minecraft.screen != null) {
            AromaCraft.LOGGER.debug("Cannot open radial menu: another screen is open");
            return;
        }
        
        AromaCraft.LOGGER.debug("Opening radial menu");
        minecraft.setScreen(new RadialMenuScreen());
    }
    
    /**
     * Opens the configuration menu.
     */
    public static void openConfigMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaCraft.LOGGER.debug("Cannot open config menu: no player");
            return;
        }
        
        AromaCraft.LOGGER.debug("Opening config menu");
        minecraft.setScreen(new ConfigScreen());
    }
    
    /**
     * Opens the blocks selection menu.
     */
    public static void openBlocksMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaCraft.LOGGER.debug("Cannot open blocks menu: no player");
            return;
        }
        
        AromaCraft.LOGGER.debug("Opening blocks menu");
        minecraft.setScreen(new BlocksMenuScreen());
    }
    
    /**
     * Opens the biomes selection menu.
     */
    public static void openBiomesMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaCraft.LOGGER.debug("Cannot open biomes menu: no player");
            return;
        }
        
        AromaCraft.LOGGER.debug("Opening biomes menu");
        minecraft.setScreen(new BiomesMenuScreen());
    }
    
    /**
     * Opens the structures selection menu.
     */
    public static void openStructuresMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaCraft.LOGGER.debug("Cannot open structures menu: no player");
            return;
        }
        
        AromaCraft.LOGGER.debug("Opening structures menu");
        minecraft.setScreen(new StructuresMenuScreen());
    }
    
    /**
     * Opens a menu by category.
     * 
     * @param category the category to open a menu for
     */
    public static void openMenuForCategory(MenuCategory category) {
        switch (category) {
            case BLOCKS -> openBlocksMenu();
            case BIOMES -> openBiomesMenu();
            case STRUCTURES -> openStructuresMenu();
        }
    }
    
    /**
     * Closes the current menu and returns to the game.
     */
    public static void closeCurrentMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BaseMenuScreen) {
            minecraft.setScreen(null);
        }
    }
    
    /**
     * Closes the current menu and opens the radial menu.
     * Useful for "back" navigation from category menus.
     */
    public static void returnToRadialMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        AromaCraft.LOGGER.debug("Returning to radial menu");
        minecraft.setScreen(new RadialMenuScreen());
    }
    
    /**
     * Checks if any AromaCraft menu is currently open.
     * 
     * @return true if an AromaCraft menu is open
     */
    public static boolean isMenuOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof BaseMenuScreen;
    }
    
    /**
     * Gets the currently open menu, if it's an AromaCraft menu.
     * 
     * @return the current menu, or null if none is open
     */
    public static BaseMenuScreen getCurrentMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof BaseMenuScreen baseMenu) {
            return baseMenu;
        }
        return null;
    }
}
