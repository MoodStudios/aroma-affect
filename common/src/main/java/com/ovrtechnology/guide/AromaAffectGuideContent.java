package com.ovrtechnology.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.util.Colors;
import com.ovrtechnology.variant.CustomNoseRegistry;
import com.ovrtechnology.variant.NoseVariant;
import com.ovrtechnology.variant.NoseVariantRegistry;
import com.ovrtechnology.variant.VariantRecipeIndex;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AromaAffectGuideContent {

    private static GuideBook cachedBook;

    private AromaAffectGuideContent() {}

    public static GuideBook getBook() {
        if (cachedBook == null) {
            cachedBook = buildBook();
        }
        return cachedBook;
    }

    public static void invalidate() {
        cachedBook = null;
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

    private static ItemStack noseItem(String id) {
        return NoseRegistry.getNose(id)
                .map(ItemStack::new)
                .orElse(new ItemStack(Items.IRON_HELMET));
    }

    private static GuideCategory buildWelcomeCategory() {
        GuideCategory loaded =
                GuideContentLoader.loadCategory("data/aromaaffect/guide/getting_started.json");
        if (loaded != null) {
            return loaded;
        }
        AromaAffect.LOGGER.warn(
                "[Guide] Getting Started JSON failed to load, using empty fallback");
        return GuideCategory.builder("welcome", "Getting Started")
                .accentColor(Colors.ACCENT_PURPLE)
                .build();
    }

    private static GuideCategory buildNosesCategory() {
        GuideCategory loaded = GuideContentLoader.loadCategory("data/aromaaffect/guide/noses.json");
        if (loaded == null) {
            AromaAffect.LOGGER.warn("[Guide] Noses JSON failed to load, using empty fallback");
            return GuideCategory.builder("noses", "Noses")
                    .icon(GuideIcon.ofItem(noseItem("foragers_nose")))
                    .accentColor(Colors.ACCENT_AMBER)
                    .build();
        }
        return mergeAndSortNosePages(loaded);
    }

    private static GuideCategory mergeAndSortNosePages(GuideCategory source) {
        record Entry(int tier, GuidePage page) {}
        List<Entry> entries = new ArrayList<>();

        for (GuidePage page : source.getPages()) {
            var def = NoseRegistry.getDefinition(page.getId());
            if (def.isPresent() && !def.get().isEnabled()) {
                continue;
            }
            int tier = def.map(d -> d.getTier()).orElse(Integer.MAX_VALUE);
            entries.add(new Entry(tier, page));
        }

        for (Map.Entry<ResourceLocation, NoseVariant> entry :
                NoseVariantRegistry.all().entrySet()) {
            GuidePage page = buildVariantPage(entry.getKey(), entry.getValue());
            if (page != null) {
                entries.add(new Entry(entry.getValue().getTier(), page));
            }
        }

        entries.sort(Comparator.comparingInt(Entry::tier));

        GuideCategory.Builder builder =
                GuideCategory.builder(source.getId(), source.getTitle())
                        .accentColor(source.getAccentColor());
        if (source.getIcon() != null) {
            builder.icon(source.getIcon());
        }
        for (Entry e : entries) {
            builder.page(e.page());
        }
        return builder.build();
    }

    private static GuidePage buildVariantPage(ResourceLocation variantId, NoseVariant variant) {
        Item customNoseItem = CustomNoseRegistry.getCUSTOM_NOSE().get();
        ItemStack icon =
                com.ovrtechnology.variant.CustomNoseItem.stackFor(
                        customNoseItem, variantId, variant);
        String name = variant.getDisplayName();
        GuideIcon pageIcon = GuideIcon.ofItem(icon);

        JsonElement curated = variant.getGuidePage();
        if (curated != null && curated.isJsonObject()) {
            try {
                JsonObject copy = curated.deepCopy().getAsJsonObject();
                if (!copy.has("id")) {
                    copy.addProperty("id", variantId.toString());
                }
                if (!copy.has("title") && !copy.has("translate") && !copy.has("text")) {
                    copy.addProperty("text", name);
                }
                GuidePage parsed = GuideContentLoader.parsePage(copy);
                return rebuildWithIcon(parsed, pageIcon);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "[Guide] Failed to parse guide_page for variant {}: {}",
                        variantId,
                        e.getMessage());
            }
        }

        GuidePage.Builder builder =
                GuidePage.builder(variantId.toString(), Component.literal(name)).icon(pageIcon);

        builder.element(GuideElement.header(Component.literal(name)));
        builder.element(GuideElement.spacer(4));

        Component statsLine =
                Component.literal(
                        "Tier "
                                + variant.getTier()
                                + "  •  "
                                + variant.getDurability()
                                + " Durability");
        Component repairComponent = resolveRepairComponent(variant.getRepair());
        if (repairComponent != null) {
            statsLine =
                    Component.literal(
                                    "Tier "
                                            + variant.getTier()
                                            + "  •  "
                                            + variant.getDurability()
                                            + " Durability  •  Repair: ")
                            .append(repairComponent);
        }
        builder.element(GuideElement.itemShowcase(icon, statsLine));
        builder.element(GuideElement.spacer(6));

        String desc = variant.getDescription();
        if (desc != null && !desc.isEmpty()) {
            builder.element(GuideElement.text(Component.literal(desc)));
            builder.element(GuideElement.spacer(8));
        }

        var unlock = variant.getUnlock();

        if (!unlock.getAbilities().isEmpty()) {
            builder.element(GuideElement.subheader(Component.literal("Abilities")));
            for (String abilityId : unlock.getAbilities()) {
                String display = humanize(abilityId);
                String description =
                        AbilityDefinitionLoader.getAbility(abilityId)
                                .map(d -> d.getDescription())
                                .orElse(null);
                String label =
                        description != null && !description.isEmpty()
                                ? display + " — " + description
                                : display;
                builder.element(GuideElement.ability(label));
            }
            builder.element(GuideElement.spacer(8));
        }

        boolean hasDetects =
                !unlock.getBlocks().isEmpty()
                        || !unlock.getBiomes().isEmpty()
                        || !unlock.getStructures().isEmpty()
                        || !unlock.getFlowers().isEmpty();
        if (hasDetects) {
            builder.element(GuideElement.subheader(Component.literal("New Detections")));
            appendDetectSection(builder, "Blocks", unlock.getBlocks(), true);
            appendDetectSection(builder, "Biomes", unlock.getBiomes(), false);
            appendDetectSection(builder, "Structures", unlock.getStructures(), false);
            appendDetectSection(builder, "Flowers", unlock.getFlowers(), true);
        }

        if (!unlock.getNoses().isEmpty()) {
            builder.element(GuideElement.spacer(6));
            List<String> parentNames = new ArrayList<>();
            for (String nid : unlock.getNoses()) {
                parentNames.add(humanize(nid));
            }
            builder.element(
                    GuideElement.text(
                            Component.literal(
                                    "Inherits abilities from: " + String.join(", ", parentNames))));
        }

        VariantRecipeIndex.get(variantId)
                .ifPresent(
                        entry -> {
                            builder.element(GuideElement.spacer(8));
                            builder.element(GuideElement.subheader(Component.literal("Recipe")));
                            builder.element(GuideElement.spacer(4));
                            ItemStack[] gridStacks = new ItemStack[9];
                            for (int i = 0; i < 9; i++) {
                                String id = entry.grid()[i];
                                if (id == null || id.isEmpty()) {
                                    gridStacks[i] = ItemStack.EMPTY;
                                } else {
                                    ResourceLocation loc = ResourceLocation.tryParse(id);
                                    gridStacks[i] =
                                            loc == null
                                                    ? ItemStack.EMPTY
                                                    : BuiltInRegistries.ITEM
                                                            .getOptional(loc)
                                                            .map(ItemStack::new)
                                                            .orElse(ItemStack.EMPTY);
                                }
                            }
                            builder.element(
                                    GuideElement.craftingGrid(
                                            gridStacks, icon, Component.literal(name)));
                        });

        return builder.build();
    }

    private static GuidePage rebuildWithIcon(GuidePage source, GuideIcon icon) {
        GuidePage.Builder b = GuidePage.builder(source.getId(), source.getTitle());
        b.icon(icon);
        for (GuideElement el : source.getElements()) {
            b.element(el);
        }
        return b.build();
    }

    private static Component resolveRepairComponent(String repairId) {
        if (repairId == null || repairId.isEmpty()) return null;
        ResourceLocation loc = ResourceLocation.tryParse(repairId);
        if (loc == null) return Component.literal(humanize(repairId));
        return BuiltInRegistries.ITEM
                .getOptional(loc)
                .map(item -> new ItemStack(item).getHoverName())
                .orElseGet(() -> Component.literal(humanize(repairId)));
    }

    private static void appendDetectSection(
            GuidePage.Builder builder, String label, List<String> ids, boolean hasItems) {
        if (ids == null || ids.isEmpty()) return;
        builder.element(GuideElement.detectionLabel(Component.literal(label)));
        for (String id : ids) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (hasItems && loc != null) {
                ItemStack stack =
                        BuiltInRegistries.ITEM
                                .getOptional(loc)
                                .map(ItemStack::new)
                                .orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    builder.element(GuideElement.iconText(stack, stack.getHoverName()));
                    continue;
                }
            }
            builder.element(GuideElement.text(Component.literal("  " + humanize(id))));
        }
        builder.element(GuideElement.spacer(4));
    }

    private static String humanize(String id) {
        if (id == null || id.isEmpty()) return "";
        String path = id;
        int colon = id.indexOf(':');
        if (colon >= 0) path = id.substring(colon + 1);
        StringBuilder sb = new StringBuilder(path.length());
        boolean cap = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '/') {
                sb.append(' ');
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static GuideCategory buildSnifferCategory() {
        GuideCategory loaded =
                GuideContentLoader.loadCategory("data/aromaaffect/guide/sniffer.json");
        if (loaded != null) {
            return loaded;
        }
        AromaAffect.LOGGER.warn("[Guide] Sniffer JSON failed to load, using empty fallback");
        return GuideCategory.builder("sniffer", "Sniffer").accentColor(Colors.ACCENT_TEAL).build();
    }

    private static GuideCategory buildEndgameCategory() {
        GuideCategory loaded =
                GuideContentLoader.loadCategory("data/aromaaffect/guide/endgame.json");
        if (loaded != null) {
            return loaded;
        }
        AromaAffect.LOGGER.warn("[Guide] Endgame JSON failed to load, using empty fallback");
        return GuideCategory.builder("endgame", "Endgame").accentColor(Colors.ACCENT_GOLD).build();
    }
}
