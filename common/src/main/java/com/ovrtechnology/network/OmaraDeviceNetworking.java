package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import net.blay09.mods.balm.Balm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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

    public record OmaraPuffS2C(String scentName, double intensity, int durationTicks) implements CustomPacketPayload {
        public static final Type<OmaraPuffS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "omara_puff_s2c"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OmaraPuffS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.scentName);
                    buf.writeDouble(payload.intensity);
                    buf.writeVarInt(payload.durationTicks);
                },
                buf -> new OmaraPuffS2C(buf.readUtf(), buf.readDouble(), buf.readVarInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Duration for Omara Device scent triggers (in ticks). 5 seconds = 100 ticks. */
    private static final int OMARA_SCENT_DURATION_TICKS = 100;

    /** Intensity for Omara Device scent triggers. */
    private static final double OMARA_SCENT_INTENSITY = 1.0;

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

        Balm.networking().registerClientboundPacket(
                OmaraPuffS2C.TYPE,
                OmaraPuffS2C.class,
                OmaraPuffS2C.STREAM_CODEC,
                (player, payload) -> {
                    String scentName = payload.scentName();
                    double intensity = payload.intensity();
                    int durationTicks = payload.durationTicks();

                    ScentTrigger trigger = ScentTrigger.fromOmaraDevice(scentName, durationTicks, intensity);
                    boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);

                    if (triggered) {
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

        AromaAffect.LOGGER.info("OmaraDeviceNetworking initialized");
    }

    /**
     * Broadcasts a scent puff to all players within a 3x3 block area of the device.
     */
    public static void broadcastPuff(ServerLevel level, BlockPos devicePos, String scentName) {
        AABB area = new AABB(devicePos).inflate(2.0);
        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, area);

        for (ServerPlayer player : nearbyPlayers) {
            Balm.networking().sendTo(player, new OmaraPuffS2C(scentName, OMARA_SCENT_INTENSITY, OMARA_SCENT_DURATION_TICKS));
        }

        AromaAffect.LOGGER.debug("Omara Device broadcast scent '{}' to {} players at {}",
                scentName, nearbyPlayers.size(), devicePos);
    }
}
