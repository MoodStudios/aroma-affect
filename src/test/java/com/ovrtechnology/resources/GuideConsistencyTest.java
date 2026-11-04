package com.ovrtechnology.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Guide content consistency")
class GuideConsistencyTest {

    private static final String GUIDE_DIR = "data/aromaaffect/aroma/guide";
    private static final String LANG_PATH = "assets/aromaaffect/lang/en_us.json";
    private static final String ITEM_MODEL_DIR = "assets/aromaaffect/items/";
    private static final String MOD_PREFIX = "aromaaffect:";

    private static final List<String> ITEM_KEYS = List.of("item", "result");
    private static final List<String> ITEM_LIST_KEYS = List.of("grid", "items");

    @Test
    @DisplayName("every mod item shown in a guide should have an item model")
    void guideItemsShouldHaveModels() throws IOException, URISyntaxException {
        Map<String, Set<String>> usages = collect((guide, node, found) -> {
            for (String key : ITEM_KEYS) {
                String value = optionalString(node, key);
                if (value != null && value.startsWith(MOD_PREFIX)) {
                    record(found, guide, value);
                }
            }
            for (String key : ITEM_LIST_KEYS) {
                if (node.has(key) && node.get(key).isJsonArray()) {
                    for (JsonElement element : node.getAsJsonArray(key)) {
                        String value = element.isJsonPrimitive() ? element.getAsString() : null;
                        if (value != null && value.startsWith(MOD_PREFIX)) {
                            record(found, guide, value);
                        }
                    }
                }
            }
        });

        assertThat(usages).as("mod items referenced by guides").isNotEmpty();
        for (Map.Entry<String, Set<String>> usage : usages.entrySet()) {
            String modelPath = ITEM_MODEL_DIR + usage.getKey().substring(MOD_PREFIX.length()) + ".json";
            assertThat(resourceUrl(modelPath))
                    .as("item model %s for %s used by %s", modelPath, usage.getKey(), usage.getValue())
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("every translate key used by a guide should exist in en_us")
    void guideTranslationKeysShouldExist() throws IOException, URISyntaxException {
        Set<String> langKeys = loadObject(LANG_PATH).keySet();
        Map<String, Set<String>> usages = collect((guide, node, found) -> {
            String key = optionalString(node, "translate");
            if (key != null) {
                record(found, guide, key);
            }
        });

        assertThat(usages).as("translate keys referenced by guides").isNotEmpty();
        for (Map.Entry<String, Set<String>> usage : usages.entrySet()) {
            assertThat(langKeys)
                    .as("translation key '%s' used by %s", usage.getKey(), usage.getValue())
                    .contains(usage.getKey());
        }
    }

    @Test
    @DisplayName("every texture referenced by a guide should be bundled")
    void guideTexturesShouldExist() throws IOException, URISyntaxException {
        Map<String, Set<String>> usages = collect((guide, node, found) -> {
            String texture = optionalString(node, "texture");
            if (texture != null) {
                record(found, guide, texture);
            }
        });

        assertThat(usages).as("textures referenced by guides").isNotEmpty();
        for (Map.Entry<String, Set<String>> usage : usages.entrySet()) {
            String texture = usage.getKey();
            int colon = texture.indexOf(':');
            assertThat(colon).as("texture %s used by %s has a namespace", texture, usage.getValue()).isPositive();
            String path = "assets/" + texture.substring(0, colon) + "/" + texture.substring(colon + 1);
            assertThat(resourceUrl(path))
                    .as("texture %s used by %s", path, usage.getValue())
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("every guide page id should be unique across the book and every page link should resolve")
    void guidePageLinksShouldResolve() throws IOException, URISyntaxException {
        Map<String, String> pageOwners = new TreeMap<>();
        Map<String, Set<String>> links = new TreeMap<>();
        for (Path file : listJsonFiles(GUIDE_DIR)) {
            JsonObject guide = loadObject(file);
            String guideName = file.getFileName().toString();
            JsonArray pages = guide.getAsJsonArray("pages");
            assertThat(pages).as("pages in %s", guideName).isNotNull();
            for (JsonElement pageElement : pages) {
                String pageId = optionalString(pageElement.getAsJsonObject(), "id");
                assertThat(pageId).as("page id in %s", guideName).isNotNull();
                String previousOwner = pageOwners.putIfAbsent(pageId, guideName);
                assertThat(previousOwner)
                        .as("page id '%s' in %s already used by %s", pageId, guideName, previousOwner)
                        .isNull();
            }
            walk(guide, node -> {
                String target = optionalString(node, "target_page");
                if (target != null) {
                    record(links, guideName, target);
                }
            });
        }

        assertThat(links).as("page links in guides").isNotEmpty();
        for (Map.Entry<String, Set<String>> link : links.entrySet()) {
            assertThat(pageOwners)
                    .as("target_page '%s' used by %s", link.getKey(), link.getValue())
                    .containsKey(link.getKey());
        }
    }

    private interface Visitor {
        void visit(String guide, JsonObject node, Map<String, Set<String>> usages);
    }

    private static Map<String, Set<String>> collect(Visitor visitor) throws IOException, URISyntaxException {
        Map<String, Set<String>> usages = new TreeMap<>();
        for (Path file : listJsonFiles(GUIDE_DIR)) {
            String guideName = file.getFileName().toString();
            walk(loadObject(file), node -> visitor.visit(guideName, node, usages));
        }
        return usages;
    }

    private static void record(Map<String, Set<String>> usages, String guide, String value) {
        usages.computeIfAbsent(value, ignored -> new TreeSet<>()).add(guide);
    }

    private static void walk(JsonElement element, Consumer<JsonObject> visitor) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            visitor.accept(object);
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                walk(entry.getValue(), visitor);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                walk(child, visitor);
            }
        }
    }

    private static String optionalString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private static List<Path> listJsonFiles(String directory) throws IOException, URISyntaxException {
        URL url = resourceUrl(directory);
        assertThat(url).as("bundled directory %s", directory).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        try (Stream<Path> files = Files.list(Path.of(url.toURI()))) {
            return files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static URL resourceUrl(String path) {
        return GuideConsistencyTest.class.getClassLoader().getResource(path);
    }

    private static JsonObject loadObject(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject loadObject(String path) throws IOException {
        try (InputStream stream = GuideConsistencyTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
