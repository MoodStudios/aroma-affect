package com.ovrtechnology.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.category.CategoryDefinitionLoader;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.scent.ScentDefinitionLoader;
import com.ovrtechnology.scentitem.ScentItemDefinitionLoader;
import com.ovrtechnology.sniffer.loot.SnifferLootRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseDefinitionLoader;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.event.EventDefinitionLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Definition cross-reference consistency")
class DefinitionReferenceConsistencyTest {

    private static final String DATA_ROOT = "data/aromaaffect/";
    private static final String SPRITE_DIR = "assets/aromaaffect/textures/gui/sprites/";
    private static final String MOD_PREFIX = "aromaaffect:";

    private static final Map<String, String> SPRITE_DIRECTORIES = Map.of(
            BiomeDefinitionLoader.BIOMES_DIR, "biomes",
            StructureDefinitionLoader.STRUCTURES_DIR, "structures"
    );

    private static final List<String> SCENTED_DIRECTORIES = List.of(
            BiomeDefinitionLoader.BIOMES_DIR,
            BlockDefinitionLoader.BLOCKS_DIR,
            StructureDefinitionLoader.STRUCTURES_DIR,
            FlowerDefinitionLoader.FLOWERS_DIR,
            MobDefinitionLoader.MOBS_DIR,
            EventDefinitionLoader.EVENTS_DIR
    );

    @Test
    @DisplayName("every scented definition should reference a defined scent")
    void scentReferencesShouldResolve() throws IOException, URISyntaxException {
        Set<String> scentIds = idsIn(ScentDefinitionLoader.SCENTS_DIR);
        assertThat(scentIds).as("bundled scents").isNotEmpty();

        for (String directory : SCENTED_DIRECTORIES) {
            for (Path file : listJsonFiles(DATA_ROOT + directory)) {
                JsonObject definition = loadObject(file);
                String scentId = optionalString(definition, "scent_id");
                assertThat(scentId).as("scent_id in %s/%s", directory, file.getFileName()).isNotNull();
                assertThat(scentIds)
                        .as("scent '%s' referenced by %s/%s", scentId, directory, file.getFileName())
                        .contains(scentId);
            }
        }
    }

    @Test
    @DisplayName("every perception reference should resolve to a defined category")
    void perceptionReferencesShouldResolve() throws IOException, URISyntaxException {
        Set<String> categoryIds = idsIn(CategoryDefinitionLoader.CATEGORY_DIR);
        assertThat(categoryIds).as("bundled categories").isNotEmpty();

        int references = 0;
        for (String directory : SCENTED_DIRECTORIES) {
            for (Path file : listJsonFiles(DATA_ROOT + directory)) {
                String perception = optionalString(loadObject(file), "perception");
                if (perception == null) {
                    continue;
                }
                references++;
                assertThat(categoryIds)
                        .as("perception '%s' referenced by %s/%s", perception, directory, file.getFileName())
                        .contains(perception);
            }
        }
        assertThat(references).as("perception references found").isPositive();
    }

    @Test
    @DisplayName("every scent item trigger should reference a defined scent item and OVR scent")
    void scentItemTriggersShouldResolve() throws IOException, URISyntaxException {
        Set<String> scentItemIds = new HashSet<>();
        for (JsonElement item : wrappedArray(ScentItemDefinitionLoader.SCENT_ITEMS_RESOURCE_PATH, "scents")) {
            scentItemIds.add(item.getAsJsonObject().get("id").getAsString());
        }
        Set<String> scentNames = new HashSet<>();
        for (Path file : listJsonFiles(DATA_ROOT + ScentDefinitionLoader.SCENTS_DIR)) {
            scentNames.add(loadObject(file).get("fallback_name").getAsString().toLowerCase(Locale.ROOT));
        }

        JsonArray triggers = wrappedArray(ScentTriggerConfigLoader.ITEM_TRIGGERS_PATH, "item_triggers");
        assertThat(triggers).as("scent item triggers").isNotEmpty();
        for (JsonElement element : triggers) {
            JsonObject trigger = element.getAsJsonObject();
            String itemId = optionalString(trigger, "item_id");
            String scentName = optionalString(trigger, "scent_name");
            assertThat(itemId).as("item_id in scent item trigger").isNotNull().startsWith(MOD_PREFIX);
            assertThat(scentItemIds)
                    .as("scent item referenced by trigger %s", itemId)
                    .contains(itemId.substring(MOD_PREFIX.length()));
            assertThat(scentName).as("scent_name in trigger %s", itemId).isNotNull();
            assertThat(scentNames)
                    .as("scent '%s' referenced by trigger %s", scentName, itemId)
                    .contains(scentName.toLowerCase(Locale.ROOT));
        }
    }

    @Test
    @DisplayName("every sniffer loot rule should reference a defined sniffer nose")
    void snifferLootRulesShouldReferenceDefinedNoses() throws IOException, URISyntaxException {
        Set<String> snifferNoseIds = new HashSet<>();
        for (JsonElement nose : wrappedArray(SnifferNoseDefinitionLoader.SNIFFER_NOSES_RESOURCE_PATH, "sniffer_noses")) {
            snifferNoseIds.add(MOD_PREFIX + nose.getAsJsonObject().get("id").getAsString());
        }
        assertThat(snifferNoseIds).as("bundled sniffer noses").isNotEmpty();

        List<Path> rules = listJsonFiles(DATA_ROOT + SnifferLootRegistry.RULES_DIR);
        assertThat(rules).as("sniffer loot rules").isNotEmpty();
        for (Path file : rules) {
            String snifferNose = optionalString(loadObject(file), "sniffer_nose");
            assertThat(snifferNose).as("sniffer_nose in %s", file.getFileName()).isNotNull();
            assertThat(snifferNoseIds)
                    .as("sniffer nose '%s' referenced by %s", snifferNose, file.getFileName())
                    .contains(snifferNose);
        }
    }

    @Test
    @DisplayName("every biome and structure should have a menu sprite")
    void biomesAndStructuresShouldHaveSprites() throws IOException, URISyntaxException {
        for (Map.Entry<String, String> entry : SPRITE_DIRECTORIES.entrySet()) {
            List<Path> files = listJsonFiles(DATA_ROOT + entry.getKey());
            assertThat(files).as("definitions in %s", entry.getKey()).isNotEmpty();
            for (Path file : files) {
                String id = optionalString(loadObject(file), "id");
                assertThat(id).as("id in %s/%s", entry.getKey(), file.getFileName()).isNotNull();
                int colon = id.indexOf(':');
                assertThat(colon).as("id %s in %s has a namespace", id, file.getFileName()).isPositive();
                String sprite = SPRITE_DIR + entry.getValue() + "/" + id.substring(0, colon) + "/" + id.substring(colon + 1) + ".png";
                assertThat(resourceUrl(sprite))
                        .as("sprite %s for %s", sprite, id)
                        .isNotNull();
            }
        }
    }

    private static JsonArray wrappedArray(String path, String wrapperKey) throws IOException {
        JsonElement root;
        try (InputStream stream = DefinitionReferenceConsistencyTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader);
            }
        }
        JsonArray array = root.isJsonArray() ? root.getAsJsonArray() : root.getAsJsonObject().getAsJsonArray(wrapperKey);
        assertThat(array).as("array '%s' in %s", wrapperKey, path).isNotNull();
        return array;
    }

    private static URL resourceUrl(String path) {
        return DefinitionReferenceConsistencyTest.class.getClassLoader().getResource(path);
    }

    private static Set<String> idsIn(String directory) throws IOException, URISyntaxException {
        Set<String> ids = new HashSet<>();
        for (Path file : listJsonFiles(DATA_ROOT + directory)) {
            String id = optionalString(loadObject(file), "id");
            assertThat(id).as("id in %s/%s", directory, file.getFileName()).isNotNull();
            ids.add(id);
        }
        return ids;
    }

    private static String optionalString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private static List<Path> listJsonFiles(String directory) throws IOException, URISyntaxException {
        URL url = resourceUrl(directory);
        assertThat(url).as("bundled directory %s", directory).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        try (Stream<Path> files = Files.list(Path.of(url.toURI()))) {
            return files.filter(path -> path.toString().endsWith(".json")).toList();
        }
    }

    private static JsonObject loadObject(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            assertThat(element.isJsonObject()).as("%s is a JSON object", file.getFileName()).isTrue();
            return element.getAsJsonObject();
        }
    }
}
