package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles server-to-client networking for path tracking scent triggers.
 *
 * <p>Since the path tracking runs on the server but the OVR WebSocket client
 * runs on the client, we need to send scent trigger commands from server to client.</p>
 */
public final class PathScentNetworking {

    private static final ResourceLocation PATH_SCENT_TRIGGER_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "path_scent_trigger");

    /**
     * Default duration for path tracking scent triggers (in ticks).
     * 5 seconds = 100 ticks.
     */
    private static final int PATH_SCENT_DURATION_TICKS = 100;

    private static boolean initialized = false;

    private PathScentNetworking() {
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

        // Register client-side receiver for scent trigger packets (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PATH_SCENT_TRIGGER_PACKET_ID, (buf, context) -> {
            String scentName = buf.readUtf();
            double intensity = buf.readDouble();
            int priorityOrdinal = buf.readVarInt();
            int durationTicks = buf.readVarInt();

            context.queue(() -> {
                ScentPriority priority = ScentPriority.values()[priorityOrdinal];

                ScentTrigger trigger = ScentTrigger.create(
                        scentName,
                        ScentTriggerSource.PATH_TRACKING,
                        priority,
                        durationTicks,
                        intensity
                );

                boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);
                AromaAffect.LOGGER.debug("Received path scent trigger from server: {} (triggered: {})",
                        scentName, triggered);
            });
        });

        AromaAffect.LOGGER.info("PathScentNetworking initialized");
    }

    /**
     * Sends a scent trigger from the server to a specific client.
     *
     * @param player    the player to send the trigger to
     * @param scentName the scent name to trigger
     * @param intensity the scent intensity (0.0 to 1.0)
     * @param priority  the scent priority
     */
    public static void sendScentTrigger(ServerPlayer player, String scentName, double intensity, ScentPriority priority) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeUtf(scentName);
        buf.writeDouble(intensity);
        buf.writeVarInt(priority.ordinal());
        buf.writeVarInt(PATH_SCENT_DURATION_TICKS);

        NetworkManager.sendToPlayer(player, PATH_SCENT_TRIGGER_PACKET_ID, buf);

        AromaAffect.LOGGER.debug("Sent path scent trigger to {}: {} (intensity: {}, priority: {})",
                player.getName().getString(), scentName, intensity, priority);
    }
}
