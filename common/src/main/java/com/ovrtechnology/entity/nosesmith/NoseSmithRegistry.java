package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Registry for the Nose Smith entity and associated items.
 * The Nose Smith is the central NPC for the Aroma Affect progression system.
 */
@UtilityClass
public final class NoseSmithRegistry {
    
    /**
     * Entity ID for the Nose Smith
     */
    public static final String NOSE_SMITH_ID = "nose_smith";
    
    /**
     * Spawn egg ID
     */
    public static final String NOSE_SMITH_SPAWN_EGG_ID = "nose_smith_spawn_egg";

    /**
     * Special Rose item ID
     */
    public static final String SPECIAL_ROSE_ID = "special_rose";

    /**
     * Iron Nose item ID
     */
    public static final String IRON_NOSE_ID = "iron_nose";
    
    /**
     * Deferred register for entity types
     */
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ENTITY_TYPE);
    
    /**
     * Deferred register for items (spawn egg)
     */
    private static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);
    
    /**
     * The Nose Smith entity type
     */
    @Getter
    public static final RegistrySupplier<EntityType<NoseSmithEntity>> NOSE_SMITH = 
            ENTITY_TYPES.register(NOSE_SMITH_ID, () -> EntityType.Builder
                    .of(NoseSmithEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F) // Same size as villager
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            Ids.mod(NOSE_SMITH_ID)
                    ))
            );
    
    /**
     * Spawn item for the Nose Smith
     * Works like a spawn egg but implemented as a custom item for 1.21 compatibility
     */
    @Getter
    public static final RegistrySupplier<Item> NOSE_SMITH_SPAWN_EGG =
            ITEMS.register(NOSE_SMITH_SPAWN_EGG_ID, () -> new NoseSmithSpawnItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .setId(ResourceKey.create(
                                    Registries.ITEM,
                                    Ids.mod(NOSE_SMITH_SPAWN_EGG_ID)
                            ))
            ));

    /**
     * Special Rose item — a rare gift from the Nose Smith
     */
    @Getter
    public static final RegistrySupplier<Item> SPECIAL_ROSE =
            ITEMS.register(SPECIAL_ROSE_ID, () -> new SpecialRoseItem(
                    new Item.Properties()
                            .stacksTo(16)
                            .rarity(Rarity.EPIC)
                            .setId(ResourceKey.create(
                                    Registries.ITEM,
                                    Ids.mod(SPECIAL_ROSE_ID)
                            ))
            ));

    /**
     * Iron Nose item — a collectible dropped by Iron Golems when given a Special Rose
     */
    @Getter
    public static final RegistrySupplier<Item> IRON_NOSE =
            ITEMS.register(IRON_NOSE_ID, () -> new Item(
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)
                            .setId(ResourceKey.create(
                                    Registries.ITEM,
                                    Ids.mod(IRON_NOSE_ID)
                            ))
            ));

    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Initialize the Nose Smith registry.
     * Must be called during mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseSmithRegistry.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing NoseSmithRegistry...");
        
        // Register entity types
        ENTITY_TYPES.register();
        
        // Register items (spawn egg)
        ITEMS.register();
        
        // Register entity attributes using Architectury's EntityAttributeRegistry
        EntityAttributeRegistry.register(NOSE_SMITH, NoseSmithEntity::createAttributes);
        
        initialized = true;
        AromaAffect.LOGGER.info("NoseSmithRegistry initialized successfully!");
    }
    
    /**
     * Get the ResourceLocation for the Nose Smith entity
     */
    public static ResourceLocation getNoseSmithLocation() {
        return Ids.mod(NOSE_SMITH_ID);
    }
}
