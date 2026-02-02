package com.ovrtechnology.worldgen;

import com.mojang.datafixers.util.Pair;
import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.common.LifecycleEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Runtime injection into vanilla village template pools.
 *
 * <p>This is intentionally runtime-driven (instead of datapack overriding vanilla pools) to be
 * modpack-friendly and avoid hard conflicts with other mods/datapacks that also patch villages.</p>
 */
public final class VillagePoolInjector {

    private enum VillageBiome {
        PLAINS("plains"),
        DESERT("desert"),
        SAVANNA("savanna"),
        SNOWY("snowy"),
        TAIGA("taiga");

        private final String id;

        VillageBiome(String id) {
            this.id = id;
        }

        public ResourceLocation townCentersPool() {
            return ResourceLocation.withDefaultNamespace("village/" + id + "/town_centers");
        }

        public ResourceLocation streetsPool() {
            return ResourceLocation.withDefaultNamespace("village/" + id + "/streets");
        }

        public ResourceLocation noseSmithHouseTemplate() {
            return ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "village/" + id + "/houses/nose_smith_house");
        }

        public ResourceLocation noseSmithTownCenterTemplate() {
            return ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "village/" + id + "/town_centers/nose_smith_start");
        }

        public ResourceLocation noseSmithResidentPool() {
            return ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "village/" + id + "/nose_smith_resident");
        }

        public ResourceLocation noseSmithHousePool() {
            return ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "village/" + id + "/nose_smith_house");
        }
    }

    private static boolean initialized = false;
    private static boolean reflectionReady = false;

    private static Field templatesField;
    private static Field rawTemplatesField;

    private VillagePoolInjector() {
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("VillagePoolInjector.init() called multiple times!");
            return;
        }

        // Must happen before any chunks generate; otherwise spawn-area villages may generate without our changes.
        LifecycleEvent.SERVER_BEFORE_START.register(VillagePoolInjector::inject);

        initialized = true;
        AromaAffect.LOGGER.info("Village pool injector initialized");
    }

    private static void inject(MinecraftServer server) {
        if (!ensureReflectionReady()) {
            return;
        }

        try {
            var registryAccess = server.registryAccess();

            Registry<StructureTemplatePool> pools = registryAccess.lookupOrThrow(Registries.TEMPLATE_POOL);

            // Requirement: every village must contain the Nose Smith house, without replacing vanilla houses.
            //
            // Approach:
            // - Override each biome's town-centers pool to always start from a custom, vanilla-derived meeting point.
            // - That meeting point contains a jigsaw connector that *always* spawns the Nose Smith house via a custom pool.
            for (VillageBiome biome : EnumSet.allOf(VillageBiome.class)) {
                StructureTemplatePool townCenters = pools
                        .get(biome.townCentersPool())
                        .orElseThrow(() -> new IllegalStateException("Missing template pool: " + biome.townCentersPool()))
                        .value();

                StructurePoolElement startElement = StructurePoolElement
                        .legacy(biome.noseSmithTownCenterTemplate().toString())
                        .apply(StructureTemplatePool.Projection.RIGID);

                forcePoolToOnly(townCenters, startElement);
                AromaAffect.LOGGER.info("Forced village start pool {} -> {}", biome.townCentersPool(), biome.noseSmithTownCenterTemplate());
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to inject village pools", e);
        }
    }

    private static boolean ensureReflectionReady() {
        if (reflectionReady) {
            return true;
        }

        try {
            // Find fields by type instead of by name, because field names differ between
            // Mojang mappings (dev/NeoForge) and intermediary mappings (Fabric runtime).
            for (Field field : StructureTemplatePool.class.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType() == ObjectArrayList.class) {
                    templatesField = field;
                } else if (field.getType() == List.class) {
                    rawTemplatesField = field;
                }
            }

            if (templatesField == null) {
                throw new NoSuchFieldException("Could not find ObjectArrayList<StructurePoolElement> field in StructureTemplatePool");
            }
            if (rawTemplatesField == null) {
                throw new NoSuchFieldException("Could not find List<Pair<StructurePoolElement, Integer>> field in StructureTemplatePool");
            }

            reflectionReady = true;
            return true;
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to access StructureTemplatePool internals", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void forcePoolToOnly(StructureTemplatePool pool, StructurePoolElement onlyElement)
            throws ReflectiveOperationException {
        // Update the flattened templates list (used by getShuffledTemplates during jigsaw assembly).
        ObjectArrayList<StructurePoolElement> templates = getMutableTemplates(pool);
        templates.clear();
        templates.add(onlyElement);

        // Also update rawTemplates so nothing can reconstruct the original list.
        List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>();
        rawTemplates.add(Pair.of(onlyElement, 1));
        rawTemplatesField.set(pool, rawTemplates);
    }

    @SuppressWarnings("unchecked")
    private static ObjectArrayList<StructurePoolElement> getMutableTemplates(StructureTemplatePool pool)
            throws ReflectiveOperationException {
        Object templatesObj = templatesField.get(pool);
        if (templatesObj instanceof ObjectArrayList<?> templates) {
            return (ObjectArrayList<StructurePoolElement>) templates;
        }

        // Extremely defensive: if some environment swaps this to an immutable list, replace it.
        ObjectArrayList<StructurePoolElement> replacement = new ObjectArrayList<>();
        if (templatesObj instanceof Collection<?> existing) {
            for (Object o : existing) {
                if (o instanceof StructurePoolElement element) {
                    replacement.add(element);
                }
            }
        }

        templatesField.set(pool, replacement);
        return replacement;
    }
}
