package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseAbilityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Menu screen for selecting structures to track.
 *
 * <p>This menu displays all structures that the currently equipped Nose can detect.
 * Structures are shown as cards with representative item icons.</p>
 *
 * <p>When a structure is selected, the path command is executed to create a particle
 * trail leading to the nearest instance of that structure.</p>
 *
 * @see MenuManager#openStructuresMenu()
 * @see SelectionMenuScreen
 */
public class StructuresMenuScreen extends SelectionMenuScreen {

    /**
     * Map of structure IDs to their representative icons and display names.
     */
    private static final Map<String, StructureInfo> STRUCTURE_INFO = new HashMap<>();

    static {
        // Villages
        STRUCTURE_INFO.put("minecraft:village_plains", new StructureInfo(Items.BELL.getDefaultInstance(), "Village (Plains)"));
        STRUCTURE_INFO.put("minecraft:village_desert", new StructureInfo(Items.SANDSTONE.getDefaultInstance(), "Village (Desert)"));
        STRUCTURE_INFO.put("minecraft:village_savanna", new StructureInfo(Items.ACACIA_LOG.getDefaultInstance(), "Village (Savanna)"));
        STRUCTURE_INFO.put("minecraft:village_snowy", new StructureInfo(Items.SNOWBALL.getDefaultInstance(), "Village (Snowy)"));
        STRUCTURE_INFO.put("minecraft:village_taiga", new StructureInfo(Items.SPRUCE_LOG.getDefaultInstance(), "Village (Taiga)"));

        // Common overworld structures
        STRUCTURE_INFO.put("minecraft:mineshaft", new StructureInfo(Items.RAIL.getDefaultInstance(), "Mineshaft"));
        STRUCTURE_INFO.put("minecraft:mineshaft_mesa", new StructureInfo(Items.POWERED_RAIL.getDefaultInstance(), "Mineshaft (Mesa)"));
        STRUCTURE_INFO.put("minecraft:ruined_portal", new StructureInfo(Items.CRYING_OBSIDIAN.getDefaultInstance(), "Ruined Portal"));
        STRUCTURE_INFO.put("minecraft:ruined_portal_nether", new StructureInfo(Items.OBSIDIAN.getDefaultInstance(), "Ruined Portal (Nether)"));
        STRUCTURE_INFO.put("minecraft:shipwreck", new StructureInfo(Items.OAK_BOAT.getDefaultInstance(), "Shipwreck"));
        STRUCTURE_INFO.put("minecraft:ocean_ruin_cold", new StructureInfo(Items.PRISMARINE_BRICKS.getDefaultInstance(), "Ocean Ruins (Cold)"));
        STRUCTURE_INFO.put("minecraft:ocean_ruin_warm", new StructureInfo(Items.PRISMARINE.getDefaultInstance(), "Ocean Ruins (Warm)"));
        STRUCTURE_INFO.put("minecraft:buried_treasure", new StructureInfo(Items.HEART_OF_THE_SEA.getDefaultInstance(), "Buried Treasure"));

        // Pyramids and temples
        STRUCTURE_INFO.put("minecraft:desert_pyramid", new StructureInfo(Items.TNT.getDefaultInstance(), "Desert Pyramid"));
        STRUCTURE_INFO.put("minecraft:jungle_pyramid", new StructureInfo(Items.MOSSY_COBBLESTONE.getDefaultInstance(), "Jungle Temple"));
        STRUCTURE_INFO.put("minecraft:igloo", new StructureInfo(Items.SNOW_BLOCK.getDefaultInstance(), "Igloo"));
        STRUCTURE_INFO.put("minecraft:swamp_hut", new StructureInfo(Items.CAULDRON.getDefaultInstance(), "Witch Hut"));

        // Pillager structures
        STRUCTURE_INFO.put("minecraft:pillager_outpost", new StructureInfo(Items.CROSSBOW.getDefaultInstance(), "Pillager Outpost"));
        STRUCTURE_INFO.put("minecraft:mansion", new StructureInfo(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Woodland Mansion"));

        // Ocean
        STRUCTURE_INFO.put("minecraft:monument", new StructureInfo(Items.PRISMARINE_SHARD.getDefaultInstance(), "Ocean Monument"));

        // End-game overworld
        STRUCTURE_INFO.put("minecraft:stronghold", new StructureInfo(Items.END_PORTAL_FRAME.getDefaultInstance(), "Stronghold"));
        STRUCTURE_INFO.put("minecraft:ancient_city", new StructureInfo(Items.SCULK.getDefaultInstance(), "Ancient City"));
        STRUCTURE_INFO.put("minecraft:trail_ruins", new StructureInfo(Items.BRUSH.getDefaultInstance(), "Trail Ruins"));
        STRUCTURE_INFO.put("minecraft:trial_chambers", new StructureInfo(Items.TRIAL_KEY.getDefaultInstance(), "Trial Chambers"));

        // Nether structures
        STRUCTURE_INFO.put("minecraft:fortress", new StructureInfo(Items.NETHER_BRICK.getDefaultInstance(), "Nether Fortress"));
        STRUCTURE_INFO.put("minecraft:bastion_remnant", new StructureInfo(Items.GILDED_BLACKSTONE.getDefaultInstance(), "Bastion Remnant"));

        // End structures
        STRUCTURE_INFO.put("minecraft:end_city", new StructureInfo(Items.PURPUR_BLOCK.getDefaultInstance(), "End City"));
    }

    public StructuresMenuScreen() {
        super(MenuCategory.STRUCTURES);
    }

    @Override
    protected void loadCards() {
        cards.clear();

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            AromaAffect.LOGGER.debug("No player available for structures menu");
            return;
        }

        // Check if player has a nose equipped
        if (!EquippedNoseHelper.hasNoseEquipped(player)) {
            AromaAffect.LOGGER.debug("No nose equipped, showing empty menu");
            return;
        }

        // Get detectable structures from equipped nose
        NoseAbilityResolver.ResolvedAbilities abilities = EquippedNoseHelper.getEquippedAbilities(player);
        Set<String> detectableStructures = abilities.getStructures();

        if (detectableStructures.isEmpty()) {
            AromaAffect.LOGGER.debug("Equipped nose has no structure detection abilities");
            return;
        }

        // Add cards for each detectable structure
        for (String structureId : detectableStructures) {
            ResourceLocation resourceLocation = ResourceLocation.parse(structureId);
            addStructureCard(resourceLocation, true);
        }

        AromaAffect.LOGGER.debug("Loaded {} structure cards from equipped nose", cards.size());
    }

    /**
     * Adds a structure card to the menu.
     *
     * @param structureId the structure's resource location
     * @param isUnlocked  whether the structure is unlocked for tracking
     */
    private void addStructureCard(ResourceLocation structureId, boolean isUnlocked) {
        StructureInfo info = STRUCTURE_INFO.get(structureId.toString());

        ItemStack icon;
        String displayName;

        if (info != null) {
            icon = info.icon;
            displayName = info.displayName;
        } else {
            // Fallback for unknown structures
            icon = Items.COMPASS.getDefaultInstance();
            displayName = capitalizeWords(structureId.getPath().replace("_", " "));
        }

        Component name = Component.literal(displayName);
        Component description = Component.translatable("menu.aromaaffect.structures.card.description", name);

        cards.add(new SelectionCard(structureId, name, icon, isUnlocked, description));
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
        ActiveTrackingState.set(card.id, card.displayName, card.icon, MenuCategory.STRUCTURES);
        AromaAffect.LOGGER.info("Selected structure for tracking: {}", card.id);

        startPathToStructure(card.id);
        MenuManager.returnToRadialMenu();
    }

    /**
     * Starts a particle path to the selected structure by executing the path command.
     *
     * @param structureId the structure's resource location
     */
    private void startPathToStructure(ResourceLocation structureId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // Send the path command to the server
        String command = String.format("aromatest path structure %s", structureId.toString());
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

    /**
     * Holds structure display information.
     */
    private record StructureInfo(ItemStack icon, String displayName) {
    }
}
