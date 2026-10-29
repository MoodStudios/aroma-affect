package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.compat.BalmCompat;
import com.ovrtechnology.compat.RegistrationCompat;
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
        noseSmith = BalmCompat.holder(entityTypes.register(NOSE_SMITH_ID, id -> RegistrationCompat.build(EntityType.Builder
                .of((EntityType.EntityFactory<NoseSmithEntity>) NoseSmithEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10), id)));
    }

    public static void registerItems(BalmRegistrar.Scoped<Item> items) {
        noseSmithSpawnEgg = BalmCompat.holder(items.register(NOSE_SMITH_SPAWN_EGG_ID, id -> new NoseSmithSpawnItem(
                RegistrationCompat.itemProperties(id)
                        .stacksTo(64))));

        specialRose = BalmCompat.holder(items.register(SPECIAL_ROSE_ID, id -> new SpecialRoseItem(
                RegistrationCompat.itemProperties(id)
                        .stacksTo(16)
                        .rarity(Rarity.EPIC))));

        ironNose = BalmCompat.holder(items.register(IRON_NOSE_ID, id -> new Item(
                RegistrationCompat.itemProperties(id)
                        .stacksTo(1)
                        .rarity(Rarity.RARE))));

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
     * Delegates to the platform-specific attribute registrar resolved through
     * {@link NoseSmithAttributesRegistrar#INSTANCE}.
     */
    public static void registerAttributes() {
        NoseSmithAttributesRegistrar.INSTANCE.registerNoseSmithAttributes();
    }

    public static Identifier getNoseSmithLocation() {
        return Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, NOSE_SMITH_ID);
    }
}
