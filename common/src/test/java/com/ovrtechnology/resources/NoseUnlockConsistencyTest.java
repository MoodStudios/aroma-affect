package com.ovrtechnology.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.nose.NoseDefinitionLoader;
import com.ovrtechnology.sniffernose.SnifferNoseDefinitionLoader;
import com.ovrtechnology.structure.StructureDefinitionLoader;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Nose unlock consistency")
class NoseUnlockConsistencyTest {

    private static final String DATA_ROOT = "data/aromaaffect/";

    private static final Map<String, String> UNLOCK_DIRECTORIES = Map.of(
            "biomes", BiomeDefinitionLoader.BIOMES_DIR,
            "blocks", BlockDefinitionLoader.BLOCKS_DIR,
            "flowers", FlowerDefinitionLoader.FLOWERS_DIR,
            "structures", StructureDefinitionLoader.STRUCTURES_DIR
    );

    private static final Map<String, String> FULLY_TRACKABLE_DIRECTORIES = Map.of(
            "flowers", FlowerDefinitionLoader.FLOWERS_DIR,
            "structures", StructureDefinitionLoader.STRUCTURES_DIR
    );

    private static final List<String> NETHER_BIOMES = List.of(
            "minecraft:nether_wastes",
            "minecraft:warped_forest",
            "minecraft:crimson_forest",
            "minecraft:soul_sand_valley",
            "minecraft:basalt_deltas"
    );

    private static final List<String> SPAWNER_MOBS = List.of(
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:spider",
            "minecraft:cave_spider",
            "minecraft:blaze",
            "minecraft:magma_cube",
            "minecraft:silverfish"
    );

    @Test
    @DisplayName("every nose unlock should reference a bundled definition")
    void unlockReferencesShouldResolve() throws IOException, URISyntaxException {
        List<JsonObject> noses = allNoses();
        assertThat(noses).as("bundled noses").isNotEmpty();

        for (Map.Entry<String, String> entry : UNLOCK_DIRECTORIES.entrySet()) {
            Set<String> definedIds = idsIn(entry.getValue());
            for (JsonObject nose : noses) {
                for (String id : unlockList(nose, entry.getKey())) {
                    assertThat(definedIds)
                            .as("%s '%s' unlocked by nose %s", entry.getKey(), id, noseId(nose))
                            .contains(id);
                }
            }
        }

        Set<String> noseIds = new HashSet<>();
        for (JsonObject nose : noses) {
            noseIds.add(noseId(nose));
        }
        for (JsonObject nose : noses) {
            for (String inherited : unlockList(nose, "noses")) {
                assertThat(noseIds)
                        .as("nose '%s' inherited by %s", inherited, noseId(nose))
                        .contains(inherited);
            }
        }
    }

    @Test
    @DisplayName("every flower and structure definition should be unlocked by a bundled nose")
    void flowersAndStructuresShouldBeTrackable() throws IOException, URISyntaxException {
        List<JsonObject> noses = allNoses();
        for (Map.Entry<String, String> entry : FULLY_TRACKABLE_DIRECTORIES.entrySet()) {
            Set<String> definedIds = idsIn(entry.getValue());
            assertThat(definedIds).as("definitions in %s", entry.getValue()).isNotEmpty();
            Set<String> unlockedIds = unlockedIds(noses, entry.getKey());
            for (String id : definedIds) {
                assertThat(unlockedIds)
                        .as("%s '%s' should be unlocked by at least one nose", entry.getKey(), id)
                        .contains(id);
            }
        }
    }

    @Test
    @DisplayName("every Nether biome should be defined and unlocked by a bundled nose")
    void netherBiomesShouldBeTrackable() throws IOException, URISyntaxException {
        Set<String> definedIds = idsIn(BiomeDefinitionLoader.BIOMES_DIR);
        Set<String> unlockedIds = unlockedIds(allNoses(), "biomes");
        for (String biome : NETHER_BIOMES) {
            assertThat(definedIds).as("biome definition for %s", biome).contains(biome);
            assertThat(unlockedIds).as("nose unlocking biome %s", biome).contains(biome);
        }
    }

    @Test
    @DisplayName("every vanilla spawner mob should have a mob definition")
    void spawnerMobsShouldHaveDefinitions() throws IOException, URISyntaxException {
        Set<String> mobIds = idsIn(MobDefinitionLoader.MOBS_DIR);
        for (String mob : SPAWNER_MOBS) {
            assertThat(mobIds).as("mob definition for spawner mob %s", mob).contains(mob);
        }
    }

    private static List<JsonObject> allNoses() throws IOException {
        List<JsonObject> noses = new ArrayList<>();
        for (JsonElement nose : wrappedArray(NoseDefinitionLoader.NOSES_RESOURCE_PATH, "noses")) {
            noses.add(nose.getAsJsonObject());
        }
        for (JsonElement nose : wrappedArray(SnifferNoseDefinitionLoader.SNIFFER_NOSES_RESOURCE_PATH, "sniffer_noses")) {
            noses.add(nose.getAsJsonObject());
        }
        return noses;
    }

    private static String noseId(JsonObject nose) {
        assertThat(nose.has("id")).as("nose has an id").isTrue();
        return nose.get("id").getAsString();
    }

    private static Set<String> unlockedIds(List<JsonObject> noses, String key) {
        Set<String> ids = new HashSet<>();
        for (JsonObject nose : noses) {
            ids.addAll(unlockList(nose, key));
        }
        return ids;
    }

    private static List<String> unlockList(JsonObject nose, String key) {
        List<String> ids = new ArrayList<>();
        if (!nose.has("unlock") || !nose.get("unlock").isJsonObject()) {
            return ids;
        }
        JsonObject unlock = nose.getAsJsonObject("unlock");
        if (!unlock.has(key) || !unlock.get(key).isJsonArray()) {
            return ids;
        }
        for (JsonElement element : unlock.getAsJsonArray(key)) {
            ids.add(element.getAsString());
        }
        return ids;
    }

    private static JsonArray wrappedArray(String path, String wrapperKey) throws IOException {
        JsonElement root;
        try (InputStream stream = NoseUnlockConsistencyTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader);
            }
        }
        JsonArray array = root.isJsonArray() ? root.getAsJsonArray() : root.getAsJsonObject().getAsJsonArray(wrapperKey);
        assertThat(array).as("array '%s' in %s", wrapperKey, path).isNotNull();
        return array;
    }

    private static Set<String> idsIn(String directory) throws IOException, URISyntaxException {
        Set<String> ids = new HashSet<>();
        for (Path file : listJsonFiles(DATA_ROOT + directory)) {
            JsonObject definition = loadObject(file);
            assertThat(definition.has("id")).as("id in %s/%s", directory, file.getFileName()).isTrue();
            ids.add(definition.get("id").getAsString());
        }
        return ids;
    }

    private static URL resourceUrl(String path) {
        return NoseUnlockConsistencyTest.class.getClassLoader().getResource(path);
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
