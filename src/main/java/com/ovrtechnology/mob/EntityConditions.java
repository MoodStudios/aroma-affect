package com.ovrtechnology.mob;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.mixin.WolfAccessor;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Checks a JSON condition object against the current state of an entity.
 *
 * <p>Every key is optional and all present keys must match:</p>
 * <ul>
 *   <li>{@code wet}: in water or rain. A wolf stays wet until it has shaken off.</li>
 *   <li>{@code glowing}: has the glowing outline, from the effect or a spectral arrow.</li>
 *   <li>{@code on_fire}: is burning.</li>
 *   <li>{@code baby}: is a baby.</li>
 *   <li>{@code tamed}: is a tamed animal.</li>
 *   <li>{@code owned_by_player}: is owned by the player who smells it.</li>
 *   <li>{@code components}: data components the entity must carry, as
 *       {@code {"minecraft:sheep/color": "red"}}. Values are written the same way
 *       as in commands and data packs.</li>
 * </ul>
 *
 * <p>Uses only state the client also receives, so the same conditions work in
 * passive mode on the client and in event triggers on the server.</p>
 */
public final class EntityConditions {

    private EntityConditions() {}

    public static boolean matches(@Nullable JsonObject conditions, Entity entity, @Nullable Player player) {
        if (conditions == null || conditions.isEmpty()) return true;
        if (entity == null) return false;

        for (Map.Entry<String, JsonElement> entry : conditions.entrySet()) {
            if (!matches(entry.getKey(), entry.getValue(), entity, player)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(String key, JsonElement expected, Entity entity, @Nullable Player player) {
        return switch (key) {
            case "wet" -> isWet(entity) == expected.getAsBoolean();
            case "glowing" -> entity.isCurrentlyGlowing() == expected.getAsBoolean();
            case "on_fire" -> entity.isOnFire() == expected.getAsBoolean();
            case "baby" -> (entity instanceof AgeableMob mob && mob.isBaby()) == expected.getAsBoolean();
            case "tamed" -> (entity instanceof TamableAnimal animal && animal.isTame()) == expected.getAsBoolean();
            case "owned_by_player" -> isOwnedBy(entity, player) == expected.getAsBoolean();
            case "components" -> expected.isJsonObject() && matchesComponents(expected.getAsJsonObject(), entity);
            default -> {
                AromaAffect.LOGGER.warn("Unknown entity condition '{}'; it never matches", key);
                yield false;
            }
        };
    }

    private static boolean isWet(Entity entity) {
        if (entity.isInWaterOrRain()) return true;
        return entity instanceof Wolf wolf && ((WolfAccessor) wolf).aromaaffect$isWet();
    }

    private static boolean isOwnedBy(Entity entity, @Nullable Player player) {
        return player != null && entity instanceof OwnableEntity ownable
                && ownable.getOwnerReference() != null
                && ownable.getOwnerReference().getUUID().equals(player.getUUID());
    }

    private static boolean matchesComponents(JsonObject components, Entity entity) {
        for (Map.Entry<String, JsonElement> entry : components.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            DataComponentType<?> type = id == null ? null : BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
            if (type == null) {
                AromaAffect.LOGGER.warn("Unknown data component '{}' in entity condition", entry.getKey());
                return false;
            }
            JsonElement actual = encode(type, entity);
            if (actual == null || !sameValue(entry.getValue(), actual)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static <T> JsonElement encode(DataComponentType<T> type, Entity entity) {
        T value = entity.get(type);
        Codec<T> codec = type.codec();
        if (value == null || codec == null) return null;
        return codec.encodeStart(entity.registryAccess().createSerializationContext(JsonOps.INSTANCE), value)
                .result()
                .orElse(null);
    }

    /** Compares two values, treating {@code "red"} and {@code "minecraft:red"} as the same ID. */
    private static boolean sameValue(JsonElement expected, JsonElement actual) {
        if (expected.equals(actual)) return true;
        if (expected.isJsonPrimitive() && actual.isJsonPrimitive()) {
            Identifier a = Identifier.tryParse(expected.getAsString());
            Identifier b = Identifier.tryParse(actual.getAsString());
            return a != null && a.equals(b);
        }
        return false;
    }
}
