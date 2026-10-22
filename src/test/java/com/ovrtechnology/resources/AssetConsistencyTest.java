package com.ovrtechnology.resources;

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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Asset consistency")
class AssetConsistencyTest {

    private static final String ASSETS_ROOT = "assets/aromaaffect/";
    private static final String MOD_PREFIX = "aromaaffect:";
    private static final String MIXIN_CONFIG = "aromaaffect.mixins.json";
    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    @Test
    @DisplayName("every sound in sounds.json should point to a bundled ogg file")
    void soundsShouldPointToBundledFiles() throws IOException {
        JsonObject sounds = loadObject(ASSETS_ROOT + "sounds.json");
        assertThat(sounds.keySet()).as("sound events").isNotEmpty();

        for (String event : sounds.keySet()) {
            JsonElement entries = sounds.getAsJsonObject(event).get("sounds");
            assertThat(entries).as("sounds list for event %s", event).isNotNull();
            assertThat(entries.isJsonArray()).as("sounds list for event %s is an array", event).isTrue();
            assertThat(entries.getAsJsonArray()).as("sounds for event %s", event).isNotEmpty();
            for (JsonElement entry : entries.getAsJsonArray()) {
                String name = entry.isJsonPrimitive() ? entry.getAsString() : entry.getAsJsonObject().get("name").getAsString();
                assertThat(name).as("sound name in event %s", event).startsWith(MOD_PREFIX);
                String oggPath = ASSETS_ROOT + "sounds/" + name.substring(MOD_PREFIX.length()) + ".ogg";
                assertThat(resourceUrl(oggPath)).as("ogg %s for event %s", oggPath, event).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("mixin config should list exactly the mixin classes present in the source tree")
    void mixinConfigShouldMatchSourceTree() throws IOException {
        JsonObject config = loadObject(MIXIN_CONFIG);
        String mixinPackage = config.get("package").getAsString();
        Path packageDir = SOURCE_ROOT.resolve(mixinPackage.replace('.', '/'));
        assertThat(Files.isDirectory(packageDir)).as("mixin package directory %s", packageDir.toAbsolutePath()).isTrue();

        Set<String> configured = new TreeSet<>();
        for (String section : List.of("client", "mixins", "server")) {
            if (!config.has(section)) {
                continue;
            }
            for (JsonElement entry : config.getAsJsonArray(section)) {
                String name = entry.getAsString();
                assertThat(configured.add(name)).as("mixin %s listed once", name).isTrue();
                Path source = packageDir.resolve(name.replace('.', '/') + ".java");
                assertThat(Files.isRegularFile(source))
                        .as("source file %s for mixin %s", source.toAbsolutePath(), name)
                        .isTrue();
            }
        }
        assertThat(configured).as("configured mixins").isNotEmpty();

        Set<String> present = new TreeSet<>();
        try (Stream<Path> files = Files.walk(packageDir)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (content.contains("@Mixin(")) {
                    String relative = packageDir.relativize(file).toString().replace('\\', '/');
                    present.add(relative.substring(0, relative.length() - ".java".length()).replace('/', '.'));
                }
            }
        }
        assertThat(configured)
                .as("mixins declared in %s vs @Mixin classes under %s", MIXIN_CONFIG, mixinPackage)
                .containsExactlyInAnyOrderElementsOf(present);
    }

    @Test
    @DisplayName("every mod model and texture referenced by item definitions, blockstates and models should be bundled")
    void modelChainShouldResolve() throws IOException, URISyntaxException {
        Map<String, Set<String>> modelRefs = new TreeMap<>();
        Map<String, Set<String>> textureRefs = new TreeMap<>();

        for (String directory : List.of("items", "blockstates")) {
            for (Path file : listJsonFiles(ASSETS_ROOT + directory)) {
                walk(loadObject(file), (key, value) -> {
                    if (key.equals("model") && value.startsWith(MOD_PREFIX)) {
                        record(modelRefs, value, directory + "/" + file.getFileName());
                    }
                });
            }
        }
        for (Path file : listJsonFilesRecursively(ASSETS_ROOT + "models")) {
            String owner = "models/" + file.getFileName();
            JsonObject model = loadObject(file);
            String parent = model.has("parent") ? model.get("parent").getAsString() : null;
            if (parent != null && parent.startsWith(MOD_PREFIX)) {
                record(modelRefs, parent, owner);
            }
            if (model.has("textures")) {
                for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                    String value = texture.getValue().getAsString();
                    if (value.startsWith(MOD_PREFIX)) {
                        record(textureRefs, value, owner);
                    }
                }
            }
        }

        assertThat(modelRefs).as("mod model references").isNotEmpty();
        assertThat(textureRefs).as("mod texture references").isNotEmpty();
        for (Map.Entry<String, Set<String>> ref : modelRefs.entrySet()) {
            String path = ASSETS_ROOT + "models/" + ref.getKey().substring(MOD_PREFIX.length()) + ".json";
            assertThat(resourceUrl(path)).as("model %s used by %s", path, ref.getValue()).isNotNull();
        }
        for (Map.Entry<String, Set<String>> ref : textureRefs.entrySet()) {
            String path = ASSETS_ROOT + "textures/" + ref.getKey().substring(MOD_PREFIX.length()) + ".png";
            assertThat(resourceUrl(path)).as("texture %s used by %s", path, ref.getValue()).isNotNull();
        }
    }

    private interface StringVisitor {
        void visit(String key, String value);
    }

    private static void walk(JsonElement element, StringVisitor visitor) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    visitor.visit(entry.getKey(), entry.getValue().getAsString());
                } else {
                    walk(entry.getValue(), visitor);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                walk(child, visitor);
            }
        }
    }

    private static void record(Map<String, Set<String>> refs, String value, String owner) {
        refs.computeIfAbsent(value, ignored -> new TreeSet<>()).add(owner);
    }

    private static List<Path> listJsonFiles(String directory) throws IOException, URISyntaxException {
        URL url = resourceUrl(directory);
        assertThat(url).as("bundled directory %s", directory).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        try (Stream<Path> files = Files.list(Path.of(url.toURI()))) {
            return files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static List<Path> listJsonFilesRecursively(String directory) throws IOException, URISyntaxException {
        URL url = resourceUrl(directory);
        assertThat(url).as("bundled directory %s", directory).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        try (Stream<Path> files = Files.walk(Path.of(url.toURI()))) {
            return files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static URL resourceUrl(String path) {
        return AssetConsistencyTest.class.getClassLoader().getResource(path);
    }

    private static JsonObject loadObject(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject loadObject(String path) throws IOException {
        try (InputStream stream = AssetConsistencyTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
