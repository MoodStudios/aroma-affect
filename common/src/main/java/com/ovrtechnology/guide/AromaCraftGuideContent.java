package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds the hardcoded Aroma Affect guide content.
 * In the future, this will be replaced by JSON-driven content loading.
 */
public final class AromaCraftGuideContent {

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
                .category(buildSnifferCategory())
                .category(buildEndgameCategory())
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static ItemStack noseItem(String id) {
        return NoseRegistry.getNose(id)
                .map(ItemStack::new)
                .orElse(new ItemStack(Items.IRON_HELMET));
    }

    private static ItemStack scentItem(String id) {
        return ScentItemRegistry.getScentItem(id)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack snifferNoseItem(String id) {
        return SnifferNoseRegistry.getSnifferNose(id)
                .map(ItemStack::new)
                .orElse(new ItemStack(Items.IRON_HELMET));
    }

    // ── Welcome / Getting Started ──────────────────────────────────

    private static GuideCategory buildWelcomeCategory() {
        return GuideCategory.builder("welcome", "Getting Started")
                .icon(GuideIcon.ofTexture(
                        ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID,
                                "textures/gui/guide/icon_getting_started.png"), 32, 32))
                .accentColor(0xFF6D5EF8)
                .page(buildWelcomePage())
                .build();
    }

    private static GuidePage buildWelcomePage() {
        ItemStack guideItem = new ItemStack(AromaGuideRegistry.getAROMA_GUIDE().get());

        ItemStack[] guideRecipe = new ItemStack[]{
                ItemStack.EMPTY,              ItemStack.EMPTY,            ItemStack.EMPTY,
                new ItemStack(Items.COMPASS), new ItemStack(Items.PAPER), ItemStack.EMPTY,
                ItemStack.EMPTY,              ItemStack.EMPTY,            ItemStack.EMPTY
        };

        return GuidePage.builder("welcome", "Welcome")
                .icon(GuideIcon.ofItem(guideItem))
                .element(GuideElement.image(
                        ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID,
                                "textures/gui/guide/ovr_logo.png"), 180, 80))
                .element(GuideElement.spacer(6))
                .element(GuideElement.header("Welcome to Aroma Affect!"))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "Aroma Affect brings the sense of smell into Minecraft. " +
                        "Your journey starts with the Aroma Guide \u2014 a compass that " +
                        "points you toward the nearest village where a Nose Smith awaits."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("The Aroma Guide"))
                .element(GuideElement.itemShowcase(guideItem, Component.literal(
                        "Given to you on first join. Points to the nearest village.")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "Lost your guide? Craft a new one:"
                ))
                .element(GuideElement.spacer(10))
                .element(GuideElement.craftingGrid(guideRecipe, guideItem, "Aroma Guide"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("The Nose Smith"))
                .element(GuideElement.text(
                        "Find the Nose Smith inside a village \u2014 a villager with an " +
                        "oversized nose. Talk to them, bring the flower they request, " +
                        "and they'll give you their nose as a reward: the Forager's Nose."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Noses & Scent Detection"))
                .element(GuideElement.text(
                        "Equip a Nose in your helmet slot to unlock scent abilities. " +
                        "Press R to open the Scent Menu and choose what to track: " +
                        "biomes, structures, flowers, and more. Higher tier Noses " +
                        "detect more targets and unlock special abilities."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("OVR Hardware"))
                .element(GuideElement.text(
                        "Connect OVR scent hardware to experience real fragrances " +
                        "as you play. The mod works without hardware too \u2014 scents " +
                        "are always indicated visually."
                ))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "To connect your OVR device, download the OVR Bridge \u2014 " +
                        "a companion app that links Minecraft with your scent hardware:"
                ))
                .element(GuideElement.spacer(4))
                .element(GuideElement.urlLink("Download OVR Bridge", "https://www.ovrtechnology.com/"))
                .element(GuideElement.spacer(6))
                .element(GuideElement.tip(
                        "Once the Bridge is installed, check the connection status " +
                        "in Settings \u2192 Bridge within the mod's configuration screen."
                ))
                .build();
    }

    // ── Noses ───────────────────────────────────────────────────────

    private static GuideCategory buildNosesCategory() {
        return GuideCategory.builder("noses", "Noses")
                .icon(GuideIcon.ofItem(noseItem("basic_nose")))
                .accentColor(0xFFE8A838)
                .page(buildBasicNosePage())
                .page(buildGoldNosePage())
                .page(buildDiamondNosePage())
                .page(buildBlazeNosePage())
                .page(buildNetheritNosePage())
                .page(buildUltimateNosePage())
                .build();
    }

    // ── Tier 1: Forager's Nose ──────────────────────────────────────

    private static GuidePage buildBasicNosePage() {
        ItemStack nose = noseItem("basic_nose");

        return GuidePage.builder("basic_nose", "Forager's Nose")
                .icon(GuideIcon.ofItem(nose))
                .element(GuideElement.header("Forager's Nose"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(nose, Component.literal(
                        "Tier 1  \u2022  100 Durability  \u2022  Repair: Leather")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "Your first Nose. Find the Nose Smith in a village, " +
                        "complete their flower quest, and they'll give you " +
                        "this Nose as a reward."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Detects"))
                .element(GuideElement.detectionLabel("Blocks"))
                .element(GuideElement.iconText(new ItemStack(Items.WATER_BUCKET), "Water"))
                .element(GuideElement.iconText("Coal Ore", new ItemStack(Items.COAL_ORE), new ItemStack(Items.DEEPSLATE_COAL_ORE)))
                .element(GuideElement.iconText("Iron Ore", new ItemStack(Items.IRON_ORE), new ItemStack(Items.DEEPSLATE_IRON_ORE)))
                .element(GuideElement.iconText("Emerald Ore", new ItemStack(Items.EMERALD_ORE), new ItemStack(Items.DEEPSLATE_EMERALD_ORE)))
                .element(GuideElement.spacer(4))
                .element(GuideElement.detectionLabel("Biomes"))
                .element(GuideElement.iconText("Plains, Forests, Deserts, Taigas",
                        new ItemStack(Items.GRASS_BLOCK), new ItemStack(Items.OAK_LOG), new ItemStack(Items.SAND), new ItemStack(Items.SPRUCE_LOG)))
                .element(GuideElement.iconText("Swamps, Savannas, Oceans, Beaches",
                        new ItemStack(Items.LILY_PAD), new ItemStack(Items.ACACIA_LOG), new ItemStack(Items.KELP), new ItemStack(Items.SAND)))
                .element(GuideElement.iconText("Snowy biomes, Mountains",
                        new ItemStack(Items.SNOW_BLOCK), new ItemStack(Items.POWDER_SNOW_BUCKET), new ItemStack(Items.STONE)))
                .element(GuideElement.spacer(4))
                .element(GuideElement.detectionLabel("Flowers"))
                .element(GuideElement.iconText("All vanilla flowers",
                        new ItemStack(Items.DANDELION), new ItemStack(Items.POPPY), new ItemStack(Items.BLUE_ORCHID),
                        new ItemStack(Items.ALLIUM), new ItemStack(Items.AZURE_BLUET), new ItemStack(Items.OXEYE_DAISY),
                        new ItemStack(Items.CORNFLOWER), new ItemStack(Items.LILY_OF_THE_VALLEY), new ItemStack(Items.TORCHFLOWER)))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("How to Obtain"))
                .element(GuideElement.text(
                        "Complete the Nose Smith's flower quest in any village. " +
                        "This Nose cannot be crafted."
                ))
                .build();
    }

    // ── Tier 2: Prospector's Nose ───────────────────────────────────

    private static GuidePage buildGoldNosePage() {
        ItemStack nose = noseItem("gold_nose");
        ItemStack basicNose = noseItem("basic_nose");
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
        ItemStack string = new ItemStack(Items.STRING);

        ItemStack[] recipe = new ItemStack[]{
                gold,   gold,      gold,
                gold,   basicNose, gold,
                string, boneMeal,  string
        };

        return GuidePage.builder("gold_nose", "Prospector's Nose")
                .icon(GuideIcon.ofItem(nose))
                .element(GuideElement.header("Prospector's Nose"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(nose, Component.literal(
                        "Tier 2  \u2022  150 Durability  \u2022  Repair: Gold Ingot")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "An upgrade from the Forager's Nose. Detects valuable " +
                        "ores and exotic biomes."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("New Detections"))
                .element(GuideElement.detectionLabel("Blocks"))
                .element(GuideElement.iconText("Gold Ore", new ItemStack(Items.GOLD_ORE), new ItemStack(Items.DEEPSLATE_GOLD_ORE), new ItemStack(Items.NETHER_GOLD_ORE)))
                .element(GuideElement.iconText("Lapis Ore", new ItemStack(Items.LAPIS_ORE), new ItemStack(Items.DEEPSLATE_LAPIS_ORE)))
                .element(GuideElement.iconText("Copper Ore", new ItemStack(Items.COPPER_ORE), new ItemStack(Items.DEEPSLATE_COPPER_ORE)))
                .element(GuideElement.iconText("Redstone Ore", new ItemStack(Items.REDSTONE_ORE), new ItemStack(Items.DEEPSLATE_REDSTONE_ORE)))
                .element(GuideElement.iconText(new ItemStack(Items.LAVA_BUCKET), "Lava"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.detectionLabel("Biomes"))
                .element(GuideElement.iconText("Meadow, Grove, Cherry Grove",
                        new ItemStack(Items.OXEYE_DAISY), new ItemStack(Items.CHERRY_LEAVES), new ItemStack(Items.FERN)))
                .element(GuideElement.iconText("Jungles, Badlands, Ice Spikes",
                        new ItemStack(Items.JUNGLE_LOG), new ItemStack(Items.RED_SAND), new ItemStack(Items.PACKED_ICE)))
                .element(GuideElement.iconText("Mushroom Fields, Caves, Deep Dark",
                        new ItemStack(Items.RED_MUSHROOM), new ItemStack(Items.DRIPSTONE_BLOCK), new ItemStack(Items.SCULK)))
                .element(GuideElement.iconText("Nether biomes",
                        new ItemStack(Items.NETHERRACK), new ItemStack(Items.CRIMSON_NYLIUM), new ItemStack(Items.WARPED_NYLIUM), new ItemStack(Items.SOUL_SAND)))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(recipe, nose, "Prospector's Nose"))
                .build();
    }

    // ── Tier 3: Jeweler's Nose ──────────────────────────────────────

    private static GuidePage buildDiamondNosePage() {
        ItemStack nose = noseItem("diamond_nose");
        ItemStack goldNose = noseItem("gold_nose");
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
        ItemStack string = new ItemStack(Items.STRING);

        ItemStack[] recipe = new ItemStack[]{
                diamond, diamond,  diamond,
                diamond, goldNose, diamond,
                string,  boneMeal, string
        };

        return GuidePage.builder("diamond_nose", "Jeweler's Nose")
                .icon(GuideIcon.ofItem(nose))
                .element(GuideElement.header("Jeweler's Nose"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(nose, Component.literal(
                        "Tier 3  \u2022  500 Durability  \u2022  Repair: Diamond")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "A powerful Nose that unlocks structure detection. " +
                        "Can find diamonds, spawners, and village variants."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("New Detections"))
                .element(GuideElement.detectionLabel("Blocks"))
                .element(GuideElement.iconText("Diamond Ore", new ItemStack(Items.DIAMOND_ORE), new ItemStack(Items.DEEPSLATE_DIAMOND_ORE)))
                .element(GuideElement.iconText(new ItemStack(Items.SPAWNER), "Spawner"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.detectionLabel("Structures"))
                .element(GuideElement.iconText("Village (Plains, Desert, Savanna, Snowy, Taiga)",
                        new ItemStack(Items.BELL), new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.COMPOSTER)))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(recipe, nose, "Jeweler's Nose"))
                .build();
    }

    // ── Tier 4: Dimensional Nose ────────────────────────────────────

    private static GuidePage buildBlazeNosePage() {
        ItemStack nose = noseItem("blaze_nose");
        ItemStack diamondNose = noseItem("diamond_nose");
        ItemStack blazePowder = new ItemStack(Items.BLAZE_POWDER);
        ItemStack flint = new ItemStack(Items.FLINT);
        ItemStack enderPearl = new ItemStack(Items.ENDER_PEARL);

        ItemStack[] recipe = new ItemStack[]{
                blazePowder, blazePowder,  blazePowder,
                blazePowder, diamondNose,  blazePowder,
                flint,       enderPearl,   flint
        };

        return GuidePage.builder("blaze_nose", "Dimensional Nose")
                .icon(GuideIcon.ofItem(nose))
                .element(GuideElement.header("Dimensional Nose"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(nose, Component.literal(
                        "Tier 4  \u2022  500 Durability  \u2022  Repair: Blaze Rod")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "A Nose attuned to other dimensions. Capable of " +
                        "detecting the most dangerous structures across all realms."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("New Detections"))
                .element(GuideElement.detectionLabel("Structures"))
                .element(GuideElement.iconText("Stronghold", new ItemStack(Items.END_PORTAL_FRAME), new ItemStack(Items.ENDER_EYE)))
                .element(GuideElement.iconText("Ancient City", new ItemStack(Items.SCULK_SHRIEKER), new ItemStack(Items.SCULK_CATALYST)))
                .element(GuideElement.iconText("Nether Fortress", new ItemStack(Items.NETHER_BRICKS), new ItemStack(Items.BLAZE_ROD)))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(recipe, nose, "Dimensional Nose"))
                .build();
    }

    // ── Tier 5: Ancient Nose ────────────────────────────────────────

    private static GuidePage buildNetheritNosePage() {
        ItemStack nose = noseItem("netherite_nose");
        ItemStack blazeNose = noseItem("blaze_nose");
        ItemStack netheriteScrap = new ItemStack(Items.NETHERITE_SCRAP);
        ItemStack ghastTear = new ItemStack(Items.GHAST_TEAR);

        ItemStack[] recipe = new ItemStack[]{
                netheriteScrap, netheriteScrap, netheriteScrap,
                netheriteScrap, blazeNose,      netheriteScrap,
                ghastTear,      netheriteScrap, ghastTear
        };

        return GuidePage.builder("netherite_nose", "Ancient Nose")
                .icon(GuideIcon.ofItem(nose))
                .element(GuideElement.header("Ancient Nose"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(nose, Component.literal(
                        "Tier 5  \u2022  750 Durability  \u2022  Repair: Netherite Ingot")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "An ancient Nose of immense power. Unlocks the Precise " +
                        "Sniffer ability, allowing you to sniff out hidden loot " +
                        "from Suspicious Sand."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Abilities"))
                .element(GuideElement.ability("Precise Sniffer \u2014 sniff Suspicious Sand for a 40% chance to find a Sniffer Egg"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("New Detections"))
                .element(GuideElement.detectionLabel("Blocks"))
                .element(GuideElement.iconText(new ItemStack(Items.ANCIENT_DEBRIS), "Ancient Debris"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.detectionLabel("Structures"))
                .element(GuideElement.iconText(new ItemStack(Items.PRISMARINE), "Ocean Monument"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(recipe, nose, "Ancient Nose"))
                .build();
    }

    // ── Tier 6: Dragon's Nose ───────────────────────────────────────

    private static GuidePage buildUltimateNosePage() {
        ItemStack nose = noseItem("ultimate_nose");
        ItemStack netheriteNose = noseItem("netherite_nose");
        ItemStack dragonsBreath = new ItemStack(Items.DRAGON_BREATH);
        ItemStack enderEye = new ItemStack(Items.ENDER_EYE);

        ItemStack[] recipe = new ItemStack[]{
                dragonsBreath, dragonsBreath,  dragonsBreath,
                dragonsBreath, netheriteNose,  dragonsBreath,
                enderEye,      dragonsBreath,  enderEye
        };

        return GuidePage.builder("ultimate_nose", "Dragon's Nose")
                .icon(GuideIcon.ofItem(nose))
                .element(GuideElement.header("Dragon's Nose"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(nose, Component.literal(
                        "Tier 6  \u2022  1000 Durability  \u2022  Repair: Nether Star")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "The ultimate Nose. Grants omniscient detection \u2014 " +
                        "nothing in the world can hide from you. Detects every " +
                        "structure, rare blocks, and even the Wither Rose."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Abilities"))
                .element(GuideElement.abilityLink("Precise Sniffer", "netherite_nose"))
                .element(GuideElement.ability("Omniscent \u2014 detect everything"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("New Detections"))
                .element(GuideElement.detectionLabel("Blocks"))
                .element(GuideElement.iconText("Nether Quartz Ore", new ItemStack(Items.NETHER_QUARTZ_ORE), new ItemStack(Items.QUARTZ)))
                .element(GuideElement.iconText("Amethyst Cluster", new ItemStack(Items.AMETHYST_CLUSTER), new ItemStack(Items.AMETHYST_SHARD)))
                .element(GuideElement.iconText("Budding Amethyst", new ItemStack(Items.BUDDING_AMETHYST), new ItemStack(Items.AMETHYST_BLOCK)))
                .element(GuideElement.iconText(new ItemStack(Items.CHEST), "Chest"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.detectionLabel("Structures"))
                .element(GuideElement.iconText("Mineshaft", new ItemStack(Items.RAIL), new ItemStack(Items.MINECART)))
                .element(GuideElement.iconText("Ruined Portal", new ItemStack(Items.OBSIDIAN), new ItemStack(Items.CRYING_OBSIDIAN)))
                .element(GuideElement.iconText("Shipwreck, Ocean Ruins", new ItemStack(Items.OAK_BOAT), new ItemStack(Items.PRISMARINE_BRICKS)))
                .element(GuideElement.iconText("Buried Treasure", new ItemStack(Items.HEART_OF_THE_SEA), new ItemStack(Items.CHEST)))
                .element(GuideElement.iconText("Pillager Outpost", new ItemStack(Items.CROSSBOW), new ItemStack(Items.DARK_OAK_PLANKS)))
                .element(GuideElement.iconText("Igloo", new ItemStack(Items.SNOW_BLOCK), new ItemStack(Items.ICE)))
                .element(GuideElement.iconText("Swamp Hut", new ItemStack(Items.CAULDRON), new ItemStack(Items.LILY_PAD)))
                .element(GuideElement.iconText("Desert Pyramid", new ItemStack(Items.SANDSTONE), new ItemStack(Items.TNT)))
                .element(GuideElement.iconText("Woodland Mansion", new ItemStack(Items.DARK_OAK_LOG), new ItemStack(Items.TOTEM_OF_UNDYING)))
                .element(GuideElement.iconText("Bastion Remnant", new ItemStack(Items.GILDED_BLACKSTONE), new ItemStack(Items.GOLD_BLOCK)))
                .element(GuideElement.iconText("End City", new ItemStack(Items.PURPUR_BLOCK), new ItemStack(Items.ELYTRA)))
                .element(GuideElement.iconText("Trail Ruins", new ItemStack(Items.DECORATED_POT), new ItemStack(Items.BRUSH)))
                .element(GuideElement.iconText("Trial Chambers", new ItemStack(Items.TRIAL_KEY), new ItemStack(Items.COPPER_BULB)))
                .element(GuideElement.spacer(4))
                .element(GuideElement.detectionLabel("Flowers"))
                .element(GuideElement.iconText(new ItemStack(Items.WITHER_ROSE), "Wither Rose"))
                .element(GuideElement.iconText(new ItemStack(Items.SPORE_BLOSSOM), "Spore Blossom"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(recipe, nose, "Dragon's Nose"))
                .build();
    }

    // ── Sniffer ─────────────────────────────────────────────────────

    private static GuideCategory buildSnifferCategory() {
        return GuideCategory.builder("sniffer", "Sniffer")
                .icon(GuideIcon.ofItem(new ItemStack(Items.SNIFFER_EGG)))
                .accentColor(0xFF4EC9B0)
                .page(buildSnifferOverviewPage())
                .page(buildSnifferTamingPage())
                .page(buildDimensionalTourPage())
                .page(buildEnhancedSnifferNosePage())
                .build();
    }

    // ── Sniffer: Overview ───────────────────────────────────────────

    private static GuidePage buildSnifferOverviewPage() {
        return GuidePage.builder("sniffer_overview", "Overview")
                .icon(GuideIcon.ofItem(new ItemStack(Items.SNIFFER_EGG)))
                .element(GuideElement.header("The Sniffer"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "The Sniffer is the key to the Aroma Affect endgame. " +
                        "Once tamed, it becomes your loyal companion \u2014 ride it " +
                        "across dimensions, equip it with a powerful nose, and " +
                        "let it sniff out the rarest materials in the world."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.tip(
                        "Finding a Sniffer Egg is easier with the Ancient Nose " +
                        "equipped. Its Precise Sniffer ability gives you a 40% " +
                        "chance to find a Sniffer Egg when sniffing Suspicious Sand."
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.abilityLink("Precise Sniffer", "netherite_nose"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("What Can the Sniffer Do?"))
                .element(GuideElement.text(
                        "\u2022 Tame it with Torch Flowers and ride it with a Saddle\n" +
                        "\u2022 Equip it with the Enhanced Sniffer Nose\n" +
                        "\u2022 Take it on a Dimensional Tour to collect rare scents\n" +
                        "\u2022 Once fully equipped, it produces Scent Base \u2014 the " +
                        "endgame crafting material"
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.itemShowcase(scentItem("scent_base"), Component.literal(
                        "Scent Base \u2014 the endgame material produced by an equipped Sniffer")))
                .build();
    }

    // ── Sniffer: Taming & Riding ────────────────────────────────────

    private static GuidePage buildSnifferTamingPage() {
        return GuidePage.builder("sniffer_taming", "Taming & Riding")
                .icon(GuideIcon.ofItem(new ItemStack(Items.SADDLE)))
                .element(GuideElement.header("Taming & Riding"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.subheader("How to Tame"))
                .element(GuideElement.text(
                        "Feed a Sniffer 4 Torch Flowers to tame it. Each flower " +
                        "advances the taming progress \u2014 you'll see heart particles " +
                        "when successful."
                ))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(new ItemStack(Items.TORCHFLOWER),
                        Component.literal("Torch Flower \u2014 feed 4 to tame a Sniffer")))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Riding"))
                .element(GuideElement.text(
                        "Once tamed, equip a Saddle on the Sniffer to ride it. " +
                        "Open the Sniffer's inventory with Shift + Right Click " +
                        "and place the Saddle in the first slot. Then right-click " +
                        "with an empty hand to mount."
                ))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(new ItemStack(Items.SADDLE),
                        Component.literal("Saddle \u2014 required in slot 1 for riding")))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Sniffer Inventory"))
                .element(GuideElement.text(
                        "The Sniffer has 2 equipment slots accessible via " +
                        "Shift + Right Click:"
                ))
                .element(GuideElement.text(
                        "\u2022 Slot 1: Saddle \u2014 enables riding\n" +
                        "\u2022 Slot 2: Sniffer Nose \u2014 enhances abilities"
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.tip(
                        "Riding controls: standard movement keys. The Sniffer moves " +
                        "at 80% of its normal speed, with reduced strafe and reverse."
                ))
                .build();
    }

    // ── Sniffer: Dimensional Tour ───────────────────────────────────

    private static GuidePage buildDimensionalTourPage() {
        ItemStack overworldScent = scentItem("overworld_scent");
        ItemStack netherScent = scentItem("nether_scent");
        ItemStack endScent = scentItem("end_scent");

        return GuidePage.builder("dimensional_tour", "Dimensional Tour")
                .icon(GuideIcon.ofItem(new ItemStack(Items.NETHER_STAR)))
                .element(GuideElement.header("Dimensional Tour"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.text(
                        "The Dimensional Tour is the quest to collect all three " +
                        "dimension scents. Take your tamed Sniffer to each " +
                        "dimension \u2014 it will sniff the air and produce a unique " +
                        "scent item the first time it visits each realm."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("How It Works"))
                .element(GuideElement.text(
                        "When your Sniffer digs in a new dimension, it has an " +
                        "85% chance to drop that dimension's scent instead of " +
                        "its usual seeds. Each scent can only be obtained once " +
                        "per Sniffer."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Dimension Scents"))
                .element(GuideElement.itemShowcase(overworldScent, Component.literal(
                        "Overworld Scent \u2014 obtained when the Sniffer digs in the Overworld")))
                .element(GuideElement.spacer(2))
                .element(GuideElement.itemShowcase(netherScent, Component.literal(
                        "Nether Scent \u2014 obtained when the Sniffer digs in the Nether")))
                .element(GuideElement.spacer(2))
                .element(GuideElement.itemShowcase(endScent, Component.literal(
                        "End Scent \u2014 obtained when the Sniffer digs in the End")))
                .element(GuideElement.spacer(8))
                .element(GuideElement.tip(
                        "Collecting all three scents grants the \"Sniffer Journey\" " +
                        "advancement and triggers a celebration with Totem of " +
                        "Undying particles!"
                ))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "Once you have all three scents, you can craft the " +
                        "Enhanced Sniffer Nose \u2014 the key to the endgame."
                ))
                .element(GuideElement.abilityLink("Enhanced Sniffer Nose", "enhanced_sniffer_nose"))
                .build();
    }

    // ── Sniffer: Enhanced Sniffer Nose ──────────────────────────────

    private static GuidePage buildEnhancedSnifferNosePage() {
        ItemStack enhancedNose = snifferNoseItem("enhanced_sniffer_nose");
        ItemStack ultimateNose = noseItem("ultimate_nose");
        ItemStack overworldScent = scentItem("overworld_scent");
        ItemStack netherScent = scentItem("nether_scent");
        ItemStack endScent = scentItem("end_scent");
        ItemStack diamond = new ItemStack(Items.DIAMOND);

        ItemStack[] recipe = new ItemStack[]{
                ItemStack.EMPTY,  overworldScent, ItemStack.EMPTY,
                netherScent,      ultimateNose,   endScent,
                ItemStack.EMPTY,  diamond,        ItemStack.EMPTY
        };

        return GuidePage.builder("enhanced_sniffer_nose", "Enhanced Sniffer Nose")
                .icon(GuideIcon.ofItem(enhancedNose))
                .element(GuideElement.header("Enhanced Sniffer Nose"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(enhancedNose, Component.literal(
                        "100 Durability  \u2022  Repair: Diamond")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "The culmination of the Dimensional Tour. Crafted from " +
                        "all three dimension scents and a Dragon's Nose, this " +
                        "powerful nose transforms your Sniffer into an endgame " +
                        "resource machine."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Abilities"))
                .element(GuideElement.ability("Scent Base Production \u2014 the Sniffer produces Scent Base, " +
                        "the endgame crafting material"))
                .element(GuideElement.ability("Enhanced Sniffing \u2014 the Sniffer sniffs more frequently " +
                        "(0.5% chance per tick when idle)"))
                .element(GuideElement.ability("Rare Drops \u2014 finds diamonds, emeralds, ores, and other " +
                        "exclusive materials at random"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(scentItem("scent_base"), Component.literal(
                        "Scent Base \u2014 produced by the Sniffer with this nose equipped")))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("How to Equip"))
                .element(GuideElement.text(
                        "Open the Sniffer's inventory (Shift + Right Click) and " +
                        "place the Enhanced Sniffer Nose in the second slot " +
                        "(decoration slot). The Sniffer will immediately start " +
                        "using its enhanced abilities."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(recipe, enhancedNose, "Enhanced Sniffer Nose"))
                .element(GuideElement.spacer(6))
                .element(GuideElement.tip(
                        "The Dragon's Nose is the center piece of this recipe. " +
                        "Make sure you've completed the full Nose progression first!"
                ))
                .element(GuideElement.abilityLink("Dragon's Nose", "ultimate_nose"))
                .build();
    }

    // ── Endgame ─────────────────────────────────────────────────────

    private static GuideCategory buildEndgameCategory() {
        GuideCategory.Builder builder = GuideCategory.builder("endgame", "Endgame")
                .icon(GuideIcon.ofItem(scentItem("scent_container")))
                .accentColor(0xFFD4AF37)
                .page(buildScentContainerIndexPage());

        // Add all 16 scent pages
        for (ScentRecipeData data : ALL_SCENT_RECIPES) {
            builder.page(buildScentPage(data));
        }

        return builder.build();
    }

    // ── Scent Container Index ───────────────────────────────────────

    private static GuidePage buildScentContainerIndexPage() {
        ItemStack container = scentItem("scent_container");
        ItemStack scentBase = scentItem("scent_base");

        ItemStack[] containerRecipe = new ItemStack[]{
                ItemStack.EMPTY,                  new ItemStack(Items.IRON_NUGGET), ItemStack.EMPTY,
                new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.IRON_NUGGET),
                new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.IRON_NUGGET)
        };

        GuidePage.Builder page = GuidePage.builder("scent_container", "Scent Container")
                .icon(GuideIcon.ofItem(container))
                .element(GuideElement.header("Scent Container"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(container, Component.literal(
                        "The vessel for capturing and storing scents")))
                .element(GuideElement.spacer(6))
                .element(GuideElement.text(
                        "The Scent Container is the foundation of the endgame " +
                        "crafting system. Combined with Scent Base and specific " +
                        "ingredients, it becomes one of 16 unique scent capsules " +
                        "that can be used with OVR scent hardware."
                ))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(containerRecipe, container, "Scent Container"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Key Ingredients"))
                .element(GuideElement.itemShowcase(scentBase, Component.literal(
                        "Scent Base \u2014 produced by a Sniffer with Enhanced Nose")))
                .element(GuideElement.spacer(2))
                .element(GuideElement.abilityLink("How to get Scent Base", "enhanced_sniffer_nose"))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("All Scents"));

        // Add links to each scent page
        for (ScentRecipeData data : ALL_SCENT_RECIPES) {
            page.element(GuideElement.abilityLink(data.displayName, data.pageId));
        }

        return page.build();
    }

    // ── Individual Scent Pages ──────────────────────────────────────

    private static GuidePage buildScentPage(ScentRecipeData data) {
        ItemStack result = scentItem(data.scentId);
        ItemStack scentBase = scentItem("scent_base");
        ItemStack container = scentItem("scent_container");

        // All scent recipes follow the same pattern structure
        ItemStack[] recipe = data.buildGrid(scentBase, container);

        return GuidePage.builder(data.pageId, data.displayName)
                .icon(GuideIcon.ofItem(result))
                .element(GuideElement.header(data.displayName))
                .element(GuideElement.spacer(4))
                .element(GuideElement.itemShowcase(result, Component.literal(data.description)))
                .element(GuideElement.spacer(8))
                .element(GuideElement.subheader("Recipe"))
                .element(GuideElement.spacer(4))
                .element(GuideElement.craftingGrid(recipe, result, data.displayName))
                .element(GuideElement.spacer(6))
                .element(GuideElement.abilityLink("Back to Scent Container", "scent_container"))
                .build();
    }

    // ── Scent Recipe Data ───────────────────────────────────────────

    private record ScentRecipeData(
            String scentId, String pageId, String displayName, String description,
            ItemStack cornerA, ItemStack sideB, ItemStack bottomC,
            // Some recipes have E (different bottom corners), null means same as A
            ItemStack bottomCornerE
    ) {
        /**
         * Builds a 3x3 crafting grid from the recipe pattern.
         * Pattern for 5-key recipes: ASA / BDB / ECE
         * Pattern for 4-key recipes: ASA / BDB / ACA (E is same as A)
         */
        ItemStack[] buildGrid(ItemStack scentBase, ItemStack container) {
            ItemStack e = bottomCornerE != null ? bottomCornerE : cornerA;
            return new ItemStack[]{
                    cornerA,   scentBase, cornerA,
                    sideB,     container, sideB,
                    e,         bottomC,   e
            };
        }
    }

    private static final ScentRecipeData[] ALL_SCENT_RECIPES = {
            new ScentRecipeData("winter_scent", "scent_winter", "Winter Scent",
                    "Cool mint, ozone, and soft snowstorm",
                    new ItemStack(Items.LIGHT_BLUE_DYE), new ItemStack(Items.BLUE_ICE),
                    new ItemStack(Items.WHITE_DYE), null),
            new ScentRecipeData("barnyard_scent", "scent_barnyard", "Barnyard Scent",
                    "Warm hay, farm animals, fresh country air",
                    new ItemStack(Items.YELLOW_DYE), new ItemStack(Items.HAY_BLOCK),
                    new ItemStack(Items.BROWN_DYE), null),
            new ScentRecipeData("sweet_scent", "scent_sweet", "Sweet Scent",
                    "Vanilla, cream, and chocolate",
                    new ItemStack(Items.YELLOW_DYE), new ItemStack(Items.HONEY_BLOCK),
                    new ItemStack(Items.ORANGE_DYE), new ItemStack(Items.PINK_DYE)),
            new ScentRecipeData("floral_scent", "scent_floral", "Floral Scent",
                    "A garden of different blooms",
                    new ItemStack(Items.PINK_DYE), new ItemStack(Items.ROSE_BUSH),
                    new ItemStack(Items.RED_DYE), new ItemStack(Items.MAGENTA_DYE)),
            new ScentRecipeData("beach_scent", "scent_beach", "Beach Scent",
                    "Sand, sunscreen, and coconut",
                    new ItemStack(Items.CYAN_DYE), new ItemStack(Items.NAUTILUS_SHELL),
                    new ItemStack(Items.LIGHT_BLUE_DYE), new ItemStack(Items.YELLOW_DYE)),
            new ScentRecipeData("kindred_scent", "scent_kindred", "Kindred Scent",
                    "Baby powder and soothing lavender",
                    new ItemStack(Items.BROWN_DYE), new ItemStack(Items.LEATHER),
                    new ItemStack(Items.RED_DYE), new ItemStack(Items.ORANGE_DYE)),
            new ScentRecipeData("petrichor_scent", "scent_petrichor", "Petrichor Scent",
                    "Rain hitting pavement, drenched streets",
                    new ItemStack(Items.GRAY_DYE), new ItemStack(Items.CLAY),
                    new ItemStack(Items.BROWN_DYE), new ItemStack(Items.BLUE_DYE)),
            new ScentRecipeData("marine_scent", "scent_marine", "Marine Scent",
                    "Ocean spray, seaweed, and driftwood",
                    new ItemStack(Items.CYAN_DYE), new ItemStack(Items.KELP),
                    new ItemStack(Items.BLUE_DYE), null),
            new ScentRecipeData("evergreen_scent", "scent_evergreen", "Evergreen Scent",
                    "Pine trees and lush greenery",
                    new ItemStack(Items.GREEN_DYE), new ItemStack(Items.SPRUCE_LEAVES),
                    new ItemStack(Items.LIME_DYE), new ItemStack(Items.CYAN_DYE)),
            new ScentRecipeData("terra_silva_scent", "scent_terra_silva", "Terra Silva Scent",
                    "Fresh dirt, wet moss, cold forest rivers",
                    new ItemStack(Items.GREEN_DYE), new ItemStack(Items.MOSS_BLOCK),
                    new ItemStack(Items.BROWN_DYE), new ItemStack(Items.LIME_DYE)),
            new ScentRecipeData("citrus_scent", "scent_citrus", "Citrus Scent",
                    "Bright, ripe lemon, orange, and grapefruit",
                    new ItemStack(Items.ORANGE_DYE), new ItemStack(Items.MELON_SLICE),
                    new ItemStack(Items.YELLOW_DYE), new ItemStack(Items.LIME_DYE)),
            new ScentRecipeData("desert_scent", "scent_desert", "Desert Scent",
                    "Dry herbal heat and sand",
                    new ItemStack(Items.YELLOW_DYE), new ItemStack(Items.SAND),
                    new ItemStack(Items.ORANGE_DYE), null),
            new ScentRecipeData("savory_spice_scent", "scent_savory_spice", "Savory Spice Scent",
                    "Coriander, ginger, black pepper, paprika",
                    new ItemStack(Items.BROWN_DYE), new ItemStack(Items.DRIED_KELP),
                    new ItemStack(Items.ORANGE_DYE), null),
            new ScentRecipeData("timber_scent", "scent_timber", "Timber Scent",
                    "Sandalwood and cedarwood lumber yard",
                    new ItemStack(Items.BROWN_DYE), new ItemStack(Items.OAK_LOG),
                    new ItemStack(Items.GREEN_DYE), null),
            new ScentRecipeData("smoky_scent", "scent_smoky", "Smoky Scent",
                    "Campfire crackling with creosote",
                    new ItemStack(Items.GRAY_DYE), new ItemStack(Items.CAMPFIRE),
                    new ItemStack(Items.BLACK_DYE), null),
            new ScentRecipeData("machina_scent", "scent_machina", "Machina Scent",
                    "Diesel fuel and industrial machinery",
                    new ItemStack(Items.GRAY_DYE), new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.BLACK_DYE), null),
    };
}
