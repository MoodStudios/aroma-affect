import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

// Only one NeoForge node may decompile/recompile Minecraft at a time, otherwise
// parallel builds of several versions run out of memory.
interface NeoForgeMutex : BuildService<BuildServiceParameters.None>

val mutex = gradle.sharedServices.registerIfAbsent("createMinecraftArtifactsMutex", NeoForgeMutex::class.java) {
    maxParallelUsages.set(1)
}

tasks.named { it == "createMinecraftArtifacts" }.configureEach {
    usesService(mutex)
}
