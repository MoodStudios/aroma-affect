package com.ovrtechnology.sniffernose;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.data.JsonResources;
import lombok.Getter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads sniffer nose definitions from JSON files.
 * 
 * <p>This loader handles parsing the sniffer nose configuration from
 * {@code data/aromaaffect/noses/sniffer_noses.json}.</p>
 */
public class SnifferNoseDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Path to the sniffer noses JSON file
     */
    private static final String SNIFFER_NOSES_RESOURCE_PATH = "data/aromaaffect/noses/sniffer_noses.json";
    
    /**
     * Default texture path for fallback
     */
    private static final String DEFAULT_TEXTURE = "item/sniffer_nose_default";
    
    /**
     * Cached list of loaded sniffer nose definitions
     */
    @Getter
    private static List<SnifferNoseDefinition> loadedSnifferNoses = new ArrayList<>();
    
    /**
     * Set of loaded IDs for duplicate detection
     */
    private static Set<String> loadedIds = new HashSet<>();
    
    /**
     * Load all sniffer nose definitions from the JSON file.
     * 
     * @return An unmodifiable list of valid sniffer nose definitions
     */
    public static List<SnifferNoseDefinition> loadAllSnifferNoses() {
        return loadAllSnifferNoses(ClasspathDataSource.INSTANCE);
    }

    public static List<SnifferNoseDefinition> loadAllSnifferNoses(DataSource dataSource) {
        loadedSnifferNoses.clear();
        loadedIds.clear();

        try {
            SnifferNoseDefinition[] snifferNoses = loadSnifferNosesFromResource(dataSource, SNIFFER_NOSES_RESOURCE_PATH);
            if (snifferNoses != null) {
                for (SnifferNoseDefinition snifferNose : snifferNoses) {
                    processSnifferNose(snifferNose);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load sniffer nose definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} sniffer nose definitions", loadedSnifferNoses.size());
        return Collections.unmodifiableList(loadedSnifferNoses);
    }

    /**
     * See {@code NoseDefinitionLoader#reloadInPlace} for the full rationale.
     * In-place mutation so pre-registered {@code SnifferNoseItem} instances
     * pick up updated data; baked Item properties (stack size, rarity, etc.)
     * stay fixed at mod-init values.
     */
    public static void reloadInPlace(DataSource dataSource) {
        SnifferNoseDefinition[] newDefs = loadSnifferNosesFromResource(dataSource, SNIFFER_NOSES_RESOURCE_PATH);
        int mutated = 0;
        int skipped = 0;
        for (SnifferNoseDefinition src : newDefs) {
            if (src == null || !src.isValid()) continue;
            SnifferNoseDefinition target = findLoadedById(src.getId());
            if (target == null) {
                AromaAffect.LOGGER.warn(
                        "[SnifferNose reload] ID '{}' is not a built-in; custom sniffer noses must use the variant system.",
                        src.getId());
                skipped++;
                continue;
            }
            copyFields(src, target);
            mutated++;
        }
        AromaAffect.LOGGER.info("SnifferNose reload: mutated {} definitions in place, skipped {} unknown IDs",
                mutated, skipped);
    }

    private static SnifferNoseDefinition findLoadedById(String id) {
        for (SnifferNoseDefinition d : loadedSnifferNoses) {
            if (id.equals(d.getId())) return d;
        }
        return null;
    }

    private static void copyFields(SnifferNoseDefinition src, SnifferNoseDefinition dst) {
        dst.setImage(src.getImage());
        dst.setModel(src.getModel());
        dst.setTier(src.getTier());
        dst.setDurability(src.getDurability());
        dst.setRepair(src.getRepair());
        dst.setEnabled(src.isEnabled());
    }
    
    /**
     * Process a single sniffer nose definition
     */
    private static void processSnifferNose(SnifferNoseDefinition snifferNose) {
        if (snifferNose == null) {
            AromaAffect.LOGGER.warn("Null sniffer nose definition found, skipping...");
            return;
        }
        
        if (!snifferNose.isValid()) {
            AromaAffect.LOGGER.warn("Invalid sniffer nose definition found (missing id), skipping...");
            return;
        }
        
        String id = snifferNose.getId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(id)) {
            AromaAffect.LOGGER.warn("Duplicate sniffer nose ID '{}' found, skipping...", id);
            return;
        }
        
        // Validate and apply fallbacks
        validateAndApplyFallbacks(snifferNose);
        
        loadedIds.add(id);
        loadedSnifferNoses.add(snifferNose);
        AromaAffect.LOGGER.debug("Loaded sniffer nose definition: {}", id);
    }
    
    /**
     * Validate definition and apply fallbacks for missing values
     */
    private static void validateAndApplyFallbacks(SnifferNoseDefinition snifferNose) {
        String id = snifferNose.getId();
        
        // Check texture
        String texturePath = snifferNose.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaAffect.LOGGER.warn("[{}] No texture defined, using fallback: {}", id, DEFAULT_TEXTURE);
            snifferNose.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaAffect.LOGGER.warn("[{}] Texture '{}' not found, using fallback: {}", id, texturePath, DEFAULT_TEXTURE);
            snifferNose.setImage(DEFAULT_TEXTURE);
        }
        
        // Check model
        if (snifferNose.getModel() == null || snifferNose.getModel().isEmpty()) {
            AromaAffect.LOGGER.info("[{}] No model defined, using default: minecraft:leather_helmet", id);
        }
    }
    
    /**
     * Check if a texture file exists in the resources
     */
    private static boolean textureExists(String texturePath) {
        String fullPath = "assets/aromaaffect/textures/" + texturePath + ".png";
        
        try (InputStream stream = SnifferNoseDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static SnifferNoseDefinition[] loadSnifferNosesFromResource(DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Sniffer noses definitions file not found: {}", resourcePath);
            return new SnifferNoseDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(element, "sniffer_noses", SnifferNoseDefinition[].class, GSON, resourcePath);
    }
    
    /**
     * Get a sniffer nose definition by ID
     */
    public static SnifferNoseDefinition getSnifferNoseById(String id) {
        for (SnifferNoseDefinition snifferNose : loadedSnifferNoses) {
            if (snifferNose.getId().equals(id)) {
                return snifferNose;
            }
        }
        return null;
    }
    
    /**
     * Check if a sniffer nose with the given ID has been loaded
     */
    public static boolean hasSnifferNoseId(String id) {
        return loadedIds.contains(id);
    }
    
    /**
     * Serialize a sniffer nose definition to JSON
     */
    public static String toJson(SnifferNoseDefinition snifferNose) {
        return GSON.toJson(snifferNose);
    }
    
    /**
     * Get the GSON instance for external use
     */
    public static Gson getGson() {
        return GSON;
    }
}
