package com.ovrtechnology.command.path;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.sub.PathSubCommand;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.util.Texts;
import dev.architectury.event.events.common.TickEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ActivePathManager {

    public enum TargetType {
        BLOCK,
        BIOME,
        STRUCTURE
    }

    private static final ActivePathManager INSTANCE = new ActivePathManager();

    private static final double ARRIVAL_THRESHOLD = 5.0;

    private static final double BIOME_ARRIVAL_THRESHOLD = 100.0;

    private static final int TICK_INTERVAL = 5;

    private final Map<UUID, ActivePath> activePaths = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private boolean initialized = false;

    private ActivePathManager() {}

    public static ActivePathManager getInstance() {
        return INSTANCE;
    }

    public static void init() {
        if (INSTANCE.initialized) {
            return;
        }

        TickEvent.SERVER_POST.register(INSTANCE::onServerTick);
        INSTANCE.initialized = true;
        AromaAffect.LOGGER.info("Active path manager initialized");
    }

    public void createPath(
            ServerPlayer player,
            net.minecraft.server.level.ServerLevel level,
            BlockPos destination) {
        createPath(player, level, destination, null, null);
    }

    public void createPath(
            ServerPlayer player,
            net.minecraft.server.level.ServerLevel level,
            BlockPos destination,
            TargetType targetType,
            String targetId) {
        UUID playerId = player.getUUID();

        activePaths.remove(playerId);

        ActivePath path =
                new ActivePath(
                        playerId, level.dimension().location(), destination, targetType, targetId);
        activePaths.put(playerId, path);

        AromaAffect.LOGGER.debug(
                "Created active path for player {} to {} (target: {} {})",
                player.getName().getString(),
                destination,
                targetType != null ? targetType : "none",
                targetId != null ? targetId : "");
    }

    public void removePath(UUID playerId) {
        activePaths.remove(playerId);
    }

    public boolean hasActivePath(UUID playerId) {
        return activePaths.containsKey(playerId);
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;

        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        Iterator<Map.Entry<UUID, ActivePath>> iterator = activePaths.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ActivePath> entry = iterator.next();
            UUID playerId = entry.getKey();
            ActivePath path = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {

                iterator.remove();
                continue;
            }

            if (!EquippedNoseHelper.hasNoseEquipped(player)) {
                PathScentNetworking.sendPathNotFound(player, "Nose unequipped");
                iterator.remove();
                continue;
            }

            if (!player.level().dimension().location().equals(path.dimension())) {

                iterator.remove();
                continue;
            }

            BlockPos playerPos = player.blockPosition();
            BlockPos destination = path.destination();

            double distanceToDestination =
                    Math.sqrt(
                            Math.pow(playerPos.getX() - destination.getX(), 2)
                                    + Math.pow(playerPos.getY() - destination.getY(), 2)
                                    + Math.pow(playerPos.getZ() - destination.getZ(), 2));

            PathScentNetworking.sendDistanceUpdate(player, (int) distanceToDestination);

            boolean arrived;
            if (path.targetType() == TargetType.BIOME && path.targetId() != null) {
                if (distanceToDestination <= BIOME_ARRIVAL_THRESHOLD) {
                    var currentBiome = player.level().getBiome(playerPos);
                    var biomeKey = currentBiome.unwrapKey().orElse(null);
                    arrived =
                            biomeKey != null
                                    && biomeKey.location().toString().equals(path.targetId());
                } else {
                    arrived = false;
                }
            } else {
                arrived = distanceToDestination <= ARRIVAL_THRESHOLD;
            }

            if (arrived) {

                if (PathSubCommand.isVerbose()) {
                    player.sendSystemMessage(
                            Texts.lit("§6[Aroma Affect] §aYou have arrived at your destination!"));
                }
                PathScentNetworking.sendPathArrived(player);
                iterator.remove();
                continue;
            }
        }
    }

    public int getActivePathCount() {
        return activePaths.size();
    }

    public void clearAll() {
        activePaths.clear();
    }

    public record ActivePath(
            UUID playerId,
            net.minecraft.resources.ResourceLocation dimension,
            BlockPos destination,
            TargetType targetType,
            String targetId) {}
}
