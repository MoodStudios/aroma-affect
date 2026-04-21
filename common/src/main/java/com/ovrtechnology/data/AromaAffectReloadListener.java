package com.ovrtechnology.data;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.entity.sniffer.config.SnifferConfigLoader;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.nose.NoseAbilityResolver;
import com.ovrtechnology.nose.NoseDefinitionLoader;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentDefinitionLoader;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.scentitem.ScentItemDefinitionLoader;
import com.ovrtechnology.sniffer.loot.SnifferLootRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseDefinitionLoader;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.variant.NoseVariantRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;

public final class AromaAffectReloadListener extends SimplePreparableReloadListener<Void> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "data_reload");

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        AromaAffect.LOGGER.info("AromaAffect reload listener firing...");
        DataSource ds = new ResourceManagerDataSource(resourceManager);

        try {
            SnifferConfigLoader.loadConfig(ds);

            AbilityDefinitionLoader.loadAllAbilities(ds);

            reloadScents(ds);

            NoseDefinitionLoader.reloadInPlace(ds);
            SnifferNoseDefinitionLoader.reloadInPlace(ds);
            ScentItemDefinitionLoader.reloadInPlace(ds);

            NoseVariantRegistry.reload(ds);
            SnifferLootRegistry.reload(ds);

            BiomeDefinitionLoader.loadAllBiomes(ds);
            BlockDefinitionLoader.loadAllBlocks(ds);
            FlowerDefinitionLoader.loadAllFlowers(ds);
            StructureDefinitionLoader.loadAllStructures(ds);
            MobDefinitionLoader.loadAllMobs(ds);

            ScentTriggerConfigLoader.reload(ds);

            NoseAbilityResolver.clearCache();
            NoseAbilityResolver.init();

            AromaAffect.LOGGER.info("AromaAffect reload complete");
        } catch (Exception e) {
            AromaAffect.LOGGER.error("AromaAffect reload failed", e);
        }
    }

    private static void reloadScents(DataSource ds) {
        List<ScentDefinition> defs = ScentDefinitionLoader.loadAllScents(ds);
        ScentRegistry.getScentDefinitions().clear();
        for (ScentDefinition def : defs) {
            ScentRegistry.getScentDefinitions().put(def.getId(), def);
        }
        AromaAffect.LOGGER.info("ScentRegistry refreshed with {} scents", defs.size());
    }
}
