package com.ovrtechnology.guide;

import com.google.gson.*;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads guide content from JSON resource files and converts them into
 * {@link GuideCategory} / {@link GuidePage} / {@link GuideElement} objects.
 *
 * <p>Text fields in JSON support two modes:</p>
 * <ul>
 *   <li>{@code "translate": "key"} → {@link Component#translatable(String)}</li>
 *   <li>{@code "text": "literal"} → {@link Component#literal(String)}</li>
 * </ul>
 * If both are present, {@code translate} takes priority.
 */
public final class GuideContentLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GuideContentLoader() {
    }

    /**
     * Loads a guide category from a JSON resource on the classpath.
     *
     * @param resourcePath path like {@code "data/aromaaffect/guide/noses.json"}
     * @return the parsed category, or {@code null} on failure
     */
    @Nullable
    public static GuideCategory loadCategory(String resourcePath) {
        try (InputStream is = GuideContentLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                AromaAffect.LOGGER.error("[Guide] Resource not found: {}", resourcePath);
                return null;
            }
            JsonObject root = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            return parseCategory(root);
        } catch (Exception e) {
            AromaAffect.LOGGER.error("[Guide] Failed to load category from {}: {}", resourcePath, e.getMessage());
            return null;
        }
    }

    // ── Category ────────────────────────────────────────────────────

    private static GuideCategory parseCategory(JsonObject json) {
        String id = json.get("id").getAsString();
        Component title = resolveText(json);

        GuideCategory.Builder builder = GuideCategory.builder(id, title);

        if (json.has("icon")) {
            builder.icon(parseIcon(json.getAsJsonObject("icon")));
        }
        if (json.has("accent_color")) {
            builder.accentColor(parseColor(json.get("accent_color").getAsString()));
        }
        if (json.has("pages")) {
            for (JsonElement pageEl : json.getAsJsonArray("pages")) {
                builder.page(parsePage(pageEl.getAsJsonObject()));
            }
        }
        return builder.build();
    }

    // ── Page ────────────────────────────────────────────────────────

    private static GuidePage parsePage(JsonObject json) {
        String id = json.get("id").getAsString();
        Component title = resolveText(json);

        GuidePage.Builder builder = GuidePage.builder(id, title);

        if (json.has("icon")) {
            builder.icon(parseIcon(json.getAsJsonObject("icon")));
        }
        if (json.has("elements")) {
            for (JsonElement elemEl : json.getAsJsonArray("elements")) {
                GuideElement element = parseElement(elemEl.getAsJsonObject());
                if (element != null) {
                    builder.element(element);
                }
            }
        }
        return builder.build();
    }

    // ── Element ─────────────────────────────────────────────────────

    @Nullable
    private static GuideElement parseElement(JsonObject json) {
        String type = json.get("type").getAsString();

        switch (type) {
            case "header":
                return GuideElement.header(resolveText(json));
            case "subheader":
                return GuideElement.subheader(resolveText(json));
            case "text": {
                if (json.has("color")) {
                    int color = parseColor(json.get("color").getAsString());
                    return GuideElement.coloredText(resolveText(json), color);
                }
                return GuideElement.text(resolveText(json));
            }
            case "spacer":
                return GuideElement.spacer(json.get("height").getAsInt());
            case "separator":
                return GuideElement.separator();
            case "tip":
                return GuideElement.tip(resolveText(json));
            case "item_showcase":
                return GuideElement.itemShowcase(resolveItem(json.get("item").getAsString()), resolveText(json));
            case "icon_text":
                return parseIconTextElement(json);
            case "detection_label":
                return GuideElement.detectionLabel(resolveText(json));
            case "ability":
                return GuideElement.ability(resolveText(json));
            case "ability_link":
                return GuideElement.abilityLink(resolveText(json), json.get("target_page").getAsString());
            case "crafting_grid":
                return parseCraftingGrid(json);
            case "image":
                return GuideElement.image(
                        Identifier.parse(json.get("texture").getAsString()),
                        json.get("width").getAsInt(),
                        json.get("height").getAsInt());
            case "url_link":
                return GuideElement.urlLink(resolveText(json), json.get("url").getAsString());
            default:
                AromaAffect.LOGGER.warn("[Guide] Unknown element type: {}", type);
                return null;
        }
    }

    private static GuideElement parseIconTextElement(JsonObject json) {
        Component text = resolveText(json);

        if (json.has("items")) {
            JsonArray itemsArr = json.getAsJsonArray("items");
            ItemStack[] icons = new ItemStack[itemsArr.size()];
            for (int i = 0; i < itemsArr.size(); i++) {
                icons[i] = resolveItem(itemsArr.get(i).getAsString());
            }
            if (icons.length == 1) {
                return GuideElement.iconText(icons[0], text);
            }
            return GuideElement.iconText(text, icons);
        }
        // Fallback: single item field
        if (json.has("item")) {
            return GuideElement.iconText(resolveItem(json.get("item").getAsString()), text);
        }
        AromaAffect.LOGGER.warn("[Guide] icon_text element missing 'items' or 'item' field");
        return GuideElement.text(text);
    }

    private static GuideElement parseCraftingGrid(JsonObject json) {
        JsonArray gridArr = json.getAsJsonArray("grid");
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            String itemId = gridArr.get(i).getAsString();
            grid[i] = resolveItem(itemId);
        }
        ItemStack result = resolveItem(json.get("result").getAsString());
        Component label = resolveText(json);
        return GuideElement.craftingGrid(grid, result, label);
    }

    // ── Icon ────────────────────────────────────────────────────────

    private static GuideIcon parseIcon(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "item":
                return GuideIcon.ofItem(resolveItem(json.get("item").getAsString()));
            case "texture":
                return GuideIcon.ofTexture(
                        Identifier.parse(json.get("texture").getAsString()),
                        json.get("width").getAsInt(),
                        json.get("height").getAsInt());
            case "symbol":
                return GuideIcon.ofSymbol(
                        json.get("symbol").getAsString(),
                        parseColor(json.get("color").getAsString()));
            default:
                AromaAffect.LOGGER.warn("[Guide] Unknown icon type: {}", type);
                return GuideIcon.ofItem(ItemStack.EMPTY);
        }
    }

    // ── Item Resolution ─────────────────────────────────────────────

    /**
     * Resolves a Minecraft item ID string to an ItemStack.
     * Returns {@link ItemStack#EMPTY} for empty strings or {@code minecraft:air}.
     */
    static ItemStack resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty() || itemId.equals("minecraft:air")) {
            return ItemStack.EMPTY;
        }
        Identifier loc = Identifier.parse(itemId);
        return BuiltInRegistries.ITEM.getOptional(loc)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    // ── Text Resolution ─────────────────────────────────────────────

    /**
     * Resolves text from a JSON object.
     * Checks for {@code "translate"} key first (i18n), then {@code "text"} (literal),
     * then {@code "title"} (category/page titles).
     */
    static Component resolveText(JsonObject json) {
        if (json.has("translate")) {
            return Component.translatable(json.get("translate").getAsString());
        }
        if (json.has("text")) {
            return Component.literal(json.get("text").getAsString());
        }
        if (json.has("title")) {
            // title field might also be a text object
            JsonElement titleEl = json.get("title");
            if (titleEl.isJsonObject()) {
                return resolveText(titleEl.getAsJsonObject());
            }
            return Component.literal(titleEl.getAsString());
        }
        return Component.empty();
    }

    // ── Color Parsing ───────────────────────────────────────────────

    /**
     * Parses a hex color string like {@code "#FFE8A838"} to an int.
     */
    private static int parseColor(String hex) {
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        return (int) Long.parseLong(cleaned, 16);
    }
}
