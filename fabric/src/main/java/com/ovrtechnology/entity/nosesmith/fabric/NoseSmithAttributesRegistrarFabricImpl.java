package com.ovrtechnology.entity.nosesmith.fabric;

import com.ovrtechnology.entity.nosesmith.NoseSmithAttributesRegistrar;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;

public final class NoseSmithAttributesRegistrarFabricImpl implements NoseSmithAttributesRegistrar {

    public NoseSmithAttributesRegistrarFabricImpl() {}

    @Override
    public void registerNoseSmithAttributes() {
        EntityType<NoseSmithEntity> type = NoseSmithRegistry.getNoseSmithType();
        if (type == null) {
            return;
        }
        FabricDefaultAttributeRegistry.register(type, NoseSmithEntity.createAttributes());
    }
}
