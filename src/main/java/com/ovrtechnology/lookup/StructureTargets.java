package com.ovrtechnology.lookup;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/**
 * Resolves structure definition IDs to world structures.
 * <p>
 * A definition ID names either a single structure or a structure tag. Tags group
 * variants that players see as one structure, such as {@code minecraft:abandoned_camp},
 * which has a separate structure for each biome.
 */
public final class StructureTargets {

    private StructureTargets() {}

    /**
     * Returns the structure with the given ID, or every structure in the tag with
     * that ID. Returns an empty list if neither exists.
     */
    public static List<Holder<Structure>> resolve(Registry<Structure> registry, Identifier id) {
        var structure = registry.get(id);
        if (structure.isPresent()) {
            return List.of(structure.get());
        }
        return registry.get(TagKey.create(Registries.STRUCTURE, id))
                .map(tag -> tag.stream().toList())
                .orElse(List.of());
    }

    /**
     * Returns whether the ID names a structure or a non-empty structure tag.
     */
    public static boolean exists(Registry<Structure> registry, Identifier id) {
        return !resolve(registry, id).isEmpty();
    }
}
