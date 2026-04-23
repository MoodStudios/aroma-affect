package com.ovrtechnology.scentitem;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.*;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public final class ScentItemRegistry {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);

    @Getter
    private static final Map<String, RegistrySupplier<ScentItem>> scentItems =
            new LinkedHashMap<>();

    @Getter
    private static final Map<String, ScentItemDefinition> scentItemDefinitions =
            new LinkedHashMap<>();

    @Getter private static boolean initialized = false;

    private ScentItemRegistry() {
        throw new UnsupportedOperationException("ScentItemRegistry is a static utility class");
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentItemRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing ScentItemRegistry...");

        List<ScentItemDefinition> definitions = ScentItemDefinitionLoader.loadAllScentItems();

        for (ScentItemDefinition definition : definitions) {
            registerScentItem(definition);
        }

        ITEMS.register();

        initialized = true;
        AromaAffect.LOGGER.info(
                "ScentItemRegistry initialized with {} scent items", scentItems.size());
    }

    private static void registerScentItem(ScentItemDefinition definition) {
        String id = definition.getId();

        if (scentItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent item ID: {}, skipping...", id);
            return;
        }

        scentItemDefinitions.put(id, definition);

        final String itemId = id;
        RegistrySupplier<ScentItem> supplier =
                ITEMS.register(id, () -> new ScentItem(definition, itemId));
        scentItems.put(id, supplier);

        AromaAffect.LOGGER.debug("Registered scent item: {}", id);
    }

    public static Optional<ScentItemDefinition> getDefinition(String id) {
        return Optional.ofNullable(scentItemDefinitions.get(id));
    }

    public static List<ScentItem> getAllScentItemsAsList() {
        List<ScentItem> result = new ArrayList<>();
        for (RegistrySupplier<ScentItem> supplier : scentItems.values()) {
            if (supplier.isPresent()) {
                result.add(supplier.get());
            }
        }
        return result;
    }

    public static List<ScentItem> getCapsuleItems() {
        List<ScentItem> result = new ArrayList<>();
        for (Map.Entry<String, RegistrySupplier<ScentItem>> entry : scentItems.entrySet()) {
            ScentItemDefinition def = scentItemDefinitions.get(entry.getKey());
            if (def != null && def.isCapsule() && entry.getValue().isPresent()) {
                result.add(entry.getValue().get());
            }
        }
        return result;
    }

    public static List<ScentItem> getScentItemsSortedByPriority() {
        List<ScentItem> result = getAllScentItemsAsList();
        result.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return result;
    }

    static DeferredRegister<Item> getItemRegister() {
        return ITEMS;
    }
}
