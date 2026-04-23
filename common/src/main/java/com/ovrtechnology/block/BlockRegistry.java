package com.ovrtechnology.block;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentRegistry;
import java.util.*;
import lombok.Getter;

public final class BlockRegistry {

    @Getter
    private static final Map<String, BlockDefinition> blockDefinitions = new LinkedHashMap<>();

    @Getter private static boolean initialized = false;

    private BlockRegistry() {
        throw new UnsupportedOperationException("BlockRegistry is a static utility class");
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("BlockRegistry.init() called multiple times!");
            return;
        }

        if (!ScentRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn(
                    "BlockRegistry.init() called before ScentRegistry! Scent validation may fail.");
        }

        AromaAffect.LOGGER.info("Initializing BlockRegistry...");

        List<BlockDefinition> definitions = BlockDefinitionLoader.loadAllBlocks();

        for (BlockDefinition definition : definitions) {
            registerBlock(definition);
        }

        initialized = true;
        AromaAffect.LOGGER.info(
                "BlockRegistry initialized with {} blocks", blockDefinitions.size());
    }

    private static void registerBlock(BlockDefinition definition) {
        String blockId = definition.getBlockId();

        if (blockDefinitions.containsKey(blockId)) {
            AromaAffect.LOGGER.warn("Duplicate block ID in registry: {}, skipping...", blockId);
            return;
        }

        blockDefinitions.put(blockId, definition);
        AromaAffect.LOGGER.debug("Registered block: {}", blockId);
    }

    public static Optional<BlockDefinition> getBlock(String blockId) {
        return Optional.ofNullable(blockDefinitions.get(blockId));
    }

    public static boolean hasBlock(String blockId) {
        return blockDefinitions.containsKey(blockId);
    }

    public static Iterable<String> getAllBlockIds() {
        return Collections.unmodifiableSet(blockDefinitions.keySet());
    }

    public static Iterable<BlockDefinition> getAllBlocks() {
        return Collections.unmodifiableCollection(blockDefinitions.values());
    }

    public static List<BlockDefinition> getAllBlocksAsList() {
        return new ArrayList<>(blockDefinitions.values());
    }

    public static List<String> validateBlockIds(List<String> blockIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : blockIds) {
            if (!hasBlock(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }

    public static void reload() {
        AromaAffect.LOGGER.info("Reloading BlockRegistry...");
        blockDefinitions.clear();

        List<BlockDefinition> definitions = BlockDefinitionLoader.reload();
        for (BlockDefinition definition : definitions) {
            registerBlock(definition);
        }

        AromaAffect.LOGGER.info("BlockRegistry reloaded with {} blocks", blockDefinitions.size());
    }

    static void clear() {
        blockDefinitions.clear();
        initialized = false;
    }
}
