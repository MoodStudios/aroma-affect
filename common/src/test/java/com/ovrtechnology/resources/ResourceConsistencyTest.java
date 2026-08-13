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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bundled resource consistency")
class ResourceConsistencyTest {

    @Test
    @DisplayName("scent container recipe should use a valid 26.2 crafting category")
    void scentContainerShouldUseValidCraftingCategory() throws IOException {
        JsonObject recipe = loadObject("data/aromaaffect/recipe/scent_container.json");

        assertThat(recipe.get("category").getAsString())
                .isIn("building", "redstone", "equipment", "misc");
    }

    @Test
    @DisplayName("every bundled sound subtitle should have an English translation")
    void soundSubtitlesShouldBeTranslated() throws IOException {
        JsonObject sounds = loadObject("assets/aromaaffect/sounds.json");
        JsonObject lang = loadObject("assets/aromaaffect/lang/en_us.json");

        for (String soundId : sounds.keySet()) {
            JsonObject sound = sounds.getAsJsonObject(soundId);
            if (sound.has("subtitle")) {
                String subtitleKey = sound.get("subtitle").getAsString();
                assertThat(lang.has(subtitleKey))
                        .as("translation for subtitle %s", subtitleKey)
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("every bundled nose ability should be defined")
    void noseAbilitiesShouldBeDefined() throws IOException {
        JsonArray definitions = loadObject("data/aromaaffect/aroma/abilities/abilities.json")
                .getAsJsonArray("abilities");
        Set<String> abilityIds = new HashSet<>();
        for (JsonElement definition : definitions) {
            abilityIds.add(definition.getAsJsonObject().get("id").getAsString());
        }

        JsonArray noses = loadObject("data/aromaaffect/aroma/noses/noses.json")
                .getAsJsonArray("noses");
        for (JsonElement noseElement : noses) {
            JsonObject nose = noseElement.getAsJsonObject();
            String noseId = nose.get("id").getAsString();
            JsonArray abilities = nose.getAsJsonObject("unlock").getAsJsonArray("abilities");
            for (JsonElement ability : abilities) {
                assertThat(abilityIds)
                        .as("registered abilities referenced by nose %s", noseId)
                        .contains(ability.getAsString());
            }
        }
    }

    @Test
    @DisplayName("custom nose should have a fallback model but no bundled recipe")
    void customNoseShouldRemainModpackInfrastructure() throws IOException, URISyntaxException {
        JsonObject itemModel = loadObject("assets/aromaaffect/items/custom_nose.json");
        assertThat(itemModel.getAsJsonObject("model").get("model").getAsString())
                .isEqualTo("aromaaffect:item/foragers_nose");

        JsonObject exampleVariant =
                loadObject("data/aromaaffect/aroma/nose_variants/example.json");
        assertThat(exampleVariant.get("example").getAsBoolean()).isTrue();

        JsonObject bundledGuide = loadObject("data/aromaaffect/aroma/guide/noses.json");
        assertThat(bundledGuide.toString()).doesNotContain("custom_nose");

        URL recipeDirectory =
                ResourceConsistencyTest.class
                        .getClassLoader()
                        .getResource("data/aromaaffect/recipe");
        assertThat(recipeDirectory).as("bundled recipe directory").isNotNull();
        assertThat(recipeDirectory.getProtocol()).isEqualTo("file");

        try (Stream<Path> recipes = Files.list(Path.of(recipeDirectory.toURI()))) {
            for (Path recipePath : recipes.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (InputStreamReader reader =
                        new InputStreamReader(Files.newInputStream(recipePath), StandardCharsets.UTF_8)) {
                    JsonObject recipe = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!recipe.has("result") || !recipe.get("result").isJsonObject()) {
                        continue;
                    }
                    JsonObject result = recipe.getAsJsonObject("result");
                    if (result.has("id")) {
                        assertThat(result.get("id").getAsString())
                                .as("bundled recipe result in %s", recipePath.getFileName())
                                .isNotEqualTo("aromaaffect:custom_nose");
                    }
                }
            }
        }
    }

    private static JsonObject loadObject(String path) throws IOException {
        try (InputStream stream =
                        ResourceConsistencyTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            try (InputStreamReader reader =
                    new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
