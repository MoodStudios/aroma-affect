package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-side handler that syncs structure presence to clients.
 *
 * <p>On a dedicated server the client cannot access {@link ServerLevel} or
 * the structure manager. This handler runs every second on the server,
 * detects which tracked structure each player is inside, and sends an
 * S2C packet so the client's {@link PassiveModeManager} can fire
 * structure-based passive-mode triggers in multiplayer.</p>
 */
public final class StructureSyncHandler {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static int tickCounter = 0;
    private static final Map<UUID, String> playerStructures = new HashMap<>();
    private static boolean initialized = false;

    private StructureSyncHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            if (ReplayCompat.isReplayServer(server)) return;
            if (++tickCounter < CHECK_INTERVAL_TICKS) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String currentStructure = detectStructure(player);
                String previous = playerStructures.get(player.getUUID());

                if (!Objects.equals(currentStructure, previous)) {
                    playerStructures.put(player.getUUID(), currentStructure);
                    PathScentNetworking.sendStructureSync(player, currentStructure);
                    AromaAffect.LOGGER.debug("[StructureSync] Player {} structure: {} -> {}",
                            player.getName().getString(), previous, currentStructure);
                }
            }
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            playerStructures.remove(player.getUUID());
        });

        AromaAffect.LOGGER.info("StructureSyncHandler initialized");
    }

    /**
     * Checks all tracked structure definitions and returns the first one
     * the player is standing inside, or {@code null} if none.
     */
    private static String detectStructure(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        for (StructureTriggerDefinition trigger : ScentTriggerConfigLoader.getAllStructureTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) continue;

            if (isInsideStructure(level, playerPos, trigger.getStructureId())) {
                return trigger.getStructureId();
            }
        }
        return null;
    }

    private static boolean isInsideStructure(ServerLevel level, BlockPos playerPos, String structureId) {
        try {
            ResourceLocation location = ResourceLocation.parse(structureId);
            var structureOpt = level.registryAccess()
                    .lookupOrThrow(Registries.STRUCTURE)
                    .getOptional(location);

            if (structureOpt.isEmpty()) return false;

            Structure structure = structureOpt.get();
            SectionPos playerSection = SectionPos.of(playerPos);
            int chunkRange = 2;

            for (int cx = -chunkRange; cx <= chunkRange; cx++) {
                for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                    int chunkX = playerSection.x() + cx;
                    int chunkZ = playerSection.z() + cz;

                    StructureStart start = level.structureManager()
                            .getStartForStructure(
                                    SectionPos.of(chunkX, 0, chunkZ),
                                    structure,
                                    level.getChunk(chunkX, chunkZ)
                            );

                    if (start != null && start.isValid()) {
                        var bb = start.getBoundingBox();
                        if (playerPos.getX() >= bb.minX() && playerPos.getX() <= bb.maxX()
                                && playerPos.getY() >= bb.minY() && playerPos.getY() <= bb.maxY()
                                && playerPos.getZ() >= bb.minZ() && playerPos.getZ() <= bb.maxZ()) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.debug("Error checking structure {}: {}", structureId, e.getMessage());
        }
        return false;
    }
}
