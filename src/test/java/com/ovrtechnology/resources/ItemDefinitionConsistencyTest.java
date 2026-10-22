package com.ovrtechnology.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.nose.NoseDefinitionLoader;
import com.ovrtechnology.scent.ScentDefinitionLoader;
import com.ovrtechnology.scentitem.ScentItemDefinitionLoader;
import com.ovrtechnology.sniffernose.SnifferNoseDefinitionLoader;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Item definition consistency")
class ItemDefinitionConsistencyTest {

    private static final String DATA_ROOT = "data/aromaaffect/";
    private static final String LANG_PATH = "assets/aromaaffect/lang/en_us.json";
    private static final String ITEM_MODEL_DIR = "assets/aromaaffect/items/";
    private static final String TEXTURE_DIR = "assets/aromaaffect/textures/";

    private static final Map<String, String> ITEM_DEFINITION_FILES = Map.of(
            ScentItemDefinitionLoader.SCENT_ITEMS_RESOURCE_PATH, "scents",
            NoseDefinitionLoader.NOSES_RESOURCE_PATH, "noses",
            SnifferNoseDefinitionLoader.SNIFFER_NOSES_RESOURCE_PATH, "sniffer_noses"
    );

    private static final Set<String> CODE_REGISTERED_ITEMS = Set.of(
            "omara_device",
            "aroma_guide",
            "iron_nose",
            "special_rose",
            "nose_smith_spawn_egg",
            "custom_nose"
    );

    private static final Pattern MOD_ITEM_REFERENCE = Pattern.compile("\"aromaaffect:([a-z0-9_]+)\"");

    @Test
    @DisplayName("scent items file should define the endgame capsule set")
    void scentItemsShouldDefineCapsules() throws IOException {
        List<JsonObject> items = definitions(ScentItemDefinitionLoader.SCENT_ITEMS_RESOURCE_PATH, "scents");
        List<String> capsules = items.stream()
                .filter(item -> "capsule".equals(optionalString(item, "type")))
                .map(item -> item.get("id").getAsString())
                .toList();
        assertThat(capsules).as("capsule scent items").hasSize(16);
        assertThat(ids(items)).contains("scent_container", "scent_base");
    }

    @Test
    @DisplayName("every item definition should have a unique id, a bundled texture, a lang entry and an item model")
    void itemDefinitionsShouldBeComplete() throws IOException {
        JsonObject lang = loadObject(LANG_PATH);
        for (Map.Entry<String, String> entry : ITEM_DEFINITION_FILES.entrySet()) {
            List<JsonObject> items = definitions(entry.getKey(), entry.getValue());
            assertThat(items).as("definitions in %s", entry.getKey()).isNotEmpty();

            Set<String> seen = new HashSet<>();
            for (JsonObject item : items) {
                String id = item.get("id").getAsString();
                assertThat(seen.add(id)).as("unique id %s in %s", id, entry.getKey()).isTrue();

                String image = optionalString(item, "image");
                assertThat(image).as("image for %s", id).isNotNull();
                String texturePath = TEXTURE_DIR + image + ".png";
                assertThat(resourceUrl(texturePath)).as("texture %s for %s", texturePath, id).isNotNull();

                String langKey = "item.aromaaffect." + id;
                assertThat(lang.has(langKey)).as("lang key %s", langKey).isTrue();

                String modelPath = ITEM_MODEL_DIR + id + ".json";
                assertThat(resourceUrl(modelPath)).as("item model %s", modelPath).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("every capsule should reference a defined OVR scent by its fallback name")
    void capsulesShouldReferenceDefinedScents() throws IOException, URISyntaxException {
        Set<String> scentNames = new HashSet<>();
        for (String stem : listJsonStems(DATA_ROOT + ScentDefinitionLoader.SCENTS_DIR)) {
            JsonObject scent = loadObject(DATA_ROOT + ScentDefinitionLoader.SCENTS_DIR + "/" + stem + ".json");
            scentNames.add(scent.get("fallback_name").getAsString().toLowerCase(Locale.ROOT));
        }

        for (JsonObject item : definitions(ScentItemDefinitionLoader.SCENT_ITEMS_RESOURCE_PATH, "scents")) {
            if (!"capsule".equals(optionalString(item, "type"))) {
                continue;
            }
            String id = item.get("id").getAsString();
            String scent = optionalString(item, "scent");
            assertThat(scent).as("scent for capsule %s", id).isNotNull();
            assertThat(scentNames)
                    .as("scent '%s' referenced by capsule %s", scent, id)
                    .contains(scent.toLowerCase(Locale.ROOT));
        }
    }

    @Test
    @DisplayName("every mod item referenced by recipes and loot tables should be a registered item")
    void recipesAndLootTablesShouldReferenceRegisteredItems() throws IOException, URISyntaxException {
        Set<String> registered = new TreeSet<>(CODE_REGISTERED_ITEMS);
        for (Map.Entry<String, String> entry : ITEM_DEFINITION_FILES.entrySet()) {
            for (JsonObject item : definitions(entry.getKey(), entry.getValue())) {
                registered.add(item.get("id").getAsString());
            }
        }

        Map<String, Set<String>> referencedBy = new LinkedHashMap<>();
        for (String directory : List.of(DATA_ROOT + "recipe", DATA_ROOT + "loot_table", "data/minecraft/loot_table")) {
            for (Path file : listJsonFiles(directory)) {
                Matcher matcher = MOD_ITEM_REFERENCE.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    referencedBy.computeIfAbsent(matcher.group(1), key -> new TreeSet<>())
                            .add(file.getFileName().toString());
                }
            }
        }

        assertThat(referencedBy).as("mod item references in recipes and loot tables").isNotEmpty();
        for (Map.Entry<String, Set<String>> reference : referencedBy.entrySet()) {
            assertThat(registered)
                    .as("item aromaaffect:%s referenced by %s", reference.getKey(), reference.getValue())
                    .contains(reference.getKey());
        }
    }

    private static List<JsonObject> definitions(String path, String wrapperKey) throws IOException {
        JsonElement root = loadJson(path);
        JsonArray array = root.isJsonArray() ? root.getAsJsonArray() : root.getAsJsonObject().getAsJsonArray(wrapperKey);
        assertThat(array).as("array '%s' in %s", wrapperKey, path).isNotNull();
        return array.asList().stream().map(JsonElement::getAsJsonObject).toList();
    }

    private static Set<String> ids(List<JsonObject> items) {
        Set<String> ids = new HashSet<>();
        for (JsonObject item : items) {
            ids.add(item.get("id").getAsString());
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

    private static List<String> listJsonStems(String directory) throws IOException, URISyntaxException {
        return listJsonFiles(directory).stream()
                .map(path -> path.getFileName().toString())
                .map(name -> name.substring(0, name.length() - ".json".length()))
                .toList();
    }

    private static URL resourceUrl(String path) {
        return ItemDefinitionConsistencyTest.class.getClassLoader().getResource(path);
    }

    private static JsonElement loadJson(String path) throws IOException {
        try (InputStream stream = ItemDefinitionConsistencyTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        }
    }

    private static JsonObject loadObject(String path) throws IOException {
        return loadJson(path).getAsJsonObject();
    }
}
