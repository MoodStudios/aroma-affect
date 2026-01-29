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

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Menu screen for selecting blocks to track.
 *
 * <p>This menu displays all blocks that the currently equipped Nose can detect.
 * Blocks are shown as cards with their item icons, organized in a scrollable grid.</p>
 *
 * <p>When a block is selected, the path command is executed to create a particle
 * trail leading to the nearest instance of that block.</p>
 *
 * @see MenuManager#openBlocksMenu()
 * @see SelectionMenuScreen
 */
public class BlocksMenuScreen extends SelectionMenuScreen {

    public BlocksMenuScreen() {
        super(MenuCategory.BLOCKS);
    }

    @Override
    protected void loadCards() {
        cards.clear();

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaAffect.LOGGER.debug("No player available for blocks menu");
            return;
        }

        // Check if player has a nose equipped
        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        // Get detectable blocks from equipped nose
        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableBlocks = abilities.getBlocks();

        if (detectableBlocks.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no block detection abilities");
            return;
        }

        // Add cards for each detectable block
        for (String blockId : detectableBlocks) {
            ResourceLocation resourceLocation = ResourceLocation.parse(blockId);
            addBlockCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} block cards from equipped nose", cards.size());
    }

    /**
     * Adds a block card to the menu.
     *
     * @param blockId    the block's resource location
     * @param isUnlocked whether the block is unlocked for tracking
     */
    private void addBlockCard(ResourceLocation blockId, boolean isUnlocked) {
        var blockOptional = BuiltInRegistries.BLOCK.get(blockId);
        if (blockOptional.isEmpty()) {
            AromaAffect.LOGGER.warn("Block not found: {}", blockId);
            return;
        }

        var block = blockOptional.get().value();
        ItemStack icon = new ItemStack(block.asItem());
        if (icon.isEmpty()) {
            // Some blocks don't have item forms (like water), use a placeholder or skip
            // For water, we could use water bucket, but for now we skip
            AromaAffect.LOGGER.debug("Block {} has no item form, skipping", blockId);
            return;
        }

        Component displayName = block.getName();
        Component description = Component.translatable("menu.aromaaffect.blocks.card.description", displayName);

        cards.add(new SelectionCard(blockId, displayName, icon, isUnlocked, description));
    }

    @Override
    protected void onCardSelected(SelectionCard card, int index) {
        selectedCardIndex = index;
        AromaAffect.LOGGER.info("Selected block for tracking: {}", card.id);

        // Close menu first
        closeCurrentMenu();

        // Execute path command to start tracking
        startPathToBlock(card.id);
    }

    /**
     * Starts a particle path to the selected block by executing the path command.
     *
     * @param blockId the block's resource location
     */
    private void startPathToBlock(ResourceLocation blockId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // Send the path command to the server
        String command = String.format("aromatest path block %s", blockId.toString());
        AromaAffect.LOGGER.debug("Executing path command: {}", command);

        // Use the connection to send the command
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
