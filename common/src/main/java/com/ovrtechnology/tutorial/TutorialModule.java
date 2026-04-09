package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialAnimationNetworking;
import com.ovrtechnology.network.TutorialChestNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialIntroNetworking;
import com.ovrtechnology.network.TutorialOliverDialogueNetworking;
import com.ovrtechnology.network.TutorialOliverTradeNetworking;
import com.ovrtechnology.network.TutorialBossCinematicNetworking;
import com.ovrtechnology.network.TutorialDreamOverlayNetworking;
import com.ovrtechnology.network.TutorialPopupNetworking;
import com.ovrtechnology.network.TutorialPortalOverlayNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.network.SearchDiamondNetworking;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.boss.TutorialBossModule;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.chest.TutorialChestHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.command.TutorialCommand;
import com.ovrtechnology.tutorial.command.sub.AnimationSubCommand;
import com.ovrtechnology.tutorial.command.sub.ChestSubCommand;
import com.ovrtechnology.tutorial.command.sub.CinematicSubCommand;
import com.ovrtechnology.tutorial.command.sub.DebugSubCommand;
import com.ovrtechnology.tutorial.command.sub.DialogueSubCommand;
import com.ovrtechnology.tutorial.dream.TutorialDreamEndHandler;
import com.ovrtechnology.tutorial.dream.command.DreamEndSubCommand;
import com.ovrtechnology.tutorial.musiczone.TutorialMusicZoneHandler;
import com.ovrtechnology.tutorial.command.sub.MusicZoneSubCommand;
import com.ovrtechnology.tutorial.command.sub.PopupZoneSubCommand;
import com.ovrtechnology.tutorial.popupzone.TutorialPopupZoneHandler;
import com.ovrtechnology.tutorial.command.sub.IntroSubCommand;
import com.ovrtechnology.tutorial.command.sub.NoseEquipSubCommand;
import com.ovrtechnology.tutorial.command.sub.NoseSmithSubCommand;
import com.ovrtechnology.tutorial.command.sub.OliverControlSubCommand;
import com.ovrtechnology.tutorial.command.sub.OliverKillSubCommand;
import com.ovrtechnology.tutorial.command.sub.OliverSpawnSubCommand;
import com.ovrtechnology.tutorial.command.sub.PortalSubCommand;
import com.ovrtechnology.tutorial.command.sub.RegenAreaSubCommand;
import com.ovrtechnology.tutorial.command.sub.ResetSubCommand;
import com.ovrtechnology.tutorial.command.sub.SearchDiamondSubCommand;
import com.ovrtechnology.tutorial.scentzone.TutorialBlockMineHandler;
import com.ovrtechnology.tutorial.scentzone.TutorialItemPickupHandler;
import com.ovrtechnology.tutorial.scentzone.TutorialItemUseHandler;
import com.ovrtechnology.tutorial.scentzone.TutorialRainHandler;
import com.ovrtechnology.tutorial.scentzone.TutorialWaterHandler;
import com.ovrtechnology.tutorial.scentzone.TutorialScentZoneHandler;
import com.ovrtechnology.tutorial.searchdiamond.SearchDiamondZoneHandler;
import com.ovrtechnology.tutorial.command.sub.SetSpawnSubCommand;
import com.ovrtechnology.tutorial.command.sub.SetWalkaroundSubCommand;
import com.ovrtechnology.tutorial.command.sub.ProtectionSubCommand;
import com.ovrtechnology.tutorial.command.sub.ArrowSubCommand;
import com.ovrtechnology.tutorial.command.sub.FountainSubCommand;
import com.ovrtechnology.tutorial.command.sub.RainButtonSubCommand;
import com.ovrtechnology.tutorial.command.sub.ScentZoneSubCommand;
import com.ovrtechnology.tutorial.command.sub.TradeSubCommand;
import com.ovrtechnology.tutorial.command.sub.WaypointSubCommand;
import com.ovrtechnology.tutorial.oliver.TutorialOliverRegistry;
import com.ovrtechnology.tutorial.noseequip.TutorialNoseEquipHandler;
import com.ovrtechnology.tutorial.portal.TutorialNetherPortalBlocker;
import com.ovrtechnology.tutorial.portal.TutorialPortalHandler;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaManager;
import com.ovrtechnology.tutorial.spawn.TutorialJoinHandler;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import dev.architectury.event.events.common.LifecycleEvent;
import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Core module for the OVR Tutorial system.
 * <p>
 * This module provides a "ghost" tutorial layer that only activates in specially
 * prepared tutorial maps. It does not affect vanilla gameplay in any way.
 * <p>
 * The module is controlled by the {@code isOvrTutorial} GameRule:
 * <ul>
 *   <li>When {@code false} (default): Tutorial features are completely invisible</li>
 *   <li>When {@code true}: Tutorial commands and features become available</li>
 * </ul>
 * <p>
 * This allows map creators to build tutorial experiences that activate automatically
 * when players load the map, without requiring any player configuration.
 */
@UtilityClass
public final class TutorialModule {

    private static boolean initialized = false;

    /**
     * Initializes the tutorial module.
     * <p>
     * Should be called during mod initialization. This method is idempotent
     * and will only perform initialization once.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        AromaAffect.LOGGER.info("Initializing Tutorial Module...");

        // Register custom GameRule
        TutorialGameRules.init();

        // Register Tutorial Oliver entity
        TutorialOliverRegistry.init();

        // Initialize networking
        TutorialOliverDialogueNetworking.init();
        TutorialWaypointNetworking.init();
        TutorialChestNetworking.init();
        TutorialAnimationNetworking.init();
        TutorialDialogueContentNetworking.init();
        TutorialOliverTradeNetworking.init();
        TutorialIntroNetworking.init();
        TutorialPortalOverlayNetworking.init();
        TutorialBossCinematicNetworking.init();
        TutorialDreamOverlayNetworking.init();
        TutorialPopupNetworking.init();
        com.ovrtechnology.network.TutorialScentCounterNetworking.init();
        com.ovrtechnology.network.TutorialFinishNetworking.init();
        SearchDiamondNetworking.init();
        TutorialScentZoneNetworking.init();
        com.ovrtechnology.tutorial.waypoint.client.TutorialStaticArrowManager.initNetworking();

        // Register tutorial subcommands
        TutorialCommand.register(new SetSpawnSubCommand());
        TutorialCommand.register(new SetWalkaroundSubCommand());
        TutorialCommand.register(new ProtectionSubCommand());
        TutorialCommand.register(new WaypointSubCommand());
        TutorialCommand.register(new PortalSubCommand());
        TutorialCommand.register(new OliverSpawnSubCommand());
        TutorialCommand.register(new OliverControlSubCommand());
        TutorialCommand.register(new OliverKillSubCommand());
        TutorialCommand.register(new ResetSubCommand());
        TutorialCommand.register(new CinematicSubCommand());
        TutorialCommand.register(new ChestSubCommand());
        TutorialCommand.register(new AnimationSubCommand());
        TutorialCommand.register(new DialogueSubCommand());
        TutorialCommand.register(new TradeSubCommand());
        TutorialCommand.register(new IntroSubCommand());
        TutorialCommand.register(new NoseEquipSubCommand());
        TutorialCommand.register(new RegenAreaSubCommand());
        TutorialCommand.register(new DebugSubCommand());
        TutorialCommand.register(new DreamEndSubCommand());
        TutorialCommand.register(new NoseSmithSubCommand());
        TutorialCommand.register(new MusicZoneSubCommand());
        TutorialCommand.register(new PopupZoneSubCommand());
        TutorialCommand.register(new com.ovrtechnology.tutorial.command.sub.FinishZoneSubCommand());
        TutorialCommand.register(new SearchDiamondSubCommand());
        TutorialCommand.register(new ScentZoneSubCommand());
        TutorialCommand.register(new RainButtonSubCommand());
        TutorialCommand.register(new FountainSubCommand());
        TutorialCommand.register(new ArrowSubCommand());
        TutorialCommand.register(new com.ovrtechnology.tutorial.command.sub.SaveTemplateSubCommand());
        TutorialCommand.register(new com.ovrtechnology.tutorial.command.sub.SkipTimerSubCommand());

        // Register tutorial commands (only visible when GameRule is active)
        TutorialCommand.init();

        // Initialize join handler for spawn teleportation and intro sequence
        TutorialJoinHandler.init();

        // Initialize waypoint area detection handler
        TutorialWaypointAreaHandler.init();

        // Initialize portal teleportation handler
        TutorialPortalHandler.init();

        // Initialize Nether portal blocker (prevents leaving tutorial via Nether)
        TutorialNetherPortalBlocker.init();

        // Initialize cinematic handler
        TutorialCinematicHandler.init();

        // Initialize chest handler
        TutorialChestHandler.init();

        // Initialize animation handler
        TutorialAnimationHandler.init();

        // Initialize nose equip trigger handler
        TutorialNoseEquipHandler.init();

        // Initialize block regeneration area handler
        TutorialRegenAreaHandler.init();

        // Initialize ore drop handler (ores drop ingots in tutorial)
        TutorialOreDropHandler.init();

        // Initialize boss module (custom blaze/dragon encounters)
        TutorialBossModule.init();

        // Initialize dream ending handler
        TutorialDreamEndHandler.init();

        // Initialize music zone handler
        TutorialMusicZoneHandler.init();

        // Initialize popup zone handler
        TutorialPopupZoneHandler.init();

        // Initialize finish screen zone handler
        com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler.init();

        // Initialize SearchDiamond zone handler
        SearchDiamondZoneHandler.init();

        // Initialize scent zone handler
        TutorialScentZoneHandler.init();

        // Initialize item pickup scent handler (flowers → Floral)
        TutorialItemPickupHandler.init();

        // Initialize rain exposure scent handler (rain → Petrichor)
        TutorialRainHandler.init();

        // Initialize block mine scent handler (any block with scent → trigger on mine)
        TutorialBlockMineHandler.init();

        // Initialize water touch handler (water → Marine)
        TutorialWaterHandler.init();

        // Initialize item use scent handler (honey bottle → Sweet)
        TutorialItemUseHandler.init();

        // Initialize damage handler (visual damage but heals after 0.5s)
        TutorialDamageHandler.init();

        // Initialize horse feed handler (apple + horse → Barnyard scent)
        TutorialHorseFeedHandler.init();

        // Initialize rain button handler
        TutorialRainButtonHandler.init();

        // Initialize fountain handler
        TutorialFountainHandler.init();

        // Initialize demo timer (15 min countdown)
        com.ovrtechnology.tutorial.demo.DemoTimerHandler.init();

        // Initialize daylight handler (keeps tutorial always at daytime)
        TutorialDaylightHandler.init();

        // Register server started event to auto-restore regen areas and force-load chunks
        LifecycleEvent.SERVER_STARTED.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (isActive(level)) {
                    int restored = TutorialRegenAreaManager.restoreAllAreas(level);
                    if (restored > 0) {
                        AromaAffect.LOGGER.info("Tutorial: auto-restored {} blocks on server start", restored);
                    }
                    forceLoadTutorialChunks(level);
                }
            }
        });

        initialized = true;
        AromaAffect.LOGGER.info("Tutorial Module initialized successfully");
    }

    /**
     * Checks if the tutorial mode is active in the given level.
     * <p>
     * This is the primary check that all tutorial features should use
     * to determine whether they should be enabled.
     *
     * @param level the server level to check
     * @return {@code true} if the tutorial is active, {@code false} otherwise
     */
    /**
     * Force-loads chunks around the tutorial spawn point.
     */
    private static void forceLoadTutorialChunks(ServerLevel level) {
        var spawnOpt = TutorialSpawnManager.getSpawn(level);
        if (spawnOpt.isEmpty()) return;
        BlockPos spawn = spawnOpt.get().pos();
        int chunkX = spawn.getX() >> 4;
        int chunkZ = spawn.getZ() >> 4;
        int radius = 8;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setChunkForced(chunkX + dx, chunkZ + dz, true);
                count++;
            }
        }
        AromaAffect.LOGGER.info("Tutorial: force-loaded {} chunks around spawn", count);
    }

    /**
     * Removes all force-loaded chunks. Call before disconnect to prevent save hang.
     */
    public static void unforceAllChunks(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!isActive(level)) continue;
            var spawnOpt = TutorialSpawnManager.getSpawn(level);
            if (spawnOpt.isEmpty()) continue;
            BlockPos spawn = spawnOpt.get().pos();
            int chunkX = spawn.getX() >> 4;
            int chunkZ = spawn.getZ() >> 4;
            int radius = 8;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    level.setChunkForced(chunkX + dx, chunkZ + dz, false);
                }
            }
            level.noSave = true;
        }
        AromaAffect.LOGGER.info("Tutorial: unforced all chunks + noSave");
    }

    public static boolean isActive(ServerLevel level) {
        if (level == null) {
            return false;
        }
        return level.getGameRules().getBoolean(TutorialGameRules.IS_OVR_TUTORIAL);
    }

}
