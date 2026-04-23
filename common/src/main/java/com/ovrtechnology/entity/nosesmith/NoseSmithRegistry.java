package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.util.Ids;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

@UtilityClass
public final class NoseSmithRegistry {

    public static final String NOSE_SMITH_ID = "nose_smith";

    public static final String NOSE_SMITH_SPAWN_EGG_ID = "nose_smith_spawn_egg";

    public static final String SPECIAL_ROSE_ID = "special_rose";

    public static final String IRON_NOSE_ID = "iron_nose";

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ENTITY_TYPE);

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);

    @Getter
    public static final RegistrySupplier<EntityType<NoseSmithEntity>> NOSE_SMITH =
            ENTITY_TYPES.register(
                    NOSE_SMITH_ID,
                    () ->
                            EntityType.Builder.of(NoseSmithEntity::new, MobCategory.MISC)
                                    .sized(0.6F, 1.95F)
                                    .clientTrackingRange(10)
                                    .build(NOSE_SMITH_ID));

    @Getter
    public static final RegistrySupplier<Item> NOSE_SMITH_SPAWN_EGG =
            ITEMS.register(
                    NOSE_SMITH_SPAWN_EGG_ID,
                    () -> new NoseSmithSpawnItem(new Item.Properties().stacksTo(64)));

    @Getter
    public static final RegistrySupplier<Item> SPECIAL_ROSE =
            ITEMS.register(
                    SPECIAL_ROSE_ID,
                    () ->
                            new SpecialRoseItem(
                                    new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    @Getter
    public static final RegistrySupplier<Item> IRON_NOSE =
            ITEMS.register(
                    IRON_NOSE_ID,
                    () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    @Getter private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseSmithRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing NoseSmithRegistry...");

        ENTITY_TYPES.register();

        ITEMS.register();

        EntityAttributeRegistry.register(NOSE_SMITH, NoseSmithEntity::createAttributes);

        initialized = true;
        AromaAffect.LOGGER.info("NoseSmithRegistry initialized successfully!");
    }

    public static ResourceLocation getNoseSmithLocation() {
        return Ids.mod(NOSE_SMITH_ID);
    }
}
