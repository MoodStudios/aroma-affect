package com.ovrtechnology.compat;

import com.ovrtechnology.definition.Definition;
import com.ovrtechnology.menu.FeedbackConfig;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

// Single answer to "does this content exist in the running Minecraft version?"
// Definitions can declare "min_minecraft" in their JSON; registry-backed content is checked lazily
// at use time so modded blocks/biomes registered after us are still seen.
public final class ContentAvailability {

    private static final int[] RUNNING_VERSION = parseVersion(FeedbackConfig.MC_VERSION);
    private static final Set<String> SKIPPED_IDS = ConcurrentHashMap.newKeySet();

    private ContentAvailability() {}

    // True when the definition targets this Minecraft version; skipped ids are remembered so
    // references to them (nose unlocks) can be dropped quietly instead of logged as invalid
    public static boolean isAvailable(@Nullable Definition definition) {
        if (definition == null) {
            return true;
        }
        if (meetsMinVersion(definition.getMinMinecraft())) {
            return true;
        }
        if (definition.getId() != null) {
            SKIPPED_IDS.add(definition.getId());
        }
        return false;
    }

    public static boolean isSkipped(String id) {
        return SKIPPED_IDS.contains(id);
    }

    public static boolean meetsMinVersion(@Nullable String minVersion) {
        if (minVersion == null || minVersion.isBlank()) {
            return true;
        }
        return compareVersions(RUNNING_VERSION, parseVersion(minVersion)) >= 0;
    }

    public static boolean blockExists(@Nullable String id) {
        Identifier location = id != null ? Identifier.tryParse(id) : null;
        return location != null && BuiltInRegistries.BLOCK.containsKey(location);
    }

    // Biomes are synced to clients, so this also works client-side in multiplayer
    public static boolean biomeExists(@Nullable RegistryAccess registryAccess, @Nullable String id) {
        Identifier location = id != null ? Identifier.tryParse(id) : null;
        if (location == null) {
            return false;
        }
        if (registryAccess == null) {
            return true;
        }
        return registryAccess.lookupOrThrow(Registries.BIOME).containsKey(location);
    }

    static int[] parseVersion(String version) {
        String[] parts = version.split("[^0-9]+");
        int[] result = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            if (!part.isEmpty()) {
                result[count++] = Integer.parseInt(part);
            }
        }
        int[] trimmed = new int[count];
        System.arraycopy(result, 0, trimmed, 0, count);
        return trimmed;
    }

    static int compareVersions(int[] a, int[] b) {
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }
}
