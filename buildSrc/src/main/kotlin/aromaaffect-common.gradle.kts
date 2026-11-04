import groovy.json.JsonOutput
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.nio.charset.StandardCharsets
import java.util.UUID

plugins {
    java
}

val modId = property("mod_id").toString()
val modName = property("mod_name").toString()
val modVersion = property("mod_version").toString()
val modAuthor = property("mod_author").toString()

// Node projects are named "<minecraft>-<loader>", e.g. "26.2-fabric"
val minecraftVersion = project.name.substringBefore('-')
val loader = project.name.substringAfter('-')
val javaVersion = if (minecraftVersion.substringBefore('.').toInt() >= 26) 25 else 21

fun dep(name: String): String = findProperty("deps.$name")?.toString() ?: ""

version = "$modVersion+MC$minecraftVersion"

// Pre-26 Minecraft builds also compile src/legacy: shims for the 26.x API plus per-file overrides.
// A file in src/legacy/java or src/legacy/resources replaces the shared file with the same path.
val legacy = minecraftVersion.startsWith("1.")
val legacyJava = rootProject.file("src/legacy/java")
val legacyResources = rootProject.file("src/legacy/resources")

fun overriddenBy(root: File) = Spec<FileTreeElement> { element ->
    !element.isDirectory &&
        !element.file.toPath().startsWith(root.toPath()) &&
        root.resolve(element.relativePath.pathString).isFile
}

if (legacy) {
    sourceSets.named("main") {
        java.srcDir(legacyJava)
        java.exclude(overriddenBy(legacyJava))
    }
}
group = property("group").toString()

base {
    archivesName = "$modId-$loader"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://maven.twelveiterations.com/repository/maven-public/") {
        name = "Twelve Iterations"
        content { includeGroup("net.blay09.mods") }
    }
    maven("https://maven.theillusivec4.top/") {
        name = "Theillusivec4 (Curios)"
        content { includeGroup("top.theillusivec4.curios") }
    }
    maven("https://maven.terraformersmc.com/releases/") {
        name = "TerraformersMC (Mod Menu)"
        content { includeGroup("com.terraformersmc") }
    }
    exclusiveContent {
        forRepository { maven("https://libraries.minecraft.net/") { name = "Minecraft" } }
        filter { includeGroupAndSubgroups("com.mojang") }
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    compileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("com.google.code.gson:gson:2.14.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testRuntimeOnly("org.slf4j:slf4j-api:2.0.17")
    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")
}

// Feedback backend config, injected at build time. The HMAC secret must not live in
// this repo: pass -PfeedbackHmacSecret=... or FEEDBACK_HMAC_SECRET for release builds.
// Dev builds default to an empty secret (feedback requests get a 401).
val feedbackEndpoint = (findProperty("feedbackEndpoint")
    ?: System.getenv("FEEDBACK_ENDPOINT")
    ?: "https://us-central1-ovr-aroma-affect-feedback-prod.cloudfunctions.net/feedback").toString()
val feedbackSecret = (findProperty("feedbackHmacSecret")
    ?: System.getenv("FEEDBACK_HMAC_SECRET")
    ?: "").toString()

fun javaEscape(s: String) =
    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

val generatedFeedbackDir = layout.buildDirectory.dir("generated/feedback")

val generateFeedbackConfig = tasks.register("generateFeedbackConfig") {
    inputs.property("endpoint", feedbackEndpoint)
    inputs.property("secret", feedbackSecret)
    inputs.property("modVersion", modVersion)
    inputs.property("mcVersion", minecraftVersion)
    outputs.dir(generatedFeedbackDir)
    doLast {
        if (feedbackSecret.isEmpty()) {
            logger.lifecycle("generateFeedbackConfig: no HMAC secret supplied; feedback requests from this build will be rejected by the backend.")
        }
        val pkgDir = generatedFeedbackDir.get().dir("com/ovrtechnology/menu").asFile
        pkgDir.mkdirs()
        pkgDir.resolve("FeedbackConfig.java").writeText(
            """
            |package com.ovrtechnology.menu;
            |
            |/** Generated at build time from Gradle properties / env vars. Do not edit or commit. */
            |public final class FeedbackConfig {
            |
            |    public static final String ENDPOINT = "${javaEscape(feedbackEndpoint)}";
            |
            |    public static final String HMAC_SECRET = "${javaEscape(feedbackSecret)}";
            |
            |    public static final String MOD_VERSION = "${javaEscape(modVersion)}";
            |
            |    public static final String MC_VERSION = "${javaEscape(minecraftVersion)}";
            |
            |    private FeedbackConfig() {}
            |}
            |""".trimMargin()
        )
    }
}

tasks.named<JavaCompile>("compileJava") {
    source(generateFeedbackConfig)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000", "-XDshould-stop.ifError=FLOW"))
}

tasks.named<Javadoc>("javadoc") {
    source(generateFeedbackConfig)
    // Mixin javadocs use custom tags the standard doclet does not know; the jar matters more.
    isFailOnError = false
    (options as StandardJavadocDocletOptions).apply {
        addBooleanOption("Xdoclint:none", true)
        addStringOption("quiet", "-quiet")
    }
}

val mcMatch = Regex("^(\\d+)\\.(\\d+)").find(minecraftVersion)
    ?: error("Unsupported Minecraft version format: $minecraftVersion")
val (mcMajor, mcMinor) = mcMatch.destructured

val expandProps = mapOf(
    "version" to version.toString(),
    "group" to group.toString(),
    "minecraft_version_range" to if (legacy) "[$minecraftVersion,$mcMajor.$mcMinor.2)" else "[$mcMajor.$mcMinor-,$mcMajor.${mcMinor.toInt() + 1})",
    "fabric_minecraft_version_range" to if (legacy) ">=$minecraftVersion <$mcMajor.$mcMinor.2" else "~$mcMajor.$mcMinor-",
    "fabric_api_version" to dep("fabric_api"),
    "fabric_loader_version" to dep("fabric_loader"),
    "mod_name" to modName,
    "mod_author" to modAuthor,
    "mod_id" to modId,
    "license" to property("license").toString(),
    "description" to description.orEmpty(),
    "neoforge_version" to dep("neoforge"),
    "credits" to property("credits").toString(),
    "java_version" to javaVersion.toString(),
    "homepage" to property("homepage").toString(),
    "issues" to property("issues").toString(),
    "sources" to property("sources").toString(),
    "balm_version" to dep("balm").substringBefore('+'),
    "curios_version" to dep("curios").substringBefore('+'),
    "access_widener" to if (minecraftVersion.startsWith("1.")) "$modId.accesswidener" else "$modId.classtweaker",
)
val jsonExpandProps = expandProps.mapValues { (_, value) -> value.replace("\n", "\\\\n") }

tasks.named<ProcessResources>("processResources") {
    inputs.properties(expandProps)

    if (legacy) {
        exclude(overriddenBy(legacyResources))
        from(legacyResources) {
            exclude("mixins.json")
        }
        inputs.dir(legacyResources).optional()
        val mixinOverrides = rootProject.file("src/legacy/mixins.json")
        inputs.file(mixinOverrides).optional()
        doLast {
            val out = destinationDir
            LegacyResources.convertRecipes(out.resolve("data"))
            LegacyResources.applyMixinOverrides(out.resolve("$modId.mixins.json"), mixinOverrides)
        }
    }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(expandProps)
    }
    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$modName" }
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Specification-Title" to modName,
            "Specification-Vendor" to modAuthor,
            "Specification-Version" to archiveVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to archiveVersion,
            "Implementation-Vendor" to modAuthor,
            "Built-On-Minecraft" to minecraftVersion,
            "MixinConfigs" to "$modId.mixins.json",
        )
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Tests read the shared sources under src/main/java, which live in the root project.
    workingDir = rootProject.projectDir

    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }

    jvmArgs("-Xmx512m", "-XX:+EnableDynamicAgentLoading")
    systemProperty("aromaaffect.legacy", legacy.toString())
}

tasks.register("prepareMultiplayerDev") {
    group = "aromaaffect"
    description = "Prepares dev server/client run directories for multiplayer testing."

    val runRoot = rootProject.file("run/${project.name}")
    val serversDat = rootProject.file("dev/multiplayer/servers.$loader.dat")
    val isFabric = loader == "fabric"
    val ownNames = if (isFabric) listOf("FabricDev1", "FabricDev2") else listOf("NeoForgeDev1", "NeoForgeDev2")
    val otherNames = if (isFabric) listOf("NeoForgeDev1", "NeoForgeDev2") else listOf("FabricDev1", "FabricDev2")
    val port = if (isFabric) 25565 else 25575
    val motd = if (isFabric) "AromaCraft Fabric Dev Server" else "AromaCraft NeoForge Dev Server"

    doLast {
        val serverDir = runRoot.resolve("dev-server")
        val clientDirs = listOf(runRoot.resolve("dev-client1"), runRoot.resolve("dev-client2"))

        serverDir.mkdirs()
        clientDirs.forEach {
            it.mkdirs()
            serversDat.copyTo(it.resolve("servers.dat"), overwrite = true)
        }

        serverDir.resolve("eula.txt").writeText("eula=true${System.lineSeparator()}")
        serverDir.resolve("server.properties").writeText(
            """
            |server-port=$port
            |online-mode=false
            |enforce-secure-profile=false
            |motd=$motd
            |pvp=false
            |allow-flight=true
            |view-distance=10
            |simulation-distance=8
            |""".trimMargin()
        )

        val ops = (ownNames + otherNames).map { name ->
            mapOf(
                "uuid" to UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(StandardCharsets.UTF_8)).toString(),
                "name" to name,
                "level" to 4,
                "bypassesPlayerLimit" to false,
            )
        }
        serverDir.resolve("ops.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(ops)))
    }
}
