plugins {
    id("net.neoforged.moddev") version "2.0.147"
    id("neoforge-mutex")
    id("aromaaffect-common")
}

val modId = property("mod_id").toString()
val balmVersion = property("deps.balm").toString()
val nodeName = project.name

sourceSets.main {
    java.exclude("**/fabric/**")
    resources.exclude("fabric.mod.json")
}

dependencies {
    implementation("net.blay09.mods:balm-neoforge:$balmVersion") {
        isChanging = balmVersion.endsWith("SNAPSHOT")
    }

    // CURIOS DISABLED: noses fall back to the vanilla HEAD slot. Restore together with
    // the optional dependency block in neoforge.mods.toml.
    // implementation("top.theillusivec4.curios:curios-neoforge:${property("deps.curios")}")
}

neoForge {
    version = property("deps.neoforge").toString()
    accessTransformers.from(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        val runRoot = rootProject.file("run/$nodeName")

        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            ideName = "NeoForge ${name.replaceFirstChar { it.uppercase() }} ($nodeName)"
        }
        register("client") {
            client()
            gameDirectory = runRoot.resolve("client")
        }
        register("server") {
            server()
            gameDirectory = runRoot.resolve("server")
        }
        register("serverDev") {
            server()
            gameDirectory = runRoot.resolve("dev-server")
        }
        register("clientDev1") {
            client()
            gameDirectory = runRoot.resolve("dev-client1")
            programArguments.addAll("--username", "NeoForgeDev1")
        }
        register("clientDev2") {
            client()
            gameDirectory = runRoot.resolve("dev-client2")
            programArguments.addAll("--username", "NeoForgeDev2")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
