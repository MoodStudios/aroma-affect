package com.ovrtechnology.entity.nosesmith;

import net.blay09.mods.balm.Balm;

/**
 * Cross-platform abstraction for registering entity attributes. Balm 26.1
 * does not ship a wrapper around Fabric's
 * {@code FabricDefaultAttributeRegistry} / NeoForge's
 * {@code EntityAttributeCreationEvent}, so we hand-roll one via
 * {@link Balm#platformProxy()}.
 */
public interface NoseSmithAttributesRegistrar {

    /**
     * Registers the {@link NoseSmithEntity} attribute supplier with the platform.
     */
    void registerNoseSmithAttributes();

    NoseSmithAttributesRegistrar INSTANCE = Balm.<NoseSmithAttributesRegistrar>platformProxy()
            .withFabric("com.ovrtechnology.entity.nosesmith.fabric.NoseSmithAttributesRegistrarFabricImpl")
            .withNeoForge("com.ovrtechnology.entity.nosesmith.neoforge.NoseSmithAttributesRegistrarNeoForgeImpl")
            .build();
}
