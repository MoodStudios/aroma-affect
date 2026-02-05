package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds the Aroma Affect guide content.
 * All categories are loaded from JSON via {@link GuideContentLoader}.
 */
public final class AromaAffectGuideContent {

    private static GuideBook cachedBook;

    private AromaAffectGuideContent() {
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

    // ── Welcome / Getting Started ──────────────────────────────────

    private static GuideCategory buildWelcomeCategory() {
        GuideCategory loaded = GuideContentLoader.loadCategory("data/aromaaffect/guide/getting_started.json");
        if (loaded != null) {
            return loaded;
        }
        AromaAffect.LOGGER.warn("[Guide] Getting Started JSON failed to load, using empty fallback");
        return GuideCategory.builder("welcome", "Getting Started")
                .accentColor(0xFF6D5EF8)
                .build();
    }

    // ── Noses ───────────────────────────────────────────────────────

    private static GuideCategory buildNosesCategory() {
        GuideCategory loaded = GuideContentLoader.loadCategory("data/aromaaffect/guide/noses.json");
        if (loaded != null) {
            return loaded;
        }
        // Hardcoded fallback if JSON loading fails
        AromaAffect.LOGGER.warn("[Guide] Noses JSON failed to load, using empty fallback");
        return GuideCategory.builder("noses", "Noses")
                .icon(GuideIcon.ofItem(noseItem("basic_nose")))
                .accentColor(0xFFE8A838)
                .build();
    }

    // ── Sniffer ─────────────────────────────────────────────────────

    private static GuideCategory buildSnifferCategory() {
        GuideCategory loaded = GuideContentLoader.loadCategory("data/aromaaffect/guide/sniffer.json");
        if (loaded != null) {
            return loaded;
        }
        AromaAffect.LOGGER.warn("[Guide] Sniffer JSON failed to load, using empty fallback");
        return GuideCategory.builder("sniffer", "Sniffer")
                .accentColor(0xFF4EC9B0)
                .build();
    }

    // ── Endgame ─────────────────────────────────────────────────────

    private static GuideCategory buildEndgameCategory() {
        GuideCategory loaded = GuideContentLoader.loadCategory("data/aromaaffect/guide/endgame.json");
        if (loaded != null) {
            return loaded;
        }
        AromaAffect.LOGGER.warn("[Guide] Endgame JSON failed to load, using empty fallback");
        return GuideCategory.builder("endgame", "Endgame")
                .accentColor(0xFFD4AF37)
                .build();
    }
}
