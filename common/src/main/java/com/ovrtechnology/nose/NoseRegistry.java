package com.ovrtechnology.nose;

import com.ovrtechnology.AromaAffect;
import lombok.Getter;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Central registry for all nose items in Aroma Affect.
 *
 * <p>Registered through {@code registrars.registrar(Registries.ITEM, NoseRegistry::register)}
 * from {@link AromaAffect#initialize(net.blay09.mods.balm.core.BalmRegistrars)}.
 * Holders are populated when the registrar callback runs; subsequent calls go via
 * {@link Holder#value()}.</p>
 *
 * <p>Recipes are loaded from {@code data/aromaaffect/recipe/} as standard Minecraft
 * recipe JSON files.</p>
 */
public final class NoseRegistry {

    @Getter
    private static final Map<String, Holder<Item>> noseItems = new LinkedHashMap<>();

    @Getter
    private static final Map<String, NoseDefinition> noseDefinitions = new LinkedHashMap<>();

    /**
     * Legacy alias holders (old IDs pointing to equivalent items).
     * Used by renderers to register the custom model for old-world items.
     */
    @Getter
    private static final List<Holder<Item>> legacyItems = new ArrayList<>();

    @Getter
    private static boolean initialized = false;

    private NoseRegistry() {}

    public static void register(BalmRegistrar.Scoped<Item> items) {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseRegistry.register() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing NoseRegistry...");

        List<NoseDefinition> definitions = NoseDefinitionLoader.loadAllNoses();
        for (NoseDefinition definition : definitions) {
            registerNose(items, definition);
        }

        registerLegacyAliases(items);

        initialized = true;
        AromaAffect.LOGGER.info("NoseRegistry initialized with {} noses", noseItems.size());
    }

    /**
     * Initialize the ability resolver after registries are bound.
     * Called from AromaAffect.initialize after the registrar callback.
     */
    public static void initAbilityResolver() {
        NoseAbilityResolver.init();
    }

    private static void registerNose(BalmRegistrar.Scoped<Item> items, NoseDefinition definition) {
        String id = definition.getId();

        if (noseItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate nose ID: {}, skipping...", id);
            return;
        }

        noseDefinitions.put(id, definition);

        final String itemId = id;
        Holder<Item> holder = items.register(id, identifier -> new NoseItem(definition, itemId));
        noseItems.put(id, holder);

        AromaAffect.LOGGER.debug("Registered nose item: {}", id);
    }

    private static void registerLegacyAliases(BalmRegistrar.Scoped<Item> items) {
        for (Map.Entry<String, String> entry : NoseIdRemapper.getMappings().entrySet()) {
            String oldId = entry.getKey();
            String newId = entry.getValue();
            NoseDefinition def = noseDefinitions.get(newId);
            if (def == null) continue;

            Holder<Item> holder = items.register(oldId, identifier -> new NoseItem(def, oldId));
            legacyItems.add(holder);
            AromaAffect.LOGGER.debug("Registered legacy alias: {} -> {}", oldId, newId);
        }
    }

    public static Optional<NoseItem> getNose(String id) {
        Holder<Item> holder = noseItems.get(id);
        if (holder != null && holder.isBound() && holder.value() instanceof NoseItem noseItem) {
            return Optional.of(noseItem);
        }
        return Optional.empty();
    }

    public static Optional<Holder<Item>> getNoseHolder(String id) {
        return Optional.ofNullable(noseItems.get(id));
    }

    public static Optional<NoseDefinition> getDefinition(String id) {
        return Optional.ofNullable(noseDefinitions.get(id));
    }

    public static Iterable<String> getAllNoseIds() {
        return noseItems.keySet();
    }

    public static Iterable<Holder<Item>> getAllNoses() {
        return noseItems.values();
    }

    public static List<NoseItem> getAllNosesAsList() {
        List<NoseItem> result = new ArrayList<>();
        for (Holder<Item> holder : noseItems.values()) {
            if (holder.isBound() && holder.value() instanceof NoseItem noseItem) {
                result.add(noseItem);
            }
        }
        return result;
    }

    public static int getNoseCount() {
        return noseItems.size();
    }

    public static boolean hasNose(String id) {
        return noseItems.containsKey(id);
    }
}
