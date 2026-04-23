package com.ovrtechnology.nose;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.*;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public final class NoseRegistry {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);

    @Getter
    private static final Map<String, RegistrySupplier<NoseItem>> noseItems = new LinkedHashMap<>();

    @Getter
    private static final Map<String, NoseDefinition> noseDefinitions = new LinkedHashMap<>();

    @Getter private static final List<RegistrySupplier<NoseItem>> legacyItems = new ArrayList<>();

    @Getter private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing NoseRegistry...");

        List<NoseDefinition> definitions = NoseDefinitionLoader.loadAllNoses();

        for (NoseDefinition definition : definitions) {
            registerNose(definition);
        }

        registerLegacyAliases();

        ITEMS.register();

        NoseAbilityResolver.init();

        initialized = true;
        AromaAffect.LOGGER.info("NoseRegistry initialized with {} noses", noseItems.size());
    }

    private static void registerNose(NoseDefinition definition) {
        String id = definition.getId();

        if (noseItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate nose ID: {}, skipping...", id);
            return;
        }

        noseDefinitions.put(id, definition);

        final String itemId = id;
        RegistrySupplier<NoseItem> supplier =
                ITEMS.register(id, () -> new NoseItem(definition, itemId));
        noseItems.put(id, supplier);

        AromaAffect.LOGGER.debug("Registered nose item: {}", id);
    }

    private static void registerLegacyAliases() {
        for (Map.Entry<String, String> entry : NoseIdRemapper.getMappings().entrySet()) {
            String oldId = entry.getKey();
            String newId = entry.getValue();
            NoseDefinition def = noseDefinitions.get(newId);
            if (def == null) continue;

            RegistrySupplier<NoseItem> supplier =
                    ITEMS.register(oldId, () -> new NoseItem(def, oldId));
            legacyItems.add(supplier);
            AromaAffect.LOGGER.debug("Registered legacy alias: {} -> {}", oldId, newId);
        }
    }

    public static Optional<NoseItem> getNose(String id) {
        RegistrySupplier<NoseItem> supplier = noseItems.get(id);
        if (supplier != null && supplier.isPresent()) {
            return Optional.of(supplier.get());
        }
        return Optional.empty();
    }

    public static Optional<RegistrySupplier<NoseItem>> getNoseSupplier(String id) {
        return Optional.ofNullable(noseItems.get(id));
    }

    public static Optional<NoseDefinition> getDefinition(String id) {
        return Optional.ofNullable(noseDefinitions.get(id));
    }

    public static Iterable<String> getAllNoseIds() {
        return noseItems.keySet();
    }

    public static Iterable<RegistrySupplier<NoseItem>> getAllNoses() {
        return noseItems.values();
    }

    public static List<NoseItem> getAllNosesAsList() {
        return new ArrayList<>(noseItems.values()).stream().map(RegistrySupplier::get).toList();
    }

    public static int getNoseCount() {
        return noseItems.size();
    }

    public static boolean hasNose(String id) {
        return noseItems.containsKey(id);
    }

    static DeferredRegister<Item> getItemRegister() {
        return ITEMS;
    }
}
