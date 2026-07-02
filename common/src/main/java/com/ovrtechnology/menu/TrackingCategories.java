package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.lookup.LookupType;
import com.ovrtechnology.nose.NoseAbilityResolver.ResolvedAbilities;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public final class TrackingCategories {

    public static final TrackingCategory STRUCTURES = TrackingCategory.builder("structures")
            .lookupType(LookupType.STRUCTURE)
            .pathCommandType("structure")
            .iconItem(Items.BELL::getDefaultInstance)
            .headerIcon(radialTexture("icon_structures"))
            .trailDomain(TrailDomain.STRUCTURE)
            .worldOutline(true)
            .screenFactory(StructuresMenuScreen::new)
            .abilityAccessor(ResolvedAbilities::getStructures)
            .build();

    public static final TrackingCategory BIOMES = TrackingCategory.builder("biomes")
            .lookupType(LookupType.BIOME)
            .pathCommandType("biome")
            .iconItem(Items.OAK_SAPLING::getDefaultInstance)
            .headerIcon(radialTexture("icon_biomes"))
            .trailDomain(TrailDomain.BIOME)
            .worldOutline(false)
            .screenFactory(BiomesMenuScreen::new)
            .abilityAccessor(ResolvedAbilities::getBiomes)
            .build();

    public static final TrackingCategory BLOCKS = TrackingCategory.builder("blocks")
            .lookupType(LookupType.BLOCK)
            .pathCommandType("block")
            .iconItem(Items.DIAMOND_ORE::getDefaultInstance)
            .headerIcon(radialTexture("icon_blocks"))
            .trailDomain(TrailDomain.BLOCK)
            .worldOutline(true)
            .screenFactory(BlocksMenuScreen::new)
            .abilityAccessor(ResolvedAbilities::getBlocks)
            .build();

    public static final TrackingCategory FLOWERS = TrackingCategory.builder("flowers")
            .lookupType(LookupType.FLOWER)
            .pathCommandType("block")
            .iconItem(Items.POPPY::getDefaultInstance)
            .headerIcon(radialTexture("icon_flowers"))
            .trailDomain(TrailDomain.BLOCK)
            .worldOutline(true)
            .screenFactory(FlowersMenuScreen::new)
            .abilityAccessor(ResolvedAbilities::getFlowers)
            .build();

    private static boolean bootstrapped = false;

    private TrackingCategories() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        TrackingCategoryRegistry.register(STRUCTURES);
        TrackingCategoryRegistry.register(BIOMES);
        TrackingCategoryRegistry.register(BLOCKS);
        TrackingCategoryRegistry.register(FLOWERS);
        AromaAffect.LOGGER.info("Registered {} tracking categories", TrackingCategoryRegistry.size());
    }

    private static Identifier radialTexture(String name) {
        return Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/radial/" + name + ".png");
    }
}
