package com.ovrtechnology.category;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import lombok.Getter;
import net.minecraft.resources.Identifier;

import java.util.*;

public class CategoryDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    public static final String CATEGORY_DIR = "aroma/category";
    

    @Getter
    private static List<CategoryDefinition> loadedCategories = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter
    private static List<String> validationWarnings = new ArrayList<>();

    public static List<CategoryDefinition> loadAllCategories() {
        return loadAllCategories(ClasspathDataSource.INSTANCE);
    }

    public static List<CategoryDefinition> loadAllCategories(DataSource dataSource) {
        loadedCategories.clear();
        loadedIds.clear();
        validationWarnings.clear();

        Map<Identifier, JsonElement> files = dataSource.listJson(CATEGORY_DIR);
        for (Map.Entry<Identifier, JsonElement> entry : files.entrySet()) {
            try {
                CategoryDefinition cat = GSON.fromJson(entry.getValue(), CategoryDefinition.class);
                processCategory(cat);
            } catch (Exception e) {
                AromaAffect.LOGGER.error("Failed to parse category {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Loaded {} category definitions from {} file(s)", loadedCategories.size(), files.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn("Category loading completed with {} validation warnings", validationWarnings.size());
        }

        return Collections.unmodifiableList(loadedCategories);
    }
    

    private static void processCategory(CategoryDefinition cat) {
        if (cat == null) {
            addWarning("Null category definition found, skipping...");
            return;
        }
        
        if (!cat.isValid()) {
            addWarning("Invalid category definition found (missing id), skipping...");
            return;
        }
        
        String catID = cat.getId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(catID)) {
            addWarning("Duplicate id '" + catID + "' found, skipping...");
            return;
        }
        
        // Validate the block entry
        validateCategory(cat);
        
        loadedIds.add(catID);
        loadedCategories.add(cat);
    }

    private static void validateCategory(CategoryDefinition cat) {
        String blockId = cat.getId();
        
        // Validate HTML color format
        String rawColor = cat.getRawColorHtml();
        if (rawColor == null || rawColor.isEmpty()) {
            addWarning("[" + blockId + "] No color_html defined, using default: " + CategoryDefinition.getColor(cat));
        } else if (!CategoryDefinition.isValidHtmlColor(rawColor)) {
            addWarning("[" + blockId + "] Invalid color_html format '" + rawColor + "', using default: " + CategoryDefinition.getColor(cat));
        }
        
        // Validate block_id format (should be namespace:path)
        if (!blockId.contains(":")) {
            addWarning("[" + blockId + "] Block ID should include namespace (e.g., 'minecraft:stone')");
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    public static CategoryDefinition parseCategoryFromJSON(String json) {
        try {
            return GSON.fromJson(json, CategoryDefinition.class);
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to parse block definition from JSON", e);
            return null;
        }
    }

    public static CategoryDefinition getCategoryFromID(String id) {
        for (CategoryDefinition cat : loadedCategories) {
            if (cat.getId().equals(id)) {
                return cat;
            }
        }
        return null;
    }

    public static boolean hasCategoryID(String id) {
        return loadedIds.contains(id);
    }
    

    public static String toJson(CategoryDefinition cat) {
        return GSON.toJson(cat);
    }
    
    public static Gson getGson() {
        return GSON;
    }

    public static List<CategoryDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading category definitions...");
        return loadAllCategories();
    }


    public static boolean hasValidationWarnings() {
        return !validationWarnings.isEmpty();
    }
}

