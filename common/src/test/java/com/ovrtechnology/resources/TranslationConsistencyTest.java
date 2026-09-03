package com.ovrtechnology.resources;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Translation consistency")
class TranslationConsistencyTest {

    private static final String LANG_DIR = "assets/aromaaffect/lang";
    private static final String REFERENCE_LANG = "en_us.json";

    private static final Pattern TRANSLATABLE_LITERAL =
            Pattern.compile("Component\\.translatable\\(\\s*\"([^\"]+)\"\\s*[,)]");

    @Test
    @DisplayName("every bundled language file should have exactly the same keys as en_us")
    void languageFilesShouldMatchReferenceKeys() throws IOException, URISyntaxException {
        List<Path> langFiles = listJsonFiles(LANG_DIR);
        Path reference = langFiles.stream()
                .filter(path -> path.getFileName().toString().equals(REFERENCE_LANG))
                .findFirst()
                .orElseThrow();
        Set<String> referenceKeys = loadObject(reference).keySet();
        assertThat(referenceKeys).as("keys in %s", REFERENCE_LANG).isNotEmpty();
        assertThat(langFiles).as("bundled language files").hasSizeGreaterThan(1);

        for (Path langFile : langFiles) {
            if (langFile.equals(reference)) {
                continue;
            }
            Set<String> keys = loadObject(langFile).keySet();
            assertThat(missingFrom(referenceKeys, keys))
                    .as("keys missing from %s", langFile.getFileName())
                    .isEmpty();
            assertThat(missingFrom(keys, referenceKeys))
                    .as("keys in %s that do not exist in %s", langFile.getFileName(), REFERENCE_LANG)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("every mod translation key used literally in source code should exist in en_us")
    void sourceTranslationKeysShouldExist() throws IOException {
        Set<String> langKeys = loadObject(resourcePath(LANG_DIR + "/" + REFERENCE_LANG)).keySet();

        Path sourceRoot = Path.of("src/main/java");
        assertThat(Files.isDirectory(sourceRoot)).as("source root %s", sourceRoot.toAbsolutePath()).isTrue();

        Map<String, Set<String>> usages = new TreeMap<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = TRANSLATABLE_LITERAL.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!key.contains("aromaaffect")) {
                        continue;
                    }
                    usages.computeIfAbsent(key, ignored -> new TreeSet<>())
                            .add(sourceRoot.relativize(file).toString());
                }
            }
        }

        assertThat(usages).as("literal translation keys found in source").isNotEmpty();
        for (Map.Entry<String, Set<String>> usage : usages.entrySet()) {
            assertThat(langKeys)
                    .as("translation key '%s' used by %s", usage.getKey(), usage.getValue())
                    .contains(usage.getKey());
        }
    }

    private static List<String> missingFrom(Set<String> expected, Set<String> actual) {
        return new TreeSet<>(expected).stream().filter(key -> !actual.contains(key)).toList();
    }

    private static Path resourcePath(String path) {
        URL url = TranslationConsistencyTest.class.getClassLoader().getResource(path);
        assertThat(url).as("classpath resource %s", path).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Path> listJsonFiles(String directory) throws IOException, URISyntaxException {
        URL url = TranslationConsistencyTest.class.getClassLoader().getResource(directory);
        assertThat(url).as("bundled directory %s", directory).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        try (Stream<Path> files = Files.list(Path.of(url.toURI()))) {
            return files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static JsonObject loadObject(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
