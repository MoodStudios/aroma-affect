package com.ovrtechnology.scentitem;

import com.ovrtechnology.AromaAffect;
import lombok.Getter;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Central registry for all scent items in Aroma Affect.
 *
 * <p>Registered through {@code registrars.registrar(Registries.ITEM, ScentItemRegistry::register)}
 * from {@link AromaAffect#initialize(net.blay09.mods.balm.core.BalmRegistrars)}.</p>
 */
public final class ScentItemRegistry {

    @Getter
    private static final Map<String, Holder<Item>> scentItems = new LinkedHashMap<>();

    @Getter
    private static final Map<String, ScentItemDefinition> scentItemDefinitions = new LinkedHashMap<>();

    @Getter
    private static boolean initialized = false;

    private ScentItemRegistry() {}

    public static void register(BalmRegistrar.Scoped<Item> items) {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentItemRegistry.register() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing ScentItemRegistry...");

        List<ScentItemDefinition> definitions = ScentItemDefinitionLoader.loadAllScentItems();
        for (ScentItemDefinition definition : definitions) {
            registerScentItem(items, definition);
        }

        initialized = true;
        AromaAffect.LOGGER.info("ScentItemRegistry initialized with {} scent items", scentItems.size());
    }

    private static void registerScentItem(BalmRegistrar.Scoped<Item> items, ScentItemDefinition definition) {
        String id = definition.getId();

        if (scentItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent item ID: {}, skipping...", id);
            return;
        }

        scentItemDefinitions.put(id, definition);

        final String itemId = id;
        Holder<Item> holder = items.register(id, identifier -> new ScentItem(definition, itemId));
        scentItems.put(id, holder);

        AromaAffect.LOGGER.debug("Registered scent item: {}", id);
    }

    public static Optional<ScentItem> getScentItem(String id) {
        Holder<Item> holder = scentItems.get(id);
        if (holder != null && holder.isBound() && holder.value() instanceof ScentItem scentItem) {
            return Optional.of(scentItem);
        }
        return Optional.empty();
    }

    public static Optional<Holder<Item>> getScentItemHolder(String id) {
        return Optional.ofNullable(scentItems.get(id));
    }

    public static Optional<ScentItemDefinition> getDefinition(String id) {
        return Optional.ofNullable(scentItemDefinitions.get(id));
    }

    public static Iterable<String> getAllScentItemIds() {
        return Collections.unmodifiableSet(scentItems.keySet());
    }

    public static Iterable<Holder<Item>> getAllScentItems() {
        return Collections.unmodifiableCollection(scentItems.values());
    }

    public static List<ScentItem> getAllScentItemsAsList() {
        List<ScentItem> result = new ArrayList<>();
        for (Holder<Item> holder : scentItems.values()) {
            if (holder.isBound() && holder.value() instanceof ScentItem scentItem) {
                result.add(scentItem);
            }
        }
        return result;
    }

    public static List<ScentItem> getCapsuleItems() {
        List<ScentItem> result = new ArrayList<>();
        for (Map.Entry<String, Holder<Item>> entry : scentItems.entrySet()) {
            ScentItemDefinition def = scentItemDefinitions.get(entry.getKey());
            if (def != null && def.isCapsule()
                    && entry.getValue().isBound()
                    && entry.getValue().value() instanceof ScentItem scentItem) {
                result.add(scentItem);
            }
        }
        return result;
    }

    public static int getScentItemCount() {
        return scentItems.size();
    }

    public static boolean hasScentItem(String id) {
        return scentItems.containsKey(id);
    }

    public static List<ScentItem> getScentItemsSortedByPriority() {
        List<ScentItem> result = getAllScentItemsAsList();
        result.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return result;
    }
}
