package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseRegistry;
import net.minecraft.network.chat.Component;
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
                .build();
    }

    // ── Welcome / Getting Started ──────────────────────────────────

    private static ItemStack getNoseItemStack() {
        return NoseRegistry.getNose("basic_nose")
                .map(nose -> new ItemStack(nose))
                .orElse(new ItemStack(Items.IRON_HELMET));
    }

    private static GuideCategory buildWelcomeCategory() {
        return GuideCategory.builder("welcome", "Getting Started")
                .icon(GuideIcon.ofItem(getNoseItemStack()))
                .accentColor(0xFF6D5EF8)
                .page(buildWelcomePage())
                .build();
    }

    private static GuidePage buildWelcomePage() {
        ItemStack guideItem = new ItemStack(AromaGuideRegistry.getAROMA_GUIDE().get());

        // Aroma Guide crafting recipe: Compass + Paper (shapeless)
        ItemStack[] guideRecipe = new ItemStack[]{
                ItemStack.EMPTY,              ItemStack.EMPTY,            ItemStack.EMPTY,
                new ItemStack(Items.COMPASS), new ItemStack(Items.PAPER), ItemStack.EMPTY,
                ItemStack.EMPTY,              ItemStack.EMPTY,            ItemStack.EMPTY
        };

        return GuidePage.builder("welcome", "Welcome")
                .icon(GuideIcon.ofItem(getNoseItemStack()))
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
                .build();
    }
}
