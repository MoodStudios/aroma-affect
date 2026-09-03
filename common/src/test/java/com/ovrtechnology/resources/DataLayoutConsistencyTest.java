package com.ovrtechnology.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.category.CategoryDefinitionLoader;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.nose.NoseDefinitionLoader;
import com.ovrtechnology.scent.ScentDefinitionLoader;
import com.ovrtechnology.scentitem.ScentItemDefinitionLoader;
import com.ovrtechnology.sniffernose.SnifferNoseDefinitionLoader;
import com.ovrtechnology.sniffer.loot.SnifferLootRegistry;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.event.EventDefinitionLoader;
import com.ovrtechnology.variant.NoseVariantRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Data layout consistency")
class DataLayoutConsistencyTest {

    private static final String DATA_ROOT = "data/aromaaffect/";

    private static final List<String> INDEXED_DIRECTORIES = List.of(
            BiomeDefinitionLoader.BIOMES_DIR,
            BlockDefinitionLoader.BLOCKS_DIR,
            CategoryDefinitionLoader.CATEGORY_DIR,
            FlowerDefinitionLoader.FLOWERS_DIR,
            MobDefinitionLoader.MOBS_DIR,
            ScentDefinitionLoader.SCENTS_DIR,
            StructureDefinitionLoader.STRUCTURES_DIR,
            EventDefinitionLoader.EVENTS_DIR,
            SnifferLootRegistry.RULES_DIR
    );

    private static final List<String> UNINDEXED_DIRECTORIES = List.of(
            NoseVariantRegistry.VARIANTS_DIR
    );

    private static final List<String> SINGLE_FILE_RESOURCES = List.of(
            ScentItemDefinitionLoader.SCENT_ITEMS_RESOURCE_PATH,
            NoseDefinitionLoader.NOSES_RESOURCE_PATH,
            SnifferNoseDefinitionLoader.SNIFFER_NOSES_RESOURCE_PATH,
            ScentTriggerConfigLoader.ITEM_TRIGGERS_PATH,
            ScentTriggerConfigLoader.SETTINGS_PATH,
            AbilityDefinitionLoader.ABILITY_PATH
    );

    @Test
    @DisplayName("every loader directory should exist in bundled resources")
    void loaderDirectoriesShouldExist() {
        Stream.concat(INDEXED_DIRECTORIES.stream(), UNINDEXED_DIRECTORIES.stream())
                .forEach(directory -> assertThat(resourceUrl(DATA_ROOT + directory))
                        .as("bundled directory %s%s", DATA_ROOT, directory)
                        .isNotNull());
    }

    @Test
    @DisplayName("every single-file loader path should resolve to a bundled JSON resource")
    void singleFileResourcesShouldExist() throws IOException {
        for (String path : SINGLE_FILE_RESOURCES) {
            assertThat(path).as("loader path %s is rooted at the data directory", path).startsWith(DATA_ROOT);
            assertThat(resourceUrl(path)).as("bundled resource %s", path).isNotNull();
            assertThat(loadJson(path)).as("parsable JSON at %s", path).isNotNull();
        }
    }

    @Test
    @DisplayName("every indexed directory should have an index matching its files exactly")
    void indexesShouldMatchDirectoryContents() throws IOException, URISyntaxException {
        for (String directory : INDEXED_DIRECTORIES) {
            int split = directory.lastIndexOf('/');
            String parent = split >= 0 ? directory.substring(0, split + 1) : "";
            String dirName = split >= 0 ? directory.substring(split + 1) : directory;
            String indexPath = DATA_ROOT + parent + "_indexes/" + dirName + ".json";

            JsonElement index = loadJson(indexPath);
            assertThat(index).as("index %s", indexPath).isNotNull();
            assertThat(index.isJsonArray()).as("index %s is a JSON array", indexPath).isTrue();

            Set<String> indexed = new HashSet<>();
            for (JsonElement name : index.getAsJsonArray()) {
                indexed.add(name.getAsString());
            }

            assertThat(indexed)
                    .as("index %s entries vs files in %s%s", indexPath, DATA_ROOT, directory)
                    .containsExactlyInAnyOrderElementsOf(listJsonStems(DATA_ROOT + directory));
        }
    }

    @Test
    @DisplayName("every mask reference in scent and category definitions should resolve to a bundled texture")
    void maskReferencesShouldResolve() throws IOException, URISyntaxException {
        for (String directory : List.of(ScentDefinitionLoader.SCENTS_DIR, CategoryDefinitionLoader.CATEGORY_DIR)) {
            for (String stem : listJsonStems(DATA_ROOT + directory)) {
                String definitionPath = DATA_ROOT + directory + "/" + stem + ".json";
                JsonObject definition = loadJson(definitionPath).getAsJsonObject();
                if (!definition.has("mask")) {
                    continue;
                }
                String mask = definition.get("mask").getAsString();
                int colon = mask.indexOf(':');
                assertThat(colon).as("mask %s in %s has a namespace", mask, definitionPath).isPositive();
                String texturePath = "assets/" + mask.substring(0, colon) + "/" + mask.substring(colon + 1) + ".png";
                assertThat(resourceUrl(texturePath))
                        .as("texture %s referenced by %s", texturePath, definitionPath)
                        .isNotNull();
            }
        }
    }

    @Test
    @DisplayName("every mask texture should be 160 wide with a height that is a multiple of 90")
    void maskTexturesShouldMatchFrameGrid() throws IOException, URISyntaxException {
        URL maskDirectory = resourceUrl("assets/aromaaffect/textures/mask");
        assertThat(maskDirectory).as("bundled mask texture directory").isNotNull();
        assertThat(maskDirectory.getProtocol()).isEqualTo("file");

        try (Stream<Path> files = Files.walk(Path.of(maskDirectory.toURI()))) {
            List<Path> textures = files.filter(path -> path.toString().endsWith(".png")).toList();
            assertThat(textures).as("bundled mask textures").isNotEmpty();
            for (Path texture : textures) {
                BufferedImage image = ImageIO.read(texture.toFile());
                assertThat(image).as("readable image %s", texture.getFileName()).isNotNull();
                assertThat(image.getWidth()).as("width of %s", texture.getFileName()).isEqualTo(160);
                assertThat(image.getHeight() % 90).as("height of %s is a multiple of 90", texture.getFileName()).isZero();
                assertThat(image.getHeight()).as("height of %s", texture.getFileName()).isGreaterThanOrEqualTo(90);
            }
        }
    }

    @Test
    @DisplayName("source code should only reference lowercase data paths and the current mask directory")
    void sourcePathLiteralsShouldBeConsistent() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        assertThat(Files.isDirectory(sourceRoot)).as("source root %s", sourceRoot.toAbsolutePath()).isTrue();

        Pattern dataLiteral = Pattern.compile("\"(data/[^\"]+)\"");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = dataLiteral.matcher(source);
                while (matcher.find()) {
                    assertThat(matcher.group(1))
                            .as("data path literal in %s", file.getFileName())
                            .startsWith(DATA_ROOT);
                }
                assertThat(source)
                        .as("no references to removed textures/masks directory in %s", file.getFileName())
                        .doesNotContain("textures/masks/");
            }
        }
    }

    private static URL resourceUrl(String path) {
        return DataLayoutConsistencyTest.class.getClassLoader().getResource(path);
    }

    private static Set<String> listJsonStems(String classpathDirectory) throws IOException, URISyntaxException {
        URL url = resourceUrl(classpathDirectory);
        assertThat(url).as("bundled directory %s", classpathDirectory).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        Set<String> stems = new HashSet<>();
        try (Stream<Path> files = Files.list(Path.of(url.toURI()))) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".json")) {
                    stems.add(name.substring(0, name.length() - ".json".length()));
                }
            }
        }
        return stems;
    }

    private static JsonElement loadJson(String path) throws IOException {
        try (InputStream stream =
                        DataLayoutConsistencyTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        }
    }
}
