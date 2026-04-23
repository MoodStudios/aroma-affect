package com.ovrtechnology.block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.scent.ScentRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

public class BlockDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String BLOCKS_DIR = "aroma_blocks";

    @Getter private static List<BlockDefinition> loadedBlocks = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter private static List<String> validationWarnings = new ArrayList<>();

    public static List<BlockDefinition> loadAllBlocks() {
        return loadAllBlocks(ClasspathDataSource.INSTANCE);
    }

    public static List<BlockDefinition> loadAllBlocks(DataSource dataSource) {
        loadedBlocks.clear();
        loadedIds.clear();
        validationWarnings.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(BLOCKS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                BlockDefinition block = GSON.fromJson(entry.getValue(), BlockDefinition.class);
                processBlock(block);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse block {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info(
                "Loaded {} block definitions from {} file(s)", loadedBlocks.size(), files.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Block loading completed with {} validation warnings",
                    validationWarnings.size());
        }

        return Collections.unmodifiableList(loadedBlocks);
    }

    private static void processBlock(BlockDefinition block) {
        if (block == null) {
            addWarning("Null block definition found, skipping...");
            return;
        }

        if (!block.isValid()) {
            addWarning("Invalid block definition found (missing block_id), skipping...");
            return;
        }

        String blockId = block.getBlockId();

        if (loadedIds.contains(blockId)) {
            addWarning("Duplicate block_id '" + blockId + "' found, skipping...");
            return;
        }

        validateBlock(block);

        loadedIds.add(blockId);
        loadedBlocks.add(block);
        AromaAffect.LOGGER.debug(
                "Loaded block definition: {} (color: {}, scent: {})",
                blockId,
                block.getColorHtml(),
                block.getScentId());
    }

    private static void validateBlock(BlockDefinition block) {
        String blockId = block.getBlockId();

        String rawColor = block.getRawColorHtml();
        if (rawColor == null || rawColor.isEmpty()) {
            addWarning(
                    "["
                            + blockId
                            + "] No color_html defined, using default: "
                            + BlockDefinition.DEFAULT_COLOR);
        } else if (!BlockDefinition.isValidHtmlColor(rawColor)) {
            addWarning(
                    "["
                            + blockId
                            + "] Invalid color_html format '"
                            + rawColor
                            + "', using default: "
                            + BlockDefinition.DEFAULT_COLOR);
        }

        if (block.hasScentId()) {
            String scentId = block.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning(
                        "["
                                + blockId
                                + "] Referenced scent_id '"
                                + scentId
                                + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning(
                    "[" + blockId + "] No scent_id defined, block will have no associated scent");
        }

        if (!blockId.contains(":")) {
            addWarning(
                    "["
                            + blockId
                            + "] Block ID should include namespace (e.g., 'minecraft:stone')");
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    public static BlockDefinition getBlockById(String blockId) {
        for (BlockDefinition block : loadedBlocks) {
            if (block.getBlockId().equals(blockId)) {
                return block;
            }
        }
        return null;
    }

    public static String toJson(BlockDefinition block) {
        return GSON.toJson(block);
    }

    public static Gson getGson() {
        return GSON;
    }

    public static List<BlockDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading block definitions...");
        return loadAllBlocks();
    }

    public static boolean hasValidationWarnings() {
        return !validationWarnings.isEmpty();
    }
}
