package com.ovrtechnology.variant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.util.Ids;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class VariantRecipeIndex {

    public record Entry(String[] grid, String resultItemId) {}

    private static final Map<ResourceLocation, Entry> BY_VARIANT = new HashMap<>();

    private VariantRecipeIndex() {}

    public static Optional<Entry> get(ResourceLocation variantId) {
        return Optional.ofNullable(BY_VARIANT.get(variantId));
    }

    public static void reload(DataSource source) {
        BY_VARIANT.clear();
        Map<ResourceLocation, JsonElement> recipes = source.listJson("recipe");
        for (Map.Entry<ResourceLocation, JsonElement> entry : recipes.entrySet()) {
            try {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject obj = entry.getValue().getAsJsonObject();

                String type = obj.has("type") ? obj.get("type").getAsString() : "";
                if (!"minecraft:crafting_shaped".equals(type)) continue;

                if (!obj.has("result") || !obj.get("result").isJsonObject()) continue;
                JsonObject result = obj.getAsJsonObject("result");

                String resultId = result.has("id") ? result.get("id").getAsString() : "";
                if (!"aromaaffect:custom_nose".equals(resultId)) continue;

                if (!result.has("components") || !result.get("components").isJsonObject()) continue;
                JsonObject components = result.getAsJsonObject("components");

                if (!components.has("aromaaffect:nose_variant")) continue;
                String variantStr = components.get("aromaaffect:nose_variant").getAsString();
                ResourceLocation variantId = Ids.parse(variantStr);
                if (variantId == null) continue;

                String[] grid = buildGrid(obj);
                if (grid == null) continue;

                BY_VARIANT.put(variantId, new Entry(grid, resultId));
            } catch (Exception e) {
                AromaAffect.LOGGER.debug(
                        "Skipped recipe {} for variant index: {}", entry.getKey(), e.getMessage());
            }
        }
        AromaAffect.LOGGER.info("VariantRecipeIndex loaded {} variant recipes", BY_VARIANT.size());
    }

    private static String[] buildGrid(JsonObject recipe) {
        if (!recipe.has("pattern") || !recipe.get("pattern").isJsonArray()) return null;
        if (!recipe.has("key") || !recipe.get("key").isJsonObject()) return null;

        JsonArray patternArr = recipe.getAsJsonArray("pattern");
        JsonObject keyObj = recipe.getAsJsonObject("key");

        int rows = patternArr.size();
        if (rows == 0 || rows > 3) return null;
        int cols = 0;
        for (int r = 0; r < rows; r++) {
            cols = Math.max(cols, patternArr.get(r).getAsString().length());
        }
        if (cols == 0 || cols > 3) return null;

        String[] grid = new String[9];
        for (int r = 0; r < rows; r++) {
            String row = patternArr.get(r).getAsString();
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch == ' ') continue;
                String key = String.valueOf(ch);
                if (!keyObj.has(key)) continue;
                String itemId = resolveIngredient(keyObj.get(key));
                if (itemId == null) continue;
                grid[r * 3 + c] = itemId;
            }
        }
        return grid;
    }

    private static String resolveIngredient(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) {
            String s = el.getAsString();
            if (s.startsWith("#")) return null;
            return s;
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("item")) return obj.get("item").getAsString();
            return null;
        }
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (JsonElement sub : arr) {
                String resolved = resolveIngredient(sub);
                if (resolved != null) return resolved;
            }
        }
        return null;
    }
}
