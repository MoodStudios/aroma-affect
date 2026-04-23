package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.guide.GuideManager;
import net.minecraft.client.Minecraft;

public final class MenuManager {

    private MenuManager() {}

    public static void openRadialMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open radial menu: no player");
            return;
        }

        if (minecraft.screen != null) {
            AromaAffect.LOGGER.debug("Cannot open radial menu: another screen is open");
            return;
        }

        AromaAffect.LOGGER.debug("Opening radial menu");
        minecraft.setScreen(new RadialMenuScreen());
    }

    public static void openConfigMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open config menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening config menu");
        minecraft.setScreen(new ConfigScreen());
    }

    public static void openBlocksMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open blocks menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening blocks menu");
        minecraft.setScreen(new BlocksMenuScreen());
    }

    public static void openBiomesMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open biomes menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening biomes menu");
        minecraft.setScreen(new BiomesMenuScreen());
    }

    public static void openStructuresMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open structures menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening structures menu");
        minecraft.setScreen(new StructuresMenuScreen());
    }

    public static void openFlowersMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open flowers menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening flowers menu");
        minecraft.setScreen(new FlowersMenuScreen());
    }

    public static void openGuide() {
        GuideManager.openGuideClient();
    }

    public static void openHistoryMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open history menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening history menu");
        minecraft.setScreen(new HistoryMenuScreen());
    }

    public static void openShopMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open shop menu: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening shop menu");
        minecraft.setScreen(new ShopScreen());
    }

    public static void returnToRadialMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        AromaAffect.LOGGER.debug("Returning to radial menu");
        minecraft.setScreen(new RadialMenuScreen());
    }
}
