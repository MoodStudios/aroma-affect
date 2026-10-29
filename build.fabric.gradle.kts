plugins {
    id("dev.kikugie.loom-back-compat")
    id("aromaaffect-common")
}

val modId = property("mod_id").toString()
val balmVersion = property("deps.balm").toString()
val obfuscated = sc.current.version.startsWith("1.")
val accessFile = if (obfuscated) "$modId.accesswidener" else "$modId.classtweaker"

sourceSets.main {
    java.exclude("**/neoforge/**")
    resources.exclude(
        "META-INF/neoforge.mods.toml",
        "data/aromaaffect/curios/**",
        "data/curios/**",
        "assets/aromaaffect/textures/slot/**",
        if (obfuscated) "$modId.classtweaker" else "$modId.accesswidener",
    )
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("net.blay09.mods:balm-fabric:$balmVersion") {
        isChanging = balmVersion.endsWith("SNAPSHOT")
        exclude(group = "mezz.jei")
    }

    // Mod Menu is optional at runtime; it is only on the dev classpath so the
    // config button can be tried from the Fabric client run.
    modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}")
    modRuntimeOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

loom {
    accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/$accessFile"),
        "build/processed.$accessFile",
    )

    runConfigs {
        val runRoot = rootProject.file("run/${project.name}")

        configureEach {
            generateRunConfig = sc.current.isActive
            appendProjectPathToDisplayName = false
        }
        named("client") {
            displayName = "Fabric Client (${project.name})"
            runDirectory = runRoot.resolve("client")
        }
        named("server") {
            displayName = "Fabric Server (${project.name})"
            runDirectory = runRoot.resolve("server")
        }
        register("serverDev") {
            server()
            displayName = "Fabric Server Dev (${project.name}, multiplayer)"
            runDirectory = runRoot.resolve("dev-server")
        }
        register("clientDev1") {
            client()
            displayName = "Fabric Client Dev 1 (${project.name})"
            runDirectory = runRoot.resolve("dev-client1")
            programArguments.addAll("--username", "FabricDev1")
        }
        register("clientDev2") {
            client()
            displayName = "Fabric Client Dev 2 (${project.name})"
            runDirectory = runRoot.resolve("dev-client2")
            programArguments.addAll("--username", "FabricDev2")
        }
    }
}
