package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.AromaAffect;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Registry for the Nose Smith entity and associated items.
 *
 * <p>Phase 4 splits the original {@code init()} into two registrar callbacks
 * ({@link #registerEntities(BalmRegistrar.Scoped)} and
 * {@link #registerItems(BalmRegistrar.Scoped)}) plus an attribute registration
 * hook ({@link #registerAttributes()}) that is wired through a platform proxy
 * in phase 7.</p>
 */
@UtilityClass
public final class NoseSmithRegistry {

    public static final String NOSE_SMITH_ID = "nose_smith";
    public static final String NOSE_SMITH_SPAWN_EGG_ID = "nose_smith_spawn_egg";
    public static final String SPECIAL_ROSE_ID = "special_rose";
    public static final String IRON_NOSE_ID = "iron_nose";

    @Getter
    private static Holder<EntityType<?>> noseSmith;

    @Getter
    private static Holder<Item> noseSmithSpawnEgg;

    @Getter
    private static Holder<Item> specialRose;

    @Getter
    private static Holder<Item> ironNose;

    @Getter
    private static boolean initialized = false;

    @SuppressWarnings("unchecked")
    public static void registerEntities(BalmRegistrar.Scoped<EntityType<?>> entityTypes) {
        noseSmith = entityTypes.register(NOSE_SMITH_ID, id -> EntityType.Builder
                .of((EntityType.EntityFactory<NoseSmithEntity>) NoseSmithEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    public static void registerItems(BalmRegistrar.Scoped<Item> items) {
        noseSmithSpawnEgg = items.register(NOSE_SMITH_SPAWN_EGG_ID, id -> new NoseSmithSpawnItem(
                new Item.Properties()
                        .stacksTo(64)
                        .setId(ResourceKey.create(Registries.ITEM, id))));

        specialRose = items.register(SPECIAL_ROSE_ID, id -> new SpecialRoseItem(
                new Item.Properties()
                        .stacksTo(16)
                        .rarity(Rarity.EPIC)
                        .setId(ResourceKey.create(Registries.ITEM, id))));

        ironNose = items.register(IRON_NOSE_ID, id -> new Item(
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE)
                        .setId(ResourceKey.create(Registries.ITEM, id))));

        initialized = true;
        AromaAffect.LOGGER.info("NoseSmithRegistry items registered");
    }

    /**
     * Bridge for entity-attribute registration. Wired into platform code via a
     * Balm.platformProxy in phase 7.
     */
    @SuppressWarnings("unchecked")
    public static EntityType<NoseSmithEntity> getNoseSmithType() {
        return noseSmith != null && noseSmith.isBound() ? (EntityType<NoseSmithEntity>) noseSmith.value() : null;
    }

    /**
     * Phase 7 platform proxy hook will call this after attributes are wired.
     */
    public static void registerAttributes() {
        // No-op stub; the actual registration is platform-specific and gets
        // injected via NoseSmithAttributesRegistrar in phase 7.
    }

    public static Identifier getNoseSmithLocation() {
        return Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, NOSE_SMITH_ID);
    }
}
