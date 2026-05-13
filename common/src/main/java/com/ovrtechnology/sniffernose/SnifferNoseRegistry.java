package com.ovrtechnology.sniffernose;

import com.ovrtechnology.AromaAffect;
import lombok.Getter;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Central registry for all sniffer nose items in Aroma Affect.
 *
 * <p>Registered through {@code registrars.registrar(Registries.ITEM, SnifferNoseRegistry::register)}
 * from {@link AromaAffect#initialize(net.blay09.mods.balm.core.BalmRegistrars)}.</p>
 *
 * <p>Sniffer noses are separate from regular player-equippable noses; they are designed
 * for the Sniffer mob.</p>
 */
public final class SnifferNoseRegistry {

    @Getter
    private static final Map<String, Holder<Item>> snifferNoseItems = new LinkedHashMap<>();

    @Getter
    private static final Map<String, SnifferNoseDefinition> snifferNoseDefinitions = new LinkedHashMap<>();

    @Getter
    private static boolean initialized = false;

    private SnifferNoseRegistry() {}

    public static void register(BalmRegistrar.Scoped<Item> items) {
        if (initialized) {
            AromaAffect.LOGGER.warn("SnifferNoseRegistry.register() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing SnifferNoseRegistry...");

        List<SnifferNoseDefinition> definitions = SnifferNoseDefinitionLoader.loadAllSnifferNoses();
        for (SnifferNoseDefinition definition : definitions) {
            registerSnifferNose(items, definition);
        }

        initialized = true;
        AromaAffect.LOGGER.info("SnifferNoseRegistry initialized with {} sniffer noses", snifferNoseItems.size());
    }

    private static void registerSnifferNose(BalmRegistrar.Scoped<Item> items, SnifferNoseDefinition definition) {
        String id = definition.getId();

        if (snifferNoseItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate sniffer nose ID: {}, skipping...", id);
            return;
        }

        snifferNoseDefinitions.put(id, definition);

        final String itemId = id;
        Holder<Item> holder = items.register(id, identifier -> new SnifferNoseItem(definition, itemId));
        snifferNoseItems.put(id, holder);

        AromaAffect.LOGGER.debug("Registered sniffer nose item: {}", id);
    }

    public static Optional<SnifferNoseItem> getSnifferNose(String id) {
        Holder<Item> holder = snifferNoseItems.get(id);
        if (holder != null && holder.isBound() && holder.value() instanceof SnifferNoseItem noseItem) {
            return Optional.of(noseItem);
        }
        return Optional.empty();
    }

    public static Optional<Holder<Item>> getSnifferNoseHolder(String id) {
        return Optional.ofNullable(snifferNoseItems.get(id));
    }

    public static Optional<SnifferNoseDefinition> getDefinition(String id) {
        return Optional.ofNullable(snifferNoseDefinitions.get(id));
    }

    public static Iterable<String> getAllSnifferNoseIds() {
        return snifferNoseItems.keySet();
    }

    public static Iterable<Holder<Item>> getAllSnifferNoses() {
        return snifferNoseItems.values();
    }

    public static List<SnifferNoseItem> getAllSnifferNosesAsList() {
        List<SnifferNoseItem> result = new ArrayList<>();
        for (Holder<Item> holder : snifferNoseItems.values()) {
            if (holder.isBound() && holder.value() instanceof SnifferNoseItem noseItem) {
                result.add(noseItem);
            }
        }
        return result;
    }

    public static int getSnifferNoseCount() {
        return snifferNoseItems.size();
    }

    public static boolean hasSnifferNose(String id) {
        return snifferNoseItems.containsKey(id);
    }
}
