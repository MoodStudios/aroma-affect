package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.guide.GuideManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;

/**
 * Central manager for opening and managing Aroma Affect menus.
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
            AromaAffect.LOGGER.debug("Cannot open radial menu: no player");
            return;
        }
        
        if (minecraft.gui.screen() != null) {
            AromaAffect.LOGGER.debug("Cannot open radial menu: another screen is open");
            return;
        }
        
        AromaAffect.LOGGER.debug("Opening radial menu");
        minecraft.setScreenAndShow(new RadialMenuScreen());
    }
    
    /**
     * Opens the configuration menu.
     */
    public static void openConfigMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open config menu: no player");
            return;
        }
        
        AromaAffect.LOGGER.debug("Opening config menu");
        minecraft.setScreenAndShow(new ConfigScreen());
    }
    
    /**
     * Opens the blocks selection menu.
     */
    public static void openBlocksMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open blocks menu: no player");
            return;
        }
        
        AromaAffect.LOGGER.debug("Opening blocks menu");
        minecraft.setScreenAndShow(new BlocksMenuScreen());
    }
    
    /**
     * Opens the biomes selection menu.
     */
    public static void openBiomesMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open biomes menu: no player");
            return;
        }
        
        AromaAffect.LOGGER.debug("Opening biomes menu");
        minecraft.setScreenAndShow(new BiomesMenuScreen());
    }
    
    /**
     * Opens the structures selection menu.
     */
    public static void openStructuresMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open structures menu: no player");
            return;
        }
        
        AromaAffect.LOGGER.debug("Opening structures menu");
        minecraft.setScreenAndShow(new StructuresMenuScreen());
    }
    
    /**
     * Opens the flowers/flora selection menu.
     */
    public static void openFlowersMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open flowers menu: no player");
            return;
        }
        
        AromaAffect.LOGGER.debug("Opening flowers menu");
        minecraft.setScreenAndShow(new FlowersMenuScreen());
    }
    
    /**
     * Opens the AromaCraft guide book.
     */
    public static void openGuide() {
        GuideManager.openGuideClient();
    }

    /**
     * Opens the tracking history screen.
     */
    public static void openHistoryMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open history menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening history menu");
        minecraft.setScreenAndShow(new HistoryMenuScreen());
    }

    /**
     * Opens the shop screen.
     */
    public static void openShopMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open shop menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening shop menu");
        minecraft.setScreenAndShow(new ShopScreen());
    }

    /**
     * Opens the feedback submission screen.
     */
    public static void openFeedbackMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open feedback menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening feedback menu");
        minecraft.setScreenAndShow(new FeedbackScreen());
    }

    /**
     * Invites player to Discord
     */
    public static void openDiscord() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot join Discord: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Joining Discord");
        Util.getPlatform().openUri("https://discord.gg/f9Cf3xKCET");
    }

    /**
     * Opens the compass/tracking menu.
     */
    public static void openCompassMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open compass menu: no player");
            return;
        }
        
        AromaAffect.LOGGER.debug("Opening compass menu");
        minecraft.setScreenAndShow(new CompassMenuScreen());
    }
    
    /**
     * Opens a menu by category.
     * 
     * @param category the category to open a menu for
     */
    public static void openMenuForCategory(TrackingCategory category) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open category menu: no player");
            return;
        }
        if (category == null) {
            AromaAffect.LOGGER.debug("Cannot open category menu: null category");
            return;
        }

        AromaAffect.LOGGER.debug("Opening menu for category {}", category.getId());
        minecraft.setScreenAndShow(category.createScreen());
    }
    
    /**
     * Closes the current menu and returns to the game.
     */
    public static void closeCurrentMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof BaseMenuScreen) {
            minecraft.setScreenAndShow(null);
        }
    }
    
    /**
     * Closes the current menu and opens the radial menu.
     * Useful for "back" navigation from category menus.
     */
    public static void returnToRadialMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        AromaAffect.LOGGER.debug("Returning to radial menu");
        minecraft.setScreenAndShow(new RadialMenuScreen());
    }
    
    /**
     * Checks if any Aroma Affect menu is currently open.
     * 
     * @return true if an Aroma Affect menu is open
     */
    public static boolean isMenuOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gui.screen() instanceof BaseMenuScreen;
    }
    
    /**
     * Gets the currently open menu, if it's an Aroma Affect menu.
     * 
     * @return the current menu, or null if none is open
     */
    public static BaseMenuScreen getCurrentMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        if (screen instanceof BaseMenuScreen baseMenu) {
            return baseMenu;
        }
        return null;
    }
}
