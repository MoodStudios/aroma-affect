package com.ovrtechnology.tutorial.oliver;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registry for the Tutorial Oliver entity.
 */
public final class TutorialOliverRegistry {

    public static final String TUTORIAL_OLIVER_ID = "tutorial_oliver";

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<TutorialOliverEntity>> TUTORIAL_OLIVER =
            ENTITY_TYPES.register(TUTORIAL_OLIVER_ID, () -> EntityType.Builder
                    .of(TutorialOliverEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, TUTORIAL_OLIVER_ID)
                    ))
            );

    private TutorialOliverRegistry() {
    }

    public static void init() {
        ENTITY_TYPES.register();

        // Register entity attributes
        EntityAttributeRegistry.register(TUTORIAL_OLIVER, TutorialOliverEntity::createAttributes);

        AromaAffect.LOGGER.debug("Tutorial Oliver entity registered");
    }
}
