package com.ovrtechnology.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

// Registry ids must be baked into item/block properties on 26.x; 1.21.1 has no such step
public final class RegistrationCompat {

    private RegistrationCompat() {}

    public static Item.Properties itemProperties(Identifier id) {
        return withId(new Item.Properties(), id);
    }

    public static Item.Properties withId(Item.Properties properties, Identifier id) {
        //? if >=26 {
        return properties.setId(ResourceKey.create(Registries.ITEM, id));
        //?} else {
        /*return properties;
        *///?}
    }

    public static BlockBehaviour.Properties withId(BlockBehaviour.Properties properties, Identifier id) {
        //? if >=26 {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id));
        //?} else {
        /*return properties;
        *///?}
    }

    public static <T extends Entity> EntityType<T> build(EntityType.Builder<T> builder, Identifier id) {
        //? if >=26 {
        return builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        //?} else {
        /*return builder.build(id.toString());
        *///?}
    }
}
