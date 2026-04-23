package com.ovrtechnology.worldgen;

import com.mojang.datafixers.util.Pair;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.util.Ids;
import dev.architectury.event.events.common.LifecycleEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

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
            return Ids.vanilla("village/" + id + "/town_centers");
        }

        public ResourceLocation noseSmithTownCenterTemplate() {
            return Ids.mod("village/" + id + "/town_centers/nose_smith_start");
        }
    }

    private static boolean initialized = false;
    private static boolean reflectionReady = false;

    private static Field templatesField;
    private static Field rawTemplatesField;

    private VillagePoolInjector() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("VillagePoolInjector.init() called multiple times!");
            return;
        }

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

            Registry<StructureTemplatePool> pools =
                    registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);

            for (VillageBiome biome : EnumSet.allOf(VillageBiome.class)) {
                StructureTemplatePool townCenters = pools.get(biome.townCentersPool());
                if (townCenters == null) {
                    throw new IllegalStateException(
                            "Missing template pool: " + biome.townCentersPool());
                }

                StructurePoolElement startElement =
                        StructurePoolElement.legacy(biome.noseSmithTownCenterTemplate().toString())
                                .apply(StructureTemplatePool.Projection.RIGID);

                forcePoolToOnly(townCenters, startElement);
                AromaAffect.LOGGER.info(
                        "Forced village start pool {} -> {}",
                        biome.townCentersPool(),
                        biome.noseSmithTownCenterTemplate());
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

            for (Field field : StructureTemplatePool.class.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType() == ObjectArrayList.class) {
                    templatesField = field;
                } else if (field.getType() == List.class) {
                    rawTemplatesField = field;
                }
            }

            if (templatesField == null) {
                throw new NoSuchFieldException(
                        "Could not find ObjectArrayList<StructurePoolElement> field in StructureTemplatePool");
            }
            if (rawTemplatesField == null) {
                throw new NoSuchFieldException(
                        "Could not find List<Pair<StructurePoolElement, Integer>> field in StructureTemplatePool");
            }

            reflectionReady = true;
            return true;
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to access StructureTemplatePool internals", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void forcePoolToOnly(
            StructureTemplatePool pool, StructurePoolElement onlyElement)
            throws ReflectiveOperationException {

        ObjectArrayList<StructurePoolElement> templates = getMutableTemplates(pool);
        templates.clear();
        templates.add(onlyElement);

        List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>();
        rawTemplates.add(Pair.of(onlyElement, 1));
        rawTemplatesField.set(pool, rawTemplates);
    }

    @SuppressWarnings("unchecked")
    private static ObjectArrayList<StructurePoolElement> getMutableTemplates(
            StructureTemplatePool pool) throws ReflectiveOperationException {
        Object templatesObj = templatesField.get(pool);
        if (templatesObj instanceof ObjectArrayList<?> templates) {
            return (ObjectArrayList<StructurePoolElement>) templates;
        }

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
