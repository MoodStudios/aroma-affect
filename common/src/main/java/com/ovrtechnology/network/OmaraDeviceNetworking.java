package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Handles server-to-client networking for the Omara Device scent triggers.
 *
 * <p>When the Omara Device puffs, the server finds all players within a 3x3 block
 * area and sends each of them a scent trigger packet. The client-side receiver
 * triggers the OVR hardware, shows the scent overlay, and displays a debug message.</p>
 */
public final class OmaraDeviceNetworking {


    /** Duration for Omara Device scent triggers (in ticks). 5 seconds = 100 ticks. */
    private static final int OMARA_SCENT_DURATION_TICKS = 100;

    /** Intensity for Omara Device scent triggers. */
    private static final double OMARA_SCENT_INTENSITY = 1.0;

    private static final ResourceLocation OMARA_PUFF_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "omara_puff_s2c");

    private static boolean initialized = false;

    private OmaraDeviceNetworking() {
    }

    /**
     * Initializes the networking handler.
     * Must be called on both client and server during mod initialization.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register client-side receiver for Omara puff packets (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OMARA_PUFF_PACKET_ID, (buf, context) -> {
            String scentName = buf.readUtf();
            double intensity = buf.readDouble();
            int durationTicks = buf.readVarInt();

            context.queue(() -> {
                ScentTrigger trigger = ScentTrigger.fromOmaraDevice(scentName, durationTicks, intensity);
                boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);

                if (triggered) {
                    // Show scent overlay (border image) for ~2 seconds
                    ScentPuffOverlay.onScentPuff(scentName, intensity);

                    if (ClientConfig.getInstance().isDebugScentMessages()) {
                        int intensityPercent = (int) Math.round(intensity * 100);
                        String message = String.format("§d[Aroma Affect] §7Scent: §e%s §7(§domara device§7) §8[%d%%]",
                            scentName, intensityPercent);
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.literal(message), false);
                        }
                    }
                }

                AromaAffect.LOGGER.debug("Received Omara puff from server: {} (triggered: {})",
                        scentName, triggered);
            });
        });

        AromaAffect.LOGGER.info("OmaraDeviceNetworking initialized");
    }

    /**
     * Broadcasts a scent puff to all players within a 3x3 block area of the device.
     * Called from the server when the Omara Device puffs.
     *
     * @param level     the server level
     * @param devicePos the block position of the Omara Device
     * @param scentName the scent name to trigger
     */
    public static void broadcastPuff(ServerLevel level, BlockPos devicePos, String scentName) {
        // 5x5 block area centered on the device (2 blocks in each direction)
        AABB area = new AABB(devicePos).inflate(2.0);
        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, area);

        for (ServerPlayer player : nearbyPlayers) {
            sendPuffToPlayer(player, scentName);
        }

        AromaAffect.LOGGER.debug("Omara Device broadcast scent '{}' to {} players at {}",
                scentName, nearbyPlayers.size(), devicePos);
    }

    private static void sendPuffToPlayer(ServerPlayer player, String scentName) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeUtf(scentName);
        buf.writeDouble(OMARA_SCENT_INTENSITY);
        buf.writeVarInt(OMARA_SCENT_DURATION_TICKS);

        if (!NetworkManager.canPlayerReceive(player, OMARA_PUFF_PACKET_ID)) {
            buf.release();
            return;
        }

        NetworkManager.sendToPlayer(player, OMARA_PUFF_PACKET_ID, buf);
    }
}
