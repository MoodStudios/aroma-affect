package com.ovrtechnology.entity.nosesmith.neoforge;

import com.ovrtechnology.entity.nosesmith.NoseSmithAttributesRegistrar;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * NeoForge subscribes to the EntityAttributeCreationEvent via the mod event
 * bus. We use the static @SubscribeEvent annotation with the
 * {@link EventBusSubscriber.Bus#MOD} bus and register attributes lazily when
 * the event fires. {@link #registerNoseSmithAttributes()} is a no-op stub --
 * the actual wiring happens through the static event handler below.
 */
@EventBusSubscriber
public final class NoseSmithAttributesRegistrarNeoForgeImpl implements NoseSmithAttributesRegistrar {

    public NoseSmithAttributesRegistrarNeoForgeImpl() {}

    @Override
    public void registerNoseSmithAttributes() {
        // Registration is performed in onEntityAttributes below, triggered by
        // NeoForge's lifecycle. Calling this method only ensures the class is
        // loaded so the @EventBusSubscriber registrations take effect.
    }

    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        EntityType<NoseSmithEntity> type = NoseSmithRegistry.getNoseSmithType();
        if (type == null) {
            return;
        }
        event.put(type, NoseSmithEntity.createAttributes().build());
    }
}
