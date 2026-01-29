package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Menu screen for selecting flowers/flora to track.
 *
 * <p>This menu displays all flower-type blocks that the currently equipped Nose can detect.
 * Flowers are shown as cards with their item icons.</p>
 *
 * <p>When a flower is selected, the path command is executed to create a particle
 * trail leading to the nearest instance of that flower block.</p>
 *
 * @see MenuManager#openFlowersMenu()
 * @see SelectionMenuScreen
 */
public class FlowersMenuScreen extends SelectionMenuScreen {

    /**
     * Map of flower IDs to their item icons (for flowers that don't have a direct item form).
     */
    private static final Map<String, ItemStack> FLOWER_ICONS = new HashMap<>();

    static {
        // Common flowers
        FLOWER_ICONS.put("minecraft:dandelion", Items.DANDELION.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:poppy", Items.POPPY.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:blue_orchid", Items.BLUE_ORCHID.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:allium", Items.ALLIUM.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:azure_bluet", Items.AZURE_BLUET.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:red_tulip", Items.RED_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:orange_tulip", Items.ORANGE_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:white_tulip", Items.WHITE_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:pink_tulip", Items.PINK_TULIP.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:oxeye_daisy", Items.OXEYE_DAISY.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:cornflower", Items.CORNFLOWER.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:lily_of_the_valley", Items.LILY_OF_THE_VALLEY.getDefaultInstance());

        // Tall flowers
        FLOWER_ICONS.put("minecraft:sunflower", Items.SUNFLOWER.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:lilac", Items.LILAC.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:rose_bush", Items.ROSE_BUSH.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:peony", Items.PEONY.getDefaultInstance());

        // Special flowers
        FLOWER_ICONS.put("minecraft:wither_rose", Items.WITHER_ROSE.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:torchflower", Items.TORCHFLOWER.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:pitcher_plant", Items.PITCHER_PLANT.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:pink_petals", Items.PINK_PETALS.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:spore_blossom", Items.SPORE_BLOSSOM.getDefaultInstance());

        // Nether flora
        FLOWER_ICONS.put("minecraft:crimson_fungus", Items.CRIMSON_FUNGUS.getDefaultInstance());
        FLOWER_ICONS.put("minecraft:warped_fungus", Items.WARPED_FUNGUS.getDefaultInstance());
    }

    public FlowersMenuScreen() {
        super(MenuCategory.FLOWERS);
    }

    @Override
    protected void loadCards() {
        cards.clear();

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaAffect.LOGGER.debug("No player available for flowers menu");
            return;
        }

        // Check if player has a nose equipped
        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        // Get detectable flowers from equipped nose
        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableFlowers = abilities.getFlowers();

        if (detectableFlowers.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no flower detection abilities");
            return;
        }

        // Add cards for each detectable flower
        for (String flowerId : detectableFlowers) {
            ResourceLocation resourceLocation = ResourceLocation.parse(flowerId);
            addFlowerCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} flower cards from equipped nose", cards.size());
    }

    /**
     * Adds a flower card to the menu.
     *
     * @param flowerId   the flower block's resource location
     * @param isUnlocked whether the flower is unlocked for tracking
     */
    private void addFlowerCard(ResourceLocation flowerId, boolean isUnlocked) {
        // Try to get icon from our map first
        ItemStack icon = FLOWER_ICONS.get(flowerId.toString());

        // If not in map, try to get from block registry
        if (icon == null) {
            var blockOptional = BuiltInRegistries.BLOCK.get(flowerId);
            if (blockOptional.isPresent()) {
                icon = new ItemStack(blockOptional.get().value().asItem());
            }
        }

        // Fallback icon
        if (icon == null || icon.isEmpty()) {
            icon = Items.POPPY.getDefaultInstance();
        }

        // Create display name from flower ID
        String flowerName = flowerId.getPath().replace("_", " ");
        flowerName = capitalizeWords(flowerName);
        Component displayName = Component.literal(flowerName);

        Component description = Component.translatable("menu.aromaaffect.flowers.card.description", displayName);

        cards.add(new SelectionCard(flowerId, displayName, icon, isUnlocked, description));
    }

    /**
     * Capitalizes the first letter of each word.
     */
    private String capitalizeWords(String str) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    @Override
    protected void onCardSelected(SelectionCard card, int index) {
        selectedCardIndex = index;
        AromaAffect.LOGGER.info("Selected flower for tracking: {}", card.id);

        // Close menu first
        closeCurrentMenu();

        // Execute path command to start tracking (flowers are blocks)
        startPathToFlower(card.id);
    }

    /**
     * Starts a particle path to the selected flower by executing the path command.
     * Flowers are tracked as blocks.
     *
     * @param flowerId the flower block's resource location
     */
    private void startPathToFlower(ResourceLocation flowerId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // Send the path command to the server (flowers are blocks)
        String command = String.format("aromatest path block %s", flowerId.toString());
        AromaAffect.LOGGER.debug("Executing path command: {}", command);

        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand(command);
        }
    }

    /**
     * Closes this menu and returns to the game.
     */
    private void closeCurrentMenu() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
