package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds the hardcoded Aroma Affect guide content.
 * In the future, this will be replaced by JSON-driven content loading.
 */
public final class AromaCraftGuideContent {

    private static final ResourceLocation NOSE_SCREENSHOT = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/guide/nose_screenshot.png");

    private static GuideBook cachedBook;

    private AromaCraftGuideContent() {
    }

    public static GuideBook getBook() {
        if (cachedBook == null) {
            cachedBook = buildBook();
        }
        return cachedBook;
    }

    private static GuideBook buildBook() {
        return GuideBook.builder("Aroma Affect Guide")
                .subtitle("Scent-Powered Exploration for Minecraft")
                .category(buildWelcomeCategory())
                .category(buildNosesCategory())
                .category(buildScentsCategory())
                .category(buildAbilitiesCategory())
                .category(buildNoseSmithCategory())
                .build();
    }

    // ── Welcome / Getting Started ──────────────────────────────────

    private static GuideCategory buildWelcomeCategory() {
        return GuideCategory.builder("welcome", "Getting Started")
                .icon(GuideIcon.ofItem(new ItemStack(Items.BOOK)))
                .accentColor(0xFF6D5EF8)
                .page(buildWelcomePage())
                .page(buildQuickStartPage())
                .page(buildControlsPage())
                .build();
    }

    private static GuidePage buildWelcomePage() {
        return GuidePage.builder("welcome", "Welcome")
                .icon(GuideIcon.ofItem(new ItemStack(Items.NETHER_STAR)))
                .element(GuideElement.header("Welcome to Aroma Affect!"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "Aroma Affect is a Minecraft mod that brings the sense of smell into your game. " +
                        "Through the Nose equipment system, you can detect ores, biomes, structures, " +
                        "and flowers by their scent signatures."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.image(NOSE_SCREENSHOT, 500, 262))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("What can you do?"))
                .element(GuideElement.text(
                        "\u2022 Equip Noses to gain scent-detection abilities\n" +
                        "\u2022 Track specific blocks, biomes, and structures\n" +
                        "\u2022 Progress through 6 tiers of increasingly powerful Noses\n" +
                        "\u2022 Connect real OVR scent hardware for immersive play\n" +
                        "\u2022 Visit the Nose Smith NPC in villages for quests"
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.tip("Use the sidebar to navigate between sections of this guide."))
                .element(GuideElement.separator())
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("Mod Version: 0.0.3 | Minecraft 1.21.10", 0xFF888888))
                .build();
    }

    private static GuidePage buildQuickStartPage() {
        return GuidePage.builder("quickstart", "Quick Start")
                .icon(GuideIcon.ofItem(new ItemStack(Items.COMPASS)))
                .element(GuideElement.header("Quick Start Guide"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.subheader("Step 1: Find a Nose Smith"))
                .element(GuideElement.text(
                        "Nose Smiths spawn naturally in villages. Look for an NPC with a large nose! " +
                        "Talk to them and they'll ask you to bring a specific flower."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("Step 2: Complete the Quest"))
                .element(GuideElement.text(
                        "Find the flower the Nose Smith requests and drop it near them. " +
                        "They'll reward you with a Forager's Nose - your first Nose item!"
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("Step 3: Equip Your Nose"))
                .element(GuideElement.text(
                        "Place the Nose in your helmet armor slot. Once equipped, you'll gain " +
                        "access to the Scent Menu and begin detecting nearby scents."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("Step 4: Open the Scent Menu"))
                .element(GuideElement.text(
                        "Press R (default) to open the radial Scent Menu. From here you can " +
                        "choose what to track: Blocks, Biomes, Structures, or Flowers."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.tip(
                        "Higher tier Noses detect more targets and have special abilities!"
                ))
                .build();
    }

    private static GuidePage buildControlsPage() {
        return GuidePage.builder("controls", "Controls")
                .icon(GuideIcon.ofItem(new ItemStack(Items.OAK_BUTTON)))
                .element(GuideElement.header("Controls & Keybindings"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text("All keybindings can be changed in Minecraft's Controls settings under the Aroma Affect category."))
                .element(GuideElement.spacer(6))
                .element(GuideElement.separator())
                .element(GuideElement.spacer(4))
                .element(GuideElement.subheader("Default Keybindings"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("\u25B6  R  \u2500  Open Scent Menu", 0xFFB8A9FF))
                .element(GuideElement.spacer(2))
                .element(GuideElement.text("Opens the radial menu for selecting tracking targets."))
                .element(GuideElement.spacer(6))
                .element(GuideElement.coloredText("\u25B6  Shift+C  \u2500  Open Configuration", 0xFFB8A9FF))
                .element(GuideElement.spacer(2))
                .element(GuideElement.text("Opens the Aroma Affect settings panel."))
                .element(GuideElement.spacer(6))
                .element(GuideElement.coloredText("\u25B6  V  \u2500  Toggle Search Mode", 0xFFB8A9FF))
                .element(GuideElement.spacer(2))
                .element(GuideElement.text("Activates search mode for scanning surroundings while a Nose is equipped."))
                .element(GuideElement.spacer(8))
                .element(GuideElement.separator())
                .element(GuideElement.spacer(4))
                .element(GuideElement.subheader("Commands"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("/aromaguide  \u2500  Opens this guide", 0xFFA8E6CF))
                .element(GuideElement.spacer(2))
                .element(GuideElement.coloredText("/aromatest  \u2500  Debug & testing tools", 0xFFA8E6CF))
                .build();
    }

    // ── Noses ──────────────────────────────────────────────────────

    private static GuideCategory buildNosesCategory() {
        return GuideCategory.builder("noses", "Noses")
                .icon(GuideIcon.ofItem(new ItemStack(Items.IRON_HELMET)))
                .accentColor(0xFFE8A838)
                .page(buildNosesOverviewPage())
                .page(buildNoseTiersPage())
                .build();
    }

    private static GuidePage buildNosesOverviewPage() {
        return GuidePage.builder("noses_overview", "Overview")
                .icon(GuideIcon.ofItem(new ItemStack(Items.IRON_HELMET)))
                .element(GuideElement.header("The Nose System"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "Noses are the core equipment of Aroma Affect. They are worn in the helmet " +
                        "slot and grant the player various scent-detection abilities based on their tier."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.image(NOSE_SCREENSHOT, 500, 262))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("How Noses Work"))
                .element(GuideElement.text(
                        "Each Nose has a tier that determines which blocks, biomes, structures, " +
                        "and flowers it can detect. Higher tier Noses unlock more targets and " +
                        "gain special passive abilities."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("Obtaining Noses"))
                .element(GuideElement.text(
                        "Your first Nose is obtained from the Nose Smith NPC in villages. " +
                        "Higher tier Noses can be crafted or found in dungeon loot."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.tip("Noses render as 3D models on your character when equipped!"))
                .build();
    }

    private static GuidePage buildNoseTiersPage() {
        return GuidePage.builder("nose_tiers", "Nose Tiers")
                .icon(GuideIcon.ofItem(new ItemStack(Items.DIAMOND)))
                .element(GuideElement.header("Nose Tier Progression"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text("There are 6 tiers of Noses, each with increasing power:"))
                .element(GuideElement.spacer(6))
                .element(GuideElement.coloredText("\u2B50 Tier 1 \u2500 Forager's Nose", 0xFFAAAAAA))
                .element(GuideElement.text("Basic scent detection. Detects common ores and nearby flowers."))
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("\u2B50 Tier 2 \u2500 Prospector's Nose", 0xFFFFD700))
                .element(GuideElement.text("Enhanced detection range. Can track biomes and uncommon ores."))
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("\u2B50 Tier 3 \u2500 Jeweler's Nose", 0xFF55FFFF))
                .element(GuideElement.text("Detects diamonds, emeralds, and ancient debris. Unlocks structure tracking."))
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("\u2B50 Tier 4 \u2500 Dimensional Nose", 0xFFFF6600))
                .element(GuideElement.text("Works across dimensions. Detects Nether and End resources."))
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("\u2B50 Tier 5 \u2500 Ancient Nose", 0xFF9966CC))
                .element(GuideElement.text("Near-maximum detection. Passive scent abilities activate automatically."))
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("\u2B50 Tier 6 \u2500 Dragon's Nose", 0xFFFF55FF))
                .element(GuideElement.text("Ultimate tier. Detects everything. Full passive scent aura and danger sense."))
                .element(GuideElement.spacer(8))
                .element(GuideElement.separator())
                .element(GuideElement.tip("Each tier also inherits all detection targets from lower tiers."))
                .build();
    }

    // ── Scents ─────────────────────────────────────────────────────

    private static GuideCategory buildScentsCategory() {
        return GuideCategory.builder("scents", "Scents")
                .icon(GuideIcon.ofItem(new ItemStack(Items.POTION)))
                .accentColor(0xFF55CC88)
                .page(buildScentsOverviewPage())
                .page(buildScentListPage())
                .build();
    }

    private static GuidePage buildScentsOverviewPage() {
        return GuidePage.builder("scents_overview", "Overview")
                .icon(GuideIcon.ofItem(new ItemStack(Items.POTION)))
                .element(GuideElement.header("The Scent System"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "Scents are Aroma Affect's way of connecting the digital Minecraft world " +
                        "to real-world smells. Each scent corresponds to a real fragrance that " +
                        "can be emitted by OVR hardware when triggered in-game."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("How Scents Trigger"))
                .element(GuideElement.text(
                        "As you explore the world with a Nose equipped, various game events " +
                        "trigger scent emissions: entering a biome, discovering a flower, " +
                        "finding a structure, or mining a specific block."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("OVR Hardware"))
                .element(GuideElement.text(
                        "When connected to OVR scent hardware via the built-in WebSocket bridge, " +
                        "triggered scents are sent to the device to emit real fragrances. " +
                        "The mod works without hardware too - scents are indicated visually."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.tip("Check the Settings menu to configure your OVR hardware connection."))
                .build();
    }

    private static GuidePage buildScentListPage() {
        return GuidePage.builder("scent_list", "Scent Collection")
                .icon(GuideIcon.ofItem(new ItemStack(Items.FLOWER_POT)))
                .element(GuideElement.header("All Scents"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text("Aroma Affect features 18 unique scent profiles:"))
                .element(GuideElement.spacer(6))
                .element(GuideElement.coloredText("\u2726 Winter", 0xFFADD8E6))
                .element(GuideElement.text("Cold air, cool mint, ozone, and watery melon"))
                .element(GuideElement.spacer(3))
                .element(GuideElement.coloredText("\u2726 Floral", 0xFFFFB7C5))
                .element(GuideElement.text("A flower garden with wafts of different blooms"))
                .element(GuideElement.spacer(3))
                .element(GuideElement.coloredText("\u2726 Petrichor", 0xFF7B8FAD))
                .element(GuideElement.text("Rain hitting pavement, drenched streets"))
                .element(GuideElement.spacer(3))
                .element(GuideElement.coloredText("\u2726 Evergreen", 0xFF228B22))
                .element(GuideElement.text("Deep forest pine trees and lush greenery"))
                .element(GuideElement.spacer(3))
                .element(GuideElement.coloredText("\u2726 Smoky", 0xFF8B4513))
                .element(GuideElement.text("Campfire crackling with creosote and cedarwood"))
                .element(GuideElement.spacer(3))
                .element(GuideElement.coloredText("\u2726 Citrus", 0xFFFFA500))
                .element(GuideElement.text("Bright lemon, orange, and grapefruit"))
                .element(GuideElement.spacer(3))
                .element(GuideElement.coloredText("\u2726 Marine", 0xFF4682B4))
                .element(GuideElement.text("Ocean spray, seaweed, and weathered driftwood"))
                .element(GuideElement.spacer(3))
                .element(GuideElement.coloredText("\u2726 Sweet", 0xFFDDA0DD))
                .element(GuideElement.text("Vanilla, cream, and chocolate fresh-baked treat"))
                .element(GuideElement.spacer(6))
                .element(GuideElement.separator())
                .element(GuideElement.spacer(4))
                .element(GuideElement.coloredText("...and 10 more! Discover them by exploring.", 0xFF888888))
                .build();
    }

    // ── Abilities ──────────────────────────────────────────────────

    private static GuideCategory buildAbilitiesCategory() {
        return GuideCategory.builder("abilities", "Abilities")
                .icon(GuideIcon.ofItem(new ItemStack(Items.ENDER_EYE)))
                .accentColor(0xFFCC5555)
                .page(buildAbilitiesPage())
                .build();
    }

    private static GuidePage buildAbilitiesPage() {
        return GuidePage.builder("abilities_overview", "Overview")
                .icon(GuideIcon.ofItem(new ItemStack(Items.ENDER_EYE)))
                .element(GuideElement.header("Nose Abilities"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "Higher-tier Noses unlock special abilities beyond basic scent detection:"
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Passive Scent Detection"))
                .element(GuideElement.coloredText("Unlocked at: Tier 2+", 0xFFFFD700))
                .element(GuideElement.text(
                        "Automatically detects scents in a radius around you without needing " +
                        "to activate search mode. The radius increases with tier."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Active Target Tracking"))
                .element(GuideElement.coloredText("Unlocked at: Tier 3+", 0xFF55FFFF))
                .element(GuideElement.text(
                        "Allows you to lock onto a specific target and receive directional " +
                        "guidance through particles and the Tracking Compass."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Structure Compass"))
                .element(GuideElement.coloredText("Unlocked at: Tier 4+", 0xFFFF6600))
                .element(GuideElement.text(
                        "Gain a built-in compass pointing toward the nearest selected structure type."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Danger Sense"))
                .element(GuideElement.coloredText("Unlocked at: Tier 6", 0xFFFF55FF))
                .element(GuideElement.text(
                        "The ultimate ability. Sense nearby hostile mobs and hazards through " +
                        "subtle scent cues, giving you an early warning system."
                ))
                .build();
    }

    // ── Nose Smith ─────────────────────────────────────────────────

    private static GuideCategory buildNoseSmithCategory() {
        return GuideCategory.builder("nose_smith", "Nose Smith")
                .icon(GuideIcon.ofItem(new ItemStack(Items.VILLAGER_SPAWN_EGG)))
                .accentColor(0xFF8888DD)
                .page(buildNoseSmithPage())
                .build();
    }

    private static GuidePage buildNoseSmithPage() {
        return GuidePage.builder("nose_smith_overview", "The Nose Smith")
                .icon(GuideIcon.ofItem(new ItemStack(Items.VILLAGER_SPAWN_EGG)))
                .element(GuideElement.header("The Nose Smith"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "The Nose Smith is a unique NPC that spawns in villages. They are " +
                        "a flower collector and master nose crafter."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("Finding a Nose Smith"))
                .element(GuideElement.text(
                        "Nose Smiths spawn naturally in custom village houses added by Aroma Affect. " +
                        "Look for a villager-like NPC with a distinctive large nose. " +
                        "Right-click to start a dialogue."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("The Flower Quest"))
                .element(GuideElement.text(
                        "Each Nose Smith requests a specific flower. The requested flower is shown " +
                        "in the dialogue UI. Find it in the world and drop it near the Nose Smith " +
                        "to complete the exchange."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.subheader("Rewards"))
                .element(GuideElement.text(
                        "Upon receiving their flower, the Nose Smith removes their own nose and " +
                        "gives it to you as a Forager's Nose item! This is how you obtain " +
                        "your very first Nose."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.tip(
                        "After completing the trade, the Nose Smith's dialogue changes to reflect " +
                        "that they no longer have a nose. Look for another village to find more!"
                ))
                .build();
    }
}
