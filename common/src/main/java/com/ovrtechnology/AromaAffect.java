package com.ovrtechnology;

import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.ability.AbilityHandler;
import com.ovrtechnology.ability.AbilityRegistry;
import com.ovrtechnology.ability.PreciseSnifferAbility;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.command.AromaTestCommand;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.data.AromaAffectReloadListener;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferSyncHandler;
import com.ovrtechnology.entity.sniffer.config.SnifferConfigLoader;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.guide.AromaGuideFirstJoinHandler;
import com.ovrtechnology.guide.AromaGuideRegistry;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.network.AromaGuideNetworking;
import com.ovrtechnology.network.IronGolemNoseNetworking;
import com.ovrtechnology.network.NoseRenderNetworking;
import com.ovrtechnology.network.NoseSmithDialogueNetworking;
import com.ovrtechnology.network.NoseSmithTradeNetworking;
import com.ovrtechnology.network.OmaraDeviceNetworking;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.registry.ModCreativeTab;
import com.ovrtechnology.registry.ModSounds;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.StructureSyncHandler;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.variant.CustomNoseRegistry;
import com.ovrtechnology.variant.ModDataComponents;
import com.ovrtechnology.worldgen.VillagePoolInjector;
import dev.architectury.registry.ReloadListenerRegistry;
import lombok.experimental.UtilityClass;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public final class AromaAffect {
    public static final String MOD_ID = "aromaaffect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Initializing Aroma Affect...");

        NoseSmithDialogueNetworking.init();
        NoseSmithTradeNetworking.init();
        PathScentNetworking.init();
        SnifferEquipmentNetworking.init();
        IronGolemNoseNetworking.init();
        NoseRenderNetworking.init();
        AromaGuideNetworking.init();
        OmaraDeviceNetworking.init();

        AbilityDefinitionLoader.loadAllAbilities();

        ModDataComponents.init();

        NoseRegistry.init();

        CustomNoseRegistry.init();

        SnifferNoseRegistry.init();

        ScentRegistry.init();

        ScentItemRegistry.init();

        AromaGuideRegistry.init();

        NoseSmithRegistry.init();

        SnifferConfigLoader.init();

        SnifferMenuRegistry.init();

        SnifferSyncHandler.init();

        OmaraDeviceRegistry.init();

        ModSounds.init();

        ModCreativeTab.init();

        LookupManager.init();

        ActivePathManager.init();

        AbilityRegistry.register(PreciseSnifferAbility.INSTANCE);
        AbilityRegistry.init();
        AbilityHandler.init();

        BiomeDefinitionLoader.loadAllBiomes();
        BlockDefinitionLoader.loadAllBlocks();
        FlowerDefinitionLoader.loadAllFlowers();
        StructureDefinitionLoader.loadAllStructures();
        MobDefinitionLoader.loadAllMobs();

        ScentTriggerConfigLoader.init();
        ScentTriggerManager.init();

        StructureSyncHandler.init();

        AromaTestCommand.init();

        AromaGuideFirstJoinHandler.init();

        VillagePoolInjector.init();

        ReloadListenerRegistry.register(
                PackType.SERVER_DATA,
                new AromaAffectReloadListener(),
                AromaAffectReloadListener.ID);

        LOGGER.info("Aroma Affect initialized successfully!");
    }
}
