package com.ovrtechnology;

import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.ability.AbilityHandler;
import com.ovrtechnology.ability.AbilityRegistry;
import com.ovrtechnology.ability.PreciseSnifferAbility;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.category.CategoryDefinitionLoader;
import com.ovrtechnology.command.AromaTestCommand;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.guide.AromaGuideFirstJoinHandler;
import com.ovrtechnology.guide.AromaGuideRegistry;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.network.NoseRenderNetworking;
import com.ovrtechnology.network.NoseSmithDialogueNetworking;
import com.ovrtechnology.network.NoseSmithTradeNetworking;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.network.AromaGuideNetworking;
import com.ovrtechnology.network.IronGolemNoseNetworking;
import com.ovrtechnology.network.OmaraDeviceNetworking;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import com.ovrtechnology.nose.NoseAbilityResolver;
import com.ovrtechnology.nose.NoseDefinitionLoader;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.registry.ModCreativeTab;
import com.ovrtechnology.registry.ModSounds;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.scentitem.ScentItemDefinitionLoader;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferSyncHandler;
import com.ovrtechnology.entity.sniffer.config.SnifferConfigLoader;
import com.ovrtechnology.sniffer.loot.SnifferLootRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseDefinitionLoader;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import com.ovrtechnology.variant.CustomNoseRegistry;
import com.ovrtechnology.variant.ModDataComponents;
import com.ovrtechnology.variant.NoseVariantRegistry;
import com.ovrtechnology.variant.VariantRecipeIndex;
import com.ovrtechnology.data.ResourceManagerDataSource;
import com.ovrtechnology.network.ScentEventNetworking;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.StructureSyncHandler;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.event.EventDefinitionLoader;
import com.ovrtechnology.trigger.event.EventTriggersConfig;
import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import com.ovrtechnology.worldgen.VillagePoolInjector;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Aroma Affect mod.
 * This mod integrates OVR's scent hardware into Minecraft through the "Nose"
 * system.
 *
 * <p>Invoked via {@code Balm.initializeMod(MOD_ID, loadContext, AromaAffect::initialize)}
 * from each platform entry point.</p>
 */
@UtilityClass
public final class AromaAffect {
    public static final String MOD_ID = "aromaaffect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void initialize(BalmRegistrars registrars) {
        LOGGER.info("Initializing Aroma Affect...");

        // === Pure JSON loaders (no registry / no events) =====================
        AbilityDefinitionLoader.loadAllAbilities();
        ScentRegistry.init();
        SnifferConfigLoader.init();
        BiomeDefinitionLoader.loadAllBiomes();
        BlockDefinitionLoader.loadAllBlocks();
        FlowerDefinitionLoader.loadAllFlowers();
        StructureDefinitionLoader.loadAllStructures();
        MobDefinitionLoader.loadAllMobs();
        EventDefinitionLoader.loadAllEvents();
        CategoryDefinitionLoader.loadAllCategories();
        ScentTriggerConfigLoader.init();
        ScentTriggerManager.init();

        // === Balm-driven registries ==========================================
        // Blocks + block entities must register before items: the Omara Device
        // BlockItem (and its BlockEntity) dereference OMARA_DEVICE.value() at
        // registration time, so the block holder must already be populated.
        registrars.registrar(Registries.BLOCK, OmaraDeviceRegistry::registerBlocks);
        registrars.blockEntityTypes(OmaraDeviceRegistry::registerBlockEntities);

        // Data components (nose_variant) must register before the custom_nose item
        // so variant stacks can carry the component at runtime.
        registrars.dataComponentTypes(ModDataComponents::register);

        // Items
        registrars.registrar(Registries.ITEM, NoseRegistry::register);
        registrars.registrar(Registries.ITEM, SnifferNoseRegistry::register);
        registrars.registrar(Registries.ITEM, ScentItemRegistry::register);
        registrars.registrar(Registries.ITEM, AromaGuideRegistry::register);
        registrars.registrar(Registries.ITEM, NoseSmithRegistry::registerItems);
        registrars.registrar(Registries.ITEM, OmaraDeviceRegistry::registerItems);
        registrars.registrar(Registries.ITEM, CustomNoseRegistry::register);

        // Entity types
        registrars.registrar(Registries.ENTITY_TYPE, NoseSmithRegistry::registerEntities);

        // Menus
        registrars.menuTypes(SnifferMenuRegistry::registerMenus);
        registrars.menuTypes(OmaraDeviceRegistry::registerMenus);

        // Sounds
        registrars.registrar(Registries.SOUND_EVENT, ModSounds::register);

        // Creative tab (depends on items being registered above)
        registrars.creativeModeTabs(ModCreativeTab::register);

        // === In-memory registries ===========================================
        // Nose ability resolver runs after the item registry callback has fired,
        // so we schedule it via a server-lifecycle event in phase 5. For now we
        // simply call it directly; if the callback ordering bites us we move it
        // into ServerLifecycleCallback.Starting.EVENT.
        NoseRegistry.initAbilityResolver();
        AbilityRegistry.register(PreciseSnifferAbility.INSTANCE);
        AbilityRegistry.init();

        // === Phase 6 — networking (still on Architectury) ===================
        NoseSmithDialogueNetworking.init();
        NoseSmithTradeNetworking.init();
        PathScentNetworking.init();
        SnifferEquipmentNetworking.init();
        IronGolemNoseNetworking.init();
        NoseRenderNetworking.init();
        AromaGuideNetworking.init();
        OmaraDeviceNetworking.init();
        ScentEventNetworking.init();

        // === Event-trigger scent system =====================================
        EventTriggersConfig.getInstance();
        ServerEventBusHandler.init();
        // Trackable content + event definitions live in per-file directories and
        // are enumerated via the ResourceManager, so they load on server-data reload
        // (world load + /reload). This is what picks up modpack datapack additions.
        registrars.resourceReloadListeners(
                reg ->
                        reg.register(
                                "aromaaffect_data_reload",
                                (net.minecraft.server.packs.resources.ResourceManager rm) -> {
                                    var ds = new ResourceManagerDataSource(rm);
                                    ScentRegistry.reload(ds);
                                    AbilityDefinitionLoader.loadAllAbilities(ds);
                                    BiomeDefinitionLoader.loadAllBiomes(ds);
                                    BlockDefinitionLoader.loadAllBlocks(ds);
                                    FlowerDefinitionLoader.loadAllFlowers(ds);
                                    StructureDefinitionLoader.loadAllStructures(ds);
                                    MobDefinitionLoader.loadAllMobs(ds);
                                    CategoryDefinitionLoader.loadAllCategories(ds);
                                    SnifferLootRegistry.reload(ds);
                                    NoseDefinitionLoader.reloadInPlace(ds);
                                    SnifferNoseDefinitionLoader.reloadInPlace(ds);
                                    ScentItemDefinitionLoader.reloadInPlace(ds);
                                    NoseVariantRegistry.reload(ds);
                                    VariantRecipeIndex.reload(ds);
                                    NoseAbilityResolver.rebuild();
                                    EventDefinitionLoader.loadAllEvents(ds);
                                    ScentTriggerConfigLoader.rebuild();
                                }));

        // === Phase 5 — event listeners (still on Architectury) ===============
        SnifferSyncHandler.init();
        LookupManager.init();
        ActivePathManager.init();
        AbilityHandler.init();
        StructureSyncHandler.init();
        AromaTestCommand.init();
        AromaGuideFirstJoinHandler.init();
        VillagePoolInjector.init();

        // === Phase 7 — platform-specific entity attributes ===================
        NoseSmithRegistry.registerAttributes();

        LOGGER.info("Aroma Affect initialized successfully!");
    }
}
