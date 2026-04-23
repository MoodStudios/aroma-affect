package com.ovrtechnology.sniffernose;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.*;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public final class SnifferNoseRegistry {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);

    @Getter
    private static final Map<String, RegistrySupplier<SnifferNoseItem>> snifferNoseItems =
            new LinkedHashMap<>();

    @Getter
    private static final Map<String, SnifferNoseDefinition> snifferNoseDefinitions =
            new LinkedHashMap<>();

    @Getter private static boolean initialized = false;

    private SnifferNoseRegistry() {
        throw new UnsupportedOperationException("SnifferNoseRegistry is a static utility class");
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("SnifferNoseRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing SnifferNoseRegistry...");

        List<SnifferNoseDefinition> definitions = SnifferNoseDefinitionLoader.loadAllSnifferNoses();

        for (SnifferNoseDefinition definition : definitions) {
            registerSnifferNose(definition);
        }

        ITEMS.register();

        initialized = true;
        AromaAffect.LOGGER.info(
                "SnifferNoseRegistry initialized with {} sniffer noses", snifferNoseItems.size());
    }

    private static void registerSnifferNose(SnifferNoseDefinition definition) {
        String id = definition.getId();

        if (snifferNoseItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate sniffer nose ID: {}, skipping...", id);
            return;
        }

        snifferNoseDefinitions.put(id, definition);

        final String itemId = id;
        RegistrySupplier<SnifferNoseItem> supplier =
                ITEMS.register(id, () -> new SnifferNoseItem(definition, itemId));
        snifferNoseItems.put(id, supplier);

        AromaAffect.LOGGER.debug("Registered sniffer nose item: {}", id);
    }

    public static Optional<SnifferNoseItem> getSnifferNose(String id) {
        RegistrySupplier<SnifferNoseItem> supplier = snifferNoseItems.get(id);
        if (supplier != null && supplier.isPresent()) {
            return Optional.of(supplier.get());
        }
        return Optional.empty();
    }

    public static Optional<SnifferNoseDefinition> getDefinition(String id) {
        return Optional.ofNullable(snifferNoseDefinitions.get(id));
    }

    public static List<SnifferNoseItem> getAllSnifferNosesAsList() {
        List<SnifferNoseItem> result = new ArrayList<>();
        for (RegistrySupplier<SnifferNoseItem> supplier : snifferNoseItems.values()) {
            if (supplier.isPresent()) {
                result.add(supplier.get());
            }
        }
        return result;
    }

    public static boolean hasSnifferNose(String id) {
        return snifferNoseItems.containsKey(id);
    }

    static DeferredRegister<Item> getItemRegister() {
        return ITEMS;
    }
}
